-- SecureAuth Reference Roles & Non-Credential Seed Data
-- Note: Development user accounts are provisioned dynamically at startup by DevDataSeeder
-- using environment variables (APP_SEED_DEV_USERS=true) so no plaintext or production credentials are stored in SQL.

USE secureauth;

INSERT IGNORE INTO roles (role_name) VALUES ('ROLE_USER');
INSERT IGNORE INTO roles (role_name) VALUES ('ROLE_ADMIN');
