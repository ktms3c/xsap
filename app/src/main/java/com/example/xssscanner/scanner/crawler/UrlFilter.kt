package com.example.xssscanner.scanner.crawler

import com.example.xssscanner.domain.model.ScanConfig
import com.example.xssscanner.util.PrivateIpGuard
import java.net.URI
import javax.inject.Inject

/**
 * The single place the "is this URL allowed to be crawled/tested" decision
 * is made (§1's "Additional rules"). Every URL source (link, form, sitemap,
 * robots, JS) must pass through here before entering the crawl queue.
 */
class UrlFilter @Inject constructor() {

    fun isAllowed(url: String, config: ScanConfig): Boolean {
        val uri = runCatching { URI(url) }.getOrNull() ?: return false
        val scheme = uri.scheme?.lowercase()
        if (scheme != "http" && scheme != "https") return false

        val host = uri.host ?: return false
        if (!PrivateIpGuard.check(host, config.localLabMode)) return false

        val whitelistDomain = config.whitelistDomain.trim().lowercase().removePrefix("www.")
        val normalizedHost = host.lowercase()
        val inScope = normalizedHost == whitelistDomain ||
            normalizedHost.endsWith(".$whitelistDomain") ||
            normalizedHost == "www.$whitelistDomain"
        if (!inScope) return false

        if (config.blacklistUrlRegex.isNotBlank()) {
            val blacklisted = runCatching { Regex(config.blacklistUrlRegex) }
                .getOrNull()?.containsMatchIn(url) ?: false
            if (blacklisted) return false
        }

        if (config.whitelistUrlRegex.isNotBlank()) {
            val whitelistedByRegex = runCatching { Regex(config.whitelistUrlRegex) }
                .getOrNull()?.containsMatchIn(url) ?: true
            if (!whitelistedByRegex) return false
        }

        return true
    }
}
