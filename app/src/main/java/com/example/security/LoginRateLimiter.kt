package com.example.security

import java.util.concurrent.ConcurrentHashMap

class LoginRateLimiter(
    private val maxAttempts: Int = 5,
    private val windowMs: Long = 5 * 60_000L,
    private val lockoutDurationMs: Long = 2 * 60_000L
) {
    private data class AttemptState(
        val failureTimestamps: MutableList<Long> = mutableListOf(),
        var lockedUntilMs: Long = 0L
    )

    private val attemptsByKey = ConcurrentHashMap<String, AttemptState>()

    fun checkAllowed(identifier: String, nowMs: Long = System.currentTimeMillis()): RateLimitStatus {
        val key = identifier.trim().lowercase()
        val state = attemptsByKey[key] ?: return RateLimitStatus(allowed = true, remainingAttempts = maxAttempts)

        synchronized(state) {
            if (state.lockedUntilMs > nowMs) {
                val waitSec = ((state.lockedUntilMs - nowMs) / 1000L).coerceAtLeast(1L)
                return RateLimitStatus(
                    allowed = false,
                    remainingAttempts = 0,
                    retryAfterSeconds = waitSec
                )
            }
            state.failureTimestamps.removeAll { nowMs - it > windowMs }
            return RateLimitStatus(
                allowed = true,
                remainingAttempts = (maxAttempts - state.failureTimestamps.size).coerceAtLeast(0)
            )
        }
    }

    fun recordFailure(identifier: String, nowMs: Long = System.currentTimeMillis()): RateLimitStatus {
        val key = identifier.trim().lowercase()
        val state = attemptsByKey.getOrPut(key) { AttemptState() }

        synchronized(state) {
            state.failureTimestamps.removeAll { nowMs - it > windowMs }
            state.failureTimestamps.add(nowMs)
            if (state.failureTimestamps.size >= maxAttempts) {
                state.lockedUntilMs = nowMs + lockoutDurationMs
                state.failureTimestamps.clear()
                val waitSec = (lockoutDurationMs / 1000L).coerceAtLeast(1L)
                return RateLimitStatus(allowed = false, remainingAttempts = 0, retryAfterSeconds = waitSec)
            }
            return RateLimitStatus(
                allowed = true,
                remainingAttempts = (maxAttempts - state.failureTimestamps.size).coerceAtLeast(0)
            )
        }
    }

    fun reset(identifier: String) {
        attemptsByKey.remove(identifier.trim().lowercase())
    }
}

data class RateLimitStatus(
    val allowed: Boolean,
    val remainingAttempts: Int,
    val retryAfterSeconds: Long = 0L
)
