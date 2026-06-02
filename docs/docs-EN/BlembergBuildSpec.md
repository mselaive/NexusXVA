# Blemberg Build Spec

This document is the implementation brief for the agent or developer building Blemberg.
Blemberg is a separate Java Spring Boot market-data service that NexusXVA will consume over HTTP.

## Mission

Blemberg must answer two questions for NexusXVA:

1. Does this `underlyingSymbol` exist and is it active?
2. What market-data inputs should NexusXVA use to price a European option?

Blemberg does not price options.
NexusXVA owns portfolios, positions, Black-Scholes pricing, exposure, and XVA workflows.

## Recommended Stack

- Java 21+
- Spring Boot
- Maven
- PostgreSQL
- Flyway
- Spring Data JPA
- Docker Compose
- Testcontainers
- REST API only, no frontend in V1

Suggested package/module structure:

```text
com.blemberg
  shared
    api
    error
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
  providers
    twelvedata
    fred
  refresh
    api
    application
    infrastructure
```

## External Data Sources

Use official APIs only.
Do not scrape Yahoo or HTML pages.

### Twelve Data

Use Twelve Data for:

- Instrument reference data.
- Current quotes/snapshots.
- Daily historical bars.
- Optional dividend data if available in the selected plan.

Useful endpoints from Twelve Data docs:

- `/quote`: latest market quote.
- `/time_series`: OHLCV historical bars.

Twelve Data Basic currently lists `8 API` credits per minute and `800 / day`.
That is enough for a small hourly/daily watchlist if Blemberg batches and throttles refreshes.

Official docs:

- https://twelvedata.com/docs
- https://twelvedata.com/pricing

### FRED

Use FRED for US Treasury rates.

Use endpoint:

```text
GET /fred/series/observations
```

Official docs:

- https://fred.stlouisfed.org/docs/api/fred/series_observations.html

Risk-free series for V1:

| Tenor | FRED series |
| --- | --- |
| 1M | `DGS1MO` |
| 3M | `DGS3MO` |
| 6M | `DGS6MO` |
| 1Y | `DGS1` |
| 2Y | `DGS2` |
| 5Y | `DGS5` |
| 10Y | `DGS10` |

FRED rates are percentages.
Blemberg must store and expose them as decimals.
Example: FRED value `4.50` becomes `0.045`.

## Initial Watchlist

Use public symbols that can be queried by NexusXVA as `underlyingSymbol`.

| Group | Symbols |
| --- | --- |
| Tech equities | `AAPL`, `MSFT`, `NVDA`, `AMZN`, `GOOGL`, `META`, `TSLA`, `AVGO`, `ORCL`, `AMD` |
| Banks | `JPM`, `BAC`, `GS`, `MS`, `C`, `WFC` |
| ETF/index proxies | `SPY`, `QQQ`, `DIA`, `IWM`, `VTI`, `TLT` |
| Metal ETF proxies | `GLD`, `SLV`, `CPER` |

Use ETF proxies in V1 because NexusXVA prices options on tradable underlyings.
`QQQ` is the Nasdaq proxy for V1, not the direct Nasdaq index.
`GLD`, `SLV`, and `CPER` are metal ETF proxies, not physical commodities.

## Data Model

Blemberg owns market data persistence.
NexusXVA must not persist this data inside portfolio positions.

Recommended tables:

### `instruments`

Stores public reference data.

Required fields:

- `id`
- `symbol`: public NexusXVA symbol, unique, uppercase.
- `name`
- `asset_class`: `EQUITY`, `ETF`, `METAL_ETF`, `FX`, or future values.
- `exchange`
- `currency`: 3-letter code.
- `provider`: `TWELVE_DATA`, `MANUAL`, etc.
- `provider_symbol`: symbol used by the external provider.
- `active`
- `created_at`
- `updated_at`

### `market_price_snapshots`

Stores latest known quote per symbol.

Required fields:

- `id`
- `symbol`
- `last_price`
- `open`
- `high`
- `low`
- `previous_close`
- `volume`
- `currency`
- `provider`
- `as_of`
- `created_at`

Keep only the latest row per symbol or keep history with a clear `latest` query.
For NexusXVA V1, only the latest usable snapshot is required.

### `market_daily_bars`

Stores historical OHLCV bars used for volatility.

Required fields:

- `id`
- `symbol`
- `bar_date`
- `open`
- `high`
- `low`
- `close`
- `volume`
- `provider`
- `created_at`

Add unique constraint:

