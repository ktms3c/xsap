package com.example.xssscanner.scanner.crawler

import com.example.xssscanner.domain.model.DiscoveredParameter
import org.jsoup.Jsoup
import java.net.URI
import javax.inject.Inject

data class ExtractedLinks(
    val urls: List<String>,
    val formParameters: List<DiscoveredParameter>,
    val jsFileUrls: List<String>
)

/**
 * Implements the HTML-extraction part of §4.2: <a href>, <form action>,
 * <iframe src>, <script src>, meta refresh.
 */
class LinkExtractor @Inject constructor() {

    fun extract(baseUrl: String, html: String): ExtractedLinks {
        val doc = runCatching { Jsoup.parse(html, baseUrl) }.getOrNull()
            ?: return ExtractedLinks(emptyList(), emptyList(), emptyList())

        val urls = mutableListOf<String>()
        val jsUrls = mutableListOf<String>()
        val formParams = mutableListOf<DiscoveredParameter>()

        doc.select("a[href]").forEach { el ->
            resolve(baseUrl, el.attr("href"))?.let { urls.add(it) }
        }
        doc.select("iframe[src]").forEach { el ->
            resolve(baseUrl, el.attr("src"))?.let { urls.add(it) }
        }
        doc.select("script[src]").forEach { el ->
            resolve(baseUrl, el.attr("src"))?.let { jsUrls.add(it) }
        }
        doc.select("meta[http-equiv=refresh]").forEach { el ->
            val content = el.attr("content")
            val urlPart = content.substringAfter("url=", missingDelimiterValue = "").trim()
            if (urlPart.isNotBlank()) resolve(baseUrl, urlPart)?.let { urls.add(it) }
        }

        doc.select("form").forEach { form ->
            val action = resolve(baseUrl, form.attr("action").ifBlank { baseUrl }) ?: baseUrl
            val method = form.attr("method").ifBlank { "GET" }.uppercase()
            urls.add(action)
            form.select("input[name], select[name], textarea[name]").forEach { input ->
                formParams.add(
                    DiscoveredParameter(
                        url = action,
                        name = input.attr("name"),
                        source = "form",
                        formMethod = method,
                        formAction = action,
                        inputType = input.attr("type").ifBlank { "text" },
                        defaultValue = input.attr("value").ifBlank { null }
                    )
                )
            }
        }

        return ExtractedLinks(urls.distinct(), formParams, jsUrls.distinct())
    }

    private fun resolve(baseUrl: String, href: String): String? {
        if (href.isBlank() || href.startsWith("javascript:") || href.startsWith("mailto:")) return null
        return runCatching { URI(baseUrl).resolve(href).toString() }.getOrNull()
    }
}
