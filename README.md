# book-lending-srv

REST microservice for managing a small library's book lending operations. Tracks books, members, and loans with configurable borrowing rules.

Built with [Quarkus 3.28](https://quarkus.io/), Java 17, MySQL 8.

## Quick start
Use Docker to spin up MySQL client or install MySQL 8.x standalone
```bash
# docker MySQL container
docker compose up -d

# dev mode (needs Docker for the MySQL container / standalone MySQL port on 3307)
./gradlew quarkusDev

# or, bring your own MySQL from docker
docker compose up -d
./gradlew quarkusBuild
java -jar build/quarkus-app/quarkus-run.jar
```

The app listens on **http://localhost:8088** by default.

## What it does

- CRUD for **books** and **members**
- **Borrow / return** workflow with three enforced rules:
  - a member can hold at most `lending.max-active-loans` books (default 3)
  - no borrowing while the member has overdue loans
  - no borrowing when no copies are available
- Role-based access via HTTP Basic Auth (admin vs user)
- Flyway-managed schema, Hibernate ORM Panache repositories
- Health checks, Prometheus metrics, OpenAPI spec + Swagger UI

## Endpoints

Everything under `/api/*` requires Basic Auth. Observability endpoints are unauthenticated.

### Books

| Method | Path | Roles | Notes |
|--------|------|-------|-------|
| GET | `/api/books` | admin, user | list all |
| GET | `/api/books/{id}` | admin, user | |
| POST | `/api/books` | admin | |
| PUT | `/api/books/{id}` | admin | adjusts available copies proportionally |
| DELETE | `/api/books/{id}` | admin | blocked while copies are on loan |

### Members

| Method | Path | Roles |
|--------|------|-------|
| GET | `/api/members` | admin |
| GET | `/api/members/{id}` | admin |
| POST | `/api/members` | admin |
| PUT | `/api/members/{id}` | admin |
| DELETE | `/api/members/{id}` | admin (blocked if member has active loans) |

### Loans

| Method | Path | Roles | Notes |
|--------|------|-------|-------|
| GET | `/api/loans` | admin | query params: `memberId`, `active` |
| GET | `/api/loans/{id}` | admin, user | |
| POST | `/api/loans/borrow` | admin, user | body: `{"bookId": 1, "memberId": 2}` |
| POST | `/api/loans/{id}/return` | admin, user | no body |

### Observability

| Path | What |
|------|------|
| `/q/health` | liveness + readiness |
| `/q/metrics` | Prometheus / OpenMetrics |
| `/q/openapi` | OpenAPI 3 spec |
| `/q/swagger-ui` | interactive docs |
| `/q/dev/` | Quarkus Dev UI (dev mode only) |

## Response format

Successful responses are wrapped in `SuccessResponse`:

```json
{
  "data": { ... },
  "timestamp": "2026-03-03T10:15:30Z"
}
```

Errors use `ErrorResponse` with a machine-readable code:

```json
{
  "errorCode": "BORROWING_RULE_VIOLATION",
  "errorMessage": "Member has reached the maximum of 3 active loans",
  "timestamp": "2026-03-03T10:15:30Z"
}
```

Error codes: `ENTITY_NOT_FOUND`, `DUPLICATE_ENTITY`, `BORROWING_RULE_VIOLATION`, `ACTIVE_LOAN_CONFLICT`, `VALIDATION_ERROR`, `INTERNAL_ERROR`.

## Configuration

Key properties (override via env vars or `application.properties`):

| Property | Default | |
|----------|--------:|-|
| `quarkus.http.port` | 8088 | HTTP listen port |
| `lending.max-active-loans` | 3 | max concurrent loans per member |
| `lending.max-loan-duration-days` | 14 | days until a loan is due |
| `MYSQL_USER` | booklending | datasource username |
| `MYSQL_PASSWORD` | booklending | datasource password |

## Default users

Defined in `application.properties` (plain-text, embedded Elytron realm):

| User | Password | Role |
|------|----------|------|
| admin | admin123 | admin |
| alice | alice123 | user |
| bob | bob123 | user |

## Project layout

```
src/main/java/com/demandlane/
  config/          LendingConfig, Roles
  dto/             request/response records, ErrorCode enum
  entity/          Book, Member, Loan (Panache entities)
  exception/       domain exceptions + GlobalExceptionMapper
  health/          custom DB health check
  repository/      Panache repositories
  resource/        JAX-RS endpoints
  service/         business logic, borrowing rules
src/main/resources/
  application.properties
  db/migration/    Flyway SQL (V1__init_schema.sql)
```

## Testing

```bash
./gradlew test
```

Tests run against an in-memory H2 database (MySQL compat mode) — no Docker needed.

Test breakdown:

| Suite | Scope | What it covers |
|-------|-------|----------------|
| `BookTest` | unit | entity invariant methods (checkout, checkin, adjustCopies) |
| `LoanServiceTest` | unit (mocked repos) | all three borrowing rules, return logic, not-found paths |
| `BookResourceTest` | integration | full CRUD, validation, auth, 404s |
| `MemberResourceTest` | integration | full CRUD, validation, auth, 404s |
| `LoanResourceTest` | integration | borrow/return flow, all business rules, delete constraints |
| `ObservabilityTest` | integration | health, metrics, OpenAPI endpoints |

JaCoCo coverage reports land in `build/jacoco-report/`.

## Tech stack

- Quarkus 3.28 (RESTEasy Reactive, Hibernate ORM Panache, Flyway, SmallRye Health/OpenAPI)
- MySQL 8 / H2 for tests
- Micrometer + Prometheus
- Gradle 9, Java 17
