package com.example.xssscanner.data.remote

import com.example.xssscanner.domain.model.ScanError
import com.example.xssscanner.domain.repository.HttpClientPort
import com.example.xssscanner.util.RateLimiter
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.Call
import okhttp3.Headers
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.IOException
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Implements HttpClientPort using the single shared OkHttpClient from
 * HttpClientModule. Applies a RateLimiter (§6: must be active alongside
 * worker-count bounding, never alone) before every request, and a limited
 * retry count with basic backoff per §6.
 */
@Singleton
class HttpClientService @Inject constructor(
    private val baseClient: OkHttpClient,
    private val rateLimiter: RateLimiter
) : HttpClientPort {

    override suspend fun execute(request: HttpClientPort.HttpRequest): HttpClientPort.HttpResult {
        rateLimiter.acquire()
        return withContext(Dispatchers.IO) {
            var lastError: IOException? = null
            // §6: "a limited retry count (small fixed value, e.g. 1-2x with backoff)"
            repeat(2) { attempt ->
                try {
                    return@withContext performCall(request)
                } catch (e: IOException) {
                    lastError = e
                    if (attempt == 0) {
                        kotlinx.coroutines.delay(300L)
                    }
                }
            }
            HttpClientPort.HttpResult.Failure(
                ScanError.NetworkError(lastError?.message ?: "request failed")
            )
        }
    }

    private fun performCall(request: HttpClientPort.HttpRequest): HttpClientPort.HttpResult {
        val headersBuilder = Headers.Builder()
        request.headers.forEach { (k, v) -> headersBuilder.add(k, v) }
        request.cookie?.let { headersBuilder.add("Cookie", it) }

        val okRequest = Request.Builder()
            .url(request.url)
            .headers(headersBuilder.build())
            .method(request.method, null)
            .build()

        val timedClient: OkHttpClient = baseClient.newBuilder()
            .callTimeout(30, TimeUnit.SECONDS)
            .build()

        val call: Call = timedClient.newCall(okRequest)
        call.execute().use { response ->
            val body = response.body?.string().orEmpty()
            return HttpClientPort.HttpResult.Success(
                statusCode = response.code,
                headers = response.headers.toMultimap().mapValues { it.value.joinToString(", ") },
                body = body,
                contentType = response.header("Content-Type")
            )
        }
    }
}
