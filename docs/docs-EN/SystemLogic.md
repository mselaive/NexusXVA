# NexusXVA System Logic

This document explains how the system logic works as we build it.
It does not replace the README or the tests: it is a living map so other developers can understand why the code is organized this way and which decisions we made at each stage.

## Chosen Path

NexusXVA is being built as a modular monolith.
That means we have one backend application, internally separated into clear modules.

The reason is simple:

- First we need to prove financial correctness and solid backend design.
- We do not need microservices, Kafka, or distributed execution yet.
- Each feature should be testable and explainable as a small vertical slice.

The chosen order is:

1. Backend foundation.
2. Stateless pricing for European options.
3. Portfolio management.
4. Portfolio-level pricing.
5. Monte Carlo simulation.
6. Exposure analytics.
7. Simplified CVA.
8. Dashboard.

The idea is that each step uses what came before it, without jumping too far ahead.

## Current State

The backend currently has:

- Health endpoint.
- Stable API error handling.
- Modular package structure.
- Stateless European option pricing with Black-Scholes.
- Main Greeks calculation.
- Persisted portfolio management in PostgreSQL.
- Stateless portfolio-level Black-Scholes pricing.
- Blemberg market-data integration for symbol validation and pricing inputs.
- Temporary local market-data pricing inputs as a development fallback.
- Exposure V1 using simple GBM Monte Carlo and Black-Scholes repricing.
- Simplified CVA V1.1 over the exposure profile.
- Dashboard V1 frontend for portfolio, pricing, exposure and CVA workflows.
- Database migrations with Flyway.
- Financial, API, and persistence tests.

The main current endpoints are:

```text
POST /api/pricing/european-options/black-scholes
POST /api/portfolios
GET /api/portfolios
GET /api/portfolios/{portfolioId}
PATCH /api/portfolios/{portfolioId}
DELETE /api/portfolios/{portfolioId}
GET /api/portfolios/{portfolioId}/instruments
GET /api/portfolios/{portfolioId}/instruments/{positionId}
POST /api/portfolios/{portfolioId}/trade-bookings/european-options
GET /api/trade-bookings/mine
GET /api/back-office/trade-bookings
POST /api/back-office/trade-bookings/{bookingId}/approve
POST /api/back-office/trade-bookings/{bookingId}/reject
POST /api/portfolios/{portfolioId}/pricing/black-scholes
POST /api/simulations/exposure
POST /api/risk/cva
```

The pricing endpoint receives European option data and returns:

- Theoretical price.
- Delta.
- Gamma.
- Vega.
- Theta.
- Rho.

The portfolio endpoints allow us to:

- Create portfolios.
- List portfolio summaries with position counts.
- Retrieve a portfolio by id.
- Update portfolio metadata.
- Delete portfolios.
- Add European option positions.
- List instruments in a portfolio.
- Get, update, and delete individual positions.
- Price persisted European option positions as a portfolio using Black-Scholes.
- Simulate future exposure profiles for persisted portfolios.
- Calculate simplified CVA from exposure profiles.
- Inspect portfolios, pricing, exposure and CVA through the dashboard.

## How a Pricing Request Flows

The current flow is:

```text
HTTP request
  -> pricing.api
  -> pricing.application
  -> pricing.domain
  -> HTTP response
```

In more detail:

1. The controller receives JSON.
2. The DTO validates required fields and basic ranges.
3. The DTO is transformed into a domain input.
4. The application service executes the use case.
5. The pure calculator applies Black-Scholes.
6. The domain validates that the result is finite.
7. The API returns price and Greeks.

This keeps the financial formula outside Spring MVC.
That separation matters because the math must be testable without starting the application.

## How a Portfolio Request Flows

The current portfolio flow is:

```text
HTTP request
  -> portfolio.api
  -> portfolio.application
  -> marketdata.application
  -> Blemberg REST API (if validation is enabled)
  -> portfolio.infrastructure
  -> PostgreSQL
  -> HTTP response
```

In more detail:

1. The controller receives JSON.
2. The DTO validates required fields and basic ranges.
3. The DTO is transformed into an application command.
4. The application service validates the symbol against market data if the integration is enabled.
5. The `PortfolioStore` port abstracts persistence.
6. The JPA adapter persists or retrieves entities.
7. The API returns DTOs, not JPA entities.

This gives us real persistence without mixing HTTP rules, domain rules, and database details.

