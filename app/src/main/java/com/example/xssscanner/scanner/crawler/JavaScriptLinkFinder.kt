package com.example.xssscanner.scanner.crawler

import com.example.xssscanner.domain.model.DiscoveredEndpoint
import javax.inject.Inject

/**
 * Implements §4.2's "Enable LinkFinder in JS files": statically extracts
 * URL/API path-like string literals from downloaded .js source via
 * regex/parsing only. JS is never eval'd/executed outside HiddenWebView.
 */
class JavaScriptLinkFinder @Inject constructor() {

    // TODO: needs confirmation — this regex targets quoted strings that look
    // like a path or absolute URL; it is intentionally conservative (fewer
    // false positives) rather than exhaustive. fetch()/XHR/axios call sites
    // specifically (§4.3) are a subset matched by the same pattern when the
    // URL argument is a string literal; dynamically built URLs are not found
    // by static regex matching and are out of scope here.
    private val pathPattern = Regex("""["'](/[a-zA-Z0-9_\-./]+|https?://[a-zA-Z0-9_\-./%?&=]+)["']""")

    fun find(jsSourceUrl: String, jsSource: String): List<DiscoveredEndpoint> {
        return pathPattern.findAll(jsSource)
            .map { it.groupValues[1] }
            .distinct()
            .map { DiscoveredEndpoint(rawPath = it, sourceJsUrl = jsSourceUrl) }
            .toList()
    }
}
