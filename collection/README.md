# Postman Collection

This folder contains import-ready Postman artifacts for local API testing.

The current collection focuses on:

- service health
- public customer onboarding
- manager and admin authentication
- IAM management flows
- catalog creation and customer book interactions
- reservation, duplicate-allocation guards, reservation return, checkout, return, and history flows
- flagged-user visibility and overdue-scan endpoints
- negative-path auth and authorization checks

## Files

- `Ecommerce Backend API.postman_collection.json`
- `Ecommerce Backend API - Dev.postman_environment.json`

## Recommended Usage

1. Start the development stack:

```bash
docker compose -f docker-compose.dev.yml up --build
```

2. Import the collection and environment into Postman
3. Select the development environment
4. Execute the folders in order from setup through cleanup

## Notes

- in `dev`, the application bootstraps predictable privileged accounts by default:
  - `admin@example.com` / `admin12345`
  - `manager@example.com` / `manager12345`
- those defaults can be overridden with `DEV_BOOTSTRAP_ADMIN_*` and `DEV_BOOTSTRAP_MANAGER_*` environment variables
- the collection now uses its own bootstrapped credential variables for admin and manager login, so stale Postman environment secrets will not break the privileged login steps
- the collection stores run-specific emails, ids, and tokens as collection variables
- privileged requests use `{{managerToken}}` and `{{adminToken}}`
- customer flows use `{{customerToken}}` and `{{readAllowedToken}}`
- default base URL is `http://localhost:9005`
- Adminer at `http://localhost:8080` is useful for inspecting the resulting database state while exercising the API

Cleanup is intentionally partial:

- unused batch-created customers are deleted
- the managed demo book is force-archived in cleanup so the run stays rerunnable even if inspection data still exists
- customers who generated history or reservations are left in place so their resulting data remains available for inspection
