# Architecture

## System Shape

The application is a single Spring Boot service with clear internal boundaries:

- `user.*`: identity, authentication support, role-based user management
- `library.*`: catalog, reservations, likes, favorites, access history, and lending rules
- `Config`: security and runtime infrastructure
- `Bean`: entities, DTOs, enums, and the `UserDetails` wrapper
- `Controller`: shared endpoints such as health and exception handling

This keeps deployment simple while preventing the main domains from collapsing into one large service layer.

## High-Level Flow

### Authentication

1. Client sends email and password to `user.controller.AuthenticatorController`
2. `AuthenticationManager` validates credentials
3. `JWTUtils` generates an HMAC-signed token
4. Client uses the token as a Bearer credential on protected routes

### Authorization

1. `JWTAuthenticationFilter` extracts and validates the token
2. `user.service.CustomUserDetailsService` loads the user
3. `CustomUserDetails` exposes role and authority data
4. Spring Security populates the security context
5. `@PreAuthorize` and service-level checks enforce route and business rules

### User lifecycle

1. Request enters `user.controller.UserController` or `user.controller.IamController`
2. Bean validation runs on the request payload
3. `user.service.CustomUserDetailsService` applies normalization and business rules
4. `user.repository.UserRepDAO` persists changes
5. DTO responses are returned

### Library lifecycle

1. Request enters `library.controller.BookController`
2. Spring Security verifies access
3. `library.service.LibraryService` applies stock, role, reservation, and overdue rules
4. `library.repository.*` persists book, history, favorite, like, and reservation changes
5. Response DTOs expose the relevant domain result

## Authorization Model

Roles:

- `ADMIN`
- `MANAGER`
- `CUSTOMER`
- `CUSTOMER_WITH_READ_ALLOWED`

The system uses two layers of control:

- route and method security through Spring Security
- service-level target validation for business-sensitive cases

Examples:

- only `ADMIN` can list all users
- `MANAGER` can manage customer-tier users but not privileged accounts
- only `CUSTOMER_WITH_READ_ALLOWED` can take physical books home
- both customer tiers can use ebook and in-store reading flows

## Persistence Model

### User domain

- `User`
  - identity and credential fields
  - role
  - address/contact fields
  - `flagged` state for overdue review workflows
  - `active` state for account deactivation and JWT rejection

### Library domain

- `Book`
  - catalog metadata
  - physical stock quantity
  - ebook availability
  - in-store reading availability
  - archive flag
- `BookLike`
- `BookFavorite`
- `BookAccessHistory`
- `BookReservation`

The current model intentionally separates customer interaction data from the main book entity so engagement and lending behavior can grow independently.

## Lending Rules

- reservations are for physical books only
- reservations consume physical availability logically until returned or fulfilled
- one user cannot hold two simultaneous active allocations for the same book
- physical checkout is limited to `CUSTOMER_WITH_READ_ALLOWED`
- maximum 3 active physical take-home books per eligible user
- due date is 1 month from checkout
- checkout creates or reuses the reservation allocation for that user
- returning a checked-out book also completes the linked reservation lifecycle
- ebook and in-store reading are available to both customer tiers
- overdue users are flagged through explicit admin or manager scan-and-resolution flows

## Error Handling

`Controller.ApiExceptionHandler` centralizes:

- validation failures
- `ResponseStatusException` handling
- access-denied responses
- fallback internal errors

This keeps controller logic focused on orchestration rather than response construction.

## Configuration and Profiles

Runtime behavior is profile-driven:

- `application.properties`: shared defaults
- `application-dev.properties`: development-specific overrides
- `application-prod.properties`: production-oriented overrides backed by environment variables

Environment values control:

- datasource connection
- JWT secret
- token lifetime
- password hash strength
- CORS origins
- Hibernate DDL mode

## Logging and Troubleshooting

Application logs are present at:

- startup and CORS initialization
- JWT parsing and auth failures
- unauthorized and forbidden requests
- validation and exception boundaries
- user-management writes
- catalog changes
- customer book interactions
- reservations, checkouts, returns, and overdue flagging
- forced returns, duration extensions, and account deactivation

The intent is practical traceability rather than verbose request dumping.

## Why This Design Fits the Current Stage

- one deployable unit keeps setup and operations straightforward
- modular packages keep IAM and lending logic isolated
- PostgreSQL gives stable relational modeling for roles, catalog, and histories
- containerized runtime makes the system easy to run locally
- CI validates more than just unit tests by smoke-testing both runtime profiles

If the platform expands later, `user` and `library` are the natural boundaries for extraction into separate services.
