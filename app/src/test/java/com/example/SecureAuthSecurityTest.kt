package com.example

import com.example.security.LoginRateLimiter
import com.example.security.PasswordSecurity
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SecureAuthSecurityTest {

    @Test
    fun `password strength evaluates weak and strong passwords`() {
        assertFalse(PasswordSecurity.evaluateStrength("12345").isAcceptable)
        assertFalse(PasswordSecurity.evaluateStrength("lowercaseonly1!").isAcceptable)
        assertTrue(PasswordSecurity.evaluateStrength("Enterprise@2026!").isAcceptable)
        assertEquals("Enterprise", PasswordSecurity.evaluateStrength("Enterprise@2026!").label)
    }

    @Test
    fun `sha256 hex produces deterministic 64 char digest`() {
        val digest = PasswordSecurity.sha256Hex("sample-reset-token")
        assertEquals(64, digest.length)
        assertEquals(digest, PasswordSecurity.sha256Hex("sample-reset-token"))
    }

    @Test
    fun `rate limiter locks after max failed attempts and resets on success`() {
        val limiter = LoginRateLimiter(maxAttempts = 3, windowMs = 60_000L, lockoutDurationMs = 30_000L)
        assertTrue(limiter.checkAllowed("user@test.com", 1000L).allowed)
        limiter.recordFailure("user@test.com", 1100L)
        limiter.recordFailure("user@test.com", 1200L)
        val afterThird = limiter.recordFailure("user@test.com", 1300L)
        assertFalse(afterThird.allowed)
        assertEquals(30L, afterThird.retryAfterSeconds)

        limiter.reset("user@test.com")
        assertTrue(limiter.checkAllowed("user@test.com", 1400L).allowed)
    }
}
