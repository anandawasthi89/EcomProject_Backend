# API Reference

Default local base URL: `http://localhost:9005`

Production-style local Docker base URL: `http://localhost:9006`

## Authentication

- Public endpoints do not require a token
- Protected endpoints require:

```http
Authorization: Bearer <jwt-token>
```

- JWTs are stateless and are not stored in the database

## Roles

- `ADMIN`
- `MANAGER`
- `CUSTOMER`
- `CUSTOMER_WITH_READ_ALLOWED`

### Effective access model

- `ADMIN` can manage all users, access all IAM endpoints, and manage catalog content
- `MANAGER` can manage customer-tier users, create and batch-create customers, upgrade customer read access, and manage catalog content
- `CUSTOMER` can use self-service user endpoints, browse the catalog, like and favorite books, reserve physical books, and access ebook or in-store reading flows
- `CUSTOMER_WITH_READ_ALLOWED` has all customer capabilities plus physical take-home checkout

## Core Endpoints

### `GET /`

Service availability check.

Response:

```json
{
  "service": "ecommerce-backend",
  "status": "ok"
}
```

### `POST /api/auth/token`

Generate a JWT for an existing user.

Alias:

- `POST /generateToken`

Request:

```json
{
  "email": "alice@example.com",
  "password": "password123"
}
```

Success response:

```json
{
  "token": "<jwt-token>"
}
```

Failure response:

```json
{
  "message": "Invalid email or password"
}
```

## User Endpoints

### `POST /api/users`

Create a user without prior authentication.

Alias:

- `POST /Users/addUser`

Request:

```json
{
  "name": "Alice",
  "email": "alice@example.com",
  "password": "password123"
}
```

Success response: `201 Created`

```json
{
  "id": 1,
  "name": "Alice",
  "email": "alice@example.com",
  "role": "CUSTOMER",
  "flagged": false,
  "active": true
}
```

### `POST /api/users/register`

Alternative public registration route.

### `GET /api/users`

Return all users.

Aliases:

- `GET /Users`
- `GET /api/users/AllUsers`

Allowed roles:

- `ADMIN`

### `GET /api/users/{id}`

Return a managed user by id.

Allowed roles:

- `ADMIN`
- `MANAGER`

Manager scope note:

- managers are restricted to `CUSTOMER` and `CUSTOMER_WITH_READ_ALLOWED` targets

### `GET /api/users/me`

Return the current authenticated user.

Alias:

- `GET /api/users/currentuser`

### `PUT /api/users/me`

Update the current authenticated user.

Request:

```json
{
  "name": "Alice Updated",
  "password": "newpassword123"
}
```

### `PUT /api/users/{id}`

Update an existing managed user.

Alias:

- `PUT /api/users/UpdateExistingUser`

Allowed roles:

- `ADMIN`
- `MANAGER`

### `DELETE /api/users/{id}`

Delete a managed user.

Alias:

- `DELETE /api/users/deleteUser/{id}`

Allowed roles:

- `ADMIN`
- `MANAGER`

## IAM Endpoints

### `GET /api/iam/me`

Return authenticated identity plus resolved authorities.

Response:

```json
{
  "id": 1,
  "name": "Admin User",
  "email": "admin@example.com",
  "role": "ADMIN",
  "flagged": false,
  "active": true,
  "authorities": [
    "ROLE_ADMIN",
    "iam:write",
    "users:read"
  ]
}
```

### `GET /api/iam/roles`

Return supported roles and their authority sets.

Allowed roles:

- `ADMIN`
- `MANAGER`

### `POST /api/iam/users`

Create one managed customer account.

Allowed roles:

- `ADMIN`
- `MANAGER`

Request:

```json
{
  "name": "Reader User",
  "email": "reader@example.com",
  "password": "password123"
}
```

### `POST /api/iam/users/batch`

Create multiple managed customer accounts in one request.

Allowed roles:

- `ADMIN`
- `MANAGER`

Request:

```json
{
  "users": [
    {
      "name": "One",
      "email": "one@example.com",
      "password": "password123"
    },
    {
      "name": "Two",
      "email": "two@example.com",
      "password": "password123"
    }
  ]
}
```

### `PATCH /api/iam/users/{id}/grant-read-access`

Upgrade an existing `CUSTOMER` to `CUSTOMER_WITH_READ_ALLOWED`.

Allowed roles:

- `ADMIN`
- `MANAGER`

Rules:

- target user must currently be `CUSTOMER`
- managers remain limited to customer-tier targets

### `GET /api/iam/users/flagged`

Return flagged users visible to the caller.

Allowed roles:

- `ADMIN`
- `MANAGER`

### `GET /api/iam/users/{id}/flag-status`

Return the current `flagged` and `active` state for one managed user.

Allowed roles:

- `ADMIN`
- `MANAGER`

### `POST /api/iam/users/flags/scan-overdue`

Scan active physical checkouts whose due date is in the past and flag the affected users.

Allowed roles:

- `ADMIN`
- `MANAGER`

### `POST /api/iam/users/{id}/flags/resolve/force-return?bookId=<bookId>`

Force-close one active physical checkout and its linked reservation lifecycle.

Allowed roles:

- `ADMIN`
- `MANAGER`

### `POST /api/iam/users/{id}/flags/resolve/extend-duration?bookId=<bookId>&extraDays=<1..20>`

Extend one active physical checkout by 1 to 20 days.

Allowed roles:

- `ADMIN`
- `MANAGER`

### `POST /api/iam/users/{id}/flags/resolve/deactivate`

Deactivate a managed user account and clear its current flag state.

Allowed roles:

- `ADMIN`
- `MANAGER`

