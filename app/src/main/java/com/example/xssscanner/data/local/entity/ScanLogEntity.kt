package com.example.xssscanner.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.example.xssscanner.domain.model.LogLevel
import com.example.xssscanner.domain.model.ScanLog

@Entity(tableName = "scan_logs")
data class ScanLogEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val scanSessionId: String,
    val timestamp: Long,
    val level: String, // LogLevel.name
    val category: String,
    val message: String,
    val url: String?,
    val statusCode: Int?
)

fun ScanLogEntity.toDomain(): ScanLog = ScanLog(
    timestamp = timestamp,
    level = LogLevel.valueOf(level),
    category = category,
    message = message,
    url = url,
    statusCode = statusCode
)

fun ScanLog.toEntity(scanSessionId: String): ScanLogEntity = ScanLogEntity(
    scanSessionId = scanSessionId,
    timestamp = timestamp,
    level = level.name,
    category = category,
    message = message,
    url = url,
    statusCode = statusCode
)
