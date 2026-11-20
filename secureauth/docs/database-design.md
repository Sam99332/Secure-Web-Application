# SecureAuth Database Design (MySQL 8)

## Entity-Relationship Summary
- `users` (1) <---> (M) `user_roles` (M) <---> (1) `roles`
- `users` (1) ---> (M) `password_reset_tokens` (`ON DELETE CASCADE`)
- `users` (1) ---> (M) `login_history` (`ON DELETE CASCADE`)
- `users` (1) ---> (M) `audit_logs` (`admin_id`, `ON DELETE RESTRICT`)
- `revoked_tokens` tracks invalidated JWT `jti` claims until natural expiration.

## Indexing & Constraints
- Unique constraints on `users.username`, `users.email`, `roles.role_name`, and `password_reset_tokens.token_hash`.
- Composite index `idx_login_history_user_time (user_id, login_time DESC)` for fast user activity feeds.
- Descending index `idx_audit_timestamp` for chronological admin audit inspection.
