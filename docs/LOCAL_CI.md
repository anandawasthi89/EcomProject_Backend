# Local CI with `act`

This repository's GitHub Actions pipeline is designed to run locally with `act` and in GitHub Actions without depending on checked-in `.env` runtime files.

## What the workflow covers

- unit tests
- JAR packaging
- startup smoke test for `dev`
- startup smoke test for `prod`
- Docker image build

The smoke test provisions a temporary PostgreSQL service automatically, so you do not need your local database running to validate the workflow.

The smoke job starts the application and verifies health within the same workflow step. That is intentional: it avoids background-process lifecycle issues that can appear when running GitHub Actions locally through `act`.

For local runtime outside Actions, use:

- `docker-compose.dev.yml` for the `dev` profile
- `docker-compose.prod.yml` for the `prod` profile

## Prerequisites

- Docker
- `act`

Recommended image mapping for better compatibility with `ubuntu-latest`:

```bash
act -P ubuntu-latest=catthehacker/ubuntu:act-latest
```

On Apple Silicon, if you hit image or runtime oddities, prefer:

```bash
act -P ubuntu-latest=catthehacker/ubuntu:act-latest --container-architecture linux/amd64
```

## Run the full pipeline locally

For a pull request style run:

```bash
act pull_request -W .github/workflows/ci.yml -P ubuntu-latest=catthehacker/ubuntu:act-latest
```

For a push style run:

```bash
act push -W .github/workflows/ci.yml -P ubuntu-latest=catthehacker/ubuntu:act-latest
```

For manual dispatch:

```bash
act workflow_dispatch -W .github/workflows/ci.yml -P ubuntu-latest=catthehacker/ubuntu:act-latest
```

To run only the smoke-test job:

```bash
act pull_request -W .github/workflows/ci.yml -j profile-smoke -P ubuntu-latest=catthehacker/ubuntu:act-latest
```

## Run the app locally with Docker Compose

Development:

```bash
docker compose -f docker-compose.dev.yml up --build
```

Production-style local run:

```bash
docker compose -f docker-compose.prod.yml up --build
```

Health checks:

```bash
curl http://localhost:9005/
curl http://localhost:9006/
```

Adminer for DB inspection in dev:

```text
http://localhost:8080
```

Use these connection details in Adminer:

- System: `PostgreSQL`
- Server: `postgres-dev`
- Username: `postgres`
- Password: `postgres`
- Database: `ecommerce_dev`

Notes:

- `dev` maps PostgreSQL to host port `5432` and the app to `9005`
- `dev` also exposes Adminer on `8080`
- `prod` maps PostgreSQL to host port `5433` and the app to `9006`
- the prod compose file sets `PROD_HIBERNATE_DDL_AUTO=update` only for local bootstrapping
- the application default for real prod is now `validate`

## Notes

- The workflow smoke-tests both `dev` and `prod` profiles with safe local values.
- The Docker image is environment-driven. Set `SPRING_PROFILES_ACTIVE` at runtime instead of baking a profile into the image.
- For real deployment workflows later, add GitHub Environments and inject actual `DEV_*` or `PROD_*` secrets there.
