package com.example.xssscanner.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.xssscanner.data.local.entity.ScanLogEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ScanLogDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertLog(entity: ScanLogEntity)

    @Query("SELECT * FROM scan_logs WHERE scanSessionId = :sessionId ORDER BY timestamp ASC")
    suspend fun getLogs(sessionId: String): List<ScanLogEntity>

    @Query("SELECT * FROM scan_logs WHERE scanSessionId = :sessionId ORDER BY timestamp ASC")
    fun observeLogs(sessionId: String): Flow<List<ScanLogEntity>>
}
