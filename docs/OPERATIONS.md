# Operations Guide

## Running Locally

### Required dependencies

- Java 17
- Maven
- Docker, if you want the containerized local stack

### Environment setup

For direct JVM execution, build a root `.env` file from the shared template and the profile template you want:

```bash
cat .env.template .env.dev.template > .env
```

or:

```bash
cat .env.template .env.prod.template > .env
```

For Docker Compose, you do not need a root `.env`; the compose files already reference the shared and profile templates directly.

Development defaults:

- Port: `9005`
- Profile: `dev`
- Database: `ecommerce_dev`

### Start command

Direct JVM run:

```bash
./mvnw spring-boot:run
```

Docker Compose development stack:

```bash
docker compose -f docker-compose.dev.yml up --build
```

Docker Compose production-style local stack:

```bash
docker compose -f docker-compose.prod.yml up --build
```

## Profiles

### `dev`

Used for local development.

Configuration source:

- [src/main/resources/application-dev.properties](../src/main/resources/application-dev.properties)

Defaults include:

- local PostgreSQL connection
- local Angular origin `http://localhost:4200`
- Adminer UI available at `http://localhost:8080` when using the dev compose stack

### `prod`

Used for deployment environments.

Configuration source:

- [src/main/resources/application-prod.properties](../src/main/resources/application-prod.properties)

All critical values are expected from environment variables.

## Environment Variables

| Variable | Required In | Notes |
| --- | --- | --- |
| `SPRING_PROFILES_ACTIVE` | all | `dev` or `prod` |
| `SERVER_PORT` | optional | defaults to `9005` |
| `JWT_EXPIRATION_MS` | optional | defaults to `36000000` |
| `PASSWORD_HASH_STRENGTH` | optional | defaults to `12` |
| `DEV_DB_URL` / `PROD_DB_URL` | profile-specific | JDBC connection string |
| `DEV_DB_USERNAME` / `PROD_DB_USERNAME` | profile-specific | database login |
| `DEV_DB_PASSWORD` / `PROD_DB_PASSWORD` | profile-specific | database password |
| `DEV_HIBERNATE_DDL_AUTO` / `PROD_HIBERNATE_DDL_AUTO` | optional | schema-management override |
| `DEV_CORS_ALLOWED_ORIGINS` / `PROD_CORS_ALLOWED_ORIGINS` | profile-specific | comma-separated origins |
| `DEV_JWT_SECRET` / `PROD_JWT_SECRET` | profile-specific | Base64-encoded JWT secret |

## Database Notes

- Global default Hibernate DDL mode is `update` for developer convenience
- Real production profile defaults toward `validate`
- Local `prod` compose overrides DDL mode back to `update` so the demo stack can bootstrap itself
- The `User` entity is stored in the `app_user` table
- Email uniqueness is enforced at the database and service levels

For stricter production change control, consider moving from `ddl-auto=update` to managed migrations such as Flyway or Liquibase.

## Production Deployment Notes

- Run behind HTTPS
- Provide a strong JWT secret via environment variable
- Restrict CORS origins to the actual frontend domains
- Use production-grade PostgreSQL credentials and role permissions
- Externalize logging, monitoring, and restart policy in the deployment platform
- Prefer managed PostgreSQL or separately managed database infrastructure over a local compose-managed database for real production

## Health Verification

Basic availability check:

```bash
curl http://localhost:9005/
```

Production-style local stack:

```bash
curl http://localhost:9006/
```

Expected response:

```json
{
  "service": "ecommerce-backend",
  "status": "ok"
}
```

## Testing and Coverage

Run:

```bash
mvn clean test
```

Coverage report:

```text
target/site/jacoco/index.html
```

For local CI workflow validation, see [LOCAL_CI.md](LOCAL_CI.md).
