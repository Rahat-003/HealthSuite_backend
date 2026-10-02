# HealthSuite Backend

REST API and real-time signaling server for **HealthSuite**, a healthcare platform for Bangladesh. It covers:
- **Personal health records (PHR):** visits, diagnoses, medications and documents.
- **Family record sharing** with fine-grained grants.
- **Concierge appointment booking.**
- **A doctor marketplace** with WebRTC video consultations and digital prescriptions.
- **A public doctor directory.**

---

## Technology

| Area | Technology |
|---|---|
| Language / runtime | **Java 21** (virtual threads enabled) |
| Framework | **Spring Boot 3.3.5**: Web MVC, Data JPA, Security, Validation, WebSocket, Actuator |
| Database | **PostgreSQL 16** |
| Migrations | **Flyway 10** (schema is migration-driven; Hibernate only validates) |
| ORM | Hibernate 6 / Spring Data JPA, HikariCP connection pool |
| Auth | Spring Security, stateless **JWT** (jjwt 0.12.6): 15-min access token + rotating 7-day refresh token, role-based access |
| Real-time | Spring WebSocket: WebRTC signaling for video consultations at `/ws/consult` |
| PDF | OpenPDF 2.0.3 (prescription PDFs) |
| Scraping | jsoup 1.18.1, Playwright 1.44 (doctor directory) |
| API docs | springdoc-openapi 2.6 (Swagger UI) |
| Boilerplate | Lombok |
| File storage | Local disk (active); AWS S3 SDK 2.26 wired but disabled |
| Push notifications | Firebase Admin 9.3 (FCM), optional and disabled unless configured |
| Testing | JUnit 5, Spring Boot Test, Spring Security Test, H2 |
| Build / packaging | Maven, multi-stage Dockerfile, Docker Compose |

---

## Project structure

Feature-first packages under `src/main/java/com/healthsuite/`. Each one has `controller/`, `dto/`, `entity/`, `repository/` and `service/`:

| Package | Responsibility |
|---|---|
| `auth` | Registration, login, token refresh, profile, roles, admin user management, JWT & share-token security filters |
| `phr` | Medical visits, diagnoses, medications, documents, file storage |
| `family` | Family connections, relationship labels, record-share grants, share tokens |
| `marketplace` | Doctor profiles, specialties, availability, consultation requests, prescriptions, media, reviews, WebSocket signaling |
| `concierge` | Appointment-booking tickets and the support-agent queue |
| `scraper` | Public doctor directory and scraper |
| `notification` | In-app notifications |
| `common` | Security / Swagger / JPA config, response wrappers, exceptions, platform settings, validators |

Database migrations: `src/main/resources/db/migration/` (`V1` … `V15`).

---

## Prerequisites

| Tool | Version | Needed for |
|---|---|---|
| Docker + Docker Compose v2 | 24+ | PostgreSQL (and optionally the backend container) |
| JDK | 21 | running locally / from the IDE |
| Maven | 3.9+ | building and running locally |

### Ports

| Service | Port |
|---|---|
| Backend API | **20580** |
| PostgreSQL | **42900** (on the host *and* inside the container) |

---

## Configuration

Create a `.env` file in this directory. It's gitignored; never commit it:

```dotenv
DATABASE_USERNAME=postgres
DATABASE_PASSWORD=postgres
JWT_SECRET=change-me-to-a-random-string-of-at-least-32-chars
```

Generate a real secret with `openssl rand -hex 32`.

These are the only variables needed. `application.yml` has the same development defaults, so running from an IDE works even without them.

| Variable | Default | Description |
|---|---|---|
| `DATABASE_URL` | `jdbc:postgresql://localhost:42900/healthsuite` | JDBC URL |
| `DATABASE_USERNAME` | `postgres` | DB user |
| `DATABASE_PASSWORD` | `postgres` | DB password |
| `JWT_SECRET` | insecure dev fallback | HMAC key, min. 32 chars. **Always set in production** |
| `SERVER_PORT` | `20580` | HTTP port |
| `SPRING_PROFILES_ACTIVE` | `dev` (in Docker) | `dev` enables SQL + debug logging |

---

## Running

### Option A — Database in Docker, backend locally (recommended for development)

```bash
# 1. Start PostgreSQL
docker compose up -d postgres

# 2. Run the backend
mvn spring-boot:run
#    …or run HealthSuiteApplication from IntelliJ
```

### Option B — Everything in Docker

```bash
docker compose up -d --build
docker compose logs -f backend      # follow logs
```

The backend container waits for PostgreSQL's health check before it starts.

> Files uploaded to the containerised backend are stored inside the container and are lost when it is recreated. Use Option A when working with documents or consultation media.

### Verify

```bash
curl http://localhost:20580/actuator/health
# {"status":"UP"}
```

On first start, Flyway creates the whole schema automatically.

---

## API documentation

| | URL |
|---|---|
| Swagger UI | http://localhost:20580/swagger-ui.html |
| OpenAPI JSON | http://localhost:20580/v3/api-docs |

All responses use a common envelope:

```json
{ "success": true, "message": "optional", "data": { }, "timestamp": "2026-10-02T12:00:00" }
```

Errors return `{ path, status, error, message, fieldErrors? }`.

**Public endpoints:**
- `/api/auth/**`
- `GET /api/doctors/**`
- `GET /api/marketplace/doctors`
- `GET /api/marketplace/specialties`
- `GET /api/phr/shared/**`
- `/files/**`
- `/actuator/health`

Everything else needs a `Authorization: Bearer <access-token>` header.

---

## Roles and first admin

Roles: `ROLE_USER`, `ROLE_PREMIUM`, `ROLE_SUPPORT`, `ROLE_DOCTOR`, `ROLE_ADMIN`. New sign-ups get `ROLE_USER`.

No admin account is created automatically. To make one:
1. Register through the API or the web app.
2. Grant the role in the database. See [`manual-role-grant.md`](manual-role-grant.md). Quick version:

```bash
docker exec -it healthsuite-postgres psql -p 42900 -U postgres -d healthsuite
```

```sql
INSERT INTO user_roles (user_id, role_id)
SELECT u.id, r.id FROM users u, roles r
WHERE u.email = 'you@example.com' AND r.name = 'ROLE_ADMIN';
```

Roles are carried inside the JWT, so log out and back in after changing them.

---

## Development

```bash
mvn -q compile -DskipTests     # fast compile check
mvn test                       # tests (H2 in-memory, Flyway disabled)
mvn package -DskipTests        # build target/health-suite-backend-1.0.0.jar
```

- **Schema changes:** add a new `V{next}__description.sql` migration. Never edit a migration that has already been applied.
- **File storage:**
  - Uploads go to `~/HealthSuite/uploads/` and are served at `/files/**`.
  - Consultation media goes to `~/HealthSuite/user_consult/`.

### Useful Docker commands

```bash
docker compose stop                 # stop containers, keep data
docker compose down                 # remove containers, keep data
docker compose down -v              # remove containers AND the database volume (wipes all data)
```

---

## Not yet enabled

TURN server for WebRTC, online payments (bKash / Rocket / Nagad), S3 storage, push notifications (Firebase), Google / Facebook login. The code for S3, Firebase and social login exists but is switched off in `application.yml`.
