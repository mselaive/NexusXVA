# Feature Development Workflow

## Purpose

Use this workflow for every non-trivial feature.

## Steps

1. Planner creates a feature specification.
2. Reviewer validates the design.
3. Planner revises the specification if needed.
4. Programmer implements the approved specification.
5. Tester validates coverage and adds or recommends missing tests.
6. Optimizer reviews only if the feature is performance-sensitive or operationally relevant.
7. Reviewer performs final review.
8. Changes are merged when the verdict is acceptable.

## Planner Requirements

The Planner must define:

- Scope.
- Out-of-scope items.
- API contracts.
- Domain model.
- Data model.
- Module responsibilities.
- Acceptance criteria.
- Test expectations.

## Programmer Requirements

The Programmer must:

- Follow the approved spec.
- Keep changes scoped.
- Add relevant tests.
- Document assumptions.

## Tester Requirements

The Tester must:

- Validate financial invariants.
- Validate edge cases.
- Validate API and persistence behavior when applicable.
- Use deterministic simulation tests.

## Reviewer Requirements

The Reviewer must:

- Identify blocking issues.
- Enforce clean boundaries.
- Reject unclear financial logic.
- Distinguish MVP concerns from future work.

