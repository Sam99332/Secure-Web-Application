package com.secureauth.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Date;
import java.util.List;
import java.util.UUID;

@Component
public class JwtTokenProvider {

    private final SecretKey secretKey;
    private final long defaultExpirationMs;
    private final long rememberMeExpirationMs;

    public JwtTokenProvider(
        @Value("${app.jwt.secret}") String secret,
        @Value("${app.jwt.expiration-ms}") long defaultExpirationMs,
        @Value("${app.jwt.remember-me-expiration-ms}") long rememberMeExpirationMs
    ) {
        byte[] bytes = secret.getBytes(StandardCharsets.UTF_8);
        if (bytes.length < 32) {
            throw new IllegalStateException("JWT_SECRET must be at least 32 bytes for HS256.");
        }
        this.secretKey = Keys.hmacShaKeyFor(bytes);
        this.defaultExpirationMs = defaultExpirationMs;
        this.rememberMeExpirationMs = rememberMeExpirationMs;
    }

    public record GeneratedToken(String token, String jti, Instant expiresAt) {}

    public GeneratedToken generateToken(Long userId, String username, List<String> roles, boolean rememberMe) {
        Instant now = Instant.now();
        long ttl = rememberMe ? rememberMeExpirationMs : defaultExpirationMs;
        Instant expiresAt = now.plusMillis(ttl);
        String jti = UUID.randomUUID().toString();

        String token = Jwts.builder()
            .id(jti)
            .subject(username)
            .claim("uid", userId)
            .claim("roles", roles)
            .issuedAt(Date.from(now))
            .expiration(Date.from(expiresAt))
            .signWith(secretKey)
            .compact();

        return new GeneratedToken(token, jti, expiresAt);
    }

    public Claims parseClaims(String token) throws JwtException {
        return Jwts.parser()
            .verifyWith(secretKey)
            .build()
            .parseSignedClaims(token)
            .getPayload();
    }

    public boolean validateToken(String token) {
        try {
            Claims claims = parseClaims(token);
            return claims.getExpiration().after(new Date());
        } catch (JwtException | IllegalArgumentException ex) {
            return false;
        }
    }
}
