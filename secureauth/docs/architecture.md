# SecureAuth System Architecture

## Overview
SecureAuth follows a layered enterprise architecture separating the React SPA presentation tier, Spring Boot 3 REST API & security filter chain, and normalized MySQL 8 persistence tier.

## Layers
1. **Presentation Tier (React + Vite + Tailwind + Axios)**
   - Uses HttpOnly `SameSite=Lax` cookies (`withCredentials: true`) so JWTs are never exposed to `localStorage` or XSS scripts.
   - Automatic CSRF token header injection (`X-XSRF-TOKEN`) and global 401 session expiration interception.
   - Role-aware route guards (`ProtectedRoute`, `AdminRoute`) paired with server-side authorization enforcement.

2. **Application & Security Tier (Spring Boot 3 + Spring Security 6)**
   - Stateless filter chain (`JwtAuthenticationFilter`) validates HS256 JWT signature, expiration, and revoked-JTI denylist (`revoked_tokens`).
   - Sliding-window `LoginRateLimiterService` mitigates credential stuffing and brute-force attacks.
   - `GlobalExceptionHandler` normalizes validation and domain errors into `ApiResponse<T>` without leaking stack traces or SQL diagnostics.

3. **Persistence Tier (Spring Data JPA + Hibernate + Flyway + MySQL 8)**
   - Parameterized JPQL and derived JPA repository queries prevent SQL injection.
   - Flyway migrations (`V1__init_schema.sql`, `V2__seed_roles.sql`) enforce reproducible schema evolution.
