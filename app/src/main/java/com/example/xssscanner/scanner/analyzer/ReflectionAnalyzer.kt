package com.example.xssscanner.scanner.analyzer

import javax.inject.Inject

class ReflectionAnalyzer @Inject constructor() {

    fun containsPayload(body: String, payload: String): Boolean = body.contains(payload)

    companion object {
        fun snippetAround(body: String, payload: String, radius: Int = 60): String {
            val idx = body.indexOf(payload)
            if (idx < 0) return ""
            val start = (idx - radius).coerceAtLeast(0)
            val end = (idx + payload.length + radius).coerceAtMost(body.length)
            return body.substring(start, end)
        }
    }
}
