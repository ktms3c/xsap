package com.example.xssscanner.util

import java.net.InetAddress
import java.net.UnknownHostException

/**
 * Implements §4.1's private-IP/localhost rejection. Rejects localhost,
 * 127.0.0.1, 10.0.0.0/8, 172.16.0.0/12, 192.168.0.0/16, and 169.254.0.0/16
 * (link-local) unless localLabMode is enabled.
 *
 * TODO: needs confirmation — resolving the host via InetAddress.getByName
 * does a real DNS/blocking lookup; §4.1 doesn't specify whether this check
 * should also run on a background dispatcher here or only be called from
 * an already-background context (UrlValidator / CrawlTargetUseCase run on
 * Dispatchers.IO per §6, so this is written as a plain blocking function
 * and is expected to be invoked from an IO-dispatched coroutine, not the
 * main thread).
 */
object PrivateIpGuard {

    private val privateRanges = listOf(
        "10.0.0.0" to 8,
        "172.16.0.0" to 12,
        "192.168.0.0" to 16,
        "169.254.0.0" to 16
    )

    fun isPrivateOrLocal(host: String): Boolean {
        val normalizedHost = host.lowercase()
        if (normalizedHost == "localhost" || normalizedHost == "127.0.0.1") return true

        val address = try {
            InetAddress.getByName(host)
        } catch (e: UnknownHostException) {
            // Unresolvable host: treat conservatively as "not private" here;
            // UrlValidator's whitelist-domain check still applies separately.
            return false
        }

        if (address.isLoopbackAddress || address.isLinkLocalAddress || address.isSiteLocalAddress) {
            return true
        }

        val ipBytes = address.address
        if (ipBytes.size != 4) return false // IPv6: TODO(verify-api) not covered by §4.1's ranges

        return privateRanges.any { (base, prefixBits) ->
            isInCidr(ipBytes, base, prefixBits)
        }
    }

    private fun isInCidr(ipBytes: ByteArray, baseIp: String, prefixBits: Int): Boolean {
        val baseBytes = baseIp.split(".").map { it.toInt() and 0xFF }
        val fullBytes = prefixBits / 8
        val remainingBits = prefixBits % 8

        for (i in 0 until fullBytes) {
            if ((ipBytes[i].toInt() and 0xFF) != baseBytes[i]) return false
        }
        if (remainingBits == 0) return true

        val mask = (0xFF shl (8 - remainingBits)) and 0xFF
        return (ipBytes[fullBytes].toInt() and mask) == (baseBytes[fullBytes] and mask)
    }

    fun check(host: String, localLabMode: Boolean): Boolean {
        // Returns true when the host is ALLOWED (i.e. passed the guard).
        if (localLabMode) return true
        return !isPrivateOrLocal(host)
    }
}
