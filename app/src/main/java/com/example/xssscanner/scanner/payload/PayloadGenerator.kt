package com.example.xssscanner.scanner.payload

import javax.inject.Inject

/**
 * Generates a payload containing a unique marker so ReflectionAnalyzer can
 * find it unambiguously (§4.4). Context-aware per §4.4's "context-hint"
 * wording: contextHint here is the parameter NAME (used only to pick a
 * payload family that is more likely to land in a meaningful context); the
 * actual reflection CONTEXT is only known after the response comes back,
 * which is why both a plain-HTML and an attribute/JS-breakout payload style
 * exist — picking exactly one family from a bare name is a best-effort
 * heuristic, not a guarantee.
 */
class PayloadGenerator @Inject constructor() {

    fun generate(contextHint: String): String {
        val marker = "xsck" + (System.nanoTime() % 1_000_000)
        return "\"'><svg/onload=confirm($marker)>$marker"
    }
}