## How a Portfolio Pricing Request Flows

The current portfolio pricing flow is:

```text
HTTP request
  -> portfolio.api
  -> portfolio.application
  -> marketdata.application pricing inputs
  -> pricing.domain Black-Scholes
  -> aggregated portfolio valuation
  -> HTTP response
```

In more detail:

1. The controller receives the portfolio id and optional `valuationDate`.
2. The application service loads the persisted portfolio and positions.
3. The service asks `marketdata` for `spot`, `volatility`, `riskFreeRate`, and `dividendYield` for each priceable symbol.
4. The service converts `maturityDate` into `timeToMaturityYears` using ACT/365.
5. The existing Black-Scholes calculator prices each live position.
6. Price and Greeks are scaled by `quantity`.
7. Expired positions are returned as `UNPRICEABLE_EXPIRED` and excluded from totals.

This is still stateless pricing.
The portfolio stores trade terms, the market-data module provides valuation inputs, and pricing results are not persisted.

## How an Exposure Simulation Request Flows

The current exposure flow is:

```text
HTTP request
  -> simulation.api
  -> exposure.application
  -> portfolio.application / portfolio.infrastructure
  -> marketdata.application pricing inputs
  -> simulation.domain GBM paths
  -> pricing.domain Black-Scholes repricing
  -> exposure.domain aggregation
  -> HTTP response
```

In more detail:

1. The controller receives simulation parameters such as `portfolioId`, `valuationDate`, `horizonDays`, `timeSteps`, `paths`, `seed`, and `pfeConfidenceLevel`.
2. The application service loads the persisted portfolio and live European option positions.
3. The service requests one pricing-input set per underlying through `marketdata`.
4. The GBM path generator simulates future spot values using `spot`, `riskFreeRate`, `dividendYield`, and `volatility`.
5. Each live position is repriced at each future date with Black-Scholes.
6. Expired positions are excluded from the future date where `maturityDate <= simulatedDate`.
7. The exposure aggregator returns expected exposure, expected negative exposure, and PFE for each time bucket.

Exposure V1 is synchronous and stateless.
It does not persist paths, market data, or exposure results.

## How a CVA Request Flows

The current CVA flow is:

```text
HTTP request
  -> cva.api
  -> cva.application
  -> exposure.application
  -> cva.domain simplified CVA formula
  -> HTTP response
```

In more detail:

1. The controller receives portfolio, simulation, and simple credit parameters.
2. The CVA application service asks Exposure V1 to produce expected exposure by future date.
3. The CVA calculator applies a flat hazard-rate default model and flat discount rate.
4. Each bucket contributes `LGD * discountFactor * expectedExposure * defaultProbabilityIncrement`.
5. The API returns total CVA plus per-date contribution details.

CVA V1 is synchronous and stateless.
It does not persist exposure, default probabilities, or CVA results.

## How the Dashboard Flows

Dashboard V1 is a Next.js frontend in `frontend/`.
It does not implement financial formulas.
The UI is split by active group. FO uses overview, `u-Pad`, portfolios, pricing, exposure and CVA. BO uses Trade Validation and Trading Limits.

The frontend flow is:

```text
dashboard user action
  -> frontend API client
  -> NexusXVA backend endpoint
  -> backend pricing / exposure / CVA modules
  -> dashboard tables and charts
```

The dashboard uses the backend as the source of truth for:

- Portfolio data.
- Pending booking submission through `u-Pad`.
- BO validation before confirmed positions are created.
- FO limit visibility in `u-Pad` and BO policy management.
- Portfolio-level Black-Scholes pricing.
- Exposure simulation.
- CVA calculation.

For local development, the frontend calls `/nexus-api/*`, which Next.js proxies to the backend.
The frontend never calls Blemberg directly.
`u-Pad` submits trade terms only; market data remains owned by `marketdata`/Blemberg.

## How Trade Validation Flows

```text
FO submits from u-Pad
  -> trade_booking_requests: PENDING_VALIDATION
  -> BO opens Trade Validation
  -> approve: create one confirmed position
  -> reject: retain the booking with a reason
```

Pending and rejected bookings are not portfolio positions and never enter pricing, exposure, or CVA.
Approval locks the request inside one transaction to prevent duplicate positions.
Confirmed positions are immutable until controlled amendment and cancellation workflows exist.

