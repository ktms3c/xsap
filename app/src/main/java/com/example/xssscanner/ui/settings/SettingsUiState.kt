package com.example.xssscanner.ui.settings

data class SettingsUiState(
    val enableJsLinkFinder: Boolean = true,
    val crawlSitemap: Boolean = true,
    val crawlRobots: Boolean = true,
    val disableRedirect: Boolean = false,
    val localLabMode: Boolean = false,
    val threads: Int = 4,
    val timeoutSeconds: Int = 15,
    val crawlDepth: Int = 2,
    val userAgent: String = "random",
    val blacklistUrlRegex: String = "",
    val whitelistUrlRegex: String = "",
    val whitelistDomain: String = "",
    val cookie: String = "",
    val customHeaders: String = "",
    val isLoaded: Boolean = false,
    val validationError: String? = null,
    val saved: Boolean = false
)
