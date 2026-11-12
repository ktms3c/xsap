package com.example.xssscanner.domain.usecase

import com.example.xssscanner.domain.model.FindingStatus
import com.example.xssscanner.domain.model.XssFinding
import com.example.xssscanner.webview.HiddenWebViewManager
import javax.inject.Inject

/**
 * Implements the dynamic confirmation half of §4.5 / §4.6. Only accepts
 * findings already at POSSIBLE_DOM_XSS or POSSIBLE_EXECUTABLE_CONTEXT, per
 * §8.3's contract; anything else is returned unchanged.
 *
 * TODO: needs confirmation — same cross-layer note as StartScanUseCase:
 * domain is not supposed to depend on Android framework types, but
 * HiddenWebViewManager (webview/) necessarily wraps android.webkit.WebView.
 * This file's own imports stay free of android.webkit (only the manager's
 * domain-typed suspend function is called), but the dependency graph does
 * reach Android through it. Flagged rather than silently routed around.
 */
class ConfirmExecutionUseCase @Inject constructor(
    private val hiddenWebViewManager: HiddenWebViewManager
) {
    suspend operator fun invoke(finding: XssFinding): XssFinding {
        if (finding.status != FindingStatus.POSSIBLE_DOM_XSS &&
            finding.status != FindingStatus.POSSIBLE_EXECUTABLE_CONTEXT
        ) {
            return finding
        }
        return hiddenWebViewManager.confirmExecution(finding)
    }
}
