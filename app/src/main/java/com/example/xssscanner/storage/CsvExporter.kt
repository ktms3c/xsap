package com.example.xssscanner.storage

import com.example.xssscanner.domain.model.CrawlResult
import com.example.xssscanner.domain.model.XssFinding
import javax.inject.Inject

/**
 * Implements §10's EXPORT CRAWL / EXPORT ANALYSIS CSV outputs. Per §10's
 * privacy rules, raw cookie/header VALUES are never written — CrawlResult
 * doesn't carry raw headers at all (§8's model has no such field), so there
 * is nothing to mask here; this is noted rather than silently assumed.
 */
class CsvExporter @Inject constructor() {

    fun exportCrawlResults(results: List<CrawlResult>): String {
        val header = "url,status,content_type,depth,source,parameters"
        val rows = results.joinToString("\n") { r ->
            listOf(
                r.url,
                r.statusCode?.toString().orEmpty(),
                r.contentType.orEmpty(),
                r.depth.toString(),
                r.source,
                r.parameters.joinToString(";")
            ).joinToString(",") { ExportWriter.csvEscape(it) }
        }
        return "$header\n$rows"
    }

    fun exportFindings(findings: List<XssFinding>): String {
        val header = "url,parameter,payload,xss_type,context,status,risk_score,evidence,request_summary,response_summary,detected_at"
        val rows = findings.joinToString("\n") { f ->
            listOf(
                f.url, f.parameter, f.payload, f.xssType, f.context, f.status.name,
                f.riskScore.toString(), f.evidence, f.requestSummary, f.responseSummary,
                f.detectedAt.toString()
            ).joinToString(",") { ExportWriter.csvEscape(it) }
        }
        return "$header\n$rows"
    }
}
