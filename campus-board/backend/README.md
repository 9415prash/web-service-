# Campus Board API

Spring Boot REST API for account registration, JWT sign-in, and public posts with
author-only updates and deletion. PostgreSQL schema changes are applied by
Flyway at startup; Hibernate validates the resulting schema.

## Requirements

- Java 17 or newer
- Maven 3.9 or newer
- A Neon PostgreSQL database (the backend does not start a local database)

## Configure and run

Copy the project-root `.env.example` to `.env` and fill in the Neon connection
and JWT secret. Spring Boot does not load `.env` files automatically. In
PowerShell, load that file into the current terminal before starting the API:

```text
DB_URL=jdbc:postgresql://YOUR_DIRECT_NEON_HOST/neondb?sslmode=require
DB_USERNAME=YOUR_NEON_ROLE
DB_PASSWORD=YOUR_NEON_PASSWORD
JWT_SECRET=BASE64_ENCODED_RANDOM_SECRET
```

Generate a signing key with Node.js:

```powershell
node -e "console.log(require('node:crypto').randomBytes(32).toString('base64'))"
```

Do not commit `.env`, database credentials, or the signing secret. Neon should
use a direct (non-pooled) connection URL for this JDBC setup. Flyway creates the
tables on the first run. If you already ran the root `schema.sql`, the initial
migration safely reuses those tables.

From the project root:

```powershell
Get-Content .env | ForEach-Object {
    if ($_ -match '^\s*([^#=]+)=(.*)$') {
        [Environment]::SetEnvironmentVariable($matches[1].Trim(), $matches[2], 'Process')
    }
}
Set-Location backend
mvn spring-boot:run
mvn test
```

The API listens on port `18707` locally (or the `PORT`/`SERVER_PORT` environment
variable). Health is available at `http://localhost:18707/actuator/health`, and
interactive API documentation is at `http://localhost:18707/swagger-ui.html`.

## API

| Method | Path | Access | Result |
| --- | --- | --- | --- |
| `POST` | `/api/auth/register` | Public | Create an account; returns `201` |
| `POST` | `/api/auth/login` | Public | Verify credentials and return `accessToken` |
| `GET` | `/api/me` | Bearer token | Return the signed-in profile |
| `GET` | `/api/posts` | Public | List posts newest first |
| `GET` | `/api/posts/{id}` | Public | Read a post |
| `POST` | `/api/posts` | Bearer token | Create a post; author comes from the token |
| `PUT` | `/api/posts/{id}` | Owner only | Update title and content |
| `DELETE` | `/api/posts/{id}` | Owner only | Delete; returns `204` |

Registration passwords must contain at least 12 characters and no more than 72
UTF-8 bytes (BCrypt's limit). Post request bodies accept only `title` and
`content`; clients cannot assign `ownerId`. Errors use a JSON body containing
`status`, `code`, `message`, and `path`. The list endpoint returns only the
latest 50 posts. See the project-root [README](../README.md) for the complete
frontend, local development, and Render deployment workflow.
