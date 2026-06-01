# Roadmap

## Guiding Principle

Build the smallest realistic platform that demonstrates strong engineering and quantitative reasoning, then expand through well-tested vertical slices.

## Milestone 0: Repository Foundation

Goals:

- Establish project documentation.
- Establish `.codex` AI workflow structure.
- Define architecture and conventions.
- Create initial README and contribution expectations.

Completion criteria:

- `.codex` directory is complete.
- Agents and workflows are documented.
- Architecture and project vision are clear.

## Milestone 1: Backend Foundation

Goals:

- Create Java 21 Spring Boot backend.
- Establish module/package structure.
- Configure testing stack.
- Configure PostgreSQL and Docker Compose.
- Add health endpoint and basic CI.

Completion criteria:

- Application starts locally.
- Unit tests run.
- Integration tests can use Testcontainers.
- Docker Compose starts required dependencies.

## Milestone 2: European Option Pricing

Status: partially completed for stateless Black-Scholes pricing.

Goals:

- Model European call and put options.
- Model simple market data inputs.
- Implement Black-Scholes pricing.
- Implement Greeks.
- Expose REST API for pricing.

Completion criteria:

- Closed-form pricing tests pass.
- Financial invariant tests pass.
- API tests validate request and response behavior.

Current notes:

- Stateless Black-Scholes API pricing with Greeks is implemented.
- The current endpoint intentionally requires positive time to maturity; exact expiry payoff is deferred to explicit instrument/payoff modeling.
- Portfolio-level pricing and persistence are handled in Milestone 3, not in this stateless pricing endpoint.

## Milestone 3: Portfolio Management

Status: partially completed for persisted portfolio V1.

Goals:

- Create portfolios.
- Add European options to portfolios.
- Retrieve portfolio contents.
- Prepare portfolio-level pricing.

Completion criteria:

- Portfolio workflows are persisted.
- API contracts are documented.
- Integration tests cover persistence.

Current notes:

- Persisted portfolio creation is implemented.
- Portfolio listing, metadata update, and deletion are implemented.
- Persisted European option positions are implemented.
- Individual position get/update/delete workflows are implemented.
- Portfolio metadata includes optional description, base currency, createdAt, and updatedAt.
- Portfolio positions store trade terms only: `underlyingSymbol`, `optionType`, `strike`, `maturityDate`, and `quantity`.
- Market data such as `spot`, `riskFreeRate`, and `volatility` is intentionally not stored in positions.
- Portfolio-level pricing is the next step and should reuse the existing Black-Scholes calculator.

## Milestone 4: Monte Carlo Simulation

Goals:

- Generate simulated spot paths.
- Use deterministic seeds for repeatability.
- Price exposures across time buckets.
- Record simulation metadata.

Completion criteria:

- Simulation tests are deterministic with fixed seeds.
- Runtime metadata is captured.
- No database access occurs inside path loops.

## Milestone 5: Exposure Analytics

Goals:

- Compute positive exposure.
- Compute Expected Exposure.
- Compute Potential Future Exposure.
- Produce exposure profiles for dashboard visualization.

Completion criteria:

- Exposure cannot be negative after clipping.
- PFE percentile behavior is tested.
- Exposure result schema is stable.

## Milestone 6: Simplified CVA

Goals:

- Compute simplified CVA from exposure profile, discount factors, default probabilities, and LGD.
- Document assumptions.
- Provide API for CVA calculation.

Completion criteria:

- CVA is zero when exposure is zero.
- CVA is zero when LGD is zero.
- CVA increases when default probability increases, all else equal.

## Milestone 7: Dashboard

Goals:

- Add Next.js dashboard.
- Visualize portfolio contents.
- Visualize pricing results.
- Visualize exposure profiles.
- Visualize CVA summary.

Completion criteria:

- Dashboard consumes backend APIs.
- Charts are readable and domain-specific.
- E2E tests cover primary flows.

## Milestone 8: Hardening

Goals:

- Improve error handling.
- Add structured logging.
- Add metrics.
- Add performance benchmarks.
- Improve documentation.

Completion criteria:

- Common failure modes are tested.
- Simulation duration is visible.
- CI is stable.

## Future Milestones

Future work can include:

- DVA.
- FVA.
- KVA.
- Stress testing.
- Scenario engine.
- More instruments.
- Distributed simulation.
- Kafka.
- Kubernetes.

These should not be added until the MVP is coherent and tested.
