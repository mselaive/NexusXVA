# NexusXVA EOD Process

## Purpose

End of Day stores an official audited valuation reference for each portfolio. It never overwrites trade economics, positions, bookings, lifecycle state, or Blemberg market data.

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

Only one `ACTIVE` close may exist for a portfolio and business date. If BO made a mistake, the correction flow voids or supersedes the old run instead of deleting it.

The manual BO action is `Run EOD for all portfolios`. One failing book does not roll back successful closes from other portfolios. After the batch, BO may select an individual portfolio to inspect its close history and correction audit.

## Corrections

Rollback in NexusXVA means an audited correction, not physical deletion:

- `Void` marks an `ACTIVE` EOD run as `VOIDED` with a required reason.
- `Recapture` marks the original run as `SUPERSEDED`, reruns pricing for the same portfolio/date, and stores a new `ACTIVE` run linked to the original.
- Daily P&L and latest-close reads use only `ACTIVE` EOD runs.
- History shows `ACTIVE`, `VOIDED`, and `SUPERSEDED` runs so the audit trail remains visible.

Portfolios are archived instead of hard-deleted. Archived portfolios disappear from operational workflows, but historical EOD snapshots remain in the database.

Daily P&L uses prior EOD value for existing positions and execution trade value for positions created after the close. Missing references remain unavailable.

The scheduler is disabled by default:

```text
NEXUSXVA_EOD_ENABLED=true
NEXUSXVA_EOD_CRON=0 15 17 * * MON-FRI
NEXUSXVA_EOD_ZONE=America/New_York
NEXUSXVA_EOD_ALLOW_STALE=false
```

Expected failures include duplicate close dates, stale market data, unpriceable active positions, unavailable Blemberg, and non-USD portfolios.
