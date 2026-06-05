# NexusXVA Integration

NexusXVA should treat Blemberg as an external HTTP market-data service.

Local Docker base URL:

```text
http://localhost:8081
```

Inside another Docker Compose network, use the service name and internal port configured for that network.

## Instrument Validation

Before accepting a portfolio position when market-data validation is enabled:

```http
GET /api/instruments/{symbol}
```

Example:

```bash
curl http://localhost:8081/api/instruments/AAPL
```

NexusXVA should reject:

- `404` unknown symbols.
- `active=false` instruments.

Symbols are case-insensitive at the API boundary because Blemberg normalizes them to uppercase.

## Pricing Inputs

For European option portfolio pricing:

```http
GET /api/market-data/pricing-inputs/european-option?symbol={symbol}&maturityDate={yyyy-mm-dd}
```

Example:

```bash
curl "http://localhost:8081/api/market-data/pricing-inputs/european-option?symbol=AAPL&maturityDate=2027-06-01"
```

Required response fields for NexusXVA:

- `symbol`
- `spot`
- `volatility`
- `riskFreeRate`
- `dividendYield`
- `currency`
- `asOf`
- `source`
- `stale`

Blemberg also returns audit fields:

- `volatilityMethod`
- `rateMethod`
- `dividendYieldMethod`

## Error Mapping

Recommended NexusXVA handling:

| Blemberg response | NexusXVA behavior |
| --- | --- |
| `404 Instrument not found` | Reject unknown underlying |
| `404 Pricing inputs not found` | Reject or fail portfolio pricing validation |
| `400 Malformed request` | Treat as caller/configuration error |
| `503 Market data service unavailable` | Map to market-data service unavailable |

## Operational Order

For a fresh local environment:

1. Start Blemberg.
2. Confirm health.
3. Trigger manual refresh.
4. Point NexusXVA market-data base URL to Blemberg.

Commands:

```bash
curl http://localhost:8081/actuator/health
curl -X POST http://localhost:8081/api/admin/market-data/refresh
```

## Boundary Reminder

Blemberg provides market data. NexusXVA owns pricing.

Do not persist spot, volatility, risk-free rate, or dividend yield inside NexusXVA portfolio positions. Store trade terms only and fetch market inputs when pricing.
