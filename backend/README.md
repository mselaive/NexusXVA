# NexusXVA Backend

Spring Boot backend for NexusXVA.

The backend currently includes the project foundation, stateless European option pricing with the Black-Scholes model and Greeks, and persisted portfolio management for European option positions. Portfolio-level pricing, Monte Carlo simulation, exposure analytics, and XVA calculations are planned future milestones.

## Requirements

- Java 21
- Maven 3.6.3 or newer
- Docker and Docker Compose for the containerized workflow
- PostgreSQL for persisted portfolio workflows

## Run Locally

```bash
mvn spring-boot:run
```

The backend starts on port `8080`.

Health check:

```bash
curl http://localhost:8080/api/health
```

Black-Scholes pricing example:

```bash
curl -X POST http://localhost:8080/api/pricing/european-options/black-scholes \
  -H 'Content-Type: application/json' \
  -d '{
    "optionType": "CALL",
    "spot": 100.0,
    "strike": 100.0,
    "timeToMaturityYears": 1.0,
    "riskFreeRate": 0.05,
    "volatility": 0.2
  }'
```

## Run With Docker

From the repository root:

```bash
docker compose up --build
```

This starts:

- `backend` on `http://localhost:8080`
- `postgres` on `localhost:5432`

Portfolio management uses PostgreSQL through Spring Data JPA. Flyway runs the database migrations on application startup.

## Run Tests

```bash
mvn test
```

The current test setup covers:

- Spring application context startup
- `/api/health`
- stable validation error response shape
- PostgreSQL integration tests with Testcontainers
- Black-Scholes known-value tests for European calls and puts
- financial invariants such as put-call parity and monotonicity
- pricing API request, response, and validation behavior
- persisted portfolio metadata, listing, update/delete, and European option position workflows

The current suite is verified with `mvn test`.

## Developer Financial Docs

Conceptual financial guides for developers live in:

- Spanish: [`../docs/docs-ES/ConceptosFinancieros.md`](../docs/docs-ES/ConceptosFinancieros.md)
- System logic ES: [`../docs/docs-ES/LogicaDelSistema.md`](../docs/docs-ES/LogicaDelSistema.md)
- System logic EN: [`../docs/docs-EN/SystemLogic.md`](../docs/docs-EN/SystemLogic.md)
- English: [`../docs/docs-EN/FinancialConcepts.md`](../docs/docs-EN/FinancialConcepts.md)

## Portfolio Management

Portfolio management persists portfolios and European option positions in PostgreSQL.

Endpoints:

```text
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

Create portfolio request:

```json
{
  "name": "Demo Portfolio",
  "description": "Equity option demo book",
  "baseCurrency": "USD"
}
```

Create portfolio response:

```json
{
  "id": "6f2d4637-ef84-4bc5-bca3-7c7aee54b4e5",
  "name": "Demo Portfolio",
  "description": "Equity option demo book",
  "baseCurrency": "USD",
  "createdAt": "2026-06-01T01:54:39.212Z",
  "updatedAt": "2026-06-01T01:54:39.212Z",
  "positions": []
}
```

List portfolios returns summaries with `positionCount`:

```json
[
  {
    "id": "6f2d4637-ef84-4bc5-bca3-7c7aee54b4e5",
    "name": "Demo Portfolio",
    "description": "Equity option demo book",
    "baseCurrency": "USD",
    "createdAt": "2026-06-01T01:54:39.212Z",
    "updatedAt": "2026-06-01T01:54:39.212Z",
    "positionCount": 1
  }
]
```

Add European option position request:

```json
{
  "underlyingSymbol": "AAPL",
  "optionType": "CALL",
  "strike": 100.0,
  "maturityDate": "2027-12-31",
  "quantity": 10.0
}
```

Persisted position fields:

- `underlyingSymbol`: normalized to uppercase.
- `optionType`: `CALL` or `PUT`.
- `strike`: must be greater than zero.
- `maturityDate`: option maturity date.
- `quantity`: can be positive or negative, but not zero.

Portfolio metadata fields:

- `name`: required.
- `description`: optional, up to 500 characters.
- `baseCurrency`: three-letter currency code, normalized to uppercase, default `USD`.
- `updatedAt`: changes when portfolio metadata changes.

Portfolio positions store trade terms only. Market data inputs such as `spot`, `riskFreeRate`, and `volatility` are intentionally not persisted in portfolio positions; they belong to pricing or market-data workflows.

Implementation shape:

- `portfolio.domain` contains portfolio and position invariants.
- `portfolio.application` owns use cases and transaction boundaries.
- `portfolio.infrastructure` contains JPA entities and repositories.
- `portfolio.api` owns REST DTOs and the controller.

## European Option Pricing

Endpoint:

```text
POST /api/pricing/european-options/black-scholes
```

Request:

```json
{
  "optionType": "CALL",
  "spot": 100.0,
  "strike": 100.0,
  "timeToMaturityYears": 1.0,
  "riskFreeRate": 0.05,
  "volatility": 0.2
}
```

Response:

```json
{
  "model": "BLACK_SCHOLES",
  "optionType": "CALL",
  "price": 10.4506,
  "greeks": {
    "delta": 0.6368,
    "gamma": 0.0188,
    "vega": 37.524,
    "theta": -6.414,
    "rho": 53.2325
  }
}
```

Pricing assumptions:

- Supports European `CALL` and `PUT` options.
- Uses the Black-Scholes closed-form model.
- Uses constant volatility and a constant continuously compounded risk-free rate.
- Does not include dividend yield in this milestone.
- Rates and volatility are decimals: `0.05` means 5%.
- `spot`, `strike`, `timeToMaturityYears`, and `volatility` must be greater than zero.
- `riskFreeRate` may be negative, but it must be finite.
- Exact expiry payoff at `timeToMaturityYears = 0` is out of scope for this endpoint because several Greeks are discontinuous or undefined at expiry.
- Greeks are reported per unit change: vega is per `1.0` volatility change, not per one volatility point.

Implementation shape:

- `pricing.domain` contains pure Java financial objects and formulas.
- `pricing.application` owns the pricing use case.
- `pricing.api` owns REST DTOs and the controller.
- Controllers stay thin; financial logic stays outside Spring MVC.

## Package Structure

```text
com.nexusxva
  shared
    api
    error
    validation
  instruments
    api
    application
    domain
    infrastructure
  marketdata
    api
    application
    domain
    infrastructure
  pricing
    api
    application
    domain
    infrastructure
  portfolio
    api
    application
    domain
    infrastructure
  simulation
    api
    application
    domain
    infrastructure
  exposure
    api
    application
    domain
    infrastructure
  xva
    api
    application
    domain
    infrastructure
```

Controllers should stay thin, application services should own use-case orchestration, domain packages should contain financial concepts and invariants, and infrastructure packages should contain persistence or external adapters when those are introduced.
