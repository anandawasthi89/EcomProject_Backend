# E-commerce Backend API

Spring Boot 2.7 backend for user registration, JWT-based authentication, and protected user management endpoints.

This repository is structured as a small, production-minded REST API:

- Stateless JWT authentication
- PostgreSQL persistence with Spring Data JPA
- Bean validation for request payloads
- Centralized JSON error handling
- Environment-based configuration for `dev` and `prod`
- Automated tests with JaCoCo coverage reporting

## Technology Stack

- Java 17
- Spring Boot 2.7.18
- Spring Web
- Spring Security
- Spring Data JPA
- PostgreSQL
- Maven
- JUnit 5 / MockMvc / Mockito
- JaCoCo

## Features

- Health-style root endpoint for service checks
- User registration without prior authentication
- JWT token issuance for valid credentials
- Protected endpoints for listing users, reading the current user, updating users, and deleting users
- Validation and business-rule errors returned as consistent JSON responses

## Project Structure

```text
.
├── src/main/java/com/project/ecomapp/ecommerce_Project
│   ├── Bean/          # Request DTOs, response DTOs, entity, UserDetails wrapper
│   ├── Config/        # Security, JWT, password encoder
│   ├── Controller/    # REST controllers and exception handler
│   ├── Repository/    # Spring Data JPA repository
│   ├── Services/      # Business logic
│   └── EcommerceProjectApplication.java
├── src/main/resources # Profile-specific application properties
├── src/test/java      # Controller, service, config, and model tests
├── docs/              # API, architecture, and operations docs
├── .env.template      # Example environment configuration
└── pom.xml
```

## API Summary

Base URL defaults to `http://localhost:9005`.

| Method | Path | Auth Required | Purpose |
| --- | --- | --- | --- |
| `GET` | `/` | No | Service status check |
| `POST` | `/api/auth/token` | No | Generate JWT token |
| `POST` | `/generateToken` | No | Legacy alias for token generation |
| `POST` | `/api/users` | No | Create a user |
| `POST` | `/Users/addUser` | No | Legacy alias for user creation |
| `POST` | `/api/users/register` | No | Register a user |
| `GET` | `/api/users` | Yes | List all users |
| `GET` | `/api/users/me` | Yes | Current authenticated user |
| `PUT` | `/api/users/{id}` | Yes | Update a user |
| `DELETE` | `/api/users/{id}` | Yes | Delete a user |

Detailed request and response examples are documented in [docs/API.md](docs/API.md).

## Getting Started

### 1. Prerequisites

- Java 17
- PostgreSQL
- Maven 3.8+

### 2. Configure environment

Copy the sample environment file and set values appropriate for your machine:

```bash
cp .env.template .env
```

At minimum for local development:

- `SPRING_PROFILES_ACTIVE=dev`
- `DEV_DB_URL`
- `DEV_DB_USERNAME`
- `DEV_DB_PASSWORD`
- `DEV_JWT_SECRET`

Notes:

- `DEV_JWT_SECRET` and `PROD_JWT_SECRET` must be Base64-encoded keys suitable for HMAC signing.
- Production credentials are intentionally externalized and must not be committed.

### 3. Start the application

With Maven:

```bash
./mvnw spring-boot:run
```

Or:

```bash
mvn spring-boot:run
```

The service starts on `http://localhost:9005` unless `SERVER_PORT` is overridden.

## Configuration

Shared defaults live in `src/main/resources/application.properties`.

Profiles:

- `dev`: [src/main/resources/application-dev.properties](src/main/resources/application-dev.properties)
- `prod`: [src/main/resources/application-prod.properties](src/main/resources/application-prod.properties)

Important environment variables:

| Variable | Description |
| --- | --- |
| `SPRING_PROFILES_ACTIVE` | Active Spring profile |
| `SERVER_PORT` | HTTP server port |
| `JWT_EXPIRATION_MS` | Token expiration time in milliseconds |
| `PASSWORD_HASH_STRENGTH` | BCrypt strength |
| `DEV_DB_URL` / `PROD_DB_URL` | JDBC URL |
| `DEV_DB_USERNAME` / `PROD_DB_USERNAME` | Database username |
| `DEV_DB_PASSWORD` / `PROD_DB_PASSWORD` | Database password |
| `DEV_CORS_ALLOWED_ORIGINS` / `PROD_CORS_ALLOWED_ORIGINS` | Allowed frontend origins |
| `DEV_JWT_SECRET` / `PROD_JWT_SECRET` | Base64 JWT signing secret |

## Authentication

This API uses Bearer tokens.

1. Create or register a user.
2. Call `POST /api/auth/token` with email and password.
3. Use the returned token in the `Authorization` header:

```http
Authorization: Bearer <jwt-token>
```

## Error Format

Validation errors:

```json
{
  "message": "Validation failed",
  "errors": {
    "email": "Email must be valid",
    "password": "Password must be at least 8 characters"
  }
}
```

Business/authentication errors:

```json
{
  "message": "Invalid email or password"
}
```

## Testing

Run tests:

```bash
mvn test
```

Generate tests and coverage:

```bash
mvn clean test
```

JaCoCo output is generated under:

```text
target/site/jacoco/index.html
```

Testing notes are documented in [src/test/README.md](src/test/README.md).

## Documentation Index

- [docs/README.md](docs/README.md)
- [docs/API.md](docs/API.md)
- [docs/ARCHITECTURE.md](docs/ARCHITECTURE.md)
- [docs/OPERATIONS.md](docs/OPERATIONS.md)
- [SECURITY.md](SECURITY.md)

## Development Principles

- Keep endpoint behavior explicit and backward compatible when possible.
- Return predictable JSON payloads for both success and failure scenarios.
- Prefer configuration through environment variables over hardcoded secrets.
- Add tests for behavioral changes before merging.

