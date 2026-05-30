# Code Review Workflow

## Purpose

Use this workflow before merging implementation changes.

## Review Order

1. Confirm the code maps to an approved Planner spec.
2. Review module boundaries.
3. Review domain correctness.
4. Review API contracts.
5. Review persistence design.
6. Review test quality.
7. Review operational concerns.
8. Produce final verdict.

## Blocking Issues

Blocking issues include:

- Untested financial formulas.
- Incorrect or undocumented financial assumptions.
- Business logic in controllers.
- Persistence entities exposed as API contracts.
- Non-deterministic simulation tests.
- Broad infrastructure added without a spec.
- Validation gaps that allow invalid financial inputs.

## Non-Blocking Issues

Non-blocking issues may include:

- Naming improvements.
- Documentation refinements.
- Small duplication.
- Future extensibility notes.
- Performance improvements that are not needed for MVP.

## Review Output

Use:

1. Blocking issues.
2. Non-blocking issues.
3. Test gaps.
4. Architecture concerns.
5. Financial correctness concerns.
6. Recommended changes.
7. Final verdict.

