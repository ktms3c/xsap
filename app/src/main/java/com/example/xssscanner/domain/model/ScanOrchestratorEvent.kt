package com.example.xssscanner.domain.model

// TODO: needs confirmation — §8.3 types StartScanUseCase's output as
// Flow<ScanOrchestratorEvent>, but §8's closed model list (domain/model)
// does not declare this type anywhere. This file adds the minimum type
// needed to satisfy that contract; it is not an unrequested feature, just
// the plumbing the spec's own use-case table requires. Flagged here instead
// of silently inventing it without a trace.
sealed class ScanOrchestratorEvent {
    // Always the first event emitted for a given start(), so observers (the
    // ViewModel) learn the session id without StartScanUseCase's return
    // type needing to change from the plain Flow<ScanOrchestratorEvent>
    // §8.3 specifies.
    data class Started(val sessionId: String) : ScanOrchestratorEvent()
    data class StatusChanged(val status: ScanStatus) : ScanOrchestratorEvent()
    data class NewLog(val log: ScanLog) : ScanOrchestratorEvent()
    data class NewFinding(val finding: XssFinding) : ScanOrchestratorEvent()
    data class ProgressUpdated(
        val crawledUrlCount: Int,
        val discoveredParamCount: Int,
        val findingCount: Int
    ) : ScanOrchestratorEvent()
    data class Finished(val status: ScanStatus) : ScanOrchestratorEvent()
}
