package com.example.xssscanner.webview

import android.webkit.WebResourceRequest
import android.webkit.WebResourceResponse
import android.webkit.WebView
import android.webkit.WebViewClient

/**
 * Implements §4.6: records requests via shouldInterceptRequest (returning
 * null so the WebView continues its normal load), and restricts navigation
 * to the whitelisted domain via shouldOverrideUrlLoading.
 */
class HiddenWebViewClient(
    private val requestRecorder: WebViewRequestRecorder,
    private val allowedHost: String,
    private val disableRedirect: Boolean
) : WebViewClient() {

    override fun shouldInterceptRequest(
        view: WebView,
        request: WebResourceRequest
    ): WebResourceResponse? {
        return requestRecorder.record(request)
    }

    override fun shouldOverrideUrlLoading(view: WebView, request: WebResourceRequest): Boolean {
        val host = request.url.host
        val inScope = host != null && (host == allowedHost || host.endsWith(".$allowedHost"))
        if (!inScope) return true // block navigation out of scope
        if (disableRedirect && request.url.toString() != view.url) return true
        return false
    }
}
