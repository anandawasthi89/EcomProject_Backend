# Architecture

## Overview

The application is organized as a conventional Spring Boot layered API:

- `Controller`: HTTP boundary and request/response handling
- `Services`: business logic, normalization, conflict checks, password hashing
- `Repository`: persistence abstraction via Spring Data JPA
- `Config`: authentication, JWT processing, password encoder, and CORS setup
- `Bean`: entity and DTO models

Around that application core, the repository also includes:

- a multi-stage `Dockerfile` for container image builds
- Docker Compose stacks for local `dev` and local `prod`-style execution
- a GitHub Actions workflow that tests, packages, smoke-tests, and validates Docker builds

## Request Flow

### Public registration

1. Request enters `UserController`
2. Bean validation runs on the request DTO
3. `CustomUserDetailsService.addNewUser(...)` normalizes email and hashes the password
4. `UserRepDAO` persists the user
5. `UserResponse` is returned to the client

### Authenticated request

1. Client sends a Bearer token
2. `JWTAuthenticationFilter` extracts and validates the JWT
3. `CustomUserDetailsService.loadUserByUsername(...)` loads the user
4. Spring Security stores the authenticated principal in the security context
5. Protected controller endpoint executes

## Authentication Design

### Token issuance

- `AuthenticatorController` accepts email/password
- `AuthenticationManager` verifies credentials
- `JWTUtils` creates an HMAC-signed JWT

### Token verification

- `JWTAuthenticationFilter` reads `Authorization`
- `JWTUtils.extractUsername(...)` parses the token
- `JWTUtils.validateToken(...)` verifies username and expiration
- Valid tokens populate the Spring Security context

### Entry point behavior

Unauthenticated access to protected routes is handled by `JWTAuthenticationEntryPoint`, which returns:

```json
{
  "message": "Unauthorized"
}
```

## Persistence Model

The current persistence model is intentionally small:

- `User`
  - `id`
  - `name`
  - `email`
  - `password`

Email is unique and normalized to lowercase trimmed form before persistence or lookup.

JWT tokens are not persisted. Authentication is stateless once a token has been issued.

## Error Handling

`ApiExceptionHandler` centralizes:

- validation errors
- `ResponseStatusException` responses
- generic exception fallback

This keeps controller code small and produces a predictable JSON response format.

## Configuration Model

Configuration is profile-driven:

- `application.properties`: shared defaults
- `application-dev.properties`: development overrides
- `application-prod.properties`: production placeholders backed by environment variables

For local demos, the repository also provides:

- `.env.template` for shared runtime settings
- `.env.dev.template` and `.env.prod.template` for profile-specific variables
- `docker-compose.dev.yml` and `docker-compose.prod.yml` for containerized execution

Important operational values include:

- datasource URL and credentials
- JWT secret
- token lifetime
- password hash strength
- allowed CORS origins

## Why This Structure Works

- Small codebase stays readable
- Security concerns are isolated in `Config`
- Business rules stay out of controllers
- DTOs separate external contract from persistence
- Test coverage can target each layer independently
- The project can be demonstrated quickly with Compose while still reflecting production-style concerns such as profiles, secrets, and CI validation
