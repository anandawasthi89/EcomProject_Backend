# Local CI with `act`

The GitHub Actions workflow is designed to run locally with `act` and in GitHub Actions without depending on committed runtime secrets.

## Workflow Coverage

The CI pipeline validates:

- unit and controller tests
- application packaging
- startup smoke test for `dev`
- startup smoke test for `prod`
- Docker image build

The smoke-test job provisions PostgreSQL dynamically and starts the application inside the workflow run, so local CI exercises both profiles in a runtime-like environment.

## Prerequisites

- Docker
- `act`

Recommended image mapping:

```bash
act -P ubuntu-latest=catthehacker/ubuntu:act-latest
```

On Apple Silicon, if needed:

```bash
act -P ubuntu-latest=catthehacker/ubuntu:act-latest --container-architecture linux/amd64
```

## Running the Workflow Locally

Pull-request style run:

```bash
act pull_request -W .github/workflows/ci.yml -P ubuntu-latest=catthehacker/ubuntu:act-latest
```

Push-style run:

```bash
act push -W .github/workflows/ci.yml -P ubuntu-latest=catthehacker/ubuntu:act-latest
```

Manual dispatch:

```bash
act workflow_dispatch -W .github/workflows/ci.yml -P ubuntu-latest=catthehacker/ubuntu:act-latest
```

Run only smoke tests:

```bash
act pull_request -W .github/workflows/ci.yml -j profile-smoke -P ubuntu-latest=catthehacker/ubuntu:act-latest
```

## Local Runtime Outside Actions

Development stack:

```bash
docker compose -f docker-compose.dev.yml up --build
```

Production-style local stack:

```bash
docker compose -f docker-compose.prod.yml up --build
```

Health checks:

```bash
curl http://localhost:9005/
curl http://localhost:9006/
```

Adminer in development:

```text
http://localhost:8080
```

Adminer connection values:

- System: `PostgreSQL`
- Server: `postgres-dev`
- Username: `postgres`
- Password: `postgres`
- Database: `ecommerce_dev`

## Notes

- the workflow smoke-tests both `dev` and `prod` profiles with safe local values
- the Docker image is environment-driven; do not bake a fixed Spring profile into the image
- local Compose runs are the fastest way to inspect runtime behavior, especially alongside Adminer
- local CI is useful for validating build and profile behavior before pushing changes
