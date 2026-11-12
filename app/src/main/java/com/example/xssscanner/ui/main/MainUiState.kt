package com.example.xssscanner.ui.main

import com.example.xssscanner.domain.model.ScanLog
import com.example.xssscanner.domain.model.ScanStatus
import com.example.xssscanner.domain.model.XssFinding

data class MainUiState(
    val targetUrlInput: String = "",
    val status: ScanStatus = ScanStatus.Idle,
    val logs: List<ScanLog> = emptyList(),
    val findings: List<XssFinding> = emptyList(),
    val crawledUrlCount: Int = 0,
    val discoveredParamCount: Int = 0,
    val findingCount: Int = 0,
    val showSafetyGate: Boolean = false,
    val exportInProgress: Boolean = false,
    val lastExportUris: List<String> = emptyList(),
    val errorMessage: String? = null
) {
    val isScanning: Boolean
        get() = status !is ScanStatus.Idle && status !is ScanStatus.Completed &&
            status !is ScanStatus.Stopped && status !is ScanStatus.Failed
}
