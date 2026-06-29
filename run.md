# Running HealthSuite Backend

## Prerequisites

| Tool | Minimum version | Notes |
|------|----------------|-------|
| Docker | 24+ | with Docker Compose v2 (`docker compose`) |
| Java | 21 | only needed for local/IDE run |
| Maven | 3.9+ | only needed for local/IDE run |

---

## Option 1 — Docker Compose (recommended)

This is the standard way to run the full stack (PostgreSQL + backend) in one command.

### 1. Copy and fill in the environment file

```bash
cp .env.example .env
```

Open `.env` and set at minimum:

| Variable | How to get it |
|----------|--------------|
| `DATABASE_PASSWORD` | Any strong password |
| `JWT_SECRET` | `openssl rand -hex 32` |
| `AWS_ACCESS_KEY` / `AWS_SECRET_KEY` | IAM user with S3 read/write on the bucket |
| `S3_BUCKET_NAME` | Your S3 bucket name |
| `GOOGLE_CLIENT_ID` | Google Cloud Console → OAuth 2.0 Client ID |
| `FACEBOOK_APP_ID` / `FACEBOOK_APP_SECRET` | Meta for Developers → App settings |
| `DOCTOR_SCRAPER_URL_1` | Target doctor-listing URL to scrape |

### 2. (Optional) Add Firebase for push notifications

Place your Firebase service account JSON file at the project root:

```
health_suite/firebase-service-account.json
```

If this file is absent the app starts normally — push notifications are silently skipped.

### 3. Start

```bash
docker compose up --build
```

- PostgreSQL starts first; the backend waits for its health check before connecting.
- Flyway runs all 5 migrations automatically on first boot.
- The API is available at `http://localhost:8080`.

### Useful commands

```bash
# Run in background
docker compose up --build -d

# Follow logs
docker compose logs -f backend

# Stop everything
docker compose down

# Stop and delete the database volume (full reset)
docker compose down -v

# Rebuild only the backend image
docker compose build backend && docker compose up -d backend
```

---

## Option 2 — Local (database only in Docker)

Use this when developing with an IDE and you want fast rebuild cycles.

### 1. Start only PostgreSQL

```bash
docker compose up -d postgres
```

### 2. Copy and fill in `.env` as above, then export the variables

```bash
set -a && source .env && set +a
```

Or set them directly in your IDE run configuration.

### 3. Run the backend

```bash
cd backend
mvn spring-boot:run
```

Or run `HealthSuiteApplication.java` from your IDE with the environment variables configured.

The API is available at `http://localhost:8080`.

---

## Option 3 — Tests only (no external services needed)

Tests use an H2 in-memory database and stub credentials — no Docker or real AWS/Firebase required.

```bash
cd backend
mvn test
```

The `test` profile is activated automatically via `@ActiveProfiles("test")` in the test class.

---

## API Documentation (Swagger UI)

Once the app is running, open:

```
http://localhost:8080/swagger-ui.html
```

To call authenticated endpoints:
1. `POST /api/auth/register` or `POST /api/auth/login` to get an `accessToken`.
2. Click **Authorize** in the top-right of the Swagger UI.
3. Paste the token (without the `Bearer ` prefix) into the `bearerAuth` field.

OpenAPI JSON spec: `http://localhost:8080/v3/api-docs`

---

## Health check

```bash
curl http://localhost:8080/actuator/health
```

Expected response:

```json
{"status": "UP"}
```

---

## First-time setup notes

- **Database schema**: Flyway runs migrations from `backend/src/main/resources/db/migration/` automatically. Never edit migration files after they have been applied.
- **S3 bucket**: The bucket must exist before the app starts. CORS and bucket policy are your responsibility.
- **Doctor scraper**: Runs automatically at **02:00 every night** (cron `0 0 2 * * *`). The first scrape will not run until that time unless triggered manually.
- **Social auth**: If `GOOGLE_CLIENT_ID` or Facebook credentials are left blank, those login endpoints return an error at runtime but do not prevent the app from starting.

---

## Environment variable reference

| Variable | Required | Default | Description |
|----------|----------|---------|-------------|
| `DATABASE_URL` | Yes | `jdbc:postgresql://localhost:5432/healthsuite` | Full JDBC URL |
| `DATABASE_USERNAME` | Yes | `postgres` | DB username |
| `DATABASE_PASSWORD` | Yes | — | DB password |
| `JWT_SECRET` | Yes | dev fallback (insecure) | HMAC-SHA256 key, min 32 chars |
| `S3_BUCKET_NAME` | Yes | `healthsuite-documents` | S3 bucket for uploaded documents |
| `AWS_REGION` | Yes | `ap-southeast-1` | AWS region |
| `AWS_ACCESS_KEY` | Yes | — | AWS access key ID |
| `AWS_SECRET_KEY` | Yes | — | AWS secret access key |
| `FIREBASE_SERVICE_ACCOUNT_PATH` | No | — | Path to Firebase JSON; omit to disable FCM |
| `GOOGLE_CLIENT_ID` | No | — | OAuth 2.0 client ID for Google login |
| `FACEBOOK_APP_ID` | No | — | Facebook App ID for social login |
| `FACEBOOK_APP_SECRET` | No | — | Facebook App Secret |
| `DOCTOR_SCRAPER_URL_1` | No | — | URL for the nightly doctor scraper |
| `SPRING_PROFILES_ACTIVE` | No | `dev` | Active Spring profile |
