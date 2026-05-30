# Monte Carlo Skill

## Core Concepts

Monte Carlo simulation estimates distributions by generating random scenarios.

For option and exposure simulation, paths often model future spot prices using geometric Brownian motion:

```text
dS = mu * S * dt + sigma * S * dW
```

For the MVP, use a simple discretized model with documented assumptions.

## Best Practices

- Use deterministic seeds in tests.
- Separate path generation from exposure aggregation.
- Keep random number generation controlled and injectable.
- Avoid database access inside simulation loops.
- Record path count, time steps, seed, and duration.
- Start single-threaded before adding concurrency.

## Project-Specific Conventions

- Simulation owns path generation and scenario metadata.
- Exposure owns transformation from scenario values to EE and PFE.
- XVA owns valuation adjustments such as CVA.
- Simulation outputs should support dashboard visualization.

## Common Mistakes

- Using uncontrolled randomness in tests.
- Allocating excessive objects in inner loops.
- Parallelizing before correctness is stable.
- Mixing simulation, exposure, and CVA in one large class.
- Ignoring units of time.

## Recommended Patterns

- `SimulationRequest` includes path count, time steps, horizon, and seed.
- `PathGenerator` produces paths or path summaries.
- `SimulationRun` stores metadata.
- `ExposureCalculator` consumes simulation values.
- Benchmarks are added before major performance rewrites.

