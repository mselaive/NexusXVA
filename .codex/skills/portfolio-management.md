# Portfolio Management Skill

## Current Scope

Portfolio management persists portfolios and European option positions in PostgreSQL.

Current endpoints:

- `POST /api/portfolios`
- `GET /api/portfolios`
- `GET /api/portfolios/{portfolioId}`
- `PATCH /api/portfolios/{portfolioId}`
- `DELETE /api/portfolios/{portfolioId}`
- `POST /api/portfolios/{portfolioId}/instruments/european-options`
- `GET /api/portfolios/{portfolioId}/instruments`
- `GET /api/portfolios/{portfolioId}/instruments/{positionId}`
- `PATCH /api/portfolios/{portfolioId}/instruments/european-options/{positionId}`
- `DELETE /api/portfolios/{portfolioId}/instruments/{positionId}`
- `POST /api/portfolios/{portfolioId}/pricing/black-scholes`

## Persisted Fields

Portfolio:

- `id`
- `name`
- `description`
- `baseCurrency`
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
- Portfolio Black-Scholes pricing V1 is USD-only and must reject non-USD portfolios until FX is explicitly implemented.
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

Replace the local demo pricing-input provider with the Blemberg HTTP pricing-input adapter once Blemberg exists, then move toward simulation/exposure.
