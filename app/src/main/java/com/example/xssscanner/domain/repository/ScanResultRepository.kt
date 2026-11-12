package com.example.xssscanner.domain.repository

import com.example.xssscanner.domain.model.CrawlResult
import com.example.xssscanner.domain.model.ScanLog
import com.example.xssscanner.domain.model.XssFinding
import kotlinx.coroutines.flow.Flow

interface ScanResultRepository {
    suspend fun saveCrawlResult(sessionId: String, result: CrawlResult)
    suspend fun saveFinding(sessionId: String, finding: XssFinding)
    suspend fun updateFinding(sessionId: String, finding: XssFinding)
    suspend fun saveLog(sessionId: String, log: ScanLog)

    fun observeFindings(sessionId: String): Flow<List<XssFinding>>
    fun observeLogs(sessionId: String): Flow<List<ScanLog>>

    suspend fun getCrawlResults(sessionId: String): List<CrawlResult>
    suspend fun getFindings(sessionId: String): List<XssFinding>
    suspend fun getLogs(sessionId: String): List<ScanLog>
}
