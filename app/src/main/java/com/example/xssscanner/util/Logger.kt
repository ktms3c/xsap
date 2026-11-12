package com.example.xssscanner.util

import android.util.Log

/**
 * Thin wrapper around android.util.Log so call sites don't hardcode a tag.
 * NEVER logs raw Cookie/Authorization/customHeaders values (§10 privacy
 * rules) — callers must pass already-masked strings.
 */
object Logger {
    private const val TAG = "XssScanner"

    fun i(message: String) = Log.i(TAG, message)
    fun w(message: String) = Log.w(TAG, message)
    fun e(message: String, throwable: Throwable? = null) = Log.e(TAG, message, throwable)
    fun d(message: String) = Log.d(TAG, message)
}
