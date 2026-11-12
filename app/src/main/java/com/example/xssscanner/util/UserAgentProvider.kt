package com.example.xssscanner.util

import kotlin.math.abs

/**
 * Implements §7.1: "random" User-Agent is deterministic per
 * hash(targetUrl + scanStartTimestampDay) % list.size, not plain Random().
 */
object UserAgentProvider {

    // TODO(verify-version): UA strings should be refreshed periodically;
    // these are representative current-generation mobile/desktop UAs.
    private val pool = listOf(
        "Mozilla/5.0 (Linux; Android 14; Pixel 8) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/125.0.0.0 Mobile Safari/537.36",
        "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/125.0.0.0 Safari/537.36",
        "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/605.1.15 (KHTML, like Gecko) Version/17.4 Safari/605.1.15",
        "Mozilla/5.0 (iPhone; CPU iPhone OS 17_4 like Mac OS X) AppleWebKit/605.1.15 (KHTML, like Gecko) Version/17.4 Mobile/15E148 Safari/604.1",
        "Mozilla/5.0 (X11; Linux x86_64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/125.0.0.0 Safari/537.36"
    )

    fun resolve(userAgentSetting: String, targetUrl: String, scanStartTimestampMillis: Long): String {
        if (userAgentSetting != "random") return userAgentSetting

        val dayBucket = scanStartTimestampMillis / (24L * 60 * 60 * 1000)
        val key = "$targetUrl|$dayBucket"
        val index = abs(key.hashCode()) % pool.size
        return pool[index]
    }
}
