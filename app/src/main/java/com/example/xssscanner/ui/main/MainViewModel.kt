package com.example.xssscanner.ui.main

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.xssscanner.domain.model.ScanOrchestratorEvent
import com.example.xssscanner.domain.model.ScanStatus
import com.example.xssscanner.domain.repository.ScanConfigRepository
import com.example.xssscanner.domain.usecase.ExportFormat
import com.example.xssscanner.domain.usecase.ExportResultsUseCase
import com.example.xssscanner.domain.usecase.StartScanUseCase
import com.example.xssscanner.domain.usecase.StopScanUseCase
import com.example.xssscanner.util.UrlValidator
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Implements §6's state-management rule: a single StateFlow<MainUiState>
 * exposed read-only, mutated only via events, with all scan-driving work
 * inside viewModelScope (never a self-managed scope in the ViewModel).
 *
 * Safety gate (§9): StartScanRequested shows the gate first; the actual
 * scan only starts after SafetyGateConfirmed.
 */
@HiltViewModel
class MainViewModel @Inject constructor(
    private val scanConfigRepository: ScanConfigRepository,
    private val startScanUseCase: StartScanUseCase,
    private val stopScanUseCase: StopScanUseCase,
    private val exportResultsUseCase: ExportResultsUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow(MainUiState())
    val uiState: StateFlow<MainUiState> = _uiState.asStateFlow()

    private var activeSessionId: String? = null

    fun onEvent(event: MainUiEvent) {
        when (event) {
            is MainUiEvent.TargetUrlChanged ->
                _uiState.update { it.copy(targetUrlInput = event.value) }

            is MainUiEvent.StartScanRequested ->
                _uiState.update { it.copy(showSafetyGate = true) }

            is MainUiEvent.SafetyGateDismissed ->
                _uiState.update { it.copy(showSafetyGate = false) }

            is MainUiEvent.SafetyGateConfirmed -> {
                _uiState.update { it.copy(showSafetyGate = false) }
                startScan()
            }

            is MainUiEvent.StopScanRequested -> {
                activeSessionId?.let { stopScanUseCase(it) }
            }

            is MainUiEvent.ExportCrawlRequested -> export(ExportFormat.CRAWL)
            is MainUiEvent.ExportAnalysisRequested -> export(ExportFormat.ANALYSIS)

            is MainUiEvent.ErrorMessageShown ->
                _uiState.update { it.copy(errorMessage = null) }

            is MainUiEvent.OpenSettingsRequested -> { /* handled by the Activity via Intent */ }
        }
    }

    private fun startScan() = viewModelScope.launch {
        val baseConfig = scanConfigRepository.getConfig()
        val rawInput = _uiState.value.targetUrlInput

        val validation = UrlValidator.validate(rawInput, baseConfig)
        val target = when (validation) {
            is UrlValidator.ValidationResult.Valid -> validation.target
            is UrlValidator.ValidationResult.Invalid -> {
                _uiState.update { it.copy(errorMessage = "Invalid or out-of-scope target: ${validation.error}") }
                return@launch
            }
        }

        val config = baseConfig.copy(targetUrl = target.normalizedUrl)
        _uiState.update {
            it.copy(logs = emptyList(), findings = emptyList(), crawledUrlCount = 0,
                discoveredParamCount = 0, findingCount = 0, status = ScanStatus.Validating)
        }

        startScanUseCase(config).let { events ->
            viewModelScope.launch {
                events.collect { event -> handleOrchestratorEvent(event) }
            }
        }
    }

    private fun handleOrchestratorEvent(event: ScanOrchestratorEvent) {
        when (event) {
            is ScanOrchestratorEvent.Started ->
                activeSessionId = event.sessionId

            is ScanOrchestratorEvent.StatusChanged ->
                _uiState.update { it.copy(status = event.status) }

            is ScanOrchestratorEvent.NewLog ->
                _uiState.update { it.copy(logs = it.logs + event.log) }

            is ScanOrchestratorEvent.NewFinding -> _uiState.update { state ->
                val existingIndex = state.findings.indexOfFirst { it.id == event.finding.id }
                val updatedFindings = if (existingIndex >= 0) {
                    state.findings.toMutableList().apply { set(existingIndex, event.finding) }
                } else {
                    state.findings + event.finding
                }
                state.copy(findings = updatedFindings)
            }

            is ScanOrchestratorEvent.ProgressUpdated ->
                _uiState.update {
                    it.copy(
                        crawledUrlCount = event.crawledUrlCount,
                        discoveredParamCount = event.discoveredParamCount,
                        findingCount = event.findingCount
                    )
                }

            is ScanOrchestratorEvent.Finished ->
                _uiState.update { it.copy(status = event.status) }
        }
    }

    private fun export(format: ExportFormat) = viewModelScope.launch {
        val sessionId = activeSessionId ?: run {
            _uiState.update { it.copy(errorMessage = "No scan session to export yet.") }
            return@launch
        }
        _uiState.update { it.copy(exportInProgress = true) }
        when (val result = exportResultsUseCase(sessionId, format)) {
            is ExportResultsUseCase.Result.Success ->
                _uiState.update {
                    it.copy(exportInProgress = false, lastExportUris = result.fileUris, status = ScanStatus.ExportReady)
                }
            is ExportResultsUseCase.Result.Error ->
                _uiState.update {
                    it.copy(exportInProgress = false, errorMessage = "Export failed: ${result.error}")
                }
        }
    }
}
