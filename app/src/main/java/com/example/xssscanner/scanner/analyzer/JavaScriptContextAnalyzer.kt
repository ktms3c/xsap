package com.example.xssscanner.scanner.analyzer

import com.example.xssscanner.scanner.payload.PayloadEncoder
import javax.inject.Inject

data class JsReflectionContext(
    val executable: Boolean,
    val encoded: Boolean
)

/**
 * Detects reflection inside an inline <script> block / JS string literal,
 * per §4.4's "js_string" context.
 *
 * TODO: needs confirmation — heuristic: payload index falls between a
 * <script> open and the next </script>, same conservative approach as
 * HtmlContextAnalyzer (no real JS parser).
 */
class JavaScriptContextAnalyzer @Inject constructor() {

    fun detect(body: String, payload: String): JsReflectionContext? {
        val idx = body.indexOf(payload)
        if (idx < 0) return null

        val before = body.substring(0, idx)
        val lastScriptOpen = before.lastIndexOf("<script", ignoreCase = true)
        val lastScriptClose = before.lastIndexOf("</script", ignoreCase = true)
        val insideScript = lastScriptOpen > lastScriptClose
        if (!insideScript) return null

        val encoded = PayloadEncoder.isEncodedAt(body, idx, payload.length)
        return JsReflectionContext(executable = !encoded, encoded = encoded)
    }
}
