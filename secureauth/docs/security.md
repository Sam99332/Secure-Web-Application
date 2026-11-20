# SecureAuth Security Architecture & Controls

1. **Password Security**: `BCryptPasswordEncoder(12)` with per-password random salt; passwords are never returned in DTOs or logged.
2. **Token Storage & Transport**: JWTs are issued inside `HttpOnly`, `SameSite=Lax` (configurable `Secure=true` in HTTPS production) cookies rather than `localStorage`.
3. **CSRF & CORS**: Spring Security CSRF `CookieCsrfTokenRepository.withHttpOnlyFalse()` paired with explicit `CORS_ALLOWED_ORIGINS` allowlist and `allowCredentials=true`.
4. **JWT Revocation**: Logout stores the token's `jti` in `revoked_tokens` until expiry so logged-out cookies cannot be replayed.
5. **SQL Injection Prevention**: Strict Spring Data JPA parameterized JPQL queries (`@Param`) and Bean Validation (`@Valid`) on all request DTOs.
6. **Mass Assignment Protection**: Dedicated immutable Java `record` DTOs prevent binding `roles` or `accountStatus` during public registration or self profile updates.
7. **Account Enumeration Prevention**: Login returns identical messages for unknown users, wrong passwords, and inactive accounts; forgot-password always returns a generic 200 notice.
8. **Brute-Force Rate Limiting**: `LoginRateLimiterService` locks an identifier for 120s after 5 failures within a 5-minute window and returns HTTP `429 Too Many Requests`.
9. **HTTP Security Headers**: Enforces `X-Content-Type-Options: nosniff`, `X-Frame-Options: DENY`, `Referrer-Policy`, and `Content-Security-Policy`.