A user may belong to several groups, but `auth_sessions.active_group_code` stores one active context. Backend authorization, not `localStorage`, enforces FO/BO access.

## How Trading Limits Flow

```text
FO submits from u-Pad
  -> load and lock the user's trading-limit policy
  -> derive current UTC hour/day usage from trade_booking_requests
  -> validate trade count and abs(quantity) * strike notional
  -> create PENDING_VALIDATION or return 409
```

There is one optional policy per active FO user. Missing or disabled policies mean `UNLIMITED`; individual null fields mean that measure is unlimited. Usage is derived from booking history rather than a duplicated counter, so submitted bookings continue to count even if BO rejects them later.

Notional V1 is denominated in USD and is only a preventive approximation. It is not premium, cash spent, P&L, current market value, or a Greek limit. If a notional control is active, non-USD portfolios are rejected until FX conversion exists. Policy locking and booking creation share a transaction so concurrent requests cannot jointly pass the same remaining capacity.

BO endpoints live under `/api/back-office/trading-limits/users`; FO can only read its own snapshot through `/api/trading-limits/me`. A breach returns sanitized `409 ApiError.metadata` with the limit, usage, requested amount, and reset time.

## Layer Responsibilities

### `pricing.api`

Owns the HTTP contract:

- URL.
- Request JSON.
- Response JSON.
- Boundary validation through annotations.
- Translation between DTOs and domain objects.

It should not contain financial formulas.

### `pricing.application`

Owns use-case orchestration.

For now it is simple: it receives the input and calls the calculator.
Later it may coordinate portfolio-level pricing, observability, or persistence integration.

### `pricing.domain`

Contains pure financial logic.

This is where we keep:

- Domain inputs.
- Results.
- Greeks.
- Black-Scholes calculator.
- Financial invariants.

This layer must not depend on controllers, HTTP, JSON, or the database.

### `portfolio.api`

Owns the portfolio HTTP contract:

- Create portfolio.
- List portfolios.
- Get portfolio by id.
- Update portfolio.
- Delete portfolio.
- Add European option to portfolio.
- List portfolio instruments.
- Get, update, and delete individual positions.

It must not return JPA entities directly.

### `portfolio.application`

Owns portfolio use cases and transactions.

For now it coordinates:

- Creating portfolios.
- Listing portfolios.
- Updating metadata.
- Deleting portfolios and their positions.
- Checking that a portfolio exists before adding/listing positions.
- Adding European option positions.
- Operating individual positions.
- Pricing a portfolio with Black-Scholes without persisting valuation results.

### `portfolio.domain`

Contains portfolio and position rules.

This is where we validate that:

- A portfolio has a name.
- Description is optional and short.
- Base currency is a 3-letter code, defaulting to `USD`.
- The underlying symbol exists and is normalized.
- Strike is greater than zero.
- Quantity is non-zero.

### `portfolio.infrastructure`

Contains JPA entities, repositories, and the adapter that implements `PortfolioStore`.

This layer knows PostgreSQL/JPA; API, application, and domain layers should not depend on those details.

### `marketdata.application`

Owns the internal ports for instrument validation and European-option pricing inputs.

Portfolio and exposure code should talk to `marketdata.application`, not directly to Blemberg adapters.

### `marketdata.infrastructure`

Owns external and temporary market-data providers:

- Blemberg REST adapters.
- Local watchlist/pricing-input adapter for development.

It should not persist market data inside NexusXVA.

### `simulation.api`

Owns the exposure simulation HTTP contract.

It validates request shape and translates DTOs into application commands.

### `simulation.domain`

Contains pure simulation logic such as deterministic GBM path generation.

It must be testable without Spring, HTTP, or the database.

### `exposure.application`

Owns exposure use-case orchestration:

- Load portfolio positions.
- Request market-data pricing inputs.
- Generate simulated spot paths.
- Reprice positions across the time grid.
- Return a stateless exposure result.

### `exposure.domain`

Contains exposure aggregation logic:

- Expected Exposure.
- Expected Negative Exposure.
- Potential Future Exposure.

### `cva.api`

Owns the CVA HTTP contract.

It validates request shape and translates DTOs into CVA application commands.

### `cva.application`

Owns CVA use-case orchestration.

It calls exposure simulation and passes the resulting exposure profile into the CVA domain calculator.

### `cva.domain`

Contains the simplified CVA formula and output points.

