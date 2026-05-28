# Operations Guide

## Runtime Modes

The service is designed to run in two local modes:

- direct JVM execution with exported environment variables
- Docker Compose with profile-specific local stacks

Supported profiles:

- `dev`
- `prod`

## Prerequisites

- Java 17
- Maven
- Docker, for containerized execution and local CI with `act`

## Direct JVM Execution

Export the required environment variables before starting Spring Boot:

```bash
export SPRING_PROFILES_ACTIVE=dev
export DEV_DB_URL=jdbc:postgresql://localhost:5432/ecommerce_dev
export DEV_DB_USERNAME=postgres
export DEV_DB_PASSWORD=postgres
export DEV_JWT_SECRET=<base64-secret>
./mvnw spring-boot:run
```

For a production-style local run:

```bash
export SPRING_PROFILES_ACTIVE=prod
export PROD_DB_URL=jdbc:postgresql://localhost:5433/ecommerce_prod
export PROD_DB_USERNAME=postgres
export PROD_DB_PASSWORD=postgres
export PROD_JWT_SECRET=<base64-secret>
./mvnw spring-boot:run
```

Template files in the repository are references for local setup. Runtime configuration should come from shell variables, IDE run configs, Docker Compose, or deployment secrets.

## Docker Compose

### Development stack

```bash
docker compose -f docker-compose.dev.yml up --build
```

Services:

- API: `http://localhost:9005`
- PostgreSQL: `localhost:5432`
- Adminer: `http://localhost:8080`

### Production-style local stack

```bash
docker compose -f docker-compose.prod.yml up --build
```

Services:

- API: `http://localhost:9006`
- PostgreSQL: `localhost:5433`

Notes:

- the development compose stack uses `.env.template` and `.env.dev.template`
- the production-style compose stack uses `.env.template` and `.env.prod.template`
- the local prod compose file sets `PROD_HIBERNATE_DDL_AUTO=update` so the stack can bootstrap itself
- the dev profile bootstraps predictable `ADMIN` and `MANAGER` accounts and now re-enables them if they were previously deactivated in the same local database

## Environment Variables

| Variable | Scope | Purpose |
| --- | --- | --- |
| `SPRING_PROFILES_ACTIVE` | all | active runtime profile |
| `SERVER_PORT` | all | HTTP port override |
| `JWT_EXPIRATION_MS` | all | token expiration in milliseconds |
| `PASSWORD_HASH_STRENGTH` | all | BCrypt strength |
| `DEV_DB_URL` / `PROD_DB_URL` | profile-specific | JDBC datasource URL |
| `DEV_DB_USERNAME` / `PROD_DB_USERNAME` | profile-specific | datasource username |
| `DEV_DB_PASSWORD` / `PROD_DB_PASSWORD` | profile-specific | datasource password |
| `DEV_HIBERNATE_DDL_AUTO` / `PROD_HIBERNATE_DDL_AUTO` | profile-specific | schema mode override |
| `DEV_CORS_ALLOWED_ORIGINS` / `PROD_CORS_ALLOWED_ORIGINS` | profile-specific | comma-separated origins |
| `DEV_JWT_SECRET` / `PROD_JWT_SECRET` | profile-specific | Base64-encoded signing secret |

## Database Notes

- Hibernate defaults are tuned for development convenience
- real production should prefer `validate` plus explicit migrations
- main user table is `app_user`
- user records include role and overdue-review flag state
- book-domain tables cover catalog data, likes, favorites, access history, and reservations
- email uniqueness is enforced by both service logic and the database

For production-grade schema control, Flyway or Liquibase is a better long-term fit than relying on `ddl-auto`.

## Library Operations Notes

- physical reservations now hold logical availability until the reservation is returned or fulfilled by checkout
- one user cannot reserve or check out the same book twice simultaneously
- physical checkout is limited to `CUSTOMER_WITH_READ_ALLOWED`
- ebook and in-store reading are available to both customer tiers
- a user can hold at most 3 active physical take-home books at once
- overdue handling is operator-driven: admins and managers can scan overdue loans, inspect flagged users, extend duration by up to 20 days, force-return a book, or deactivate a user
- the archive endpoint supports `forceArchiveWithActiveLoans=true` for operational cleanup and demo reset scenarios

## Logging

Operational logs now include:

- startup and security initialization
- authentication success and failure
- unauthorized and forbidden requests
- validation and exception boundaries
- IAM writes and role upgrades
- catalog writes and customer engagement actions
- reservation, checkout, return, and overdue-flag events

This level of logging is intended for local troubleshooting, CI visibility, and basic operational tracing.

## Production Considerations

- run behind HTTPS only
- store secrets outside the repository
- restrict CORS to known frontend origins
- use stronger database credentials and least-privilege roles
- centralize logs and restart policy in the deployment platform
- prefer managed PostgreSQL or separately managed database infrastructure

## Health and Verification

Basic availability checks:

```bash
curl http://localhost:9005/
curl http://localhost:9006/
```

Expected response:

```json
{
  "service": "ecommerce-backend",
  "status": "ok"
}
```

Run the test suite:

```bash
./mvnw test
```

Run a clean coverage pass:

```bash
./mvnw clean test
```

Coverage report:

```text
target/site/jacoco/index.html
```

For local workflow validation, see [LOCAL_CI.md](LOCAL_CI.md).
