# Testing Workflow

## Purpose

Use this workflow to create or review tests for a feature.

## Steps

1. Read the Planner spec.
2. Identify financial invariants.
3. Identify input validation cases.
4. Identify domain unit tests.
5. Identify API tests.
6. Identify persistence integration tests.
7. Identify deterministic simulation tests.
8. Add or recommend tests.
9. Report residual risks.

## Required Test Categories

- Formula correctness tests.
- Financial invariant tests.
- Boundary and invalid input tests.
- API contract tests.
- Persistence tests where data is stored.
- Regression tests for fixed bugs.

## Monte Carlo Testing

Monte Carlo tests must:

- Use fixed seeds.
- Use tolerances.
- Avoid relying on wall-clock timing.
- Avoid asserting fragile exact paths unless intentionally testing path generation.
- Validate distribution-level properties where appropriate.

## Test Report Format

1. Test objective.
2. Scope.
3. Cases covered.
4. Cases missing.
5. Risks.
6. Recommended next tests.

