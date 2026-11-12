package com.example.xssscanner.ui.common

import com.example.xssscanner.R
import com.example.xssscanner.data.remote.HttpResponseAnalyzer
import com.example.xssscanner.domain.model.FindingStatus
import com.example.xssscanner.domain.model.LogLevel

/**
 * Maps a ScanLog/XssFinding to one of the color resources declared in §5's
 * colors.xml block — the ONLY place those color values are defined, per
 * that file's own comment.
 */
object LogColorMapper {

    fun colorForLevel(level: LogLevel): Int = when (level) {
        LogLevel.INFO -> R.color.log_info
        LogLevel.SUCCESS -> R.color.log_success
        LogLevel.WARNING -> R.color.log_warning
        LogLevel.ERROR -> R.color.log_error
    }

    fun colorForStatusCode(statusCode: Int?): Int? {
        if (statusCode == null) return null
        return when (HttpResponseAnalyzer.statusBucket(statusCode)) {
            "2xx" -> R.color.log_http_2xx
            "3xx" -> R.color.log_http_3xx
            "4xx" -> R.color.log_http_4xx
            "5xx" -> R.color.log_http_5xx
            else -> null
        }
    }

    fun colorForFindingStatus(status: FindingStatus): Int = when (status) {
        FindingStatus.PARAM_FOUND -> R.color.log_param_found
        FindingStatus.PAYLOAD_SENT -> R.color.log_payload_sent
        FindingStatus.REFLECTED -> R.color.log_reflected
        FindingStatus.POSSIBLE_EXECUTABLE_CONTEXT, FindingStatus.POSSIBLE_DOM_XSS -> R.color.log_possible
        FindingStatus.CONFIRMED_EXECUTION -> R.color.log_confirmed_execution
        FindingStatus.BLOCKED -> R.color.log_blocked
        FindingStatus.ERROR -> R.color.log_error
    }
}
