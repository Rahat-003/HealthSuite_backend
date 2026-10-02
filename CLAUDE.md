# HealthSuite Backend

Spring Boot 3.3.5 · Java 21 (virtual threads on) · PostgreSQL 16 · Flyway · Spring Security + JWT (jjwt) · Lombok · springdoc OpenAPI.

## Commands

```bash
docker compose up -d postgres            # DB on 42900 (host and container); reads .env
mvn spring-boot:run                      # app on :20580 (override with SERVER_PORT)
mvn -q compile -DskipTests               # fast typecheck — run after every change
mvn -q package -DskipTests               # rebuild jar (a stale jar 404s new endpoints)
mvn test                                 # unit tests (*Test, Mockito) — no Docker
mvn verify                               # + integration tests (*IT, Testcontainers Postgres) + JaCoCo — needs Docker
```

- Swagger UI: `http://localhost:20580/swagger-ui.html` · health: `/actuator/health`
- `.env` (gitignored) holds only `DATABASE_USERNAME`, `DATABASE_PASSWORD` (`postgres`/`postgres`) and `JWT_SECRET`, the same values `application.yml` falls back to, so an IDE run needs no env vars. Don't add S3/AWS/Firebase/Google/Facebook vars back: the app doesn't read them while those blocks stay commented out in `application.yml`. Without `.env`, compose starts Postgres with a blank password and it crash-loops ("container is unhealthy").

## Docker

- `docker-compose.yml` has two services. Day to day only `postgres` runs in Docker, and the backend runs from IntelliJ.
- **Postgres listens on 42900 inside the container too** (`command: postgres -p 42900`, mapped `42900:42900`):
  - The health check and any `psql` inside the container need `-p 42900`: `docker exec -it healthsuite-postgres psql -p 42900 -U postgres -d healthsuite`.
  - `docker ps` still shows `5432/tcp`. That's just image metadata; nothing listens there.
- From the host, the JDBC URL is `jdbc:postgresql://localhost:42900/healthsuite` (the `application.yml` default). The `backend` container instead uses `jdbc:postgresql://postgres:42900/healthsuite` (service name, set in compose).
- `docker compose up -d` with no service name also builds and starts the backend container on 20580. Don't do that while the IntelliJ backend is running, or the two fight over the port.
- Uploads inside the backend container go to `/root/HealthSuite/...` with no volume, so they're lost when the container is recreated. Use the IntelliJ run for anything involving files.
- Data lives in the `backend_postgres_data` volume. `docker compose down -v` deletes it, so don't use `-v` unless a wipe is wanted. There are no seeded users: register through the UI, then grant roles per `manual-role-grant.md`.

## Package layout

Feature-first under `com.healthsuite`. Every feature has `controller/ dto/{request,response}/ entity/ enums/ repository/ service/`:

| Package | Owns |
|---|---|
| `auth` | users, roles, login/register/refresh, profile, admin user mgmt; `auth/security` = JWT + share-token filters, `UserPrincipal` |
| `phr` | medical visits, diagnoses, medications, documents; file storage (`LocalFileStorageServiceImpl` active, S3 impl present) |
| `family` | family connections, relationship labels, record-share grants (Nothing / All / Selected), share tokens |
| `marketplace` | doctor profiles, specialties, availability, consultation requests, prescriptions (PDF via OpenPDF), media, reviews; `marketplace/ws` = WebRTC signaling at `/ws/consult` |
| `concierge` | booking tickets with a SUPPORT-role queue |
| `scraper` | public doctor directory (`/api/doctors`), jsoup/Playwright scraping |
| `notification` | in-app notifications |
| `common` | `config/` (Security, JPA auditing, Swagger, storage), `exception/`, `response/`, `settings/` (platform settings), `util/SecurityUtils`, `validation/@BangladeshPhone` |

The empty `api/`, `domain/`, `dto/`, `service/`, `security/`, `config/`, `exception/` dirs at the package root are leftovers from an old layered layout. **Don't put code there.**

## Conventions

