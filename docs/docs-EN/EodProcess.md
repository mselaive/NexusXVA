# NexusXVA EOD Process

## Purpose

End of Day stores an official immutable valuation reference for each portfolio. It never overwrites trade economics, positions, bookings, lifecycle state, or Blemberg market data.

## Ownership

- **FO** consumes Daily P&L and Unrealized P&L.
- **BO** may run a manual close from `EOD Control`.
- **SYSTEM** may run scheduled closes.
- **ADMIN** does not close portfolios by default.

## Flow

```text
Blemberg refresh
  -> BO selects the business date
  -> NexusXVA iterates over all portfolios
  -> pricing and quality gates run independently for each book
  -> portfolio and position EOD snapshots are persisted
  -> each portfolio reports CAPTURED, SKIPPED, or FAILED
```

Only one close may exist for a portfolio and business date.

The manual BO action is `Run EOD for all portfolios`. One failing book does not roll back successful closes from other portfolios. After the batch, BO may select an individual portfolio only to inspect its immutable close history.

Daily P&L uses prior EOD value for existing positions and execution trade value for positions created after the close. Missing references remain unavailable.

The scheduler is disabled by default:

```text
NEXUSXVA_EOD_ENABLED=true
NEXUSXVA_EOD_CRON=0 15 17 * * MON-FRI
NEXUSXVA_EOD_ZONE=America/New_York
NEXUSXVA_EOD_ALLOW_STALE=false
```

Expected failures include duplicate close dates, stale market data, unpriceable active positions, unavailable Blemberg, and non-USD portfolios.
