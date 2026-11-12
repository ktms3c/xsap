package com.example.xssscanner.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.xssscanner.domain.model.ScanConfig
import com.example.xssscanner.domain.repository.ScanConfigRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Implements §7.2's validation rules: regex fields must compile, Custom
 * Headers must be "Header: value" per line, whitelistDomain must be a
 * syntactically plausible host. Invalid state blocks Save and shows
 * validationError rather than silently dropping the bad input.
 */
@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val scanConfigRepository: ScanConfigRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(SettingsUiState())
    val uiState: StateFlow<SettingsUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            val config = scanConfigRepository.getConfig()
            _uiState.update {
                it.copy(
                    enableJsLinkFinder = config.enableJsLinkFinder,
                    crawlSitemap = config.crawlSitemap,
                    crawlRobots = config.crawlRobots,
                    disableRedirect = config.disableRedirect,
                    localLabMode = config.localLabMode,
                    threads = config.threads,
                    timeoutSeconds = config.timeoutSeconds,
                    crawlDepth = config.crawlDepth,
                    userAgent = config.userAgent,
                    blacklistUrlRegex = config.blacklistUrlRegex,
                    whitelistUrlRegex = config.whitelistUrlRegex,
                    whitelistDomain = config.whitelistDomain,
                    cookie = config.cookie,
                    customHeaders = config.customHeaders,
                    isLoaded = true
                )
            }
        }
    }

    fun update(transform: (SettingsUiState) -> SettingsUiState) {
        _uiState.update { transform(it).copy(saved = false, validationError = null) }
    }

    fun save() = viewModelScope.launch {
        val state = _uiState.value
        val error = validate(state)
        if (error != null) {
            _uiState.update { it.copy(validationError = error) }
            return@launch
        }

        scanConfigRepository.saveConfig(
            ScanConfig(
                targetUrl = "", // not persisted here; set at scan-start time (§7)
                enableJsLinkFinder = state.enableJsLinkFinder,
                crawlSitemap = state.crawlSitemap,
                crawlRobots = state.crawlRobots,
                disableRedirect = state.disableRedirect,
                localLabMode = state.localLabMode,
                threads = state.threads,
                timeoutSeconds = state.timeoutSeconds,
                crawlDepth = state.crawlDepth,
                userAgent = state.userAgent,
                blacklistUrlRegex = state.blacklistUrlRegex,
                whitelistUrlRegex = state.whitelistUrlRegex,
                whitelistDomain = state.whitelistDomain,
                cookie = state.cookie,
                customHeaders = state.customHeaders
            )
        )
        _uiState.update { it.copy(saved = true) }
    }

    private fun validate(state: SettingsUiState): String? {
        if (state.whitelistDomain.isBlank()) return "Whitelist domain is required."
        if (!state.whitelistDomain.matches(Regex("^[a-zA-Z0-9.-]+\\.[a-zA-Z]{2,}$"))) {
            return "Whitelist domain doesn't look like a valid host."
        }
        if (state.blacklistUrlRegex.isNotBlank() && !isValidRegex(state.blacklistUrlRegex)) {
            return "Blacklist URL regex is invalid."
        }
        if (state.whitelistUrlRegex.isNotBlank() && !isValidRegex(state.whitelistUrlRegex)) {
            return "Whitelist URL regex is invalid."
        }
        if (state.customHeaders.isNotBlank()) {
            val allValid = state.customHeaders.lines().filter { it.isNotBlank() }.all { line ->
                val idx = line.indexOf(':')
                idx > 0 && line.substring(0, idx).isNotBlank()
            }
            if (!allValid) return "Custom headers must be 'Header: value', one per line."
        }
        if (state.threads !in 1..16) return "Threads must be between 1 and 16."
        if (state.timeoutSeconds !in 1..120) return "Timeout must be between 1 and 120 seconds."
        if (state.crawlDepth !in 0..10) return "Crawl depth must be between 0 and 10."
        return null
    }

    private fun isValidRegex(pattern: String): Boolean =
        runCatching { Regex(pattern) }.isSuccess
}
