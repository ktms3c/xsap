package com.example.xssscanner.domain.model

data class DiscoveredParameter(
    val url: String,
    val name: String,
    val source: String,                 // e.g. "query", "form", "js"
    val formMethod: String? = null,     // non-null when source == "form"
    val formAction: String? = null,
    val inputType: String? = null,
    val defaultValue: String? = null,
    val looksInteresting: Boolean = false
)
