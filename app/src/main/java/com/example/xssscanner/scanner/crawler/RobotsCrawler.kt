package com.example.xssscanner.scanner.crawler

import com.example.xssscanner.domain.repository.HttpClientPort
import javax.inject.Inject

data class RobotsResult(
    val sitemapUrls: List<String>,
    val disallowPaths: List<String>
)

/**
 * Implements §4.2's "Try to crawl robots.txt": extracts Sitemap: directives
 * (followed the same way as sitemap entries) and Disallow: entries, which
 * the caller must only log as LogLevel.INFO and NEVER auto-enqueue.
 */
class RobotsCrawler @Inject constructor(
    private val httpClient: HttpClientPort
) {
    suspend fun fetch(baseUrl: String): RobotsResult {
        val robotsUrl = baseUrl.trimEnd('/') + "/robots.txt"
        val result = httpClient.execute(HttpClientPort.HttpRequest(url = robotsUrl))
        val body = (result as? HttpClientPort.HttpResult.Success)?.body
            ?: return RobotsResult(emptyList(), emptyList())

        val sitemapUrls = mutableListOf<String>()
        val disallow = mutableListOf<String>()
        body.lineSequence().forEach { rawLine ->
            val line = rawLine.trim()
            when {
                line.startsWith("Sitemap:", ignoreCase = true) ->
                    sitemapUrls.add(line.substringAfter(":").trim())
                line.startsWith("Disallow:", ignoreCase = true) ->
                    disallow.add(line.substringAfter(":").trim())
            }
        }
        return RobotsResult(sitemapUrls, disallow)
    }
}
