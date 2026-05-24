# Test Suite

The test suite is organized by responsibility and is intended to protect both HTTP contracts and business rules.

## Layout

- `controller/`: MockMvc tests for endpoint behavior, security, and JSON contracts
- `controller/BusinessFlowIntegrationTest.java`: end-to-end business flows against a real Spring context and in-memory database
- `service/`: business-rule tests for IAM and library workflows
- `config/`: JWT, security filter, auth entrypoint, and password encoder tests
- `bean/`: DTO, entity, and application configuration model tests

## What the Suite Covers

- public registration and authentication flows
- protected user-management behavior
- role-based access restrictions
- validation and error response format
- book catalog and lending rules
- reservation and overdue-flagging behavior
- overdue scan, flagged-user resolution, and deactivation behavior
- security and configuration utilities

The integration-style business-flow suite uses the `test` Spring profile with H2 in PostgreSQL compatibility mode so the API flows run without depending on the local Docker or dev PostgreSQL database.

## Running Tests

```bash
./mvnw test
```

Coverage run:

```bash
./mvnw clean test
```

Coverage output:

```text
target/site/jacoco/index.html
```

Current clean-run reference:

- line coverage: `97.24%`

## CI Context

The GitHub Actions workflow validates:

- test execution
- JAR packaging
- runtime startup for both `dev` and `prod`
- Docker image build

This keeps changes verifiable at the unit, HTTP, and runtime levels.
