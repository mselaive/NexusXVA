# Testing Skill

## Core Concepts

Testing must protect:

- Financial correctness.
- Domain behavior.
- API contracts.
- Persistence behavior.
- Simulation reproducibility.
- Regression safety.

## Best Practices

- Unit test pure formulas.
- Integration test database and API behavior.
- Use Testcontainers for PostgreSQL.
- Use Mockito for external dependencies or expensive collaborators.
- Use seeded randomness.
- Use tolerances for numerical tests.
- Test validation errors as first-class behavior.

## Project-Specific Conventions

Required financial invariants:

- Option price is not negative.
- European call price should not decrease when spot increases.
- European call price should not decrease when volatility increases.
- At expiry, option value equals intrinsic value.
- Positive exposure cannot be negative.
- PFE at a high percentile should generally be greater than or equal to EE for the same bucket.
- CVA is zero when exposure is zero.
- CVA is zero when LGD is zero.
- CVA increases when default probability increases, all else equal.

## Common Mistakes

- Testing only happy paths.
- Asserting exact Monte Carlo values without tolerance.
- Mocking the class under test.
- Depending on test execution order.
- Ignoring invalid financial inputs.

## Recommended Patterns

- Unit tests for calculators.
- Slice or integration tests for controllers.
- Testcontainers for repositories.
- Regression tests for previously found bugs.
- Test plans stored or derived from `.codex/templates/test-plan.md`.