```text
(symbol, bar_date)
```

### `risk_free_rates`

Stores Treasury rates from FRED.

Required fields:

- `id`
- `tenor_code`: `1M`, `3M`, `6M`, `1Y`, `2Y`, `5Y`, `10Y`.
- `tenor_months`
- `fred_series_id`
- `rate_decimal`
- `observation_date`
- `as_of`
- `created_at`

### `dividend_yields`

Stores dividend yield assumptions.

Required fields:

- `id`
- `symbol`
- `dividend_yield`
- `method`: `PROVIDER`, `TRAILING_12M`, `MANUAL_ZERO`, or `UNKNOWN_ZERO`.
- `as_of`
- `created_at`

If no dividend data is available, store or return `0.0` with method `UNKNOWN_ZERO`.
Do not omit `dividendYield` from NexusXVA responses.

### `refresh_runs`

Stores refresh observability.

Required fields:

- `id`
- `job_name`
- `status`: `SUCCESS`, `PARTIAL_SUCCESS`, `FAILED`.
- `started_at`
- `finished_at`
- `symbols_requested`
- `symbols_succeeded`
- `symbols_failed`
- `error_message`

Do not expose raw provider stack traces in public API errors.

## Refresh Policy

V1 refresh schedule:

- Instrument reference data: daily.
- Price snapshots: hourly.
- Daily bars: daily after US market close.
- FRED rates: daily.
- Dividend yields: daily or weekly.

Respect Twelve Data limits.
For the Basic plan, assume a small budget of `8` API credits per minute and `800` per day.
Implement a rate limiter and retry policy.

Recommended behavior:

- Provider refresh writes successful values to PostgreSQL.
- If a refresh fails, keep the latest usable cached value.
- Mark stale data with `stale=true` in API responses.
- If there is no usable cached value, return a clean API error.

## Derived Pricing Inputs

Endpoint:

```text
GET /api/market-data/pricing-inputs/european-option?symbol=AAPL&maturityDate=2027-06-01
```

Build the response from persisted data:

- `spot`: latest snapshot `last_price`.
- `volatility`: historical realized volatility from daily bars.
- `riskFreeRate`: nearest or interpolated Treasury rate for the option maturity.
- `dividendYield`: dividend yield assumption for the symbol.
- `currency`: instrument/snapshot currency.
- `asOf`: newest relevant timestamp among the input data, or the snapshot timestamp.
- `source`: `BLEMBERG`.
- `stale`: true if any required input is older than the freshness policy.

### Historical Volatility

Use daily close prices.
Default window: 60 trading days.

Formula:

```text
return_i = ln(close_i / close_{i-1})
volatility = stddev(return_i) * sqrt(252)
```

Return decimal annualized volatility.
Example: `0.22` means 22%.

### Risk-Free Rate

Compute the time between request date and `maturityDate`.
Select or interpolate between FRED tenors.

V1 accepted methods:

- `NEAREST_TENOR`: choose the closest available tenor.
- `LINEAR_INTERPOLATION`: interpolate between two available tenors.

NexusXVA currently only consumes `riskFreeRate`, but Blemberg should internally track `rateMethod` for auditability.

### Dividend Yield

Return a non-negative decimal continuous annual yield.

Accepted V1 methods:

- Provider dividend yield when available.
- Trailing dividends divided by current price when available.
- `0.0` when unavailable.

For ETFs and metals, `0.0` is acceptable in V1 if Blemberg cannot source a reliable value.

## Public API V1

### Health

```text
GET /actuator/health
```

### List Instruments

```text
GET /api/instruments?assetClass=EQUITY&active=true&symbol=AAPL
```

Response:

```json
[
  {
    "symbol": "AAPL",
    "active": true,
    "name": "Apple Inc.",
    "assetClass": "EQUITY",
    "exchange": "NASDAQ",
    "currency": "USD",
    "provider": "TWELVE_DATA",
    "providerSymbol": "AAPL"
  }
]
```

### Get Instrument

```text
GET /api/instruments/AAPL
```

Response:

```json
{
  "symbol": "AAPL",
  "active": true,
  "name": "Apple Inc.",
  "assetClass": "EQUITY",
  "exchange": "NASDAQ",
  "currency": "USD",
  "provider": "TWELVE_DATA",
  "providerSymbol": "AAPL"
}
```

Unknown symbol:

```json
{
  "status": 404,
  "error": "Not Found",
  "message": "Instrument not found",
  "path": "/api/instruments/FAKE"
}
```

