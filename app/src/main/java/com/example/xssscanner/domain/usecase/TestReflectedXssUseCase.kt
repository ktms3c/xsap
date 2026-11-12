package com.example.xssscanner.domain.usecase

import com.example.xssscanner.domain.model.DiscoveredParameter
import com.example.xssscanner.domain.model.FindingStatus
import com.example.xssscanner.domain.model.ScanConfig
import com.example.xssscanner.domain.model.XssFinding
import com.example.xssscanner.domain.repository.HttpClientPort
import com.example.xssscanner.scanner.analyzer.HtmlContextAnalyzer
import com.example.xssscanner.scanner.analyzer.JavaScriptContextAnalyzer
import com.example.xssscanner.scanner.analyzer.ReflectionAnalyzer
import com.example.xssscanner.scanner.analyzer.RiskScorer
import com.example.xssscanner.scanner.payload.PayloadGenerator
import java.net.URI
import java.net.URLEncoder
import java.util.UUID
import javax.inject.Inject

/**
 * Implements §4.4. Raw substring matching alone is never treated as
 * sufficient — ReflectionAnalyzer only confirms the payload marker is
 * present; HtmlContextAnalyzer/JavaScriptContextAnalyzer determine the
 * reflection context and whether the dangerous characters were encoded
 * at that point before any execution-capable status is assigned.
 * CONFIRMED_EXECUTION is intentionally never set here (§4.4 reserves it
 * for ConfirmExecutionUseCase via the WebView).
 */
class TestReflectedXssUseCase @Inject constructor(
    private val httpClient: HttpClientPort,
    private val payloadGenerator: PayloadGenerator,
    private val reflectionAnalyzer: ReflectionAnalyzer,
    private val htmlContextAnalyzer: HtmlContextAnalyzer,
    private val jsContextAnalyzer: JavaScriptContextAnalyzer,
    private val riskScorer: RiskScorer
) {
    suspend operator fun invoke(
        parameter: DiscoveredParameter,
        config: ScanConfig
    ): XssFinding? {
        val payload = payloadGenerator.generate(contextHint = parameter.name)
        val testUrl = buildTestUrl(parameter.url, parameter.name, payload) ?: return null

        val headers = ScanConfigHeaders.parse(config.customHeaders)
        val response = httpClient.execute(
            HttpClientPort.HttpRequest(
                url = testUrl,
                headers = headers,
                cookie = config.cookie.ifBlank { null }
            )
        )

        val success = response as? HttpClientPort.HttpResult.Success ?: return null

        if (!reflectionAnalyzer.containsPayload(success.body, payload)) {
            // Not reflected at all -> PARAM_FOUND only, per §4.4.
            return XssFinding(
                id = UUID.randomUUID().toString(),
                url = parameter.url,
                parameter = parameter.name,
                payload = payload,
                xssType = "reflected",
                context = "none",
                status = FindingStatus.PARAM_FOUND,
                riskScore = 0,
                evidence = "",
                requestSummary = "GET $testUrl",
                responseSummary = "HTTP ${success.statusCode}",
                detectedAt = System.currentTimeMillis()
            )
        }

        val htmlContext = htmlContextAnalyzer.detect(success.body, payload)
        val jsContext = jsContextAnalyzer.detect(success.body, payload)
        val (contextName, executable, encoded) = when {
            jsContext != null -> Triple("js_string", jsContext.executable, jsContext.encoded)
            htmlContext != null -> Triple(htmlContext.contextName, htmlContext.executable, htmlContext.encoded)
            else -> Triple("html_body", false, true)
        }

        val status = if (executable && !encoded) {
            FindingStatus.POSSIBLE_EXECUTABLE_CONTEXT
        } else {
            FindingStatus.REFLECTED
        }

        val evidence = ReflectionAnalyzer.snippetAround(success.body, payload)

        return XssFinding(
            id = UUID.randomUUID().toString(),
            url = parameter.url,
            parameter = parameter.name,
            payload = payload,
            xssType = "reflected",
            context = contextName,
            status = status,
            riskScore = riskScorer.score(status, contextName),
            evidence = evidence,
            requestSummary = "GET $testUrl",
            responseSummary = "HTTP ${success.statusCode} (${success.contentType ?: "unknown content-type"})",
            detectedAt = System.currentTimeMillis()
        )
    }

    private fun buildTestUrl(originalUrl: String, paramName: String, payload: String): String? {
        return runCatching {
            val uri = URI(originalUrl)
            val encodedPayload = URLEncoder.encode(payload, "UTF-8")
            val existingQuery = uri.rawQuery
                ?.split("&")
                ?.filterNot { it.startsWith("$paramName=") }
                ?.joinToString("&")
                .orEmpty()
            val newParam = "$paramName=$encodedPayload"
            val newQuery = if (existingQuery.isBlank()) newParam else "$existingQuery&$newParam"
            URI(uri.scheme, uri.authority, uri.path, newQuery, uri.fragment).toString()
        }.getOrNull()
    }
}

/**
 * TODO: needs confirmation — §7.2 defines Custom Header's on-disk format
 * ("Header: value", one per line) and its validation, but does not say
 * which layer turns that raw string into a Map<String, String> for
 * HttpClientPort. Parsing it here (and in AnalyzeDomXssUseCase's HTTP calls,
 * if any) keeps the raw string format a data/UI concern (SettingsDataStore)
 * while actual requests get a real header map. Invalid lines are skipped
 * defensively here; SettingsViewModel is the place that rejects invalid
 * lines at save-time (§7.2) so by the time a scan runs, config is assumed
 * already validated.
 */
internal object ScanConfigHeaders {
    fun parse(customHeaders: String): Map<String, String> {
        if (customHeaders.isBlank()) return emptyMap()
        return customHeaders.lines()
            .mapNotNull { line ->
                val idx = line.indexOf(':')
                if (idx <= 0) return@mapNotNull null
                val key = line.substring(0, idx).trim()
                val value = line.substring(idx + 1).trim()
                if (key.isBlank()) null else key to value
            }
            .toMap()
    }
}
