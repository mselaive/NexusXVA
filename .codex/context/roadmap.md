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

Status: partially completed for persisted portfolio V1 plus stateless portfolio-level Black-Scholes pricing V1.

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
- Portfolio positions store trade terms only: `underlyingSymbol`, `optionType`, `strike`, `maturityDate`, `quantity`, createdAt, and updatedAt.
- Market data such as `spot`, `riskFreeRate`, `volatility`, and `dividendYield` is intentionally not stored in positions.
- Optional Blemberg validation for `underlyingSymbol` is implemented behind `nexusxva.market-data.validation.enabled`.
- Temporary local watchlist validation is available with `nexusxva.market-data.provider=local`; it does not replace Blemberg.
- Stateless portfolio-level Black-Scholes pricing is implemented at `POST /api/portfolios/{portfolioId}/pricing/black-scholes`.
- Portfolio pricing requests pricing inputs through `marketdata`, scales price and Greeks by quantity, excludes expired positions as `UNPRICEABLE_EXPIRED`, and does not persist valuation results.
- Portfolio pricing V1 is USD-only because FX conversion is not implemented yet.
- The local market-data provider supplies temporary demo pricing inputs, including dividend yield, for development; real pricing inputs should come from Blemberg when that service is running.
- Blemberg HTTP adapter tests should protect instrument validation and European-option pricing input contracts, including stale data, dividend yield, provider failures, and malformed responses.
- `docs/docs-EN/BlembergBuildSpec.md` is the handoff document for the separate Blemberg repo. Keep NexusXVA aligned with that contract instead of persisting provider/reference/market data locally.
- Local Blemberg currently runs at `http://localhost:8081`; use `BLEMBERG_BASE_URL` when another network/compose topology needs a different address.
- Exposure V1 now simulates GBM paths using `spot`, `volatility`, `riskFreeRate`, and `dividendYield`, then reprices the existing portfolio over a time grid.

## Milestone 4: Monte Carlo Simulation

Status: partially completed for synchronous stateless GBM exposure simulations.

Goals:

- Generate simulated spot paths.
- Use deterministic seeds for repeatability.
- Price exposures across time buckets.
- Return simulation metadata in the API response.

Completion criteria:

- Simulation tests are deterministic with fixed seeds.
- Runtime metadata is returned.
- No database access occurs inside path loops.

Current notes:

- `POST /api/simulations/exposure` is implemented as a stateless synchronous simulation endpoint.
- The GBM path generator lives in `simulation.domain` and is deterministic with fixed seeds.
- Exposure orchestration lives in `exposure.application`.
- V1 uses Blemberg/local `marketdata` pricing inputs and Black-Scholes repricing.
- Simulation and exposure results are not persisted yet.

## Milestone 5: Exposure Analytics

Status: partially completed for EE, ENE, and PFE profiles.

Goals:

- Compute positive exposure.
- Compute Expected Exposure.
- Compute Potential Future Exposure.
- Produce exposure profiles for dashboard visualization.

Completion criteria:

- Exposure cannot be negative after clipping.
- PFE percentile behavior is tested.
- Exposure result schema is stable.

Current notes:

- Exposure aggregation returns `expectedExposure`, `expectedNegativeExposure`, and `pfe` per future date.
- Empty portfolios and all-expired portfolios return zero exposure points.
- Expired positions are excluded at future dates where `maturityDate <= simulatedDate`.
- V1 remains USD-only and European-options-only.
- The next functional milestone is simplified CVA over the exposure profile, after a real Blemberg smoke path is stable.

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
