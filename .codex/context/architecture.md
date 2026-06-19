# Architecture Context

## Architectural Style

NexusXVA starts as a modular monolith.

This means:

- One deployable backend application.
- Clear internal module boundaries.
- No distributed-system complexity in the MVP.
- Module interfaces should be explicit enough that future extraction is possible.
- Local development should remain fast and understandable.

Microservices, Kafka, distributed simulation, Kubernetes, and advanced orchestration are future topics, not MVP defaults.

## Backend Module Map

Recommended backend modules:

- `portfolio`: owns portfolios, portfolio composition, and portfolio-level operations.
- `instruments`: owns derivative definitions such as European options.
- `market-data`: owns market data inputs, curves, volatility assumptions, and validation.
- `pricing`: owns pricing models, Greeks, pricing requests, and pricing results.
- `simulation`: owns stochastic path generation, random seeds, scenario generation, and simulation jobs.
- `exposure`: owns exposure profiles, Expected Exposure, and Potential Future Exposure.
- `cva`: owns the current simplified CVA slice; broader XVA adjustments can become their own modules when needed.
- `jobs`: owns asynchronous or long-running execution coordination when needed.
- `reporting`: owns result summaries and export-ready views.
- `auth`: owns login, users, groups, group memberships, and opaque HTTP sessions.
- `tradebooking`: owns FO booking requests, BO approval/rejection, and booking audit history.
- `tradinglimits`: owns per-FO preventive policies, UTC usage derivation, and booking-time limit enforcement.

## Layering

Each backend module should follow this general layering:

- API layer: REST controllers and request/response DTOs.
- Application layer: use cases, orchestration, transaction boundaries.
- Domain layer: financial concepts, calculations, policies, and invariants.
- Infrastructure layer: persistence, external integrations, adapters.

Rules:

- Controllers should not contain business logic.
- Domain services should not depend on Spring MVC or persistence details.
- Persistence entities should not dictate the domain model when the concepts diverge.
- DTOs should define external contracts explicitly.
- Financial formulas should be independently testable.

## Initial Vertical Slice

The recommended first vertical slice is:

1. Define a European option.
2. Define simple market data.
3. Price the option using Black-Scholes.
4. Calculate Greeks.
5. Expose REST endpoints.
6. Persist only what is needed for the workflow.
7. Add unit and integration tests.

After this slice is stable, add:

1. Portfolio creation.
2. Portfolio instruments.
3. Monte Carlo path generation.
4. Exposure profiles.
5. Simplified CVA.

## API Principles

APIs should be boring, explicit, and stable.

Use:

- Clear resource names.
- Explicit request and response DTOs.
- Input validation.
- Stable identifiers.
- Consistent error responses.
- No leaking of persistence internals.

API examples:

- `POST /api/portfolios`
- `POST /api/portfolios/{portfolioId}/trade-bookings/european-options`
- `POST /api/back-office/trade-bookings/{bookingId}/approve`
- `POST /api/pricing/european-options/black-scholes`
- `POST /api/simulations/exposure`
- `POST /api/risk/cva`
- `GET /api/jobs/{jobId}`

## Data Model Principles

Separate conceptual domain objects from database persistence concerns where useful.

Initial concepts:

- Portfolio.
- Instrument.
- EuropeanOption.
- MarketDataSnapshot.
- PricingRequest.
- PricingResult.
- Greeks.
- SimulationRun.
- ExposureProfile.
- ExposurePoint.
- CvaResult.

Database design should be practical, not abstract. Avoid a universal "everything is a JSON blob" model for core concepts. JSON can be used for flexible outputs, diagnostics, or future result payloads when justified.

## Observability

The backend should track:

- Request identifiers.
- Pricing execution duration.
- Simulation execution duration.
- Number of paths and time steps.
- Failures with structured error context.
- Job status for long-running operations.

Observability should help explain system behavior without overwhelming the codebase.

## Evolution Path

Phase 1:

- Modular monolith.
- European option pricing.
- Greeks.
- Basic REST API.
- PostgreSQL persistence.

Phase 2:

- Portfolios.
- Monte Carlo.
- Exposure metrics.
- Simplified CVA.
- Dashboard views.

Phase 3:

- Stress scenarios.
- More instruments.
- Batch jobs.
- Performance benchmarks.
- Better reporting.

Phase 4:

- DVA, FVA, KVA.
- Distributed simulation exploration.
- Kafka where event workflows justify it.
- Kubernetes deployment manifests.