It must stay independent from Spring, HTTP, Blemberg, and the database.

## Current Financial Decisions

For the first pricing slice we decided to:

- Support only European `CALL` and `PUT` options.
- Use classic Black-Scholes.
- Use constant volatility.
- Use a constant continuously compounded risk-free rate.
- Support optional continuous `dividendYield`, defaulting to `0.0`.
- Use decimals for rates and volatility: `0.05` means 5%.
- Allow a negative risk-free rate if the result remains finite.
- Require `timeToMaturityYears > 0`.
- Keep `OptionType` in `instruments.domain` because it is shared by pricing and portfolio.

The exact expiry case (`timeToMaturityYears = 0`) is outside the current endpoint.
The reason is that at expiry the price becomes intrinsic payoff and several Greeks may be discontinuous or undefined.
That case will be modeled better once we have more explicit instruments/payoffs.

## Current Portfolio Decisions

For the first portfolio slice we decided to:

- Persist portfolios in PostgreSQL.
- Persist European option positions.
- Use UUIDs as public and database identifiers.
- Store portfolio metadata: `name`, `description`, `baseCurrency`, `createdAt`, and `updatedAt`.
- Store `underlyingSymbol`, `optionType`, `strike`, `maturityDate`, `quantity`, `createdAt`, and `updatedAt`.
- Allow positive or negative quantity for long/short positions.
- Reject zero quantity.
- Allow past `maturityDate`; pricing decides whether expired trades can be valued.
- Not store `spot`, `volatility`, or `riskFreeRate` inside the position.
- Validate `underlyingSymbol` against Blemberg when `nexusxva.market-data.validation.enabled=true`.
- Allow `nexusxva.market-data.provider=local` as a temporary watchlist and pricing-input provider for local development.
- Price portfolios only in `USD` for V1; FX conversion is not implemented yet.
- Use Blemberg as the target real source of pricing inputs.
- Use the local provider only as a temporary source of demo pricing inputs.
- Keep portfolio pricing stateless; do not persist valuation results.

The trade/market-data separation is intentional.
A portfolio describes which instruments we hold; market data describes the market state used to value those instruments.
That is why NexusXVA keeps positions as trade terms and uses the `marketdata` module as the boundary to Blemberg.
Blemberg validates instrument existence and provides real `spot`, `volatility`, `riskFreeRate`, and `dividendYield`.
The local provider validates known symbols and provides temporary demo pricing inputs, but it does not persist market data or replace Blemberg.

## Current Exposure Decisions

For Exposure V1 we decided to:

- Support persisted European option portfolios only.
- Use a simple GBM model for future spot paths.
- Reuse Black-Scholes for repricing at future dates.
- Use `spot`, `volatility`, `riskFreeRate`, and `dividendYield` from `marketdata`.
- Keep simulation deterministic when a fixed `seed` is provided.
- Return expected exposure, expected negative exposure, and PFE by date.
- Exclude positions once they are expired for a simulated future date.
- Keep exposure stateless and synchronous.
- Keep USD-only until FX conversion is implemented.

This is the bridge between portfolio pricing and CVA.
CVA should consume a tested exposure profile instead of jumping directly from current pricing to credit valuation.

## Current CVA Decisions

For CVA V1 we decided to:

- Reuse Exposure V1 instead of creating a separate simulation flow.
- Use expected exposure, not PFE, for the CVA contribution.
- Support either a flat annual counterparty hazard rate or request-provided credit curves.
- Support either a flat continuously compounded discount rate or request-provided discount curves.
- Linearly interpolate curve values for exposure dates inside the curve range.
- Use `lossGivenDefault` directly, with values between `0.0` and `1.0`.
- Keep CVA stateless and synchronous.
- Keep USD-only and European-options-only through the reused exposure flow.

This is intentionally simplified.
It is enough to prove the XVA path from portfolio to exposure to credit adjustment without introducing persisted counterparties, persisted credit curves, collateral, or netting sets yet.

## Error Policy

The API should return stable and clear errors.

Current rules:

- Invalid inputs return `400 Bad Request`.
- Missing fields return `ApiError` with `details`.
- Invalid enum values return a clean message, without internal Jackson names.
- Unexpected errors must not leak stack traces or internal class names.
- Numeric `NaN` or `Infinity` results are rejected.

This matters because the frontend and future consumers need predictable contracts.

## How We Test This Logic

The pricing slice is protected with:

