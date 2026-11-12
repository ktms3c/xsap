package com.example.xssscanner.data.remote

/**
 * Small shared helpers for classifying an HTTP status code, used by
 * ui/common/LogColorMapper (§5) to bucket codes into 2xx/3xx/4xx/5xx.
 */
object HttpResponseAnalyzer {
    fun statusBucket(statusCode: Int): String = when (statusCode) {
        in 200..299 -> "2xx"
        in 300..399 -> "3xx"
        in 400..499 -> "4xx"
        in 500..599 -> "5xx"
        else -> "unknown"
    }
}
