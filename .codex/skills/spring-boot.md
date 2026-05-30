# Spring Boot Skill

## Core Concepts

- REST controllers expose API contracts.
- Services coordinate use cases.
- Repositories handle persistence.
- Configuration should be explicit and environment-aware.
- Validation belongs at boundaries and domain creation points.

## Best Practices

- Keep controllers thin.
- Use request and response DTOs.
- Use `@Valid` for API input validation.
- Use centralized error handling.
- Keep transactions in application services.
- Use Testcontainers for database integration tests.
- Avoid making every class a Spring bean by habit.

## Project-Specific Conventions

- Backend APIs live under `/api`.
- Long-running simulation jobs should eventually use job resources rather than blocking every request.
- Financial calculators should remain plain Java objects when possible.
- Spring configuration should support local development and CI.

## Common Mistakes

- Returning JPA entities directly from controllers.
- Hiding business rules in annotations only.
- Placing transaction boundaries around CPU-heavy simulation loops unnecessarily.
- Using Spring events as a substitute for clear module interfaces.

## Recommended Patterns

- `Controller -> ApplicationService -> DomainService/Calculator -> Repository`.
- Controller advice for errors.
- Configuration properties for simulation defaults.
- Integration tests for repository and API behavior.

