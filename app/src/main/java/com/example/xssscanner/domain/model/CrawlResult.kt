package com.example.xssscanner.domain.model

data class CrawlResult(
    val url: String,
    val statusCode: Int?,
    val contentType: String?,
    val depth: Int,
    val discoveredAt: Long,
    val source: String,                 // e.g. "link", "form", "sitemap", "robots", "js"
    val parameters: List<String>
)
