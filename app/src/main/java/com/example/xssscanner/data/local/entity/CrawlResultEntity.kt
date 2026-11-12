package com.example.xssscanner.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.example.xssscanner.domain.model.CrawlResult

@Entity(tableName = "crawl_results")
data class CrawlResultEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val scanSessionId: String,
    val url: String,
    val statusCode: Int?,
    val contentType: String?,
    val depth: Int,
    val discoveredAt: Long,
    val source: String,
    val parametersCsv: String // comma-joined; List<String> is not a Room-storable type without a TypeConverter
)

fun CrawlResultEntity.toDomain(): CrawlResult = CrawlResult(
    url = url,
    statusCode = statusCode,
    contentType = contentType,
    depth = depth,
    discoveredAt = discoveredAt,
    source = source,
    parameters = if (parametersCsv.isBlank()) emptyList() else parametersCsv.split(",")
)

fun CrawlResult.toEntity(scanSessionId: String): CrawlResultEntity = CrawlResultEntity(
    scanSessionId = scanSessionId,
    url = url,
    statusCode = statusCode,
    contentType = contentType,
    depth = depth,
    discoveredAt = discoveredAt,
    source = source,
    parametersCsv = parameters.joinToString(",")
)
