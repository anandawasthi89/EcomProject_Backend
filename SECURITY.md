# Security Policy

## Supported Use

This repository is intended to be deployed with externally managed secrets and environment-specific configuration.

Security-sensitive configuration includes:

- Database credentials
- JWT signing secrets
- Allowed CORS origins

## Operational Security Notes

- Never commit real values for `.env`, production database credentials, or production JWT secrets.
- The Docker Compose credentials in this repository are local-demo defaults only and must not be reused in real deployments.
- Use a strong Base64-encoded secret of at least 32 bytes for JWT signing.
- Restrict production CORS origins to known frontend domains.
- Rotate credentials immediately if they are exposed.
- Review logs and deployment configuration after any auth or secret rotation change.

## Hardening Recommendations

- Add rate limiting in front of authentication endpoints.
- Run behind HTTPS only in production.
- Use separate PostgreSQL roles per environment.
- Keep production schema changes out of auto-migration mode; this project defaults real prod toward `validate`.
- Add audit logging for sensitive user-management actions.
- Consider refresh-token or session revocation support if authentication scope expands.
