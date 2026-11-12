package com.example.xssscanner.domain.model

/**
 * A single validated crawl/test target derived from the user's input, after
 * UrlValidator + PrivateIpGuard + whitelist-domain checks have passed (§4.1).
 * Not persisted directly; ScanConfig.targetUrl is the source of truth for a
 * scan session. This exists as a small value type for passing a validated
 * target between UrlValidator -> StartScanUseCase without re-validating.
 */
data class ScanTarget(
    val normalizedUrl: String,
    val host: String,
    val scheme: String
)
