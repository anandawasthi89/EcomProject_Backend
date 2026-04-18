# API Reference

Default local base URL: `http://localhost:9005`

Production-style local Docker base URL: `http://localhost:9006`

## Authentication Model

- Public endpoints can be called without a token.
- JWTs are stateless and are not stored in the database.
- Protected endpoints require:

```http
Authorization: Bearer <jwt-token>
```

## Endpoints

### `GET /`

Service status endpoint.

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
  "email": "alice@example.com"
}
```

### `POST /api/users/register`

Alternative registration endpoint.

Request:

```json
{
  "id": null,
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
  "email": "alice@example.com"
}
```

### `GET /api/users`

Return all users.

Aliases:

- `GET /Users`
- `GET /api/users/AllUsers`

Authentication required: Yes

Success response:

```json
[
  {
    "id": 1,
    "name": "Alice",
    "email": "alice@example.com"
  }
]
```

### `GET /api/users/me`

Return the currently authenticated user.

Alias:

- `GET /api/users/currentuser`

Authentication required: Yes

Success response:

```json
{
  "id": 1,
  "name": "Alice",
  "email": "alice@example.com"
}
```

### `PUT /api/users/{id}`

Update an existing user.

Alias:

- `PUT /api/users/UpdateExistingUser`

Authentication required: Yes

Request:

```json
{
  "id": 1,
  "name": "Alice Updated",
  "email": "alice.updated@example.com",
  "password": "newpassword123"
}
```

Success response:

```json
{
  "id": 1,
  "name": "Alice Updated",
  "email": "alice.updated@example.com"
}
```

### `DELETE /api/users/{id}`

Delete a user.

Alias:

- `DELETE /api/users/deleteUser/{id}`

Authentication required: Yes

Success response: `204 No Content`

## Validation Rules

### User creation

- `email` is required
- `email` must be valid
- `password` is required
- `password` must be at least 8 characters
- `name` is optional for `POST /api/users`; if blank, email is used as the fallback display name

### User update and register

- `name` is required
- `email` is required
- `email` must be valid
- `password` is required
- `password` must be at least 8 characters

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

### Business rule or resource error

Examples:

```json
{
  "message": "Email already registered"
}
```

```json
{
  "message": "User not found"
}
```

## Status Codes

| Code | Meaning |
| --- | --- |
| `200` | Successful read or token generation |
| `201` | User created successfully |
| `204` | User deleted successfully |
| `400` | Validation failed |
| `401` | Authentication failed or token missing/invalid |
| `404` | Resource not found |
| `409` | Business conflict such as duplicate email |
| `500` | Internal server error |
