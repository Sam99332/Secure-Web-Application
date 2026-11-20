package com.example.security

import android.util.Base64
import java.security.MessageDigest
import java.security.SecureRandom
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.PBEKeySpec

data class PasswordStrengthResult(
    val score: Int, // 0..4
    val label: String,
    val hasMinLength: Boolean,
    val hasUppercase: Boolean,
    val hasLowercase: Boolean,
    val hasDigit: Boolean,
    val hasSpecial: Boolean
) {
    val isAcceptable: Boolean
        get() = hasMinLength && hasUppercase && hasLowercase && hasDigit && hasSpecial
}

object PasswordSecurity {
    private const val ALGORITHM = "PBKDF2WithHmacSHA256"
    private const val ITERATIONS = 120_000
    private const val KEY_LENGTH_BITS = 256
    private const val SALT_BYTES = 16
    private val secureRandom = SecureRandom()

    fun hashPassword(rawPassword: String): String {
        val salt = ByteArray(SALT_BYTES).also { secureRandom.nextBytes(it) }
        val derived = pbkdf2(rawPassword.toCharArray(), salt, ITERATIONS, KEY_LENGTH_BITS)
        val saltB64 = Base64.encodeToString(salt, Base64.NO_WRAP)
        val hashB64 = Base64.encodeToString(derived, Base64.NO_WRAP)
        return "\$pbkdf2\$$ITERATIONS\$$saltB64\$$hashB64"
    }

    fun verifyPassword(rawPassword: String, storedHash: String): Boolean {
        val parts = storedHash.split("$")
        // Expected format: ["", "pbkdf2", iterations, saltB64, hashB64]
        if (parts.size != 5 || parts[1] != "pbkdf2") return false
        val iterations = parts[2].toIntOrNull() ?: return false
        val salt = runCatching { Base64.decode(parts[3], Base64.NO_WRAP) }.getOrNull() ?: return false
        val expected = runCatching { Base64.decode(parts[4], Base64.NO_WRAP) }.getOrNull() ?: return false
        val actual = pbkdf2(rawPassword.toCharArray(), salt, iterations, expected.size * 8)
        return MessageDigest.isEqual(actual, expected)
    }

    fun sha256Hex(input: String): String {
        val digest = MessageDigest.getInstance("SHA-256").digest(input.toByteArray(Charsets.UTF_8))
        return digest.joinToString("") { "%02x".format(it) }
    }

    fun generateSecureToken(byteLength: Int = 24): String {
        val bytes = ByteArray(byteLength).also { secureRandom.nextBytes(it) }
        return Base64.encodeToString(bytes, Base64.URL_SAFE or Base64.NO_WRAP or Base64.NO_PADDING)
    }

    fun evaluateStrength(password: String): PasswordStrengthResult {
        val hasMinLength = password.length >= 8
        val hasUppercase = password.any { it.isUpperCase() }
        val hasLowercase = password.any { it.isLowerCase() }
        val hasDigit = password.any { it.isDigit() }
        val hasSpecial = password.any { !it.isLetterOrDigit() }

        val metCount = listOf(hasMinLength, hasUppercase, hasLowercase, hasDigit, hasSpecial).count { it }
        val score = when {
            password.isEmpty() -> 0
            !hasMinLength || metCount <= 2 -> 1
            metCount == 3 || metCount == 4 -> 2
            password.length >= 12 && metCount == 5 -> 4
            else -> 3
        }
        val label = when (score) {
            0 -> "Empty"
            1 -> "Weak"
            2 -> "Fair"
            3 -> "Strong"
            else -> "Enterprise"
        }
        return PasswordStrengthResult(
            score = score,
            label = label,
            hasMinLength = hasMinLength,
            hasUppercase = hasUppercase,
            hasLowercase = hasLowercase,
            hasDigit = hasDigit,
            hasSpecial = hasSpecial
        )
    }

    private fun pbkdf2(password: CharArray, salt: ByteArray, iterations: Int, keyLengthBits: Int): ByteArray {
        val spec = PBEKeySpec(password, salt, iterations, keyLengthBits)
        val factory = SecretKeyFactory.getInstance(ALGORITHM)
        return factory.generateSecret(spec).encoded
    }
}
