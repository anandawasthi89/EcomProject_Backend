# E-commerce Backend API

Spring Boot 2.7 backend that combines identity and access management with a book-centric commerce domain. The current implementation models an online and in-store library workflow: user onboarding, JWT authentication, role-based authorization, catalog management, reservations, physical take-home rules, and access history.

## Overview

- Stateless JWT authentication with Spring Security
- Role-based authorization for `ADMIN`, `MANAGER`, `CUSTOMER`, and `CUSTOMER_WITH_READ_ALLOWED`
- IAM endpoints for identity lookup, managed-user creation, batch onboarding, and controlled role upgrades
- Book catalog with stock, ebook availability, in-store reading, likes, favorites, reservations, and access history
- Physical take-home rules with reservation-backed allocation, checkout limits, due dates, overdue scan and resolution flows, and operational logs
- PostgreSQL persistence with profile-driven configuration
- Docker Compose stacks for local `dev` and `prod`-style execution
- GitHub Actions CI with local execution support through `act`
- Centralized JSON error handling and layered test coverage

## Technology Stack

- Java 17
- Spring Boot 2.7.18
- Spring Web
- Spring Security
- Spring Data JPA
- PostgreSQL
- Maven
- Docker and Docker Compose
- JUnit 5, MockMvc, Mockito, JaCoCo
- GitHub Actions and `act`

## Domain Capabilities

### IAM and user management

- Public registration and token generation
- Self-service account read and update
- Admin-only full user listing
- Manager-scoped user operations limited to customer-tier accounts
- Controlled upgrade path from `CUSTOMER` to `CUSTOMER_WITH_READ_ALLOWED`

### Book and lending domain

- Catalog CRUD for admins and managers
- Customer likes and favorites
- Ebook and in-store reading for both customer tiers
- Physical reservations when stock is available
- Reservation return before pickup
- Physical checkout limited to `CUSTOMER_WITH_READ_ALLOWED`
- Maximum 3 active physical take-home books per eligible user
- One-month physical checkout duration
- Access history across physical, ebook, and in-store usage
- Admin and manager overdue scan, flagged-user review, duration extension, forced return, and deactivation flows

## Module Structure

```text
src/main/java/com/project/ecomapp/ecommerce_Project
├── Bean/          # Entities, request DTOs, response DTOs, enums, UserDetails wrapper
├── Config/        # Security, JWT, password encoder, auth entrypoint, filter chain
├── Controller/    # Shared controllers such as health and exception handling
├── user/          # IAM and account-management controllers, service, repository
├── library/       # Catalog, reservation, and history controllers, service, repositories
└── EcommerceProjectApplication.java
```

This remains a single deployable service, but the code is split into `user` and `library` modules so the main business areas stay isolated without introducing distributed-system overhead prematurely.

## API Summary

Base URL defaults to `http://localhost:9005`.

### Public endpoints

- `GET /`
- `POST /api/auth/token`
- `POST /generateToken`
- `POST /api/users`
- `POST /Users/addUser`
- `POST /api/users/register`

### IAM endpoints

- `GET /api/users`
- `GET /api/users/{id}`
- `GET /api/users/me`
- `PUT /api/users/me`
- `PUT /api/users/{id}`
- `DELETE /api/users/{id}`
- `GET /api/iam/me`
- `GET /api/iam/roles`
- `POST /api/iam/users`
- `POST /api/iam/users/batch`
- `PATCH /api/iam/users/{id}/grant-read-access`
- `GET /api/iam/users/flagged`
- `GET /api/iam/users/{id}/flag-status`
- `POST /api/iam/users/flags/scan-overdue`
- `POST /api/iam/users/{id}/flags/resolve/force-return`
- `POST /api/iam/users/{id}/flags/resolve/extend-duration`
- `POST /api/iam/users/{id}/flags/resolve/deactivate`

### Library endpoints

- `GET /api/books`
- `GET /api/books/{id}`
- `POST /api/books`
- `PUT /api/books/{id}`
- `DELETE /api/books/{id}`
- `POST /api/books/{id}/like`
- `DELETE /api/books/{id}/like`
- `POST /api/books/{id}/favorite`
- `DELETE /api/books/{id}/favorite`
- `POST /api/books/{id}/access/ebook`
- `POST /api/books/{id}/access/in-store`
- `POST /api/books/{id}/reserve`
- `POST /api/books/{id}/reservation/return`
- `POST /api/books/{id}/checkout`
- `POST /api/books/{id}/return`
- `GET /api/books/history/me`
- `GET /api/books/history/users/{userId}`
- `GET /api/books/reservations/me`
- `GET /api/books/reservations/users/{userId}`

