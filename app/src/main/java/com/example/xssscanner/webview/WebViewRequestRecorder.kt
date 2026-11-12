package com.example.xssscanner.webview

import android.webkit.WebResourceRequest
import android.webkit.WebResourceResponse
import java.util.concurrent.ConcurrentLinkedQueue

data class RecordedRequest(val url: String, val method: String)

/**
 * Records requests observed via shouldInterceptRequest (§4.6) without
 * altering them. Used for diagnostics/evidence; always returns null from
 * the recording call site so the WebView continues its default load.
 */
class WebViewRequestRecorder {
    private val requests = ConcurrentLinkedQueue<RecordedRequest>()

    fun record(request: WebResourceRequest): WebResourceResponse? {
        requests.add(RecordedRequest(url = request.url.toString(), method = request.method))
        return null
    }

    fun snapshot(): List<RecordedRequest> = requests.toList()

    fun clear() = requests.clear()
}
