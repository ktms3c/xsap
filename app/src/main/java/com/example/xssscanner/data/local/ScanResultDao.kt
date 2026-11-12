package com.example.xssscanner.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.xssscanner.data.local.entity.CrawlResultEntity
import com.example.xssscanner.data.local.entity.XssFindingEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ScanResultDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCrawlResult(entity: CrawlResultEntity)

    @Query("SELECT * FROM crawl_results WHERE scanSessionId = :sessionId ORDER BY discoveredAt ASC")
    suspend fun getCrawlResults(sessionId: String): List<CrawlResultEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertFinding(entity: XssFindingEntity)

    @Update
    suspend fun updateFinding(entity: XssFindingEntity)

    @Query("SELECT * FROM xss_findings WHERE scanSessionId = :sessionId ORDER BY detectedAt ASC")
    suspend fun getFindings(sessionId: String): List<XssFindingEntity>

    @Query("SELECT * FROM xss_findings WHERE scanSessionId = :sessionId ORDER BY detectedAt ASC")
    fun observeFindings(sessionId: String): Flow<List<XssFindingEntity>>
}
