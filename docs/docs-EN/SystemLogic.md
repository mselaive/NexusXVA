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
4. Monte Carlo simulation.
5. Exposure analytics.
6. Simplified CVA.
7. Dashboard.

The idea is that each step uses what came before it, without jumping too far ahead.

## Current State

The backend currently has:

- Health endpoint.
- Stable API error handling.
- Modular package structure.
- Stateless European option pricing with Black-Scholes.
- Main Greeks calculation.
- Persisted portfolio management in PostgreSQL.
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
POST /api/portfolios/{portfolioId}/instruments/european-options
GET /api/portfolios/{portfolioId}/instruments
GET /api/portfolios/{portfolioId}/instruments/{positionId}
PATCH /api/portfolios/{portfolioId}/instruments/european-options/{positionId}
DELETE /api/portfolios/{portfolioId}/instruments/{positionId}
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
  -> portfolio.infrastructure
  -> PostgreSQL
  -> HTTP response
```

In more detail:

1. The controller receives JSON.
2. The DTO validates required fields and basic ranges.
3. The DTO is transformed into an application command.
4. The application service opens the transaction and executes the use case.
5. The `PortfolioStore` port abstracts persistence.
6. The JPA adapter persists or retrieves entities.
7. The API returns DTOs, not JPA entities.

This gives us real persistence without mixing HTTP rules, domain rules, and database details.

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

## Current Financial Decisions

For the first pricing slice we decided to:

- Support only European `CALL` and `PUT` options.
- Use classic Black-Scholes.
- Use constant volatility.
- Use a constant continuously compounded risk-free rate.
- Exclude dividend yield for now.
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
- Store `underlyingSymbol`, `optionType`, `strike`, `maturityDate`, and `quantity`.
- Allow positive or negative quantity for long/short positions.
- Reject zero quantity.
- Allow past `maturityDate`; pricing decides whether expired trades can be valued.
- Not store `spot`, `volatility`, or `riskFreeRate` inside the position.

The trade/market-data separation is intentional.
A portfolio describes which instruments we hold; market data describes the market state used to value those instruments.
That is why future portfolio-level pricing should read persisted positions and receive market data from the request or from a dedicated module.

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
- API tests for creating/listing/getting/updating/deleting positions.
- `400` and `404` error tests with `ApiError`.

The rule is:

> If a financial formula or persisted workflow changes, the tests should tell us whether it changed for an explicit reason or because we broke an expected property.

## What Portfolio Gives Us

Portfolio now has a first persisted version.

This gives us:

- A persisted business unit.
- A clear way to store European option positions.
- A base for portfolio-level pricing.
- A base for simulation, exposure, and CVA later.

The order we followed was:

1. Price one individual option.
2. Create portfolios.
3. Add options to portfolios.
4. Retrieve portfolios.
5. Prepare portfolio-level pricing.

This avoids building Monte Carlo or CVA before we have a persisted unit on which to calculate risk.

## Recommended Next Milestone

The recommended next milestone is portfolio-level pricing.

Suggested first version:

- Receive a portfolio id.
- Read persisted positions.
- Receive market data in the request.
- Convert `maturityDate` into `timeToMaturityYears`.
- Reuse the existing Black-Scholes calculator.
- Return price and Greeks per position plus a portfolio total.

Out of initial scope:

- Monte Carlo.
- Exposure.
- CVA.
- Auth.
- Multi-currency.
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
