# NexusXVA Backend

Spring Boot backend for NexusXVA.

The backend currently includes the project foundation, stateless European option pricing with the Black-Scholes model and Greeks, persisted portfolio management for European option positions, stateless portfolio-level Black-Scholes pricing using market-data pricing inputs, Exposure V1 with simple GBM Monte Carlo simulation, and simplified CVA V1.

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
    "volatility": 0.2,
    "dividendYield": 0.0
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
- portfolio-level Black-Scholes pricing with local market-data inputs
- Exposure V1 Monte Carlo simulation, deterministic GBM paths, and exposure aggregation
- simplified CVA V1 over the exposure profile

The current suite is verified with `mvn test` and has `130` tests, including one real Blemberg smoke test that is skipped unless explicitly enabled.

## Developer Financial Docs

Conceptual financial guides for developers live in:

- Spanish: [`../docs/docs-ES/ConceptosFinancieros.md`](../docs/docs-ES/ConceptosFinancieros.md)
- System logic ES: [`../docs/docs-ES/LogicaDelSistema.md`](../docs/docs-ES/LogicaDelSistema.md)
- System logic EN: [`../docs/docs-EN/SystemLogic.md`](../docs/docs-EN/SystemLogic.md)
- English: [`../docs/docs-EN/FinancialConcepts.md`](../docs/docs-EN/FinancialConcepts.md)
- Blemberg needs: [`../docs/docs-EN/BlembergNeeds.md`](../docs/docs-EN/BlembergNeeds.md)
- Blemberg contract fixtures: [`../docs/docs-EN/BlembergContractFixtures.md`](../docs/docs-EN/BlembergContractFixtures.md)
- Blemberg build spec: [`../docs/docs-EN/BlembergBuildSpec.md`](../docs/docs-EN/BlembergBuildSpec.md)
- Blemberg runtime guide: [`../docs/docs-EN/how-it-works-blemberg.md`](../docs/docs-EN/how-it-works-blemberg.md)
- NexusXVA/Blemberg integration guide: [`../docs/docs-EN/nexusxva-integration-blemberg.md`](../docs/docs-EN/nexusxva-integration-blemberg.md)
- Exposure V1 plan: [`../docs/docs-EN/ExposureV1Plan.md`](../docs/docs-EN/ExposureV1Plan.md)

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
POST /api/portfolios/{portfolioId}/pricing/black-scholes
POST /api/simulations/exposure
POST /api/risk/cva
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
- `createdAt` and `updatedAt`: position lifecycle timestamps.

Portfolio metadata fields:

- `name`: required.
- `description`: optional, up to 500 characters.
- `baseCurrency`: three-letter currency code, normalized to uppercase, default `USD`.
- `updatedAt`: changes when portfolio metadata changes.

Portfolio positions store trade terms only. Market data inputs such as `spot`, `riskFreeRate`, `volatility`, and `dividendYield` are intentionally not persisted in portfolio positions; they belong to pricing or market-data workflows.

### Portfolio Black-Scholes Pricing

Portfolio pricing is stateless: it reads persisted positions, requests pricing inputs through the `marketdata` boundary, reuses the Black-Scholes calculator, and returns per-position plus portfolio-level totals. Pricing results are not persisted.

Endpoint:

```text
POST /api/portfolios/{portfolioId}/pricing/black-scholes
```

Request:

```json
{
  "valuationDate": "2026-06-01"
}
```

If `valuationDate` is omitted, the backend uses the current UTC date.

Response shape:

```json
{
  "portfolioId": "6f2d4637-ef84-4bc5-bca3-7c7aee54b4e5",
  "valuationDate": "2026-06-01",
  "model": "BLACK_SCHOLES",
  "baseCurrency": "USD",
  "totalPrice": 1234.56,
  "totalGreeks": {
    "delta": 12.3,
    "gamma": 0.45,
    "vega": 89.1,
    "theta": -7.2,
    "rho": 14.8
  },
  "positions": [],
  "unpriceablePositions": []
}
```

V1 pricing rules:

- `baseCurrency` must be `USD`; FX conversion is not implemented yet.
- `quantity` scales price and Greeks, so negative quantities represent short exposure.
- Positions with `maturityDate <= valuationDate` are returned in `unpriceablePositions` with `UNPRICEABLE_EXPIRED` and are excluded from totals.
- Missing market-data pricing inputs return `400 Bad Request`.
- Blemberg outages return `503 Service Unavailable`.

