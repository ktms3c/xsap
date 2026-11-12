package com.example.xssscanner.webview

import androidx.webkit.WebMessageCompat
import androidx.webkit.WebViewCompat
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.receiveAsFlow

/**
 * Receives the "execution confirmed" callback from injected instrumentation
 * JS, per §4.6: postMessage()/WebMessagePort is used for JS<->native
 * communication — addJavascriptInterface() is never used on an untrusted
 * target page (§2). Origin is validated on every message.
 *
 * TODO(verify-api): WebViewCompat.addWebMessageListener /
 * WebViewCompat.WebMessageListener (androidx.webkit) requires checking
 * WebViewFeature.isFeatureSupported(WebViewFeature.WEB_MESSAGE_LISTENER)
 * before use on older API levels (§0.4 lists androidx-webkit specifically
 * for this). The exact listener callback signature is marked verify-api
 * since it is a less common API surface than core WebView methods.
 */
class ExecutionConfirmationBridge {

    private val channel = Channel<Boolean>(capacity = Channel.CONFLATED)
    val confirmations = channel.receiveAsFlow()

    // JS object name exposed to the page via WebMessageListener, NOT via
    // addJavascriptInterface (§2's explicit prohibition).
    companion object {
        const val BRIDGE_OBJECT_NAME = "xssScannerBridge"
    }

    fun handleMessage(message: WebMessageCompat, sourceOrigin: android.net.Uri, expectedOrigin: String) {
        if (sourceOrigin.toString().trimEnd('/') != expectedOrigin.trimEnd('/')) {
            // Origin validation failure: ignore the message entirely.
            return
        }
        val data = message.data
        if (data == "XSS_SCANNER_EXECUTION_CONFIRMED") {
            channel.trySend(true)
        }
    }
}