### `GET /api/iam/access/admin-console`

Admin-only access probe.

### `GET /api/iam/access/manager-console`

Manager/admin access probe.

### `GET /api/iam/access/customer-read`

Read-allowed access probe.

## Book and Lending Endpoints

### `GET /api/books`

Return the active catalog for the authenticated user.

Allowed roles:

- any authenticated role

Response shape:

```json
[
  {
    "id": 7,
    "title": "Clean Architecture",
    "author": "Robert C. Martin",
    "genre": "Technology",
    "publisher": "Prentice Hall",
    "physicalStockQuantity": 4,
    "ebookAvailable": true,
    "inStoreReadAvailable": true,
    "likeCount": 3,
    "favoriteCount": 2,
    "likedByCurrentUser": false,
    "favoritedByCurrentUser": true
  }
]
```

### `GET /api/books/{id}`

Return one catalog title.

Allowed roles:

- any authenticated role

### `POST /api/books`

Create a catalog title.

Allowed roles:

- `ADMIN`
- `MANAGER`

### `PUT /api/books/{id}`

Update a catalog title.

Allowed roles:

- `ADMIN`
- `MANAGER`

### `DELETE /api/books/{id}`

Archive a catalog title.

Allowed roles:

- `ADMIN`
- `MANAGER`

Optional query parameter:

- `forceArchiveWithActiveLoans=false|true`

If omitted or `false`, archiving is rejected when the title still has active reservations or active physical checkouts.

### `POST /api/books/{id}/like`

Like a title.

Allowed roles:

- any authenticated role

### `DELETE /api/books/{id}/like`

Remove a like.

### `POST /api/books/{id}/favorite`

Favorite a title.

Allowed roles:

- any authenticated role

### `DELETE /api/books/{id}/favorite`

Remove a favorite.

### `POST /api/books/{id}/access/ebook`

Record ebook access.

Allowed roles:

- `CUSTOMER`
- `CUSTOMER_WITH_READ_ALLOWED`

### `POST /api/books/{id}/access/in-store`

Record in-store reading.

Allowed roles:

- `CUSTOMER`
- `CUSTOMER_WITH_READ_ALLOWED`

### `POST /api/books/{id}/reserve`

Create a physical reservation if at least one copy is not already reserved or checked out by another user.

Allowed roles:

- `CUSTOMER`
- `CUSTOMER_WITH_READ_ALLOWED`

Success response: `201 Created`

```json
{
  "id": 3,
  "bookId": 7,
  "bookTitle": "Clean Architecture",
  "userId": 9,
  "userEmail": "customer@example.com",
  "status": "ACTIVE",
  "reservedAt": "2026-04-19T12:00:00",
  "checkedOutAt": null,
  "completedAt": null
}
```

Conflict cases:

- all physical copies are already reserved or checked out by other users
- the same user already has an active reservation for that title

### `POST /api/books/{id}/reservation/return`

Cancel and return an active reservation that has not been checked out yet.

Allowed roles:

- `CUSTOMER`
- `CUSTOMER_WITH_READ_ALLOWED`

### `POST /api/books/{id}/checkout`

Take a physical book home.

Allowed roles:

- `CUSTOMER_WITH_READ_ALLOWED`

Rules:

- maximum 3 active physical take-home books per user
- checkout duration is 1 month
- a user cannot check out the same book twice simultaneously
- checkout creates or reuses that user's active reservation allocation
- checkout is rejected when all copies are already reserved or checked out by other users

### `POST /api/books/{id}/return`

Return a physical book.

Allowed roles:

- `CUSTOMER_WITH_READ_ALLOWED`

Behavior:

- the active physical access-history record is closed
- the linked reservation lifecycle is completed

### `GET /api/books/history/me`

Return the current user's access history.

History includes:

- `PHYSICAL_TAKE_HOME`
- `EBOOK`
- `IN_STORE_READ`

### `GET /api/books/history/users/{userId}`

Return another user's access history.

Allowed roles:

- `ADMIN`
- `MANAGER`

Manager scope note:

- managers are restricted to customer-tier users

### `GET /api/books/reservations/me`

Return the current user's reservations.

### `GET /api/books/reservations/users/{userId}`

Return another user's reservations.

Allowed roles:

- `ADMIN`
- `MANAGER`

## Validation Rules

### User creation

- `email` is required and must be valid
- `password` is required and must be at least 8 characters
- `name` is optional for public user creation; email is used as fallback display name if omitted

### User update and register

- `name` is required
- `email` is required and must be valid
- `password` is required and must be at least 8 characters

### Book writes

- `title`, `author`, `genre`, and `publisher` are required
- `physicalStockQuantity` must be provided

## Error Contract

### Validation error

```json
{
  "message": "Validation failed",
  "errors": {
    "email": "Email must be valid",
    "password": "Password must be at least 8 characters"
  }
}
```

### Unauthorized

```json
{
  "message": "Unauthorized"
}
```

### Forbidden

```json
{
  "message": "Forbidden"
}
```

### Business or resource error

Examples:

```json
{
  "message": "Email already registered"
}
```

```json
{
  "message": "No physical copies are currently available because all copies are reserved or checked out"
}
```

```json
{
  "message": "You already have an active reservation for this book"
}
```

## Status Codes

| Code | Meaning |
| --- | --- |
| `200` | Successful read or token generation |
| `201` | User or reservation created successfully |
| `204` | Delete completed successfully |
| `400` | Validation failed or business rule rejected |
| `401` | Authentication failed or token missing/invalid |
| `403` | Authenticated but not allowed |
| `404` | Resource not found |
| `409` | Conflict such as duplicate email or duplicate reservation |
| `500` | Internal server error |
