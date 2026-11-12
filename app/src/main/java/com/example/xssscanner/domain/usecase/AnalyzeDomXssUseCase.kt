package com.example.xssscanner.domain.usecase

import com.example.xssscanner.domain.model.CrawlResult
import com.example.xssscanner.domain.model.FindingStatus
import com.example.xssscanner.domain.model.XssFinding
import com.example.xssscanner.scanner.analyzer.DomSourceSinkAnalyzer
import com.example.xssscanner.scanner.analyzer.RiskScorer
import java.util.UUID
import javax.inject.Inject

/**
 * Implements the static half of §4.5. Only produces a POSSIBLE_DOM_XSS
 * candidate from static source->sink matching; dynamic confirmation
 * (WebView) is ConfirmExecutionUseCase's job, never this one's.
 */
class AnalyzeDomXssUseCase @Inject constructor(
    private val domSourceSinkAnalyzer: DomSourceSinkAnalyzer,
    private val riskScorer: RiskScorer
) {
    operator fun invoke(crawlResult: CrawlResult): XssFinding? {
        val jsBody = crawlResult.takeIf { it.contentType?.contains("javascript") == true }
            ?: return null

        // TODO: needs confirmation — AnalyzeDomXssUseCase's input is typed
        // as CrawlResult in §8.3, but (like DiscoverParametersUseCase) the
        // domain CrawlResult model carries no raw JS source text. The actual
        // static scan of JS content happens in CrawlEngine while the body is
        // available (see scanner/crawler/CrawlEngine.kt), which calls
        // DomSourceSinkAnalyzer directly and emits any resulting finding
        // into the crawl's event stream. This use case is kept as a thin,
        // spec-conformant entry point but cannot do real work without a body.
        val candidate = domSourceSinkAnalyzer.analyzeStatic(sourceCode = "", sourceUrl = jsBody.url)
            ?: return null

        return XssFinding(
            id = UUID.randomUUID().toString(),
            url = jsBody.url,
            parameter = candidate.sourceExpression,
            payload = "",
            xssType = "dom",
            context = candidate.sinkExpression,
            status = FindingStatus.POSSIBLE_DOM_XSS,
            riskScore = riskScorer.score(FindingStatus.POSSIBLE_DOM_XSS, candidate.sinkExpression),
            evidence = candidate.evidenceLine,
            requestSummary = "GET ${jsBody.url}",
            responseSummary = "static JS analysis",
            detectedAt = System.currentTimeMillis()
        )
    }
}
