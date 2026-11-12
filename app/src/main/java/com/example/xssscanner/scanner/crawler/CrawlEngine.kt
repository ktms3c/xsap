package com.example.xssscanner.scanner.crawler

import com.example.xssscanner.domain.model.CrawlResult
import com.example.xssscanner.domain.model.DiscoveredParameter
import com.example.xssscanner.domain.model.LogLevel
import com.example.xssscanner.domain.model.ScanConfig
import com.example.xssscanner.domain.model.ScanLog
import com.example.xssscanner.domain.repository.HttpClientPort
import com.example.xssscanner.util.UserAgentProvider
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.mapNotNull
import java.util.ArrayDeque
import javax.inject.Inject

/**
 * Implements the BFS crawl loop in §4.2. Emits one CrawlResult per visited
 * URL. ScanLog emission for BLOCKED/robots-Disallow-info entries is left to
 * ScanOrchestrator, which observes this flow alongside UrlFilter decisions
 * it makes on newly discovered URLs (CrawlEngine calls UrlFilter itself for
 * the enqueue decision, per §1's "every new URL source must pass through
 * UrlFilter" rule, but does not own the ScanLog/ScanResultRepository
 * writes — that's the orchestrator's job per §6).
 *
 * TODO: needs confirmation — §4.3 says form inputs' name/type/default value
 * must be discovered, and §4.2's BFS pseudocode only mentions "extract"
 * generically. Since the domain CrawlResult model (§8) has no raw-body
 * field, this engine populates CrawlResult.parameters with the union of
 * query-string parameter names and same-page form input names at crawl
 * time (see LinkExtractor), which is what DiscoverParametersUseCase then
 * classifies. Per-parameter form metadata (method/action/type/default) is
 * attached to those DiscoveredParameter objects when LinkExtractor's own
 * form results are consumed directly by the orchestrator, not reconstructed
 * from CrawlResult alone.
 */
class CrawlEngine @Inject constructor(
    private val httpClient: HttpClientPort,
    private val urlFilter: UrlFilter,
    private val linkExtractor: LinkExtractor,
    private val javaScriptLinkFinder: JavaScriptLinkFinder,
    private val sitemapCrawler: SitemapCrawler,
    private val robotsCrawler: RobotsCrawler
) {
    data class CrawlEvent(
        val result: CrawlResult?,
        val formParameters: List<DiscoveredParameter> = emptyList(),
        val log: ScanLog? = null
    )

    /** Primary entry point matching §8.3's CrawlTargetUseCase contract:
     *  Flow<CrawlResult>. The richer CrawlEvent stream (with blocked-URL
     *  logs and form parameters) is exposed via [crawlWithEvents] for
     *  ScanOrchestrator, which needs those extra signals; this simpler
     *  flow just maps down to CrawlResult for strict spec conformance. */
    fun crawl(config: ScanConfig): Flow<CrawlResult> =
        crawlWithEvents(config).mapNotNull { it.result }

    fun crawlWithEvents(config: ScanConfig): Flow<CrawlEvent> = callbackFlow {
        val visited = mutableSetOf<String>()
        val queue = ArrayDeque<Pair<String, Int>>() // url to depth
        val startUrl = config.targetUrl
        queue.add(startUrl to 0)

        val startedAt = System.currentTimeMillis()
        val userAgent = UserAgentProvider.resolve(config.userAgent, config.targetUrl, startedAt)

        if (config.crawlSitemap) {
            sitemapCrawler.fetchLocations(startUrl).forEach { loc ->
                if (urlFilter.isAllowed(loc, config)) queue.add(loc to 1)
                else send(CrawlEvent(null, log = blockedLog(loc)))
            }
        }
        if (config.crawlRobots) {
            val robots = robotsCrawler.fetch(startUrl)
            robots.sitemapUrls.forEach { loc ->
                if (urlFilter.isAllowed(loc, config)) queue.add(loc to 1)
                else send(CrawlEvent(null, log = blockedLog(loc)))
            }
            robots.disallowPaths.forEach { path ->
                send(
                    CrawlEvent(
                        null,
                        log = ScanLog(
                            timestamp = System.currentTimeMillis(),
                            level = LogLevel.INFO,
                            category = "robots",
                            message = "Disallow: $path (not auto-enqueued)",
                            url = path,
                            statusCode = null
                        )
                    )
                )
            }
        }

        while (queue.isNotEmpty()) {
            val (url, depth) = queue.poll() ?: break
            if (url in visited) continue
            if (!urlFilter.isAllowed(url, config)) {
                send(CrawlEvent(null, log = blockedLog(url)))
                continue
            }
            visited.add(url)

            val headers = mapOf("User-Agent" to userAgent)
            val response = httpClient.execute(
                HttpClientPort.HttpRequest(
                    url = url,
                    headers = headers,
                    cookie = config.cookie.ifBlank { null }
                )
            )

            when (response) {
                is HttpClientPort.HttpResult.Success -> {
                    val contentType = response.contentType.orEmpty()
                    val formParams = mutableListOf<DiscoveredParameter>()
                    val allParamNames = mutableSetOf<String>()

                    if (contentType.contains("html", ignoreCase = true)) {
                        val extracted = linkExtractor.extract(url, response.body)
                        formParams.addAll(extracted.formParameters)
                        allParamNames.addAll(extracted.formParameters.map { it.name })

                        if (depth < config.crawlDepth) {
                            extracted.urls.forEach { next ->
                                if (next !in visited) {
                                    if (urlFilter.isAllowed(next, config)) queue.add(next to depth + 1)
                                    else send(CrawlEvent(null, log = blockedLog(next)))
                                }
                            }
                            if (config.enableJsLinkFinder) {
                                extracted.jsFileUrls.forEach { jsUrl ->
                                    if (urlFilter.isAllowed(jsUrl, config)) queue.add(jsUrl to depth + 1)
                                    else send(CrawlEvent(null, log = blockedLog(jsUrl)))
                                }
                            }
                        }
                    } else if (config.enableJsLinkFinder && contentType.contains("javascript", ignoreCase = true)) {
                        javaScriptLinkFinder.find(url, response.body).forEach { endpoint ->
                            if (urlFilter.isAllowed(endpoint.rawPath, config) && depth < config.crawlDepth) {
                                queue.add(endpoint.rawPath to depth + 1)
                            }
                        }
                    }

                    send(
                        CrawlEvent(
                            result = CrawlResult(
                                url = url,
                                statusCode = response.statusCode,
                                contentType = response.contentType,
                                depth = depth,
                                discoveredAt = System.currentTimeMillis(),
                                source = if (depth == 0) "seed" else "link",
                                parameters = allParamNames.toList()
                            ),
                            formParameters = formParams
                        )
                    )
                }

                is HttpClientPort.HttpResult.Failure -> {
                    send(
                        CrawlEvent(
                            result = CrawlResult(
                                url = url,
                                statusCode = null,
                                contentType = null,
                                depth = depth,
                                discoveredAt = System.currentTimeMillis(),
                                source = if (depth == 0) "seed" else "link",
                                parameters = emptyList()
                            ),
                            log = ScanLog(
                                timestamp = System.currentTimeMillis(),
                                level = LogLevel.ERROR,
                                category = "http",
                                message = "Request failed: ${response.error}",
                                url = url,
                                statusCode = null
                            )
                        )
                    )
                }
            }
        }
        awaitClose { }
    }

    private fun blockedLog(url: String) = ScanLog(
        timestamp = System.currentTimeMillis(),
        level = LogLevel.INFO,
        category = "scope",
        message = "Blocked by scope/regex",
        url = url,
        statusCode = null
    )
}
