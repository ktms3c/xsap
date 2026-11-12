package com.example.xssscanner.ui.main

sealed class MainUiEvent {
    data class TargetUrlChanged(val value: String) : MainUiEvent()
    data object StartScanRequested : MainUiEvent()
    data object SafetyGateConfirmed : MainUiEvent()
    data object SafetyGateDismissed : MainUiEvent()
    data object StopScanRequested : MainUiEvent()
    data object ExportCrawlRequested : MainUiEvent()
    data object ExportAnalysisRequested : MainUiEvent()
    data object ErrorMessageShown : MainUiEvent()
    data object OpenSettingsRequested : MainUiEvent()
}
