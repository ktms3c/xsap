package com.example.xssscanner.scanner.analyzer

import javax.inject.Inject

data class DomXssCandidate(
    val sourceExpression: String,
    val sinkExpression: String,
    val evidenceLine: String
)

/**
 * Implements the static half of §4.5: looks for attacker-controllable
 * sources flowing into dangerous sinks within the same line (a simple,
 * auditable heuristic rather than full data-flow analysis across variables
 * — §4.5 allows "traceable through a variable via static analysis" as an
 * extension, marked here as a TODO since cross-line taint tracking is a
 * much larger undertaking than a line-level regex scan).
 */
class DomSourceSinkAnalyzer @Inject constructor() {

    private val sources = listOf(
        "location.href", "location.search", "location.hash", "document.referrer",
        "postMessage", "localStorage", "sessionStorage"
    )
    private val sinks = listOf(
        "innerHTML", "outerHTML", "document.write", "document.writeln",
        "eval", "Function", "setTimeout", "setInterval", "setAttribute",
        "insertAdjacentHTML"
    )

    fun analyzeStatic(sourceCode: String, sourceUrl: String): DomXssCandidate? {
        if (sourceCode.isBlank()) return null

        sourceCode.lineSequence().forEach { line ->
            val matchedSource = sources.firstOrNull { line.contains(it) }
            val matchedSink = sinks.firstOrNull { line.contains(it) }
            if (matchedSource != null && matchedSink != null) {
                return DomXssCandidate(
                    sourceExpression = matchedSource,
                    sinkExpression = matchedSink,
                    evidenceLine = line.trim().take(300)
                )
            }
        }
        // TODO: needs confirmation — cross-line/variable-traced source->sink
        // detection (the "traceable through a variable" clause of §4.5) is
        // not implemented; only same-line matches are reported.
        return null
    }
}
