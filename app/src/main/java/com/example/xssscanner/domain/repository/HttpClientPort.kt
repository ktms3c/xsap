package com.example.xssscanner.domain.repository

import com.example.xssscanner.domain.model.ScanError

/**
 * Domain-level contract for making an HTTP request, per §0.2: domain must not
 * import OkHttp directly, so the concrete OkHttp call lives in
 * data/remote/HttpClientService.kt. HttpRequest/HttpResult are plain Kotlin
 * types (no OkHttp Request/Response leaking into domain or scanner/usecase code).
 */
interface HttpClientPort {

    suspend fun execute(request: HttpRequest): HttpResult

    data class HttpRequest(
        val url: String,
        val method: String = "GET",
        val headers: Map<String, String> = emptyMap(),
        val cookie: String? = null,
        val body: String? = null
    )

    sealed class HttpResult {
        data class Success(
            val statusCode: Int,
            val headers: Map<String, String>,
            val body: String,
            val contentType: String?
        ) : HttpResult()

        data class Failure(val error: ScanError) : HttpResult()
    }
}
