# Campus Board

A small public campus board built with Vue, Spring Boot, and PostgreSQL. Visitors
can read the newest 50 posts. Registered users can publish posts and manage only
their own posts; the API enforces ownership independently of the browser UI.

## Project layout

- `frontend/` — Vue 3 single-page application, built with Vite.
- `backend/` — Spring Boot REST API, JWT authentication, JPA, and Flyway.
- `schema.sql` — optional standalone PostgreSQL schema setup.
- `Dockerfile` — builds the frontend and packages it into the Spring Boot app.
- `render.yaml` — Render Blueprint for the single web service.
- `scripts/smoke-test.ps1` — end-to-end API and ownership check.

## Run locally

Requirements: JDK 17 or newer, Maven 3.9 or newer, and Node.js 20.19 or newer
(Node 24 is recommended), or Bun 1.3.14. You do not need a local PostgreSQL
installation; use a PostgreSQL database such as Neon.

1. Copy `.env.example` to `.env`, fill in the database credentials, and generate
   a fresh Base64 signing key:

   ```powershell
   node -e "console.log(require('node:crypto').randomBytes(32).toString('base64'))"
   ```

2. Load the environment variables and start the API:

   ```powershell
   Get-Content .env | ForEach-Object {
       if ($_ -match '^\s*([^#=]+)=(.*)$') {
           [Environment]::SetEnvironmentVariable($matches[1].Trim(), $matches[2], 'Process')
       }
   }
   Set-Location backend
   mvn spring-boot:run
   ```

   Flyway creates the schema at startup. The default API port is `18707`.

3. In another terminal, start the Vue development server:

   ```powershell
   Set-Location frontend
   bun install --frozen-lockfile
   bun run dev
   ```

   Open the Vite URL shown in the terminal. `/api` requests are proxied to the
   local Spring Boot server. If using Node.js and npm instead, run `npm install`
   and `npm run dev`. Build with `bun run build` (or `npm run build`); the
   Docker build packages the generated assets into Spring.

Run the backend tests from `backend/` with `mvn test`. With the API running, run
the end-to-end check from the project root:

```powershell
.\scripts\smoke-test.ps1
```

The smoke test creates two uniquely named test accounts (which remain in the
database) and a temporary post. It uses the second account to verify that
another author receives `403` when attempting to edit the post, then deletes
the post as its owner. If a check fails after post creation, the script also
attempts cleanup and warns if it cannot remove the test post.

## Deploy to Render and Neon

1. Create a PostgreSQL database in Neon and use its **direct** connection
   string, including `sslmode=require`.
2. Push this project to a Git repository and create a Render Blueprint from
   `render.yaml`, or create a Docker web service with the repository root as its
   build context.
3. Set `DB_URL`, `DB_USERNAME`, `DB_PASSWORD`, and `JWT_SECRET` in Render. Use
   a newly generated Base64 secret that decodes to at least 32 random bytes.
   Do not put real credentials in the repository.
4. Deploy and verify `/actuator/health`, then open `/swagger-ui.html` on the
   deployed service to exercise registration, login, and the post endpoints.

The Docker image builds the Vue app and serves it and the API from one Spring
Boot service. Render supplies `PORT`; the application honors it automatically.
The health endpoint is exposed without database details.

## API

| Method | Path | Access | Purpose |
| --- | --- | --- | --- |
| `POST` | `/api/auth/register` | Public | Register an account |
| `POST` | `/api/auth/login` | Public | Sign in and receive a JWT |
| `GET` | `/api/me` | Authenticated | Return the current profile |
| `GET` | `/api/posts` | Public | List the latest 50 posts |
| `GET` | `/api/posts/{id}` | Public | Read a post |
| `POST` | `/api/posts` | Authenticated | Create a post as the signed-in user |
| `PUT` | `/api/posts/{id}` | Owner only | Update title and content |
| `DELETE` | `/api/posts/{id}` | Owner only | Delete a post |

Passwords must be at least 12 characters and at most 72 UTF-8 bytes. Post
requests accept only `title` and `content`; ownership is derived from the
verified JWT. API errors have a JSON body with `status`, `code`, `message`, and
`path`.
