# Development Conventions

## General

- Prefer simple, explicit designs.
- Keep changes scoped to the current feature.
- Document important tradeoffs.
- Do not introduce infrastructure before there is a concrete need.
- Treat financial simplifications as explicit assumptions.

## Java

- Use Java 21 or newer.
- Prefer immutable value objects for domain concepts where practical.
- Use `BigDecimal` for persisted monetary amounts when exact decimal representation matters.
- Use `double` for numerical analytics and Monte Carlo calculations when appropriate, with tests and documented tolerance.
- Keep pure pricing formulas framework-independent.
- Use meaningful domain names.

## Spring Boot

- Controllers should be thin.
- Application services own use-case orchestration.
- Domain services own financial behavior.
- Repositories should be infrastructure details.
- Request and response DTOs should be explicit.
- Validation should happen at API boundaries and in domain constructors/factories where needed.

## Testing

- Financial formulas require unit tests.
- Use deterministic seeds for Monte Carlo tests.
- Use Testcontainers for database integration tests.
- Avoid mocking pure domain objects.
- Prefer property-style tests for invariants.
- Include exact-value tests for known pricing examples.

## Persistence

- Keep schema understandable.
- Use explicit identifiers.
- Avoid premature inheritance hierarchies in database tables.
- Avoid storing core business concepts only as opaque JSON.
- Use migrations when the backend project is established.
- Persisted positions should store trade terms, not market data inputs such as spot, volatility, or rates, unless a feature explicitly changes that boundary.
- External instrument validation must go through the `marketdata` application boundary; portfolio code should not call Blemberg or other providers directly.
- Portfolio pricing inputs must also go through the `marketdata` application boundary; do not read provider data directly from portfolio code.
- Pricing results are stateless by default and should not be persisted unless the feature explicitly introduces valuation storage.

## API Design

- Use stable URLs under `/api`.
- Prefer nouns for resources and explicit action endpoints for calculations.
- Validate inputs and return clear errors.
- Do not expose internal entity shapes directly.
- Do not expose framework-internal exception messages, class names, or stack details in API error responses.

## Documentation

- Every major feature should have:
  - Planner specification.
  - API notes.
  - Test plan.
  - Relevant architecture decision if tradeoffs are important.
- When conceptual docs exist under `docs/`, link them from relevant README or feature docs.
- Keep docs aligned with explicit financial assumptions such as expiry behavior, rate conventions, and unsupported model inputs.
- Treat `docs/docs-ES/LogicaDelSistema.md` and `docs/docs-EN/SystemLogic.md` as the living system-logic narrative and update them after meaningful vertical slices.

## AI Agent Usage

- The Planner Agent designs first.
- The Reviewer Agent reviews design before implementation for non-trivial features.
- The Programmer Agent implements the approved spec.
- The Tester Agent validates correctness and coverage.
- The Optimizer Agent runs only after working code and tests exist.
- The Reviewer Agent gives final merge guidance.
