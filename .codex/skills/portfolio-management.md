# Portfolio Management Skill

## Current Scope

Portfolio management persists portfolios and confirmed European option positions in PostgreSQL. FO booking requests are a separate workflow.

Current endpoints:

- `POST /api/portfolios`
- `GET /api/portfolios`
- `GET /api/portfolios/{portfolioId}`
- `PATCH /api/portfolios/{portfolioId}`
- `DELETE /api/portfolios/{portfolioId}` archives the portfolio; it must not hard-delete history.
- `GET /api/portfolios/{portfolioId}/instruments`
- `GET /api/portfolios/{portfolioId}/instruments/{positionId}`
- `POST /api/portfolios/{portfolioId}/trade-bookings/european-options`
- `GET /api/trade-bookings/mine`
- `GET /api/back-office/trade-bookings`
- `POST /api/back-office/trade-bookings/{bookingId}/approve`
- `POST /api/back-office/trade-bookings/{bookingId}/reject`
- `POST /api/portfolios/{portfolioId}/pricing/black-scholes`

## Persisted Fields

Portfolio:

- `id`
- `name`
- `description`
- `baseCurrency`
- `archivedAt`
- `archivedByUserId`
- `archiveReason`
- `createdAt`
- `updatedAt`

European option position:

- `id`
- `portfolioId`
- `underlyingSymbol`
- `optionType`
- `strike`
- `maturityDate`
- `quantity`
- `createdAt`
- `updatedAt`

## Project Rules

- Portfolio positions store trade terms only.
- Pending and rejected booking requests are not portfolio positions and must not enter analytics.
- Archived portfolios are not operational books. They should be filtered out of FO/BO risk workflows and EOD batch, but their historical bookings, lifecycle records, and EOD snapshots must remain available for audit.
- BO approval is the only public workflow that creates confirmed positions.
- Confirmed positions are immutable until amendment/cancellation workflows exist.
- Do not persist `spot`, `riskFreeRate`, `volatility`, or `dividendYield` inside positions.
- Treat those values as market data or pricing inputs.
- Validate `underlyingSymbol` through `marketdata` when Blemberg validation is enabled.
- Do not call Blemberg directly from portfolio API, domain, or infrastructure code.
- Request pricing inputs through `marketdata`; portfolio code should not own provider-specific market-data logic.
- `nexusxva.market-data.provider=local` is temporary. It validates symbols and provides demo pricing inputs including `spot`, `volatility`, `riskFreeRate`, and `dividendYield`, but it does not persist market data or replace Blemberg.
- `OptionType` lives in `instruments.domain` because pricing and portfolio both use it.
- Use UUIDs as public and database identifiers.
- Use `BigDecimal` for persisted strike and quantity.
- Use a 3-letter uppercase `baseCurrency`, default `USD`.
- Allow positive and negative quantity for long/short positions, but reject zero quantity.
- Allow past `maturityDate`; pricing decides whether a trade can be valued.
- Portfolio Black-Scholes pricing supports FX V1: convert monetary values to portfolio `baseCurrency` through `marketdata`.
- Do not persist FX rates, spot, vol, risk-free rate, dividend yield, or provider metadata inside positions.
- Keep `delta` in underlying units; convert monetary values and monetary Greeks (`gamma`, `vega`, `theta`, `rho`) to the portfolio base currency.
- Expired positions should be reported as `UNPRICEABLE_EXPIRED` and excluded from valuation totals.
- Do not persist portfolio pricing results unless a future valuation-storage feature explicitly asks for it.

## Persistence

- Use Flyway migrations for schema changes.
- Use JPA entities/repositories inside `portfolio.infrastructure`.
- Do not return JPA entities from controllers.
- Keep transaction boundaries in application services.
- Integration tests for portfolio persistence should use PostgreSQL/Testcontainers.

## Portfolio Pricing

Portfolio-level Black-Scholes pricing currently:

- Read persisted European option positions.
- Request pricing inputs through `marketdata`.
- Convert `maturityDate` to `timeToMaturityYears` with ACT/365.
- Reuse the existing Black-Scholes calculator.
- Return per-position price/Greeks plus a portfolio total.

## Next Natural Step

Add controlled amendment/cancellation workflows after BO Trade Validation is stable, then build ADMIN user/group management.
