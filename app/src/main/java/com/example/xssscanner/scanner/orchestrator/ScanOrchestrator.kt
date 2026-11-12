package com.example.xssscanner.scanner.orchestrator

import com.example.xssscanner.domain.model.LogLevel
import com.example.xssscanner.domain.model.ScanConfig
import com.example.xssscanner.domain.model.ScanError
import com.example.xssscanner.domain.model.ScanLog
import com.example.xssscanner.domain.model.ScanOrchestratorEvent
import com.example.xssscanner.domain.model.ScanStatus
import com.example.xssscanner.domain.repository.ScanResultRepository
import com.example.xssscanner.domain.usecase.AnalyzeDomXssUseCase
import com.example.xssscanner.domain.usecase.ConfirmExecutionUseCase
import com.example.xssscanner.domain.usecase.DiscoverParametersUseCase
import com.example.xssscanner.domain.usecase.TestReflectedXssUseCase
import com.example.xssscanner.scanner.crawler.CrawlEngine
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow

import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

/**
 * The single owner of the scan pipeline's CoroutineScope (§6). Built from
 * SupervisorJob() + Dispatchers.IO, held per scan session, never
 * GlobalScope. Worker parallelism is bounded by ScanConfig.threads via a
 * Semaphore; combined with HttpClientService's RateLimiter (already applied
 * at the HTTP layer), both controls required by §6 are active at once.
 */
@Singleton
class ScanOrchestrator @Inject constructor(
    private val crawlEngine: CrawlEngine,
    private val discoverParametersUseCase: DiscoverParametersUseCase,
    private val testReflectedXssUseCase: TestReflectedXssUseCase,
    private val analyzeDomXssUseCase: AnalyzeDomXssUseCase,
    private val confirmExecutionUseCase: ConfirmExecutionUseCase,
    private val scanResultRepository: ScanResultRepository
) {
    private var scopeJob: Job? = null
    private var scope: CoroutineScope? = null
    private var activeSessionId: String? = null

    fun start(config: ScanConfig): Flow<ScanOrchestratorEvent> = callbackFlow {
        val sessionId = UUID.randomUUID().toString()
        activeSessionId = sessionId

        val job = SupervisorJob()
        val pipelineScope = CoroutineScope(job + Dispatchers.IO)
        scopeJob = job
        scope = pipelineScope

        val semaphore = Semaphore(config.threads.coerceIn(1, 16))
        // Captured explicitly (rather than relying on Kotlin resolving
        // `send`/`close` through the nested launch{} blocks' own implicit
        // CoroutineScope receiver) to keep the control flow easy to audit.
        val producerScope = this
        val emit: suspend (ScanOrchestratorEvent) -> Unit = { producerScope.send(it) }

        pipelineScope.launch {
            try {
                emit(ScanOrchestratorEvent.Started(sessionId))
                emit(ScanOrchestratorEvent.StatusChanged(ScanStatus.Crawling))
                emitLog(sessionId, LogLevel.INFO, "scan", "Scan started for ${config.targetUrl}", emit = emit)

                var crawledCount = 0
                var paramCount = 0
                var findingCount = 0

                // TODO: needs confirmation — audited from an initial
                // collectLatest() to a plain collect(): collectLatest would
                // cancel an in-flight saveCrawlResult/emit for one crawl
                // result as soon as the next result arrived, risking lost
                // saves under fast crawls. Every result must be fully
                // processed, so collect() is correct here.
                crawlEngine.crawlWithEvents(config).collect { event ->
                    event.log?.let { log ->
                        emitLog(sessionId, log.level, log.category, log.message, log.url, log.statusCode, emit)
                    }
                    event.result?.let { result ->
                        crawledCount++
                        scanResultRepository.saveCrawlResult(sessionId, result)
                        emitLog(
                            sessionId, LogLevel.INFO, "http",
                            "${result.statusCode ?: "ERR"} ${result.url}", result.url, result.statusCode, emit
                        )
                        emit(
                            ScanOrchestratorEvent.ProgressUpdated(crawledCount, paramCount, findingCount)
                        )

                        // §4.5 static DOM analysis candidate, per-result.
                        analyzeDomXssUseCase(result)?.let { domFinding ->
                            findingCount++
                            scanResultRepository.saveFinding(sessionId, domFinding)
                            emit(ScanOrchestratorEvent.NewFinding(domFinding))
                        }

                        // §4.3/§4.4 reflected XSS testing, bounded by
                        // config.threads via the semaphore (§6).
                        val params = discoverParametersUseCase(result)
                        paramCount += params.size
                        params.forEach { param ->
                            pipelineScope.launch {
                                try {
                                    semaphore.withPermit {
                                        emitLog(
                                            sessionId, LogLevel.INFO, "param",
                                            "Parameter discovered: ${param.name}", param.url, null, emit
                                        )
                                        val finding = testReflectedXssUseCase(param, config)
                                        if (finding != null) {
                                            findingCount++
                                            scanResultRepository.saveFinding(sessionId, finding)
                                            emit(ScanOrchestratorEvent.NewFinding(finding))

                                            if (finding.status.name == "POSSIBLE_EXECUTABLE_CONTEXT") {
                                                val confirmed = confirmExecutionUseCase(finding)
                                                if (confirmed != finding) {
                                                    scanResultRepository.updateFinding(sessionId, confirmed)
                                                    emit(ScanOrchestratorEvent.NewFinding(confirmed))
                                                }
                                            }
                                        }
                                    }
                                } catch (e: Exception) {
                                    // A single parameter's test failing must never kill the
                                    // whole scan (SupervisorJob children are independent, but
                                    // an uncaught exception here would still surface as a
                                    // crash via the default exception handler) — log and move on.
                                    emitLog(
                                        sessionId, LogLevel.ERROR, "param",
                                        "Test failed for ${param.name}: ${e.message}", param.url, null, emit
                                    )
                                }
                            }
                        }
                    }
                }

                emit(ScanOrchestratorEvent.StatusChanged(ScanStatus.Completed))
                emit(ScanOrchestratorEvent.Finished(ScanStatus.Completed))
            } catch (e: Exception) {
                val error = ScanError.NetworkError(e.message ?: "scan failed")
                emit(ScanOrchestratorEvent.StatusChanged(ScanStatus.Failed(error)))
                emit(ScanOrchestratorEvent.Finished(ScanStatus.Failed(error)))
            } finally {
                producerScope.close()
            }
        }

        awaitClose {
            job.cancel()
            if (activeSessionId == sessionId) {
                activeSessionId = null
            }
        }
    }

    fun cancel(scanSessionId: String) {
        if (activeSessionId != scanSessionId) return // §8.3: scoped to the matching session
        scopeJob?.cancel()
        activeSessionId = null
    }

    private suspend fun emitLog(
        sessionId: String,
        level: LogLevel,
        category: String,
        message: String,
        url: String? = null,
        statusCode: Int? = null,
        emit: suspend (ScanOrchestratorEvent) -> Unit
    ) {
        val log = ScanLog(
            timestamp = System.currentTimeMillis(),
            level = level,
            category = category,
            message = message,
            url = url,
            statusCode = statusCode
        )
        scanResultRepository.saveLog(sessionId, log)
        emit(ScanOrchestratorEvent.NewLog(log))
    }
}
