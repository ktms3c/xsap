package com.example.xssscanner.webview

/**
 * Builds the instrumentation JS injected into the candidate page to verify
 * a source actually flows into a sink (§4.5's dynamic step). This hooks
 * only (wraps the sink function to detect the marker reaching it) without
 * altering the target app's own logic, per §4.6.
 *
 * TODO: needs confirmation — §4.5 does not specify exact hook
 * implementation; this uses a conservative approach: monkey-patch the
 * named sink to detect the test marker, then call through to the original
 * so the page's real behavior is preserved, and postMessage back to native
 * via the ExecutionConfirmationBridge's expected origin only.
 */
object DomInstrumentation {

    fun buildHookScript(marker: String, sinkExpression: String, expectedOrigin: String): String {
        // Only a small, fixed set of sinks is ever hooked (§4.5's own list);
        // sinkExpression is not interpolated as arbitrary attacker input.
        val safeSink = sinkExpression.takeIf { it in KNOWN_SINKS } ?: return ""
        return """
            (function() {
              try {
                var marker = ${org.json.JSONObject.quote(marker)};
                var original = window.$safeSink;
                if (typeof original !== 'function') return;
                window.$safeSink = function() {
                  var args = Array.prototype.slice.call(arguments);
                  var joined = args.join(' ');
                  if (joined.indexOf(marker) !== -1) {
                    window.postMessage('XSS_SCANNER_EXECUTION_CONFIRMED', '$expectedOrigin');
                  }
                  return original.apply(this, args);
                };
              } catch (e) { /* instrumentation must never crash the page */ }
            })();
        """.trimIndent()
    }

    private val KNOWN_SINKS = setOf(
        "innerHTML", "outerHTML", "eval", "Function", "setTimeout", "setInterval"
        // document.write/writeln/setAttribute/insertAdjacentHTML are member
        // calls, not bare globals, and are not safely monkey-patchable this
        // way — TODO: needs confirmation on a per-sink hooking strategy for
        // those if dynamic confirmation of them is required.
    )
}
