# XVA Domain Skill

## Core Concepts

XVA means valuation adjustments applied to derivative pricing to account for counterparty credit risk, funding, capital, and related effects.

Important terms:

- Exposure: the value at risk if a counterparty defaults.
- Positive exposure: exposure after clipping negative values to zero.
- Expected Exposure: average positive exposure at a future time.
- PFE: Potential Future Exposure, usually a percentile of the exposure distribution.
- Counterparty risk: risk that the other party defaults before fulfilling obligations.
- Default probability: likelihood of counterparty default over a time interval.
- Loss Given Default: fraction of exposure lost after recovery.
- Discount factor: present-value adjustment for future cash flows or losses.

## CVA

CVA, or Credit Valuation Adjustment, estimates the expected loss from counterparty default.

Simplified CVA can be represented as:

```text
CVA = sum over time buckets of discounted expected exposure * marginal default probability * loss given default
```

Project assumptions for MVP:

- Use exposure profile as input.
- Use simplified default probabilities.
- Use fixed or simple discount factors.
- Use fixed LGD.
- Ignore wrong-way risk initially.
- Ignore collateral and netting initially unless explicitly planned.

## DVA

DVA, or Debit Valuation Adjustment, reflects the value impact of the institution's own default risk.

Not in MVP. Future implementation should document assumptions carefully because DVA can be conceptually controversial and accounting-sensitive.

## FVA

FVA, or Funding Valuation Adjustment, reflects funding costs or benefits associated with derivative positions.

Not in MVP. Future implementation requires funding curve assumptions and careful scope control.

## KVA

KVA, or Capital Valuation Adjustment, reflects the cost of regulatory or economic capital.

Not in MVP. Future implementation requires capital model assumptions and should be treated as advanced functionality.

## Exposure and Counterparty Risk

Exposure is scenario-dependent and time-dependent. For a simulated portfolio value at future time `t`:

```text
positiveExposure(t) = max(portfolioValue(t), 0)
```

Expected Exposure is the average positive exposure over paths at a time bucket.

PFE is a percentile, such as the 95th percentile, of the positive exposure distribution.

## Monte Carlo Concepts

Monte Carlo simulation estimates future distributions by generating many random paths.

For the MVP:

- Simulate underlying spot paths.
- Use deterministic seeds for reproducibility.
- Use fixed time buckets.
- Price or approximate exposure at each bucket.
- Aggregate path results into exposure metrics.

## Best Practices

- State all assumptions.
- Separate market simulation from exposure aggregation.
- Keep formulas testable.
- Use financial invariants in tests.
- Prefer simple models first.

## Common Mistakes

- Treating CVA as a single arbitrary percentage.
- Forgetting discounting.
- Using cumulative default probability where marginal default probability is intended.
- Allowing negative exposure after positive exposure clipping.
- Using non-repeatable randomness in tests.

## Recommended Patterns

- `SimulationResult` contains path or aggregated scenario values.
- `ExposureProfile` contains time-bucketed EE and PFE.
- `CvaCalculator` consumes exposure, default probabilities, discount factors, and LGD.
- Tests verify monotonicity and zero cases.

