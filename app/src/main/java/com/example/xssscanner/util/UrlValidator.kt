package com.example.xssscanner.util

import com.example.xssscanner.domain.model.ScanConfig
import com.example.xssscanner.domain.model.ScanError
import com.example.xssscanner.domain.model.ScanTarget
import java.net.URI

/**
 * Implements §4.1's full validation pipeline (normalize -> parse -> scheme
 * check -> PrivateIpGuard -> whitelist domain match).
 */
object UrlValidator {

    sealed class ValidationResult {
        data class Valid(val target: ScanTarget) : ValidationResult()
        data class Invalid(val error: ScanError) : ValidationResult()
    }

    fun normalize(rawInput: String): String {
        val trimmed = rawInput.trim()
        return if (trimmed.contains("://")) trimmed else "https://$trimmed"
    }

    fun validate(rawInput: String, config: ScanConfig): ValidationResult {
        if (rawInput.isBlank()) return ValidationResult.Invalid(ScanError.InvalidUrl)

        val normalized = normalize(rawInput)
        val uri = try {
            URI(normalized)
        } catch (e: Exception) {
            return ValidationResult.Invalid(ScanError.InvalidUrl)
        }

        val scheme = uri.scheme?.lowercase()
        if (scheme != "http" && scheme != "https") {
            return ValidationResult.Invalid(ScanError.InvalidUrl)
        }

        val host = uri.host ?: return ValidationResult.Invalid(ScanError.InvalidUrl)

        if (!PrivateIpGuard.check(host, config.localLabMode)) {
            return ValidationResult.Invalid(ScanError.DomainNotAllowed)
        }

        val whitelistDomain = config.whitelistDomain.trim()
        if (whitelistDomain.isEmpty()) {
            return ValidationResult.Invalid(ScanError.DomainNotAllowed)
        }
        if (!hostMatchesDomain(host, whitelistDomain)) {
            return ValidationResult.Invalid(ScanError.DomainNotAllowed)
        }

        return ValidationResult.Valid(ScanTarget(normalizedUrl = normalized, host = host, scheme = scheme))
    }

    private fun hostMatchesDomain(host: String, domain: String): Boolean {
        val h = host.lowercase()
        val d = domain.lowercase().removePrefix("www.")
        return h == d || h.endsWith(".$d") || h == "www.$d"
    }
}
