package com.example.security

import android.util.Base64
import org.json.JSONArray
import org.json.JSONObject
import java.security.MessageDigest
import java.security.SecureRandom
import java.util.UUID
import javax.crypto.Mac
import javax.crypto.spec.SecretKeySpec

data class JwtClaims(
    val subject: String,
    val userId: Long,
    val roles: List<String>,
    val jti: String,
    val issuedAtMs: Long,
    val expiresAtMs: Long,
    val rawToken: String
)

class JwtService(
    private val secretKeyBytes: ByteArray = defaultEphemeralOrKey(),
    private val defaultTtlMs: Long = 3_600_000L // 1 hour
) {
    fun generateToken(
        userId: Long,
        username: String,
        roles: List<String>,
        rememberMe: Boolean = false,
        nowMs: Long = System.currentTimeMillis()
    ): JwtClaims {
        val ttl = if (rememberMe) 7L * 24L * 3_600_000L else defaultTtlMs
        val expMs = nowMs + ttl
        val jti = UUID.randomUUID().toString()

        val headerJson = JSONObject().apply {
            put("alg", "HS256")
            put("typ", "JWT")
        }.toString()

        val payloadJson = JSONObject().apply {
            put("sub", username)
            put("uid", userId)
            put("roles", JSONArray(roles))
            put("jti", jti)
            put("iat", nowMs / 1000L)
            put("exp", expMs / 1000L)
            put("iss", "secureauth-platform")
        }.toString()

        val encodedHeader = base64UrlEncode(headerJson.toByteArray(Charsets.UTF_8))
        val encodedPayload = base64UrlEncode(payloadJson.toByteArray(Charsets.UTF_8))
        val signingInput = "$encodedHeader.$encodedPayload"
        val signature = hmacSha256(signingInput.toByteArray(Charsets.UTF_8))
        val token = "$signingInput.${base64UrlEncode(signature)}"

        return JwtClaims(
            subject = username,
            userId = userId,
            roles = roles,
            jti = jti,
            issuedAtMs = nowMs,
            expiresAtMs = expMs,
            rawToken = token
        )
    }

    fun parseAndVerify(token: String, nowMs: Long = System.currentTimeMillis()): JwtClaims? {
        val segments = token.split(".")
        if (segments.size != 3) return null
        val signingInput = "${segments[0]}.${segments[1]}"
        val expectedSig = hmacSha256(signingInput.toByteArray(Charsets.UTF_8))
        val actualSig = runCatching { base64UrlDecode(segments[2]) }.getOrNull() ?: return null
        if (!MessageDigest.isEqual(expectedSig, actualSig)) return null

        return runCatching {
            val header = JSONObject(String(base64UrlDecode(segments[0]), Charsets.UTF_8))
            if (header.optString("alg") != "HS256") return null
            val payload = JSONObject(String(base64UrlDecode(segments[1]), Charsets.UTF_8))
            val expMs = payload.getLong("exp") * 1000L
            if (nowMs >= expMs) return null

            val rolesArray = payload.getJSONArray("roles")
            val roles = buildList {
                for (i in 0 until rolesArray.length()) {
                    add(rolesArray.getString(i))
                }
            }
            JwtClaims(
                subject = payload.getString("sub"),
                userId = payload.getLong("uid"),
                roles = roles,
                jti = payload.getString("jti"),
                issuedAtMs = payload.getLong("iat") * 1000L,
                expiresAtMs = expMs,
                rawToken = token
            )
        }.getOrNull()
    }

    private fun hmacSha256(data: ByteArray): ByteArray {
        val mac = Mac.getInstance("HmacSHA256")
        mac.init(SecretKeySpec(secretKeyBytes, "HmacSHA256"))
        return mac.doFinal(data)
    }

    private fun base64UrlEncode(bytes: ByteArray): String =
        Base64.encodeToString(bytes, Base64.URL_SAFE or Base64.NO_WRAP or Base64.NO_PADDING)

    private fun base64UrlDecode(text: String): ByteArray =
        Base64.decode(text, Base64.URL_SAFE or Base64.NO_WRAP or Base64.NO_PADDING)

    companion object {
        private fun defaultEphemeralOrKey(): ByteArray {
            val bytes = ByteArray(32)
            SecureRandom().nextBytes(bytes)
            return bytes
        }
    }
}
