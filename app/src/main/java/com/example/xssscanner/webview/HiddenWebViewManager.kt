package com.example.xssscanner.webview

import android.content.Context
import android.view.ViewGroup
import android.webkit.CookieManager
import android.webkit.WebView
import com.example.xssscanner.domain.model.FindingStatus
import com.example.xssscanner.domain.model.XssFinding
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withTimeoutOrNull
import java.net.URI
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Off-screen WebView per §4.6: attached to a container that is never added
 * to a visible Activity (not relying on visibility = GONE alone).
 *
 * TODO: needs confirmation — WebView must be created on the main thread;
 * this class exposes a suspend function and internally hops to
 * Dispatchers.Main for the WebView-touching parts via
 * withContext(Dispatchers.Main), which is the standard pattern, but is
 * marked here since §4.6 doesn't spell out threading explicitly.
 */
@Singleton
class HiddenWebViewManager @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private var webView: WebView? = null
    private var bridge: ExecutionConfirmationBridge? = null
    private var container: ViewGroup? = null

    suspend fun confirmExecution(finding: XssFinding): XssFinding {
        return withContext(Dispatchers.Main) {
            val host = runCatching { URI(finding.url).host }.getOrNull() ?: return@withContext finding
            val expectedOrigin = runCatching { URI(finding.url) }.getOrNull()
                ?.let { "${it.scheme}://${it.host}${if (it.port != -1) ":${it.port}" else ""}" }
                ?: return@withContext finding

            val recorder = WebViewRequestRecorder()
            val confirmationBridge = ExecutionConfirmationBridge()
            bridge = confirmationBridge

            val wv = createOrReuseWebView(host, disableRedirect = false)
            webView = wv

            // TODO(verify-api): real origin-validated registration uses
            // WebViewCompat.addWebMessageListener(wv, BRIDGE_OBJECT_NAME,
            // setOf(expectedOrigin), listener) guarded by
            // WebViewFeature.isFeatureSupported(...). Wiring the exact
            // listener callback is left as verify-api per ExecutionConfirmationBridge's
            // own note, since this is a less-common androidx.webkit surface.

            val marker = "xsconf" + finding.id.take(8)
            val hookScript = DomInstrumentation.buildHookScript(marker, finding.context, expectedOrigin)

            wv.loadUrl(finding.url)

            val confirmed = withTimeoutOrNull(10_000L) {
                if (hookScript.isNotBlank()) {
                    wv.evaluateJavascript(hookScript, null)
                }
                confirmationBridge.confirmations.first()
            } ?: false

            cleanup()

            if (confirmed) {
                finding.copy(status = FindingStatus.CONFIRMED_EXECUTION)
            } else {
                finding // left unchanged: "indicated but execution unverified" (§4.5)
            }
        }
    }

    private fun createOrReuseWebView(allowedHost: String, disableRedirect: Boolean): WebView {
        val wv = WebView(context)
        wv.settings.javaScriptEnabled = true
        wv.settings.setAllowFileAccess(false) // §4.6
        wv.settings.allowContentAccess = false
        wv.webViewClient = HiddenWebViewClient(WebViewRequestRecorder(), allowedHost, disableRedirect)

        // §4.6: off-screen, never added to a visible Activity. A 1x1
        // container that is never attached to any parent view hierarchy.
        val offscreenContainer = android.widget.FrameLayout(context)
        offscreenContainer.layoutParams = ViewGroup.LayoutParams(1, 1)
        offscreenContainer.addView(wv)
        container = offscreenContainer

        return wv
    }

    private fun cleanup() {
        val wv = webView ?: return
        wv.stopLoading()
        wv.clearHistory()
        wv.clearCache(true)
        CookieManager.getInstance().let { cm ->
            // TODO(verify-api): clearing cookies for a specific domain only
            // (vs. all cookies) is not a single documented CookieManager
            // call; removeAllCookies() is used here as the conservative,
            // guaranteed-correct option since this WebView is single-use
            // and off-screen.
            cm.removeAllCookies(null)
        }
        wv.destroy()
        webView = null
        container = null
        bridge = null
    }
}
