# NexusXVA Frontend

Dashboard V1 for the NexusXVA backend. It visualizes portfolios, portfolio-level Black-Scholes pricing, Exposure V1 and flat-mode CVA V1.1.

## Pages

- `/`: workflow overview.
- `/upad`: `u-Pad` trade capture for portfolios and European option positions.
- `/portfolios`: portfolio inventory and position blotter.
- `/pricing`: portfolio-level Black-Scholes valuation.
- `/exposure`: Monte Carlo exposure profile.
- `/cva`: flat-mode CVA V1.1.

The dashboard includes info buttons near each workflow panel. They explain the backend flow and current modeling boundary without moving calculations into the frontend.

## Run With Docker Compose

From the repository root:

```bash
docker compose up --build
```

Open `http://localhost:3000`.

In Docker, the dashboard proxy is built with:

```bash
NEXUSXVA_API_BASE_URL=http://backend:8080
```

That means browser calls to `/nexus-api/*` are handled by the frontend container and forwarded to the backend container.

## Run Locally

```bash
npm install
npm run dev
```

Open `http://localhost:3000`.

By default, the dashboard calls `/nexus-api/*`, and `next.config.ts` proxies those requests to:

```bash
http://localhost:8080/api/*
```

To point the proxy at another backend:

```bash
NEXUSXVA_API_BASE_URL=http://localhost:8080 npm run dev
```

If you intentionally want browser requests to call the backend directly, set:

```bash
NEXT_PUBLIC_NEXUSXVA_API_BASE_URL=http://localhost:8080
```

That direct mode requires backend CORS support. The proxy mode is recommended for local development.

## Scope

- The frontend does not calculate prices, Greeks, exposure or CVA.
- The backend remains the source of truth for all quantitative logic.
- Dashboard V1 supports USD European option portfolios.
- CVA curve mode exists in the backend, but Dashboard V1 exposes flat CVA inputs first.
- `u-Pad` books trade terms only. Spot, volatility, rates and dividend yield still come from market data during valuation.

## Checks

```bash
npm run test
npm run build
```