The USD-only rule prevents incorrect totals. Without FX conversion, values such as `100 USD + 100 EUR` cannot be safely reported as one portfolio total.

### Blemberg Market Data Validation

NexusXVA can validate portfolio position symbols against Blemberg, the separate market-data service responsible for reference data and pricing inputs.

Configuration:

```yaml
nexusxva:
  market-data:
    provider: blemberg
    validation:
      enabled: false
    blemberg:
      base-url: http://localhost:8081
      timeout: 2s
```

Environment overrides:

```bash
NEXUSXVA_MARKET_DATA_PROVIDER=blemberg
NEXUSXVA_MARKET_DATA_VALIDATION_ENABLED=true
BLEMBERG_BASE_URL=http://localhost:8081
BLEMBERG_TIMEOUT=2s
```

Before using Blemberg locally, start that service, trigger its refresh, and verify health:

```bash
curl http://localhost:8081/actuator/health
curl -X POST http://localhost:8081/api/admin/market-data/refresh
curl http://localhost:8081/api/admin/market-data/refresh-runs
curl "http://localhost:8081/api/market-data/snapshots?symbols=AAPL,SPY,QQQ,MSFT"
curl "http://localhost:8081/api/market-data/pricing-inputs/european-option?symbol=AAPL&maturityDate=2027-06-01"
```

When NexusXVA runs from the provided Docker Compose file, `BLEMBERG_BASE_URL` defaults to `http://host.docker.internal:8081`. If both services run inside the same Docker network, override it with the Blemberg service name, for example `http://blemberg:8081`.

Local validation and pricing-input mock without Blemberg:

```bash
NEXUSXVA_MARKET_DATA_PROVIDER=local
NEXUSXVA_MARKET_DATA_VALIDATION_ENABLED=true
```

The local provider uses a fixed development watchlist for symbols such as `AAPL`, `MSFT`, `NVDA`, `JPM`, `SPY`, `QQQ`, `GLD`, and `SLV`. It validates symbols and provides temporary demo `spot`, `volatility`, `riskFreeRate`, and `dividendYield` values for portfolio pricing. These values are not persisted and must not be treated as real market data.

When validation is enabled:

- creating a European option position validates `underlyingSymbol` with `GET /api/instruments/{symbol}` on Blemberg.
- updating a position validates only when `underlyingSymbol` changes.
- portfolio pricing requests `GET /api/market-data/pricing-inputs/european-option?symbol={symbol}&maturityDate={yyyy-mm-dd}` on Blemberg.
- unknown or inactive instruments return `400 Bad Request` with `Unknown underlyingSymbol`.
- unavailable Blemberg calls return `503 Service Unavailable` with `Market data service unavailable`.

NexusXVA still persists only the symbol and trade terms. Blemberg owns instrument reference data and real pricing inputs such as spot, historical volatility, risk-free rates, and dividend yields.

Blemberg snapshot responses are diagnostic only for NexusXVA. In Blemberg V1, `GET /api/market-data/snapshots` returns:

```json
{
  "snapshots": [],
  "missingSymbols": []
}
```

Portfolio pricing and Exposure V1 do not consume raw snapshots; they consume the pricing-input endpoint. Blemberg V1 may also return `501 Not Implemented` for `GET /v3/api-docs`; NexusXVA does not depend on OpenAPI for runtime integration.

Optional real Blemberg smoke test:

```bash
RUN_REAL_BLEMBERG_SMOKE=true BLEMBERG_BASE_URL=http://localhost:8081 \
  mvn test -Dtest=BlembergRealSmokeTest
```

The real smoke test is skipped in normal `mvn test` runs unless `RUN_REAL_BLEMBERG_SMOKE=true` is set.

### Exposure Simulation V1

Exposure V1 is stateless: it loads a persisted portfolio, requests market-data pricing inputs through the `marketdata` boundary, simulates future spot paths with a simple GBM model, reprices live European option positions with Black-Scholes across the time grid, and aggregates exposure measures. Results are not persisted.

Endpoint:

```text
POST /api/simulations/exposure
```

Request:

```json
{
  "portfolioId": "6f2d4637-ef84-4bc5-bca3-7c7aee54b4e5",
  "valuationDate": "2026-06-05",
  "horizonDays": 365,
  "timeSteps": 12,
  "paths": 1000,
  "seed": 12345,
  "pfeConfidenceLevel": 0.95
}
```

Response shape:

