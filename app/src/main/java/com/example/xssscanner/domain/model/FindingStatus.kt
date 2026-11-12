package com.example.xssscanner.domain.model

enum class FindingStatus {
    PARAM_FOUND,
    PAYLOAD_SENT,
    REFLECTED,
    POSSIBLE_EXECUTABLE_CONTEXT,
    POSSIBLE_DOM_XSS,
    CONFIRMED_EXECUTION,
    BLOCKED,
    ERROR
}
