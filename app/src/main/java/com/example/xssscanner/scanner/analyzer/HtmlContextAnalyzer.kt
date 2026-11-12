package com.example.xssscanner.scanner.analyzer

import com.example.xssscanner.scanner.payload.PayloadEncoder
import javax.inject.Inject

data class HtmlReflectionContext(
    val contextName: String,   // "html_body" | "attribute"
    val executable: Boolean,
    val encoded: Boolean
)

/**
 * Determines whether a reflected payload landed inside an HTML tag
 * attribute vs. the plain HTML body, per §4.4.
 *
 * TODO: needs confirmation — real context breakout detection normally
 * needs an HTML-aware tokenizer; this uses a conservative heuristic (does
 * the nearest preceding unmatched `<` have an unmatched `"` or `'` after
 * it, i.e. are we inside a quoted attribute value) rather than a full
 * parser, to avoid overclaiming precision the spec does not ask us to
 * guarantee.
 */
class HtmlContextAnalyzer @Inject constructor() {

    fun detect(body: String, payload: String): HtmlReflectionContext? {
        val idx = body.indexOf(payload)
        if (idx < 0) return null

        val before = body.substring(0, idx)
        val lastOpenTag = before.lastIndexOf('<')
        val lastCloseTag = before.lastIndexOf('>')

        val insideTag = lastOpenTag > lastCloseTag
        val contextName = if (insideTag) "attribute" else "html_body"

        val encoded = PayloadEncoder.isEncodedAt(body, idx, payload.length)
        // Executable if reflected raw inside the body (new tags/attrs can be
        // injected) or raw inside an attribute value (can break out of it).
        val executable = !encoded

        return HtmlReflectionContext(contextName, executable, encoded)
    }
}
