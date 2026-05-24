# Security Policy

## Security-Sensitive Configuration

The application expects security-sensitive values to be supplied externally per environment.

Examples:

- database credentials
- JWT signing secrets
- allowed CORS origins

These values should not be committed to the repository.

## Current Security Controls

- stateless JWT authentication
- Spring Security filter chain with method-level authorization
- role-based access control across IAM and library operations
- centralized unauthorized, forbidden, validation, and exception handling
- password hashing through BCrypt
- environment-specific runtime configuration
- operational logging for authentication, authorization, IAM writes, and lending events

## Operational Guidance

- never commit real `.env` files or production credentials
- use a strong Base64-encoded JWT secret with at least 32 bytes of entropy
- restrict production CORS origins to known frontend domains
- rotate credentials immediately if exposed
- review application logs after auth or permission-related changes

## Hardening Recommendations

- run behind HTTPS only
- add rate limiting in front of authentication endpoints
- use separate PostgreSQL roles per environment
- move production schema changes to managed migrations
- centralize log aggregation and retention
- introduce audit-focused reporting for flagged users and privileged IAM actions
- add refresh-token or revocation support if session control requirements grow
