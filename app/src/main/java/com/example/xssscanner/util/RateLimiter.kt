package com.example.xssscanner.util

import kotlinx.coroutines.delay
import java.util.concurrent.atomic.AtomicLong
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Used by HttpClientService AND combined with worker-count bounding in
 * ScanOrchestrator (§6 requires both controls active simultaneously). Must
 * be a single shared instance (its AtomicLong state is the rate limit), so
 * it is @Singleton + constructor-injected rather than instantiated ad hoc —
 * without @Inject here, Hilt cannot resolve HttpClientService's dependency
 * on this class at all (this was caught during audit; the original draft
 * omitted it).
 *
 * TODO: needs confirmation — §6 does not specify a target requests/second
 * figure, only that a RateLimiter must exist. A conservative default of
 * ~5 requests/second (200ms min interval) is used; this should be made
 * configurable if the spec author wants a different ceiling.
 *
 * TODO(verify-api): a Kotlin default-value constructor parameter (e.g.
 * `minIntervalMillis: Long = 200L`) is NOT honored by Dagger/Hilt's
 * generated Java-style constructor call — Hilt would instead look for an
 * unqualified Long binding and fail with a missing-binding error. The
 * interval is hardcoded as a private val instead of a constructor
 * parameter for exactly this reason.
 */
@Singleton
class RateLimiter @Inject constructor() {

    private val minIntervalMillis: Long = 200L
    private val lastRequestAt = AtomicLong(0L)

    suspend fun acquire() {
        while (true) {
            val now = System.currentTimeMillis()
            val last = lastRequestAt.get()
            val elapsed = now - last
            if (elapsed >= minIntervalMillis) {
                if (lastRequestAt.compareAndSet(last, now)) return
            } else {
                delay(minIntervalMillis - elapsed)
            }
        }
    }
}
