package com.example.xssscanner.domain.model

data class ScanLog(
    val timestamp: Long,
    val level: LogLevel,
    val category: String,
    val message: String,
    val url: String?,
    val statusCode: Int?
)
