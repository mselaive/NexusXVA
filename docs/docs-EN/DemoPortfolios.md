# Demo Portfolios

NexusXVA includes an optional SQL seed for large demo portfolios:

```text
backend/src/main/resources/db/demo/demo_portfolios.sql
```

These portfolios are not Flyway migrations. They are intentionally loaded only when a developer wants a rich local database for demos, manual QA, pricing, exposure, CVA, pre-trade analysis and stress testing.

## How To Load

With Docker Compose running:

```bash
docker compose exec -T postgres psql -U nexusxva -d nexusxva < backend/src/main/resources/db/demo/demo_portfolios.sql
```

The script is idempotent by fixed UUID. Running it again updates the same demo books and positions instead of creating duplicates.

## What It Creates

- `Demo - Mega Cap AI Options Book`: AAPL, MSFT, NVDA, AMZN, GOOGL, META, TSLA, AVGO, ORCL and AMD.
- `Demo - US Banks Rates Book`: JPM, BAC, GS, MS, C and WFC, with SPY/QQQ/TLT hedges.
- `Demo - Index Macro Hedge Book`: SPY, QQQ, DIA, IWM, VTI and TLT.
- `Demo - Metals Inflation Hedge Book`: GLD, SLV and CPER, with equity and duration hedges.
- `Demo - Cross Asset FO Test Book`: mixed technology, banks, ETFs, metals and duration.

The seed creates 5 USD portfolios and 72 confirmed European option positions. Positions are already confirmed so pricing, exposure, CVA and stress testing can run immediately without BO approval.

## Design Rationale

The symbols are restricted to the NexusXVA/Blemberg V1 watchlist. That keeps the demo compatible with both the local market-data provider and Blemberg pricing inputs.

Strikes are aligned with the local market-data mock spots, using a mix of ATM, OTM and hedge-like positions. Quantities include long and short positions so Greeks, stress impacts and exposure are more interesting than a one-direction book.

These are demo portfolios, not investment recommendations or official trading strategies.
