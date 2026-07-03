# Demo Portfolios

NexusXVA includes an optional SQL seed for large demo portfolios:

```text
backend/src/main/resources/db/demo/demo_portfolios.sql
```

It also includes an optional workflow seed:

```text
backend/src/main/resources/db/demo/demo_workflows.sql
```

For heavier local load testing, NexusXVA also includes:

```text
backend/src/main/resources/db/demo/demo_heavy_portfolios.sql
```

These portfolios are not Flyway migrations. They are intentionally loaded only when a developer wants a rich local database for demos, manual QA, pricing, exposure, CVA, pre-trade analysis and stress testing.

## How To Load

With Docker Compose running:

```bash
docker compose exec -T postgres psql -U nexusxva -d nexusxva < backend/src/main/resources/db/demo/demo_portfolios.sql
```

To load workflow demo books:

```bash
docker compose exec -T postgres psql -U nexusxva -d nexusxva < backend/src/main/resources/db/demo/demo_workflows.sql
```

To load heavier portfolios:

```bash
docker compose exec -T postgres psql -U nexusxva -d nexusxva < backend/src/main/resources/db/demo/demo_heavy_portfolios.sql
```

The script is idempotent by fixed UUID. Running it again updates the same demo books and positions instead of creating duplicates.

## What It Creates

- `Demo - Mega Cap AI Options Book`: AAPL, MSFT, NVDA, AMZN, GOOGL, META, TSLA, AVGO, ORCL and AMD.
- `Demo - US Banks Rates Book`: JPM, BAC, GS, MS, C and WFC, with SPY/QQQ/TLT hedges.
- `Demo - Index Macro Hedge Book`: SPY, QQQ, DIA, IWM, VTI and TLT.
- `Demo - Metals Inflation Hedge Book`: GLD, SLV and CPER, with equity and duration hedges.
- `Demo - Cross Asset FO Test Book`: mixed technology, banks, ETFs, metals and duration.

The seed creates 5 USD portfolios and 72 confirmed European option positions. Positions are already confirmed so pricing, exposure, CVA and stress testing can run immediately without BO approval.

The workflow seed creates:

- `Workflow Demo - FO Tech Intake`
- `Workflow Demo - Macro Approval Queue`
- `Workflow Demo - Metals Lifecycle`

It also creates trade booking requests in `PENDING_VALIDATION`, `CONFIRMED`, and `REJECTED`, plus lifecycle requests in `PENDING_VALIDATION`, `APPROVED`, and `REJECTED`.

To inspect them:

1. Log in as an ADMIN user.
2. Open `Workflows`.
3. Use the portfolio filter to choose one of the `Workflow Demo - ...` books, or clear the filter to see all workflow entries.
4. Switch between `New trade bookings` and `Position lifecycle`.

The same data is also useful from BO:

- `Trade Validation` shows pending new trades and lifecycle requests.
- `Lifecycle Reporting` shows lifecycle queue pressure, aging and symbol/portfolio breakdown.

## Heavy Demo Portfolios

The heavy seed creates:

- `Heavy Demo - Mega Tech Vol Warehouse`
- `Heavy Demo - Cross Asset Scenario Grid`
- `Heavy Demo - Metals Macro Hedge Stack`

It creates around 512 European option positions and 32 cash equity positions across the three books. It also adds 99 booking requests and 81 lifecycle requests distributed across pending, confirmed/approved and rejected states so the books appear in both Admin workflow tabs.

Use these books when you want to stress:

- Portfolio list/detail rendering with many rows.
- Portfolio pricing aggregation.
- Stress Testing scenario matrices.
- Exposure/CVA runtime on larger books.
- Delta Hedge with option and cash-equity positions.
- Admin workflow visualization for heavy books with accepted, rejected and pending entries in both `New trade bookings` and `Position lifecycle`.

These are synthetic load-test books. They are intentionally large for local QA and are not realistic production portfolios or recommendations.

## Design Rationale

The symbols are restricted to the NexusXVA/Blemberg V1 watchlist. That keeps the demo compatible with both the local market-data provider and Blemberg pricing inputs.

Strikes are aligned with the local market-data mock spots, using a mix of ATM, OTM and hedge-like positions. Quantities include long and short positions so Greeks, stress impacts and exposure are more interesting than a one-direction book.

These are demo portfolios, not investment recommendations or official trading strategies.
