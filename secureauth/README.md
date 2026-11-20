# SecureAuth – Secure User Management & Authentication System

Enterprise-grade Identity & Access Management (IAM) full-stack application featuring **React.js (Vite + Tailwind CSS)**, **Java 17 + Spring Boot 3**, **Spring Security 6 + HttpOnly JWT**, **BCrypt hashing**, **Flyway**, and **MySQL 8**.

## Objectives
- Implement production-grade authentication, authorization (RBAC), and user lifecycle management.
- Demonstrate defense-in-depth security controls (BCrypt, HttpOnly cookies, CSRF, rate limiting, anti-enumeration, SQL injection prevention, audit logging).
- Provide clean RESTful APIs documented via Swagger/OpenAPI with automated JUnit 5, Mockito, MockMvc, and Vitest test coverage.

## Core Features
- **Public Flows**: Enterprise Landing Page, Registration with live password-strength meter, Login with Remember Me & brute-force rate limiting, Forgot/Reset Password with SHA-256 single-use expiring tokens.
- **User Dashboard**: View account metadata, update profile/avatar, rotate password, inspect personal login activity history, and revoke session on logout.
- **Admin Console**: Overview metrics (total/active/inactive/admins), paginated/searchable/filterable/sortable User Management, role assignment (`ROLE_USER`, `ROLE_ADMIN`), status toggling, deletion with confirmation dialogs, and immutable Admin Audit Logs.
- **UI/UX**: Light & Dark mode, responsive sidebar + mobile navigation, loading states, toast notifications, and confirmation dialogs.

## Technology Stack
- **Frontend**: React 18, Vite, Tailwind CSS, Axios, React Router DOM, React Hook Form, Lucide Icons, Vitest + React Testing Library.
- **Backend**: Java 17, Spring Boot 3.3, Spring Security 6, Spring Data JPA, Hibernate, Maven, JJWT, SpringDoc OpenAPI (Swagger).
- **Database**: MySQL 8, Flyway Migrations (`V1__init_schema.sql`, `V2__seed_roles.sql`).
- **DevOps**: Docker, Docker Compose, GitHub-ready `.env.example` & `.gitignore`.

## System Architecture & Database Design
- See [`docs/architecture.md`](docs/architecture.md)
- See [`docs/database-design.md`](docs/database-design.md)
- See [`docs/api-documentation.md`](docs/api-documentation.md)
- See [`docs/security.md`](docs/security.md)

## Folder Structure
```text
secureauth/
├── frontend/                 # React + Vite + Tailwind SPA
├── backend/                  # Spring Boot 3 + Spring Security + Flyway API
├── database/                 # Standalone MySQL schema & seed scripts
├── docs/                     # Architecture, DB, API, Security docs & Postman collection
├── screenshots/              # Visual verification guide
├── docker-compose.yml
├── Dockerfile
├── .env.example
└── README.md
```

## Setup & Running Locally

### 1. MySQL 8 Configuration
```sql
CREATE DATABASE secureauth CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
CREATE USER 'secureauth_user'@'localhost' IDENTIFIED BY 'YourStrongLocalDbPass!';
GRANT ALL PRIVILEGES ON secureauth.* TO 'secureauth_user'@'localhost';
FLUSH PRIVILEGES;
```
Flyway automatically applies `V1__init_schema.sql` and `V2__seed_roles.sql` on Spring Boot startup.

### 2. Environment Variables & Development Seeding
Copy `.env.example` to `.env` (and `frontend/.env.example` to `frontend/.env`):
```bash
cp .env.example .env
cp frontend/.env.example frontend/.env
```
To auto-seed development demo accounts safely on first boot without hardcoding credentials in source code, set in your local `.env`:
```env
APP_SEED_DEV_USERS=true
SEED_ADMIN_EMAIL=admin@secureauth.dev
SEED_ADMIN_PASSWORD=ChooseYourDevAdminPassword1!
SEED_USER_EMAIL=alex@secureauth.dev
SEED_USER_PASSWORD=ChooseYourDevUserPassword1!
```

### 3. Run Backend (Spring Boot 3)
```bash
cd backend
mvn spring-boot:run
```
Swagger UI: `http://localhost:8080/swagger-ui.html`

### 4. Run Frontend (React + Vite)
```bash
cd frontend
npm install
npm run dev
```
Open: `http://localhost:5173`

### 5. Run with Docker Compose
```bash
docker compose up --build
```

## Testing Instructions
- **Backend Unit & Integration Tests (JUnit 5 + Mockito + MockMvc + H2)**:
  ```bash
  cd backend
  mvn test
  ```
- **Frontend Tests (Vitest + Testing Library)**:
  ```bash
  cd frontend
  npm install
  npm test
  ```
- **Postman**: Import `docs/SecureAuth.postman_collection.json`.

## Future Enhancements
- TOTP / WebAuthn Two-Factor Authentication (2FA).
- Refresh token rotation with Redis-backed distributed rate limiting.
- SMTP email provider integration (SendGrid / SES) for password reset delivery.

## License
Licensed under the [MIT License](LICENSE).