Detailed contracts are documented in [docs/API.md](docs/API.md).

## Running Locally

### Prerequisites

- Java 17
- Maven 3.8+
- Docker, if using the containerized setup

### Fastest path: Docker Compose

Development stack:

```bash
docker compose -f docker-compose.dev.yml up --build
```

Available services:

- API: `http://localhost:9005`
- PostgreSQL: `localhost:5432`
- Adminer: `http://localhost:8080`

Production-style local stack:

```bash
docker compose -f docker-compose.prod.yml up --build
```

Available services:

- API: `http://localhost:9006`
- PostgreSQL: `localhost:5433`

### Direct JVM execution

Export runtime variables before starting Spring Boot:

```bash
export SPRING_PROFILES_ACTIVE=dev
export DEV_DB_URL=jdbc:postgresql://localhost:5432/ecommerce_dev
export DEV_DB_USERNAME=postgres
export DEV_DB_PASSWORD=postgres
export DEV_JWT_SECRET=<base64-secret>
./mvnw spring-boot:run
```

Template files in the repository are reference material for local setup. Runtime values should come from shell variables, IDE run configuration, Docker Compose, or CI/CD secrets.

## Configuration

Profiles:

- `dev`: [src/main/resources/application-dev.properties](src/main/resources/application-dev.properties)
- `prod`: [src/main/resources/application-prod.properties](src/main/resources/application-prod.properties)

Key environment variables:

- `SPRING_PROFILES_ACTIVE`
- `SERVER_PORT`
- `JWT_EXPIRATION_MS`
- `PASSWORD_HASH_STRENGTH`
- `DEV_DB_URL` / `PROD_DB_URL`
- `DEV_DB_USERNAME` / `PROD_DB_USERNAME`
- `DEV_DB_PASSWORD` / `PROD_DB_PASSWORD`
- `DEV_HIBERNATE_DDL_AUTO` / `PROD_HIBERNATE_DDL_AUTO`
- `DEV_CORS_ALLOWED_ORIGINS` / `PROD_CORS_ALLOWED_ORIGINS`
- `DEV_JWT_SECRET` / `PROD_JWT_SECRET`

## Authorization Model

- `ADMIN`: full IAM and catalog control
- `MANAGER`: operational management of customer-tier users and catalog content
- `CUSTOMER`: standard authenticated user with ebook and in-store reading access
- `CUSTOMER_WITH_READ_ALLOWED`: customer privileges plus physical take-home eligibility

Managers cannot operate on `ADMIN` or `MANAGER` accounts. Physical checkout is reserved for `CUSTOMER_WITH_READ_ALLOWED`, while ebook and in-store reading remain available to both customer tiers.
Flagged-user handling is explicit: admins and managers review and resolve overdue cases through IAM endpoints rather than relying on customer-facing actions.

## Observability

Application logs now cover:

- application startup and CORS configuration
- authentication success and failure
- unauthorized and forbidden access
- validation and business-rule failures
- managed user operations
- catalog writes and customer book interactions
- reservations, checkouts, returns, and overdue flagging

This makes local troubleshooting and environment verification much easier during development, CI runs, and demos.

## Quality and Delivery

- Unit and controller tests cover security, validation, IAM flows, and library business rules
- An endpoint-level integration suite exercises reservation, checkout, overdue scan, forced return, and deactivation flows against a real Spring context
- JaCoCo coverage is generated during Maven test runs and currently reports `97.24%` line coverage after `./mvnw clean test`
- GitHub Actions validates tests, packaging, startup smoke tests for `dev` and `prod`, and Docker image build
- The same workflow can be executed locally with `act`

See:

- [docs/API.md](docs/API.md)
- [docs/ARCHITECTURE.md](docs/ARCHITECTURE.md)
- [docs/OPERATIONS.md](docs/OPERATIONS.md)
- [docs/LOCAL_CI.md](docs/LOCAL_CI.md)
