package com.example.xssscanner.domain.usecase

import com.example.xssscanner.domain.model.CrawlResult
import com.example.xssscanner.domain.model.DiscoveredParameter
import org.jsoup.Jsoup
import java.net.URI
import javax.inject.Inject

/**
 * Extracts candidate parameters from a single CrawlResult per §4.3:
 * - query parameters already present on the crawled URL
 * - every <form>'s action/method/each input's name/type/default value
 *   (requires the response body; CrawlResult itself does not carry the raw
 *   body — see TODO below)
 *
 * TODO: needs confirmation — §4.3 describes extracting form inputs and JS
 * fetch/XHR endpoints, but the domain CrawlResult model (§8) only carries
 * url/statusCode/contentType/depth/discoveredAt/source/parameters — no raw
 * HTML/JS body. CrawlEngine is expected to have already populated
 * CrawlResult.parameters with query-string + form-derived parameter names
 * while it had the body in hand (see scanner/crawler/CrawlEngine.kt). This
 * use case therefore treats CrawlResult.parameters as the authoritative list
 * and classifies each by source/interest, rather than re-parsing HTML here
 * without a body to parse. The Jsoup import above is unused.
 */
class DiscoverParametersUseCase @Inject constructor() {

    private val interestingNames = setOf(
        "q", "search", "keyword", "name", "id", "redirect",
        "url", "callback", "next", "return"
    )

    operator fun invoke(crawlResult: CrawlResult): List<DiscoveredParameter> {
        val queryParams = runCatching {
            URI(crawlResult.url).query
                ?.split("&")
                ?.mapNotNull { it.substringBefore("=", missingDelimiterValue = "").ifBlank { null } }
                ?: emptyList()
        }.getOrDefault(emptyList())

        val names = (queryParams + crawlResult.parameters).distinct()

        return names.map { paramName ->
            DiscoveredParameter(
                url = crawlResult.url,
                name = paramName,
                source = if (paramName in queryParams) "query" else crawlResult.source,
                looksInteresting = paramName.lowercase() in interestingNames
            )
        }
    }
}
