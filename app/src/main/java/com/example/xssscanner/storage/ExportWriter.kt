package com.example.xssscanner.storage

/**
 * Minimal shared CSV-escaping helper so CsvExporter has one place that
 * defines "how a field gets escaped," per the zero-silent-conflict policy.
 */
object ExportWriter {
    fun csvEscape(value: String): String {
        val needsQuoting = value.contains(",") || value.contains("\"") || value.contains("\n")
        val escaped = value.replace("\"", "\"\"")
        return if (needsQuoting) "\"$escaped\"" else escaped
    }
}