```json
{
  "portfolioId": "6f2d4637-ef84-4bc5-bca3-7c7aee54b4e5",
  "valuationDate": "2026-06-05",
  "model": "GBM_BLACK_SCHOLES_EXPOSURE_V1",
  "paths": 1000,
  "timeSteps": 12,
  "pfeConfidenceLevel": 0.95,
  "points": [
    {
      "date": "2026-07-05",
      "expectedExposure": 123.45,
      "expectedNegativeExposure": 12.34,
      "pfe": 456.78
    }
  ]
}
```

V1 simulation rules:

- Supports persisted European option portfolios only.
- Supports USD portfolios and USD market-data inputs only.
- Uses `spot`, `volatility`, `riskFreeRate`, and `dividendYield` from `marketdata`.
- Uses deterministic seeds so tests and dev runs are repeatable.
- Excludes positions once `maturityDate <= futureDate`.
- Empty portfolios or all-expired portfolios return zero exposure points.
- Does not persist market data, simulated paths, or exposure results.

### Simplified CVA V1

CVA V1 is stateless: it reuses Exposure V1, then applies a simple credit valuation adjustment formula over expected exposure by date. Results are not persisted.

Endpoint:

```text
POST /api/risk/cva
```

Request:

```json
{
  "portfolioId": "6f2d4637-ef84-4bc5-bca3-7c7aee54b4e5",
  "valuationDate": "2026-06-05",
  "horizonDays": 365,
  "timeSteps": 12,
  "paths": 1000,
  "seed": 12345,
  "pfeConfidenceLevel": 0.95,
  "lossGivenDefault": 0.6,
  "counterpartyHazardRate": 0.02,
  "discountRate": 0.05
}
```

Response shape:

```json
{
  "portfolioId": "6f2d4637-ef84-4bc5-bca3-7c7aee54b4e5",
  "valuationDate": "2026-06-05",
  "model": "SIMPLIFIED_CVA_V1",
  "exposureModel": "GBM_BLACK_SCHOLES_EXPOSURE_V1",
  "paths": 1000,
  "timeSteps": 12,
  "pfeConfidenceLevel": 0.95,
  "lossGivenDefault": 0.6,
  "counterpartyHazardRate": 0.02,
  "discountRate": 0.05,
  "cva": 12.34,
  "points": [
    {
      "date": "2026-07-05",
      "expectedExposure": 123.45,
      "discountFactor": 0.9959,
      "survivalProbability": 0.9984,
      "defaultProbabilityIncrement": 0.0016,
      "discountedExpectedExposure": 122.94,
      "cvaContribution": 0.12
    }
  ]
}
```

V1 CVA rules:

- Formula: `CVA = LGD * sum(discountFactor_i * expectedExposure_i * defaultProbabilityIncrement_i)`.
- `counterpartyHazardRate` is a flat annual hazard rate expressed as a decimal.
- `discountRate` is a flat continuously compounded annual discount rate expressed as a decimal.
- `lossGivenDefault` must be between `0.0` and `1.0`.
- CVA reuses Exposure V1, so USD-only and European-options-only limitations still apply.
- No counterparty entity, credit curve, netting set, collateral, wrong-way risk, or persisted CVA result is implemented in V1.

Implementation shape:

- `portfolio.domain` contains portfolio and position invariants.
- `portfolio.application` owns use cases and transaction boundaries.
- `portfolio.infrastructure` contains JPA entities and repositories.
- `portfolio.api` owns REST DTOs and the controller.
- `marketdata.application` owns the internal ports used to validate symbols and request pricing inputs.
- `marketdata.infrastructure` owns the Blemberg REST adapters and temporary local watchlist/pricing-input adapter.
- `exposure.application` owns exposure simulation orchestration.
- `cva.domain` owns the simplified CVA formula.
- `cva.application` coordinates exposure simulation and CVA calculation.
- `cva.api` owns the CVA REST contract.

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
  "volatility": 0.2,
  "dividendYield": 0.0
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
- Supports optional continuous dividend yield; omitted `dividendYield` defaults to `0.0`.
- Rates and volatility are decimals: `0.05` means 5%.
- `spot`, `strike`, `timeToMaturityYears`, and `volatility` must be greater than zero.
- `riskFreeRate` may be negative, but it must be finite.
- `dividendYield` must be greater than or equal to zero.
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
  cva
    api
    application
    domain
```

Controllers should stay thin, application services should own use-case orchestration, domain packages should contain financial concepts and invariants, and infrastructure packages should contain persistence or external adapters when those are introduced.
