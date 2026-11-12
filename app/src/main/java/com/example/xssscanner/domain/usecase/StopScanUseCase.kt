package com.example.xssscanner.domain.usecase

import com.example.xssscanner.scanner.orchestrator.ScanOrchestrator
import javax.inject.Inject

class StopScanUseCase @Inject constructor(
    private val scanOrchestrator: ScanOrchestrator
) {
    // sessionId is accepted per §8.3's contract; ScanOrchestrator currently
    // tracks a single active session scope (§6), so the id is used only to
    // guard against cancelling a session that isn't the active one.
    operator fun invoke(scanSessionId: String) {
        scanOrchestrator.cancel(scanSessionId)
    }
}
