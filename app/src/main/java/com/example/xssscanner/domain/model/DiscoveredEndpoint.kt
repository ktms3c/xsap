package com.example.xssscanner.domain.model

/**
 * An endpoint statically extracted from JS source (fetch/XHR/axios calls or
 * path-like string literals found by JavaScriptLinkFinder), before it has
 * been resolved against the crawl's base URL or passed through UrlFilter.
 */
data class DiscoveredEndpoint(
    val rawPath: String,
    val sourceJsUrl: String,
    val method: String? = null
)
