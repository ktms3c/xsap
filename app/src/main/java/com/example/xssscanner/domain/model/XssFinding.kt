package com.example.xssscanner.domain.model

data class XssFinding(
    val id: String,
    val url: String,
    val parameter: String,
    val payload: String,
    val xssType: String,                // e.g. "reflected", "dom"
    val context: String,                // e.g. "html_body", "attribute", "js_string", "url", "css"
    val status: FindingStatus,
    val riskScore: Int,
    val evidence: String,
    val requestSummary: String,
    val responseSummary: String,
    val detectedAt: Long
)
