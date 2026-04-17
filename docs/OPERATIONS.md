# Operations Guide

## Running Locally

### Required dependencies

- Java 17
- PostgreSQL
- Maven

### Environment setup

Use `.env.template` as the source of truth for required environment variables.

Development defaults:

- Port: `9005`
- Profile: `dev`
- Database: `ecommerce_dev`

### Start command

```bash
./mvnw spring-boot:run
```

## Profiles

### `dev`

Used for local development.

Configuration source:

- [src/main/resources/application-dev.properties](../src/main/resources/application-dev.properties)

Defaults include:

- local PostgreSQL connection
- local Angular origin `http://localhost:4200`

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
| `DEV_CORS_ALLOWED_ORIGINS` / `PROD_CORS_ALLOWED_ORIGINS` | profile-specific | comma-separated origins |
| `DEV_JWT_SECRET` / `PROD_JWT_SECRET` | profile-specific | Base64-encoded JWT secret |

## Database Notes

- Hibernate DDL mode is currently `update`
- The `User` entity is stored in the `app_user` table
- Email uniqueness is enforced at the database and service levels

For stricter production change control, consider moving from `ddl-auto=update` to managed migrations such as Flyway or Liquibase.

## Production Deployment Notes

- Run behind HTTPS
- Provide a strong JWT secret via environment variable
- Restrict CORS origins to the actual frontend domains
- Use production-grade PostgreSQL credentials and role permissions
- Externalize logging, monitoring, and restart policy in the deployment platform

## Health Verification

Basic availability check:

```bash
curl http://localhost:9005/
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

