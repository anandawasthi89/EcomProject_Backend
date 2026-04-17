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

