package com.example.xssscanner.scanner.analyzer

import com.example.xssscanner.domain.model.FindingStatus
import javax.inject.Inject

/**
 * TODO: needs confirmation — §4 does not define an explicit risk-scoring
 * formula; §3.1's mockup shows a severity label ("Medium") per finding.
 * A simple, documented 0-100 scale is used: the finding's status dominates,
 * with a small adjustment for context. This is the single place risk score
 * is computed, so the formula can be revised without touching call sites.
 */
class RiskScorer @Inject constructor() {

    fun score(status: FindingStatus, context: String): Int {
        val base = when (status) {
            FindingStatus.CONFIRMED_EXECUTION -> 95
            FindingStatus.POSSIBLE_EXECUTABLE_CONTEXT -> 75
            FindingStatus.POSSIBLE_DOM_XSS -> 70
            FindingStatus.REFLECTED -> 40
            FindingStatus.PARAM_FOUND -> 5
            FindingStatus.PAYLOAD_SENT -> 5
            FindingStatus.BLOCKED -> 0
            FindingStatus.ERROR -> 0
        }
        val contextBonus = when (context) {
            "html_body" -> 10
            "attribute" -> 5
            "js_string" -> 10
            else -> 0
        }
        return (base + contextBonus).coerceIn(0, 100)
    }

    fun severityLabel(score: Int): String = when {
        score >= 80 -> "Critical"
        score >= 60 -> "High"
        score >= 30 -> "Medium"
        score > 0 -> "Low"
        else -> "Info"
    }
}
