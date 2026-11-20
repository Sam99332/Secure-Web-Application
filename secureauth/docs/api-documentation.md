# SecureAuth REST API Documentation

Base URL: `http://localhost:8080/api`
Interactive Swagger UI: `http://localhost:8080/swagger-ui.html`
OpenAPI JSON: `http://localhost:8080/v3/api-docs`

## Authentication Endpoints
- `POST /api/auth/register` — Register a new `ROLE_USER` account and set `SECUREAUTH_ACCESS_TOKEN` HttpOnly cookie.
- `POST /api/auth/login` — Authenticate by email or username, record `login_history`, enforce rate limits, and issue HttpOnly JWT cookie.
- `POST /api/auth/logout` — Persist JWT `jti` in `revoked_tokens` and clear cookie.
- `POST /api/auth/forgot-password` — Issue 15-minute SHA-256 hashed reset token (anti-enumeration safe).
- `POST /api/auth/reset-password` — Validate single-use token and update BCrypt password hash.

## User Endpoints (`ROLE_USER` or `ROLE_ADMIN`)
- `GET /api/users/me` — Fetch authenticated user profile.
- `PUT /api/users/me` — Update own `fullName`, `email`, and `avatarUrl`.
- `PUT /api/users/me/password` — Verify current password and rotate to new BCrypt hash.
- `GET /api/users/me/activity` — Retrieve own login history.

## Admin Endpoints (`ROLE_ADMIN` required via `@PreAuthorize`)
- `GET /api/admin/dashboard` — Total/active/inactive/admin counts + recent registrations.
- `GET /api/admin/users?search=&status=&role=&page=0&size=10&sortBy=createdAt&sortDir=desc` — Paginated, filtered, sorted user directory.
- `GET /api/admin/users/{id}` — Inspect user details.
- `PUT /api/admin/users/{id}` — Edit user info + log `USER_UPDATED`.
- `PATCH /api/admin/users/{id}/status` — Activate/deactivate user + log `STATUS_CHANGED`.
- `PATCH /api/admin/users/{id}/roles` — Assign roles + log `ROLES_UPDATED`.
- `DELETE /api/admin/users/{id}` — Delete user + log `USER_DELETED`.
- `GET /api/admin/audit-logs?page=0&size=20` — Paginated admin audit trail.
