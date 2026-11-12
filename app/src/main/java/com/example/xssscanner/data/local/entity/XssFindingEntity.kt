package com.example.xssscanner.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.example.xssscanner.domain.model.FindingStatus
import com.example.xssscanner.domain.model.XssFinding

@Entity(tableName = "xss_findings")
data class XssFindingEntity(
    @PrimaryKey val id: String,
    val scanSessionId: String,
    val url: String,
    val parameter: String,
    val payload: String,
    val xssType: String,
    val context: String,
    val status: String, // FindingStatus.name — never a free-form string (§13)
    val riskScore: Int,
    val evidence: String,
    val requestSummary: String,
    val responseSummary: String,
    val detectedAt: Long
)

fun XssFindingEntity.toDomain(): XssFinding = XssFinding(
    id = id,
    url = url,
    parameter = parameter,
    payload = payload,
    xssType = xssType,
    context = context,
    status = FindingStatus.valueOf(status),
    riskScore = riskScore,
    evidence = evidence,
    requestSummary = requestSummary,
    responseSummary = responseSummary,
    detectedAt = detectedAt
)

fun XssFinding.toEntity(scanSessionId: String): XssFindingEntity = XssFindingEntity(
    id = id,
    scanSessionId = scanSessionId,
    url = url,
    parameter = parameter,
    payload = payload,
    xssType = xssType,
    context = context,
    status = status.name,
    riskScore = riskScore,
    evidence = evidence,
    requestSummary = requestSummary,
    responseSummary = responseSummary,
    detectedAt = detectedAt
)
