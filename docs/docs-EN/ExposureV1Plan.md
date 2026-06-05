# Exposure V1 Behavior

Exposure V1 is the current stateless simulation slice after portfolio pricing and Blemberg integration hardening.
It uses existing persisted portfolios and market-data pricing inputs.

## Goal

Calculate future portfolio exposure for European option portfolios using a simple Monte Carlo engine.
This is not CVA yet.
Exposure V1 creates the time-grid data that CVA will consume later.

## Inputs

`POST /api/simulations/exposure` takes:

- `portfolioId`
- `valuationDate`
- simulation horizon
- number of time steps
- number of paths
- random seed
- confidence level for PFE

Market data comes through the existing `marketdata` boundary:

- `spot`
- `volatility`
- `riskFreeRate`
- `dividendYield`
- `currency`
- `asOf`
- `stale`

## Model

V1 uses a single-factor equity GBM model per underlying:

```text
dS = (riskFreeRate - dividendYield) * S * dt + volatility * S * dW
```

Use the exact discretization:

```text
S(t + dt) = S(t) * exp((r - q - 0.5 * sigma^2) * dt + sigma * sqrt(dt) * Z)
```

Where:

- `r` is `riskFreeRate`.
- `q` is `dividendYield`.
- `sigma` is `volatility`.
- `Z` is a standard normal random variable.

## Portfolio Repricing

At each future date:

- Skip positions already expired at that date.
- Reprice remaining positions with Black-Scholes.
- Use simulated spot.
- Use the same `volatility`, `riskFreeRate`, and `dividendYield` from the input snapshot.
- Scale each result by position quantity.
- Sum position values into one portfolio value per path and date.

## Outputs

For each future date, return:

- `date`
- `expectedExposure`: average of `max(portfolioValue, 0)`
- `expectedNegativeExposure`: average of `max(-portfolioValue, 0)`
- `pfe`: percentile of positive exposure at the requested confidence level

## V1 Constraints

- USD-only, matching current portfolio pricing.
- European options only.
- No FX conversion.
- No counterparty, netting sets, collateral, or credit spreads.
- No persisted exposure results.
- No CVA calculation in this endpoint.

## Tests

- Deterministic output with a fixed random seed.
- Empty portfolio returns zero exposure.
- Expired positions are excluded by future date.
- Short positions can create negative portfolio values.
- `dividendYield` affects the GBM drift.
- Non-USD portfolios reject consistently with portfolio pricing.
- Missing market data returns a clean API error.
