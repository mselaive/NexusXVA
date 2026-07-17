# NexusXVA Recent Recap

This recap summarizes the latest product and technical slices implemented in NexusXVA.
It is meant to help developers quickly understand where the application stands before planning the next milestone.

## Current Product Shape

NexusXVA is now more than a pricing API. It is a small risk workstation with role-based workflows:

- **FO** analyzes portfolios, prepares trades, runs pricing/risk views and monitors P&L.
- **BO** validates trades, controls lifecycle requests, manages EOD and reviews operational reporting.
- **ADMIN** manages users, permissions, operating hours, XVA setup, audit logs and workflow visibility.

The current end-to-end flow is:

```text
FO analyzes / books
  -> BO validates
  -> confirmed active positions
  -> pricing / exposure / CVA / stress / delta hedge
  -> EOD snapshots and P&L
  -> reporting, run history and audit trail
```

## Major Recent Changes

### FO Workstation

FO now has a richer daily workflow:

- **FO Desk** as the main cockpit.
- **Pre-Trade Analysis** for hypothetical trade impact before booking.
- **Stress Testing** with scenario matrices.
- **u-Pad** for sending bookings to BO.
- **Delta Hedge** for option/cash-equity hedge analysis.
- **Pricing**, **Exposure** and **CVA** screens.
- **Run History** and **Report History** for reviewing previous runs/views.

Important boundary:

- Pre-trade/stress analysis does not book trades.
- u-Pad creates pending booking requests.
- Pricing, exposure, CVA, stress and delta hedge use confirmed active positions only.

### Maker-Checker Trade Lifecycle

Trade workflows are now controlled through BO:

- New trades are submitted as booking requests.
- BO approves or rejects bookings.
- Approved bookings create confirmed positions.
- FO can request amendments or cancellations.
- BO approves/rejects lifecycle requests.
- Approved amendments mark the original position as `AMENDED` and create a new `ACTIVE` position.
- Approved cancellations mark the position as `CANCELLED`.

Analytics use only `ACTIVE` positions.
Historical positions remain visible for audit and lifecycle tracking.

### Cash Equities and Lots

Cash equities were added alongside European options:

- FO can book cash equity positions.
- BO approval creates confirmed cash equity positions.
- Cash equity execution lots are persisted.
- Average cost, cost basis, unrealized P&L and realized P&L are now modeled.
- Reducing a cash equity position through amendment can generate realized P&L.

This is still not a full accounting ledger, but it is a stronger base for future trade economics and P&L.

### EOD and P&L Hardening

EOD is now treated as an audited control process:

- BO can run global EOD across active portfolios.
- EOD creates immutable portfolio/position close snapshots.
- Daily P&L uses the latest active EOD close.
- Since-trade P&L uses execution economics.
- Incorrect closes are corrected through `VOIDED` or `SUPERSEDED`, not physical deletion.
- Recapture creates a new active close for the same business date.
- Archived portfolios do not participate in new operations, but their history remains.

EOD can also be scheduled from ADMIN Operational Control.

### FO/BO Reporting

Reporting was improved for operational review:

- FO P&L Snapshot.
- BO Operations Reporting.
- BO Lifecycle Reporting.
- Report History stores saved report views in `report_snapshots`.

Report History answers “what did the user see at that time?”.
It does not replace valuation runs, EOD, accounting or recalculation.

### Valuation Run History

Pricing, Exposure and CVA runs now persist audit snapshots:

- input JSON.
- result JSON.
- summary JSON.
- user, active group and scope.
- portfolio or netting-set context.
- status: `SUCCESS` or `FAILED`.

Run History is audit/replay context.
It is not official EOD, accounting or market data.

### XVA Setup, Netting and Collateral

ADMIN now owns XVA reference data:

- counterparties.
- netting sets.
- portfolio assignments.
- active/inactive status.
- static collateral.
- credit curves.
- discount curves.

FO can run CVA on:

- a single portfolio.
- an active netting set.

Netting-set CVA V1 is profile-level only:

- aggregate assigned portfolio exposures.
- subtract static collateral from positive exposure buckets.
- apply the simplified CVA formula.

