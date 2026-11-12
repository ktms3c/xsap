package com.example.xssscanner.domain.model

sealed class ScanStatus {
    data object Idle : ScanStatus()
    data object Validating : ScanStatus()
    data object Crawling : ScanStatus()
    data object Analyzing : ScanStatus()
    data object TestingReflectedXss : ScanStatus()
    data object ConfirmingDomXss : ScanStatus()
    data object Completed : ScanStatus()
    data object ExportReady : ScanStatus()
    data object Stopped : ScanStatus()
    data class Failed(val error: ScanError) : ScanStatus()
}
