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

## Project Rules

- Portfolio positions store trade terms only.
- Do not persist `spot`, `riskFreeRate`, or `volatility` inside positions.
- Treat those values as market data or pricing inputs.
- `OptionType` lives in `instruments.domain` because pricing and portfolio both use it.
- Use UUIDs as public and database identifiers.
- Use `BigDecimal` for persisted strike and quantity.
- Use a 3-letter uppercase `baseCurrency`, default `USD`.
- Allow positive and negative quantity for long/short positions, but reject zero quantity.
- Allow past `maturityDate`; pricing decides whether a trade can be valued.

## Persistence

- Use Flyway migrations for schema changes.
- Use JPA entities/repositories inside `portfolio.infrastructure`.
- Do not return JPA entities from controllers.
- Keep transaction boundaries in application services.
- Integration tests for portfolio persistence should use PostgreSQL/Testcontainers.

## Next Natural Step

Portfolio-level pricing should:

- Read persisted European option positions.
- Receive market data separately.
- Convert `maturityDate` to `timeToMaturityYears`.
- Reuse the existing Black-Scholes calculator.
- Return per-position price/Greeks plus a portfolio total.
