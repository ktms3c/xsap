package com.example.xssscanner.scanner.crawler

import com.example.xssscanner.domain.repository.HttpClientPort
import org.jsoup.Jsoup
import org.jsoup.parser.Parser
import javax.inject.Inject

/**
 * Implements §4.2's "Try to crawl sitemap.xml": GET /sitemap.xml, parse
 * <loc> entries. The caller is responsible for passing every returned URL
 * through UrlFilter + depth check before enqueueing (this class does not
 * filter).
 */
class SitemapCrawler @Inject constructor(
    private val httpClient: HttpClientPort
) {
    suspend fun fetchLocations(baseUrl: String): List<String> {
        val sitemapUrl = baseUrl.trimEnd('/') + "/sitemap.xml"
        val result = httpClient.execute(HttpClientPort.HttpRequest(url = sitemapUrl))
        val body = (result as? HttpClientPort.HttpResult.Success)?.body ?: return emptyList()

        return runCatching {
            // TODO(verify-api): parsing XML via Jsoup's XML parser; Jsoup
            // supports this via Parser.xmlParser() (present since early
            // Jsoup 1.x releases).
            val doc = Jsoup.parse(body, sitemapUrl, Parser.xmlParser())
            doc.select("loc").map { it.text().trim() }.filter { it.isNotBlank() }
        }.getOrDefault(emptyList())
    }
}
