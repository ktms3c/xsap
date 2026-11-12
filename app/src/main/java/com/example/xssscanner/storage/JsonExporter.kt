package com.example.xssscanner.storage

import com.example.xssscanner.domain.model.CrawlResult
import com.example.xssscanner.domain.model.XssFinding
import org.json.JSONArray
import org.json.JSONObject
import javax.inject.Inject

/**
 * TODO(verify-api): uses org.json (bundled with the Android platform SDK,
 * not a Gradle dependency) rather than a 3rd-party JSON library, since none
 * is listed in §0.4's mandatory dependency set and adding one would violate
 * "do not use experimental/alpha libraries unless explicitly named" in
 * spirit (adding any new library not in that list needs sign-off).
 */
class JsonExporter @Inject constructor() {

    fun exportCrawlResults(results: List<CrawlResult>): String {
        val array = JSONArray()
        results.forEach { r ->
            array.put(
                JSONObject().apply {
                    put("url", r.url)
                    put("status", r.statusCode ?: JSONObject.NULL)
                    put("contentType", r.contentType ?: JSONObject.NULL)
                    put("depth", r.depth)
                    put("source", r.source)
                    put("parameters", JSONArray(r.parameters))
                    put("discoveredAt", r.discoveredAt)
                }
            )
        }
        return array.toString(2)
    }

    fun exportFindings(findings: List<XssFinding>): String {
        val array = JSONArray()
        findings.forEach { f ->
            array.put(
                JSONObject().apply {
                    put("url", f.url)
                    put("parameter", f.parameter)
                    put("payload", f.payload)
                    put("xssType", f.xssType)
                    put("context", f.context)
                    put("status", f.status.name)
                    put("riskScore", f.riskScore)
                    put("evidence", f.evidence)
                    put("requestSummary", f.requestSummary)
                    put("responseSummary", f.responseSummary)
                    put("detectedAt", f.detectedAt)
                }
            )
        }
        return array.toString(2)
    }
}
