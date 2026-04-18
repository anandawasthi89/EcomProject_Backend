# Test Suite

The test suite is organized by responsibility rather than by framework only.

## Layout

- `controller/`: endpoint and HTTP contract tests using MockMvc
- `service/`: business-rule and persistence interaction tests using Mockito
- `config/`: JWT, auth filter, password encoder, and security-related unit tests
- `bean/`: DTO, entity, and application configuration model tests

## Goals

- Protect the public API contract
- Verify authentication and validation behavior
- Catch regressions in user lifecycle logic
- Keep configuration and security utilities testable in isolation
- Support the CI workflow, which also smoke-tests both `dev` and `prod` profiles against temporary PostgreSQL instances

## Run Tests

```bash
mvn test
```

Generate coverage:

```bash
mvn clean test
```

Coverage output is written to:

```text
target/site/jacoco/index.html
```

## CI Context

The GitHub Actions pipeline runs:

- unit tests
- application packaging
- profile-based smoke tests
- Docker image build validation

This keeps the portfolio demo story strong: the project is not only coded, it is also testable, containerized, and automation-friendly.
