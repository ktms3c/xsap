package com.example.xssscanner.domain.usecase

import com.example.xssscanner.domain.model.ScanConfig
import com.example.xssscanner.domain.model.ScanOrchestratorEvent
import com.example.xssscanner.scanner.orchestrator.ScanOrchestrator
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

/**
 * TODO: needs confirmation — §0.2 says a use case needing I/O should declare
 * an interface in domain with the implementation in data/scanner. But
 * domain/repository's file list is declared final with exactly 3 files
 * (HttpClientPort, ScanConfigRepository, ScanResultRepository), with no
 * orchestrator port among them. The simplest, safest reading that doesn't
 * invent a 4th domain/repository file against an explicitly "final" list is
 * to depend on ScanOrchestrator directly here. ScanOrchestrator itself does
 * not import Hilt (it is driven by plain constructor injection), so this
 * does not pull a Hilt import into domain; it is a cross-layer dependency
 * the spec does not fully resolve, called out here rather than hidden.
 */
class StartScanUseCase @Inject constructor(
    private val scanOrchestrator: ScanOrchestrator
) {
    operator fun invoke(config: ScanConfig): Flow<ScanOrchestratorEvent> {
        return scanOrchestrator.start(config)
    }
}
