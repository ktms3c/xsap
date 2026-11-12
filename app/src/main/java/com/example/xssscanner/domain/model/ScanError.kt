package com.example.xssscanner.domain.model

sealed class ScanError {
    data object InvalidUrl : ScanError()
    data object DomainNotAllowed : ScanError()
    data class BlacklistedUrl(val url: String) : ScanError()
    data class NetworkError(val message: String) : ScanError()
    data object Timeout : ScanError()
    data object ScanCancelled : ScanError()
    data class ExportFailed(val reason: String) : ScanError()
}
