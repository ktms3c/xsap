package com.example.xssscanner.scanner.payload

/**
 * Encoding helpers used both to build payload variants and, on the analysis
 * side, to recognize whether a reflected value was HTML/JS-escaped.
 */
object PayloadEncoder {

    fun htmlEncode(input: String): String = input
        .replace("&", "&amp;")
        .replace("<", "&lt;")
        .replace(">", "&gt;")
        .replace("\"", "&quot;")
        .replace("'", "&#x27;")

    fun jsStringEscape(input: String): String = input
        .replace("\\", "\\\\")
        .replace("'", "\\'")
        .replace("\"", "\\\"")

    /** Returns true if the dangerous characters <, >, ", ', &, \ appear
     * encoded (not raw) at/around the given index in [body]. Used by the
     * context analyzers per §4.4's "minimum check of the encoding state". */
    fun isEncodedAt(body: String, markerIndex: Int, markerLength: Int): Boolean {
        if (markerIndex < 0) return true
        val window = body.substring(
            (markerIndex - 5).coerceAtLeast(0),
            (markerIndex + markerLength + 5).coerceAtMost(body.length)
        )
        val rawDangerous = listOf("<", ">", "\"", "'", "&", "\\")
        val hasRawDangerous = rawDangerous.any { window.contains(it) && !window.contains("&amp;") }
        val hasEntity = window.contains("&lt;") || window.contains("&gt;") ||
            window.contains("&quot;") || window.contains("&#x27;") || window.contains("&amp;")
        return hasEntity || !hasRawDangerous
    }
}
