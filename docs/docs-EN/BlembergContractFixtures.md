# Blemberg Contract Fixtures

These examples are the response shapes NexusXVA expects from Blemberg.
They can be used as fixtures in the Blemberg repository and in NexusXVA adapter tests.

For the complete Blemberg implementation brief, see `BlembergBuildSpec.md`.

## Active Instrument

```http
GET /api/instruments/AAPL
```

```json
{
  "symbol": "AAPL",
  "active": true,
  "name": "Apple Inc.",
  "assetClass": "EQUITY",
  "exchange": "NASDAQ",
  "provider": "TWELVE_DATA",
  "providerSymbol": "AAPL",
  "currency": "USD"
}
```

## Inactive Instrument

NexusXVA treats inactive instruments as invalid underlyings when validation is enabled.

```json
{
  "symbol": "AAPL",
  "active": false,
  "name": "Apple Inc.",
  "assetClass": "EQUITY",
  "exchange": "NASDAQ",
  "provider": "TWELVE_DATA",
  "providerSymbol": "AAPL",
  "currency": "USD"
}
```

## Unknown Instrument

```http
HTTP/1.1 404 Not Found
Content-Type: application/json
```

```json
{
  "status": 404,
  "error": "Not Found",
  "message": "Instrument not found",
  "path": "/api/instruments/FAKE"
}
```

## European Option Pricing Inputs

```http
GET /api/market-data/pricing-inputs/european-option?symbol=AAPL&maturityDate=2027-06-01
```

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

Required conventions:

- `spot` must be positive and finite.
- `volatility` must be positive, finite, decimal, and annualized.
- `riskFreeRate` must be finite, decimal, and annualized.
- `dividendYield` must be non-negative, finite, decimal, annualized, and continuous.
- If Blemberg has no dividend data, return `dividendYield: 0.0`.
- `currency` must be a 3-letter currency code.
- `asOf` must be the timestamp of the data used.
- `stale` may be `true`; NexusXVA will expose it in portfolio pricing responses.

## Market Snapshots

Snapshots are diagnostic/cache data for NexusXVA smoke checks. NexusXVA portfolio pricing and Exposure V1 should use pricing inputs instead of raw snapshots.

```http
GET /api/market-data/snapshots?symbols=AAPL,SPY,QQQ,MSFT
```

```json
{
  "snapshots": [
    {
      "symbol": "AAPL",
      "lastPrice": 312.68,
      "open": 312.85,
      "high": 315.15,
      "low": 312.265,
      "previousClose": 311.23001,
      "volume": 1829914,
      "currency": "USD",
      "asOf": "2026-06-05T13:30:00Z",
      "source": "TWELVE_DATA",
      "stale": false
    }
  ],
  "missingSymbols": ["SPY", "QQQ", "MSFT"]
}
```

If at least one requested symbol has a usable snapshot, Blemberg should return `200` with available snapshots and `missingSymbols`. It should return `404` only when none of the requested symbols has a usable snapshot.

## OpenAPI V1

Blemberg V1 does not enable OpenAPI. This is expected and should not be treated as a NexusXVA integration failure.

```http
GET /v3/api-docs
HTTP/1.1 501 Not Implemented
```

## Missing Pricing Inputs

Blemberg can return `404` or `400` when no usable pricing inputs exist.
NexusXVA maps that to a clean portfolio-pricing validation error.

```json
{
  "status": 404,
  "error": "Not Found",
  "message": "Pricing inputs not found",
  "path": "/api/market-data/pricing-inputs/european-option"
}
```

## Provider Failure

Provider outages, refresh failures, timeouts, or malformed internal provider data should not leak raw provider details.

```json
{
  "status": 503,
  "error": "Service Unavailable",
  "message": "Market data service unavailable",
  "path": "/api/market-data/pricing-inputs/european-option"
}
```

## Current Currency Rule

NexusXVA portfolio pricing V1 accepts only USD portfolios and USD market data.
FX is a future milestone, so Blemberg should return USD pricing inputs for the current watchlist.