### Get Snapshots

```text
GET /api/market-data/snapshots?symbols=AAPL,MSFT,QQQ
```

Response:

```json
[
  {
    "symbol": "AAPL",
    "lastPrice": 190.0,
    "open": 188.5,
    "high": 191.2,
    "low": 187.9,
    "previousClose": 188.1,
    "volume": 53200000,
    "currency": "USD",
    "asOf": "2026-06-02T12:00:00Z",
    "source": "TWELVE_DATA",
    "stale": false
  }
]
```

### Get European Option Pricing Inputs

```text
GET /api/market-data/pricing-inputs/european-option?symbol=AAPL&maturityDate=2027-06-01
```

Response:

```json
{
  "symbol": "AAPL",
  "spot": 190.0,
  "volatility": 0.22,
  "volatilityMethod": "HISTORICAL_REALIZED_60D",
  "riskFreeRate": 0.045,
  "rateMethod": "LINEAR_INTERPOLATION",
  "dividendYield": 0.005,
  "dividendYieldMethod": "PROVIDER",
  "currency": "USD",
  "asOf": "2026-06-02T12:00:00Z",
  "source": "BLEMBERG",
  "stale": false
}
```

NexusXVA currently requires these fields:

- `symbol`
- `spot`
- `volatility`
- `riskFreeRate`
- `dividendYield`
- `currency`
- `asOf`
- `source`
- `stale`

The method fields are recommended for Blemberg and future NexusXVA versions.

### Manual Refresh

```text
POST /api/admin/market-data/refresh
```

Response:

```json
{
  "runId": "5f0d60dc-0a08-44e4-bad8-df5511ee8036",
  "status": "SUCCESS",
  "startedAt": "2026-06-02T12:00:00Z",
  "finishedAt": "2026-06-02T12:00:08Z",
  "symbolsRequested": 25,
  "symbolsSucceeded": 25,
  "symbolsFailed": 0
}
```

## Provider Mapping Examples

### Twelve Data Quote

Blemberg should map provider quote fields into its own snapshot response.

Provider concept to Blemberg field:

- latest close/price -> `lastPrice`
- open -> `open`
- high -> `high`
- low -> `low`
- previous close -> `previousClose`
- volume -> `volume`
- datetime/timestamp -> `asOf`
- currency -> `currency`

### Twelve Data Daily Bars

Blemberg should store:

- date/time -> `barDate`
- open -> `open`
- high -> `high`
- low -> `low`
- close -> `close`
- volume -> `volume`

### FRED Observations

For each Treasury series:

- observation `date` -> `observationDate`
- observation `value` -> percentage rate
- decimal rate -> `value / 100`

Ignore observations with missing values such as `"."`.

## Error Rules

Blemberg must expose clean API errors:

- Unknown instrument: `404 Instrument not found`.
- Inactive instrument: return `200` with `active=false`.
- Missing pricing inputs: `404` or `400` with a clean API error.
- Provider failure: `503 Market data service unavailable`.
- Malformed request: `400 Malformed request body`.

Do not expose:

- Twelve Data raw error payloads.
- FRED raw error payloads.
- Java stack traces.
- Internal table names.
- API keys.

## Tests Required

Unit tests:

- Symbol normalization.
- Historical volatility calculation.
- FRED percentage-to-decimal conversion.
- Risk-free tenor selection/interpolation.
- Dividend yield default to `0.0`.
- Stale-data detection.

API tests:

- Known active instrument.
- Unknown instrument.
- Inactive instrument.
- Snapshot response from persisted cache.
- Pricing inputs response with all required fields.
- Missing pricing inputs.
- Provider unavailable.

Integration tests:

- Flyway migrations.
- JPA persistence for instruments, snapshots, bars, rates, dividend yields, refresh runs.
- Refresh job stores successes and failures.

Provider adapter tests:

- Mock Twelve Data HTTP responses.
- Mock FRED HTTP responses.
- Do not call real providers in automated tests.

## Current NexusXVA Constraints

Blemberg V1 should support NexusXVA as it exists now:

- USD instruments only for portfolio pricing.
- European options only.
- Historical realized volatility, not implied volatility.
- No options chains required.
- No FX required yet.
- No market data is persisted in NexusXVA.

Future requirements:

- FX rates for multi-currency portfolio totals.
- Volatility surfaces or implied volatility.
- More underlyings and asset classes.
- Better dividend models.
- Stale-data policy configurable by asset class.
