package com.example.xssscanner.data.repository

import com.example.xssscanner.data.local.ScanLogDao
import com.example.xssscanner.data.local.ScanResultDao
import com.example.xssscanner.data.local.entity.toDomain
import com.example.xssscanner.data.local.entity.toEntity
import com.example.xssscanner.domain.model.CrawlResult
import com.example.xssscanner.domain.model.ScanLog
import com.example.xssscanner.domain.model.XssFinding
import com.example.xssscanner.domain.repository.ScanResultRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject

class ScanResultRepositoryImpl @Inject constructor(
    private val scanResultDao: ScanResultDao,
    private val scanLogDao: ScanLogDao
) : ScanResultRepository {

    override suspend fun saveCrawlResult(sessionId: String, result: CrawlResult) {
        scanResultDao.insertCrawlResult(result.toEntity(sessionId))
    }

    override suspend fun saveFinding(sessionId: String, finding: XssFinding) {
        scanResultDao.insertFinding(finding.toEntity(sessionId))
    }

    override suspend fun updateFinding(sessionId: String, finding: XssFinding) {
        scanResultDao.updateFinding(finding.toEntity(sessionId))
    }

    override suspend fun saveLog(sessionId: String, log: ScanLog) {
        scanLogDao.insertLog(log.toEntity(sessionId))
    }

    override fun observeFindings(sessionId: String): Flow<List<XssFinding>> =
        scanResultDao.observeFindings(sessionId).map { list -> list.map { it.toDomain() } }

    override fun observeLogs(sessionId: String): Flow<List<ScanLog>> =
        scanLogDao.observeLogs(sessionId).map { list -> list.map { it.toDomain() } }

    override suspend fun getCrawlResults(sessionId: String): List<CrawlResult> =
        scanResultDao.getCrawlResults(sessionId).map { it.toDomain() }

    override suspend fun getFindings(sessionId: String): List<XssFinding> =
        scanResultDao.getFindings(sessionId).map { it.toDomain() }

    override suspend fun getLogs(sessionId: String): List<ScanLog> =
        scanLogDao.getLogs(sessionId).map { it.toDomain() }
}
