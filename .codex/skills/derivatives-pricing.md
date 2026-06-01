# Derivatives Pricing Skill

## Core Concepts

Derivative pricing estimates the fair value of financial contracts whose value depends on underlying market variables.

Initial instrument:

- European option.

Initial model:

- Black-Scholes with constant volatility, constant risk-free rate, no dividends unless explicitly added.

## European Options

A European option can be exercised only at maturity.

Call payoff:

```text
max(S - K, 0)
```

Put payoff:

```text
max(K - S, 0)
```

Where:

- `S` is spot price.
- `K` is strike price.

## Black-Scholes Inputs

Core inputs:

- Spot price.
- Strike price.
- Time to maturity.
- Risk-free rate.
- Volatility.
- Option type: call or put.

Project API conventions:

- The current Black-Scholes endpoint requires `timeToMaturityYears > 0`.
- Exact payoff at expiry (`timeToMaturityYears = 0`) is out of scope for the current endpoint because Greeks can be discontinuous or undefined there.
- Rates and volatility are decimals: `0.05` means 5%.
- The risk-free rate may be negative when the final price and Greeks remain finite.

Potential later inputs:

- Dividend yield.
- Yield curves.
- Volatility surfaces.

## Greeks

Greeks measure sensitivity:

- Delta: sensitivity to spot.
- Gamma: second-order sensitivity to spot.
- Vega: sensitivity to volatility.
- Theta: sensitivity to time.
- Rho: sensitivity to interest rate.

## Best Practices

- Validate positive spot, strike, volatility, and time to maturity.
- Define behavior at expiry explicitly.
- Validate final price and Greeks are finite; reject `NaN` and `Infinity`.
- Use tolerances in numerical tests.
- Test known pricing values.
- Test monotonicity properties.

## Project-Specific Conventions

- Keep Black-Scholes formulas in a pure Java class.
- Keep API requests separate from pricing input domain objects.
- Return both price and Greeks where requested by the feature spec.
- Document whether rates are continuously compounded.
- Keep expiry payoff behavior separate from the current Black-Scholes-with-Greeks endpoint until instrument/payoff modeling exists.

## Common Mistakes

- Forgetting expiry behavior.
- Allowing negative volatility.
- Confusing percentages with decimals.
- Returning NaN instead of validation errors.
- Treating Monte Carlo estimates as exact values.

## Recommended Patterns

- `OptionType` enum with `CALL` and `PUT`.
- `EuropeanOption` domain object.
- `BlackScholesInput` value object.
- `BlackScholesResult` containing price and Greeks.
- Unit tests with known values and invariants.
