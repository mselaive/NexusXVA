# Frontend Dashboard Skill

## Core Concepts

The frontend is a dashboard and visualization layer for the backend risk platform.

It should help users inspect:

- Portfolios.
- Instruments.
- Pricing results.
- Greeks.
- Simulation metadata.
- Exposure profiles.
- CVA results.

## Best Practices

- Keep UI focused on workflows, not marketing.
- Use TypeScript types for API responses.
- Use charts for time-series exposure and PFE.
- Show assumptions and metadata near results.
- Handle loading, empty, and error states.
- Avoid duplicating financial logic in the frontend.

## Project-Specific Conventions

- Backend remains source of truth for calculations.
- Frontend requests calculations and visualizes results.
- Dashboard should be dense, readable, and professional.
- Charts should label axes and units.
- Tables should support scanning and comparison.

## Common Mistakes

- Reimplementing pricing formulas in TypeScript.
- Building a landing page instead of the actual dashboard.
- Hiding simulation assumptions.
- Showing charts without units or time buckets.
- Ignoring API error states.

## Recommended Patterns

- API client layer.
- Typed response models.
- Dashboard pages by workflow.
- Exposure chart with EE and PFE lines.
- Summary cards for price, Greeks, CVA, and simulation duration.

