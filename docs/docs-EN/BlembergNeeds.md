# Blemberg Needs for NexusXVA

This document describes what NexusXVA expects from Blemberg, the separate market-data service.
It is a functional contract for developers, not a full Blemberg implementation guide.

For concrete JSON examples, see `BlembergContractFixtures.md`.
For the full build brief, including modules, tables, refresh policy, provider mapping, and tests, see `BlembergBuildSpec.md`.

## Purpose

NexusXVA owns portfolios, positions, pricing, exposure, and XVA workflows.
Blemberg owns market data and instrument reference data.

The boundary is intentional:

- NexusXVA stores trade terms only.
- Blemberg validates whether symbols exist.
- Blemberg provides market-data inputs for valuation.
- NexusXVA does not persist real market data inside portfolio positions.

## Current NexusXVA Usage

NexusXVA currently uses Blemberg for two workflows.

### Instrument Validation

Used when market-data validation is enabled:

```text
GET /api/instruments/{symbol}
```

Expected response:

```json
{
  "symbol": "AAPL",
  "active": true,
  "name": "Apple Inc.",
  "assetClass": "EQUITY",
  "currency": "USD"
}
```

Rules:

- Unknown symbols should return `404`.
- Inactive instruments should return `active=false`.
- NexusXVA treats unknown or inactive instruments as invalid underlyings.
- Symbols should be normalized to uppercase.

### European Option Pricing Inputs

Used by portfolio-level Black-Scholes pricing:

```text
GET /api/market-data/pricing-inputs/european-option?symbol=AAPL&maturityDate=2027-06-01
```

Expected response:

```json
{
  "symbol": "AAPL",
  "spot": 190.0,
  "volatility": 0.22,
  "riskFreeRate": 0.045,
  "dividendYield": 0.005,
  "currency": "USD",
  "asOf": "2026-06-02T12:00:00Z",
  "source": "BLEMBERG",
  "stale": false
}
```

Required pricing fields:

- `spot`: positive finite current price.
- `volatility`: positive finite annualized volatility.
- `riskFreeRate`: finite decimal annual rate.
- `dividendYield`: non-negative finite decimal annual continuous yield.
- `currency`: 3-letter currency code.
- `asOf`: timestamp of the market data.
- `source`: provider/source label.
- `stale`: whether data is older than the freshness policy.

## Financial Conventions

- Rates, volatility, and dividend yield are decimals.
- `0.05` means 5%.
- Volatility should be annualized.
- `dividendYield` should be a continuous annual dividend yield.
- If Blemberg has no dividend data for an instrument, return `0.0` rather than omitting the field.
- Blemberg does not price options; it only provides pricing inputs.

## Current Limitations

NexusXVA portfolio pricing V1 is USD-only.
Until FX conversion exists, Blemberg should return USD instruments and USD pricing inputs for the current watchlist.

NexusXVA will reject non-USD market data in portfolio pricing V1.

## Local Mock Replacement

NexusXVA currently has a local provider for development.
It validates the watchlist and returns demo values for:

- `spot`
- `volatility`
- `riskFreeRate`
- `dividendYield`

Blemberg should eventually replace that local provider by implementing the same functional behavior over HTTP.

## Error Expectations

Blemberg should avoid leaking provider internals in API errors.

Expected behavior:

- Unknown instrument: `404`.
- Known but inactive instrument: `200` with `active=false`.
- Missing usable pricing inputs: clean `404` or `400` with an API error.
- Provider unavailable or refresh failure: clean `5xx` with an API error.

NexusXVA maps unavailable Blemberg calls to `503 Market data service unavailable`.

## Future Needs

Future NexusXVA milestones will likely require:

- FX rates for multi-currency portfolio totals.
- Historical daily bars for volatility calculation and simulation.
- Risk-free curves by tenor.
- Clear stale-data policy.
- Possibly implied volatility or volatility surfaces.

These are not required before the current NexusXVA portfolio-pricing V1 can run.