- Known-value tests for calls and puts.
- Put-call parity.
- Call monotonicity with respect to spot.
- Call monotonicity with respect to volatility.
- Valid negative rates.
- Invalid input rejection.
- Non-finite result rejection.
- API tests with numeric tolerances.
- API tests for error shape.

The portfolio slice is protected with:

- Domain tests for names, symbols, strikes, and quantities.
- Integration tests with PostgreSQL/Testcontainers.
- API tests for creating/listing/updating/deleting portfolios.
- API tests for submitting, approving, and rejecting bookings.
- Tests confirming pending bookings do not affect pricing, exposure, or CVA.
- Tests for Blemberg validation enabled and disabled.
- Tests for the temporary local watchlist.
- `400` and `404` error tests with `ApiError`.

The exposure slice is protected with:

- Deterministic GBM path tests with fixed seeds.
- Drift tests showing that `dividendYield` affects simulated paths.
- Exposure aggregation fixture tests for EE, ENE, and PFE.
- Application tests for empty portfolios, expired positions, USD-only rules, and missing market data.
- API tests for valid exposure requests and invalid simulation parameters.

The CVA slice is protected with:

- Formula tests for bucket-level default probability increments and discounting.
- Invariant tests: zero exposure, zero LGD, and increasing hazard rate.
- Application tests confirming CVA consumes Exposure V1.
- API tests for valid CVA requests, validation errors, unknown portfolios, and USD-only behavior.

The rule is:

> If a financial formula or persisted workflow changes, the tests should tell us whether it changed for an explicit reason or because we broke an expected property.

## What Portfolio Gives Us

Portfolio now has a first persisted version and a first stateless Black-Scholes valuation flow.

This gives us:

- A persisted business unit.
- A clear way to store European option positions.
- Portfolio-level pricing for USD European option positions.
- Exposure profiles for USD European option portfolios.
- A simplified CVA calculation over exposure profiles.

The order we followed was:

1. Price one individual option.
2. Create portfolios.
3. Submit bookings from FO and confirm them from BO.
4. Retrieve portfolios.
5. Prepare portfolio-level pricing.
6. Price portfolios with Black-Scholes using inputs from `marketdata`.
7. Simulate exposure profiles using GBM plus Black-Scholes repricing.
8. Calculate simplified CVA from expected exposure and flat credit assumptions.

This avoided building Monte Carlo or CVA before we had a persisted unit on which to calculate risk.
Now CVA V1.1 exists as the first XVA adjustment slice and Dashboard V1 is the active product slice.

## Recommended Next Milestone

The recommended next milestone is finishing Dashboard V1 and then hardening user-facing workflows.

Suggested next version:

- Keep the current portfolio pricing, exposure and CVA endpoints.
- Keep the dashboard focused on visualization and workflow orchestration.
- Do not move pricing, Monte Carlo or CVA calculations into the frontend.
- Use real Blemberg as the source of `spot`, `volatility`, `riskFreeRate`, and `dividendYield` whenever it is running.
- Keep the Blemberg response fixtures in `BlembergContractFixtures.md` aligned with NexusXVA adapter tests.
- Use `BlembergBuildSpec.md` as the implementation brief for the separate Blemberg repository.
- Use `http://localhost:8081` as the local Blemberg base URL unless overridden with `BLEMBERG_BASE_URL`.
- Keep NexusXVA from persisting market data.
- Treat Blemberg snapshots as diagnostic/cache data only; pricing and exposure must keep using pricing inputs.
- Accept Blemberg V1 `501` for `/v3/api-docs` as expected because runtime integration does not depend on OpenAPI.
- Keep the optional real Blemberg smoke test disabled by default and enable it with `RUN_REAL_BLEMBERG_SMOKE=true`.
- Keep simplified CVA stateless until we explicitly introduce persisted valuation runs.
- Add FX only when we want to support multi-currency totals.
- Add CVA curve-mode UI, real counterparties, netting, and collateral only after Dashboard V1 is usable.

Out of initial scope:

- ADMIN user-management screens.
- Multi-currency without FX.
- Netting/collateral.

## How to Maintain This Document

Every time we add an important slice, update:

- What new flow exists.
- Which modules participate.
- Which financial assumptions we made.
- What remains out of scope.
- Which tests protect the logic.
- Which milestone naturally comes next.

This file should stay short, practical, and decision-oriented.
