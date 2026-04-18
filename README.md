# E-commerce Backend API

Spring Boot 2.7 backend for user registration, JWT-based authentication, and protected user management endpoints.

This repository is designed as a portfolio-ready backend module that can sit behind a storefront, admin panel, or broader e-commerce platform. It demonstrates application-layer security, profile-driven configuration, containerized local infrastructure, and CI validation in a compact but production-minded codebase.

## Portfolio Highlights

- Stateless JWT authentication with Spring Security filter-chain integration
- PostgreSQL persistence with Spring Data JPA and profile-driven datasource configuration
- Layered architecture with clear separation between controllers, services, repositories, config, and DTO/entity models
- Validation, business-rule enforcement, and centralized JSON exception handling
- Dockerized local development stack with PostgreSQL and Adminer for database inspection
- GitHub Actions CI pipeline with unit tests, packaging, Docker build validation, and profile-based smoke tests
- Local CI emulation through `act`, so workflows can be exercised before pushing
- Backward-compatible alias endpoints alongside cleaner REST-style routes

## Technology Stack

- Java 17
- Spring Boot 2.7.18
- Spring Web
- Spring Security
- Spring Data JPA
- PostgreSQL
- Maven
- Docker / Docker Compose
- JUnit 5 / MockMvc / Mockito
- JaCoCo
- GitHub Actions / `act`

## Features

- Health-style root endpoint for service checks
- User registration without prior authentication
- JWT token issuance for valid credentials
- Protected endpoints for listing users, reading the current user, updating users, and deleting users
- Validation and business-rule errors returned as consistent JSON responses
- Local dev stack with containerized PostgreSQL and browser-based DB inspection via Adminer
- CI smoke testing for both `dev` and `prod` application profiles

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
├── docker-compose.*   # Local dev/prod-like runtime stacks
├── Dockerfile         # Multi-stage image build
├── .env.template      # Shared environment configuration
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
- Maven 3.8+
- Docker Desktop or Docker Engine if you want the containerized stack

### 2. Fastest local start: Docker Compose

Development stack:

```bash
docker compose -f docker-compose.dev.yml up --build
```

This brings up:

- API at `http://localhost:9005`
- PostgreSQL at `localhost:5432`
- Adminer at `http://localhost:8080`

Production-style local stack:

```bash
docker compose -f docker-compose.prod.yml up --build
```

This brings up:

- API at `http://localhost:9006`
- PostgreSQL at `localhost:5433`

### 3. Run directly with Maven

If you want to run the app outside Docker, create a root `.env` file from the shared template and the matching profile template:

```bash
cat .env.template .env.dev.template > .env
```

For a production-style local run:

```bash
cat .env.template .env.prod.template > .env
```

At minimum for local JVM development:

- `SPRING_PROFILES_ACTIVE=dev`
- `DEV_DB_URL`
- `DEV_DB_USERNAME`
- `DEV_DB_PASSWORD`
- `DEV_JWT_SECRET`

Notes:

- `DEV_JWT_SECRET` and `PROD_JWT_SECRET` must be Base64-encoded keys suitable for HMAC signing.
- Production credentials are intentionally externalized and must not be committed.

Start the application:

With Maven:

```bash
./mvnw spring-boot:run
```

Or:

```bash
mvn spring-boot:run
```

The service starts on `http://localhost:9005` unless `SERVER_PORT` is overridden.

The application can auto-read a project-root `.env` file before Spring Boot starts. Docker Compose does not require a root `.env` because the compose files inject the relevant template values directly.

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
| `DEV_HIBERNATE_DDL_AUTO` / `PROD_HIBERNATE_DDL_AUTO` | Optional profile-specific schema mode override |
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

## Local CI

Run the GitHub Actions workflow locally with `act`:

```bash
act pull_request -W .github/workflows/ci.yml -P ubuntu-latest=catthehacker/ubuntu:act-latest
```

Local CI notes are documented in [docs/LOCAL_CI.md](docs/LOCAL_CI.md).

## Documentation Index

- [docs/README.md](docs/README.md)
- [docs/API.md](docs/API.md)
- [docs/ARCHITECTURE.md](docs/ARCHITECTURE.md)
- [docs/LOCAL_CI.md](docs/LOCAL_CI.md)
- [docs/OPERATIONS.md](docs/OPERATIONS.md)
- [SECURITY.md](SECURITY.md)

## Development Principles

- Keep endpoint behavior explicit and backward compatible when possible.
- Return predictable JSON payloads for both success and failure scenarios.
- Prefer configuration through environment variables over hardcoded secrets.
- Keep the project easy to demo locally with Docker Compose and inspectable infrastructure.
- Add tests for behavioral changes before merging.
