# Optimization Workflow

## Purpose

Use this workflow after a feature works and has tests.

## Steps

1. Confirm correctness tests exist.
2. Identify performance-sensitive paths.
3. Measure or define required measurement.
4. Recommend MVP-safe improvements.
5. Implement only approved changes.
6. Re-run correctness tests.
7. Add benchmarks where justified.
8. Document tradeoffs.

## Optimization Targets

- Monte Carlo inner loops.
- Memory allocation in simulation.
- API latency.
- Database query patterns.
- Long-running job tracking.
- Docker and local development speed.

## Rules

- Correctness comes first.
- Measure before large rewrites.
- Keep simulation deterministic under test.
- Avoid database calls inside computational loops.
- Avoid distributed architecture until local performance is understood.

## Output Format

1. Current behavior.
2. Bottleneck or risk.
3. Measurement.
4. Recommendation.
5. Tradeoffs.
6. Correctness risk.
7. Test or benchmark plan.