- **Controllers** return `ResponseEntity<ApiResponse<T>>` built with `ApiResponse.ok(data)` / `ok("message", data)` / `ok("message")`; return `201` for creates. Paginated results use `PagedResponse.from(page)`. Get the caller with `@AuthenticationPrincipal UserPrincipal principal` and pass `principal.getId()` into the service.
- **Services** own authorization checks on records (ownership, family grants, share tokens) — don't rely on URL rules alone. Use `@Transactional` on writes and `@Transactional(readOnly = true)` on reads. Constructor injection via `@RequiredArgsConstructor`.
- **DTOs** are separate from entities; validate request DTOs with `@Valid` + Jakarta constraints (`@BangladeshPhone` for phone numbers).
- **Errors:** throw subclasses of `AppException` (`ResourceNotFoundException`, `BadRequestException`, `ConflictException`, `UnauthorizedException`, `FamilyAccessDeniedException`, `PremiumRequiredException`, `ShareTokenException`) — `GlobalExceptionHandler` maps them to `ErrorResponse`. Don't build error bodies in controllers.
- **Entities** extend `common/entity/BaseEntity` for audit columns (`created_at/by`, `updated_at/by`). PKs are `Long` identity. A UUID-PK refactor was considered and rejected; don't propose it again.
- Add Swagger `@Tag` on controllers and `@Operation` where the behaviour isn't obvious.

## Database / Flyway

- Schema lives **only** in `src/main/resources/db/migration/V{n}__snake_case.sql`. `ddl-auto: validate` means an entity that doesn't match the schema fails startup.
- Add a new `V{next}` file for every change and never edit an applied migration (`validate-on-migrate: true` rejects checksum changes). The latest is **V15**.
- Integration tests run every Flyway migration against a real Testcontainers PostgreSQL 16, so a broken migration fails `mvn verify` (H2 was removed).

## Tests

- **Unit tests** are named `*Test` and run in Surefire: JUnit 5, Mockito and AssertJ, with no Spring context.
- **Integration tests** are named `*IT` and run in Failsafe. Annotate them with `support/IntegrationTest` (full context, `test` profile, shared Postgres container from `support/PostgresContainerConfig`).
- Integration tests share one database and don't roll back, so use `support/TestData.email()` / `TestData.phone()` for unique users. Never rely on table counts.
- For HTTP-level tests, add `@AutoConfigureMockMvc` and get a real JWT through `/api/auth/register` or `/api/auth/login`. `@WithMockUser` doesn't produce a `UserPrincipal`, so controllers that read `principal.getId()` would hit a null pointer.
- Testcontainers is pinned to 1.21.4 in `pom.xml`, because Docker 29 rejects the older client API. Don't drop the override.
- CI is `.github/workflows/ci.yml`: `mvn verify` on every push and PR, and on `main` it also pushes the image to `ghcr.io/rahat-003/healthsuite-backend`.
- There's no seeded admin. Grant ADMIN/SUPPORT/DOCTOR by inserting into `user_roles` — see `manual-role-grant.md`.

## Security

- URL rules live in `common/config/SecurityConfig.java`: `/api/auth/**`, `GET /api/doctors/**`, `GET /api/marketplace/doctors[/*]`, `GET /api/marketplace/specialties`, `GET /api/phr/shared/**`, `/files/**` and `/ws/**` are public. `/api/doctor/**` needs DOCTOR and `/api/admin/**` needs ADMIN; everything else needs authentication.
- **A new public endpoint must be added to `SecurityConfig` explicitly.**
- Missing or expired tokens return **401** (not 403) so the frontend refresh interceptor works; keep it that way.
- The WebSocket at `/ws/consult` authenticates the JWT in `ConsultHandshakeInterceptor`, not in the filter chain.
- Refresh tokens rotate on every `/api/auth/refresh`.

## Storage

- Uploads go to `~/HealthSuite/uploads` (served at `/files/**`). Consultation recordings and Rx copies go to `~/HealthSuite/user_consult/{YYYYMMDD}/`.
- S3, Firebase and social login have code but are switched off (their `application.yml` blocks are commented out). Firebase's only use is `concierge/service/FcmNotificationService`, which sends a push when support confirms a concierge ticket; without config it logs a skip and returns. The `firebase-service-account.json` mount was removed from compose.

## Not done yet

TURN server for WebRTC, payments (bKash/Rocket/Nagad), S3 storage, push notifications, Google/Facebook login.