It does not yet model path-level legal netting, CSA margining, collateral calls, wrong-way risk or DVA/FVA/KVA.

### Curve Lifecycle

Persisted CVA curves now have lifecycle and versioning:

```text
ADMIN creates curve
  -> DRAFT version
  -> ADMIN approves or rejects
  -> APPROVED curve becomes active
  -> previous active approved version becomes SUPERSEDED
  -> CVA can reference only active APPROVED curves
```

Rules:

- New curves start as `DRAFT`.
- Draft curves can be edited.
- Approved/rejected/superseded curves are immutable history.
- Approving a new version supersedes the previous active approved version.
- CVA can use only active `APPROVED` persisted curves.
- `source` supports `MANUAL`, `IMPORT` and `MARKET_DATA`.
- Actual imports and Blemberg-sourced curve ingestion are future work.

### Operational Control

ADMIN can configure a global operating calendar:

- timezone.
- business days.
- trading open/close.
- EOD schedule.
- stale market data policy for EOD.
- separate blocking switches for:
  - FO bookings/lifecycle requests.
  - risk runs such as pricing, pre-trade, stress, delta hedge, exposure and CVA.

The backend is the authority.
Frontend disabled buttons are a convenience, not a security boundary.

### Audit Trail and Technical Logs

NexusXVA now separates:

- **Audit Trail** in PostgreSQL for user activity and operational control.
- **Technical logs** in rotated files for debugging backend behavior.

Audit events include logins, group changes, denied access, bookings, approvals, lifecycle decisions, EOD corrections, setup changes and valuation requests.

Technical logs include system, auth, market-data integration, EOD jobs and errors, with correlation ids.

### Blemberg Integration

NexusXVA uses Blemberg through the `marketdata` boundary:

- instrument validation.
- pricing inputs.
- diagnostic snapshots/coverage in the frontend.
- priority refresh through NexusXVA proxy endpoints.

NexusXVA still does not persist market data as an official source of truth.
Blemberg owns cached market data; NexusXVA owns portfolios, workflows, valuation and audit.

## Current Data Ownership

High-level ownership:

- **Portfolio module** owns portfolios, confirmed positions and trade terms.
- **Trade booking/lifecycle modules** own pending/approved/rejected workflow state.
- **Portfolio/EOD modules** own close snapshots and P&L references.
- **XVA module** owns counterparties, netting sets, collateral and curves.
- **Valuation runs** own calculation audit snapshots.
- **Report snapshots** own saved workstation views.
- **Audit module** owns user activity history.
- **Blemberg** owns market data.

## Important Boundaries

NexusXVA intentionally does not:

- persist market data from Blemberg as source of truth.
- use draft curves for CVA.
- use pending bookings in pricing/exposure/CVA.
- physically delete EOD closes.
- physically delete operational history.
- treat Report History as recalculation data.
- treat Run History as official accounting.
- implement full ledger accounting yet.

## What Feels Stable Now

The strongest areas are:

- role-based FO/BO/ADMIN workflows.
- European option pricing and portfolio pricing.
- Exposure V1 and simplified CVA.
- BO trade validation and lifecycle.
- EOD correction model.
- XVA setup with netting-set CVA.
- curve versioning/approval.
- run/report/audit history foundations.

## Natural Next Steps

Recommended next work:

1. **Curve imports and market-data sourced curves**
   - CSV/manual upload.
   - Blemberg-sourced discount/risk-free curves.
   - validation preview before approval.

2. **More complete cash equity accounting**
   - buy/sell lots.
   - partial closes.
   - realized P&L across reductions.
   - average-cost vs FIFO/LIFO policy discussion.

3. **Better report exports**
   - CSV export for BO reports.
   - PDF or printable run summaries.
   - saved filters.

4. **Counterparty/CVA hardening**
   - collateral profiles.
   - credit curve assignment by counterparty.
   - netting-set level curve defaults.

5. **Market risk and historical analytics**
   - once Blemberg historical coverage is mature.
   - VaR/stress from historical returns.
   - avoid storing duplicate market data in NexusXVA unless a clear derived-data use case exists.

