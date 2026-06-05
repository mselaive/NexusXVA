# Java Engineering Skill

## Core Concepts

- Java 21 language features.
- Strong typing and explicit domain models.
- Immutability where practical.
- Clear package boundaries.
- Unit-testable business logic.
- Meaningful exceptions and validation.

## Best Practices

- Keep domain calculations independent from frameworks.
- Prefer small classes with clear responsibilities.
- Use records for simple immutable DTOs or value carriers when suitable.
- Use sealed types only when the closed hierarchy is stable.
- Use `double` for numerical routines where performance and standard quant formulas expect it.
- Use `BigDecimal` for persisted monetary values when decimal precision is required.
- Define numerical tolerances in tests.

## Project-Specific Conventions

- Pricing formulas belong in the `pricing` domain/application area.
- Simulation path generation belongs in `simulation`.
- Exposure metrics belong in `exposure`.
- Simplified CVA calculations currently belong in `cva`.
- Domain behavior should be testable without starting Spring.

## Common Mistakes

- Mixing REST DTOs with domain objects everywhere.
- Putting financial formulas in controllers.
- Using `BigDecimal` inside tight Monte Carlo loops without justification.
- Ignoring numerical tolerance.
- Building generic abstract engines too early.

## Recommended Patterns

- Pure calculator class for closed-form pricing.
- Application service for use-case orchestration.
- DTO mapper at API boundary.
- Explicit validation for domain inputs.
- Deterministic random seed for simulation tests.
