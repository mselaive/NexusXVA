# NexusXVA Backend

Spring Boot backend for NexusXVA.

The backend currently includes the project foundation, stateless European option pricing with the Black-Scholes model and Greeks, persisted portfolio management for European option and cash equity positions, portfolio-level pricing using market-data pricing inputs, Exposure V1 with simple GBM Monte Carlo simulation, simplified CVA V1.2, XVA reference data for counterparties/netting sets/collateral/curves, cash-equity delta hedge analysis, persisted valuation run history, persisted report snapshots, Audit Trail V1, and rotated technical logs.

## Requirements

- Java 21
- Maven 3.6.3 or newer
- Docker and Docker Compose for the containerized workflow
- PostgreSQL for persisted portfolio workflows

## Run Locally

```bash
mvn spring-boot:run
```

The backend starts on port `8080`.

Health check:

```bash
curl http://localhost:8080/api/health
```

Black-Scholes pricing example:

```bash
curl -X POST http://localhost:8080/api/pricing/european-options/black-scholes \
  -H 'Content-Type: application/json' \
  -d '{
    "optionType": "CALL",
    "spot": 100.0,
    "strike": 100.0,
    "timeToMaturityYears": 1.0,
    "riskFreeRate": 0.05,
    "volatility": 0.2,
    "dividendYield": 0.0
  }'
```

## Run With Docker

From the repository root:

```bash
docker compose up --build
```

This starts:

- `backend` on `http://localhost:8080`
- `frontend` on `http://localhost:3000`
- `postgres` on `localhost:5432`

Portfolio management uses PostgreSQL through Spring Data JPA. Flyway runs the database migrations on application startup. The frontend proxies `/nexus-api/*` to the backend service inside Docker.

Docker Compose enables Auth V1 by default. The initial development login is:

- username: `admin`
- password: `admin12345`

Flyway also creates demo users for local testing:

- FO only: `fo.trader` / `fo12345`
- BO only: `bo.ops` / `bo12345`
- Multi-group: `raul` / `multi12345` with `FO`, `BO`, and `ADMIN`

Override it before sharing an environment:

```bash
NEXUSXVA_BOOTSTRAP_ADMIN_USERNAME=admin \
NEXUSXVA_BOOTSTRAP_ADMIN_PASSWORD='change-this-password' \
docker compose up --build
```

Docker Compose defaults to the local market-data provider so the dashboard can run pricing, exposure and CVA without Blemberg. To use Blemberg instead:

```bash
NEXUSXVA_MARKET_DATA_PROVIDER=blemberg \
NEXUSXVA_MARKET_DATA_VALIDATION_ENABLED=true \
BLEMBERG_BASE_URL=http://host.docker.internal:8081 \
docker compose up --build
```

Optional large demo portfolios can be loaded after Docker Compose is running:

```bash
docker compose exec -T postgres psql -U nexusxva -d nexusxva < backend/src/main/resources/db/demo/demo_portfolios.sql
```

This creates 5 USD demo portfolios and 72 confirmed European option positions across the supported Blemberg/local watchlist. The seed is idempotent and is not a Flyway migration, so tests and clean environments are not forced to include demo data.

Optional workflow demo data can also be loaded:

```bash
docker compose exec -T postgres psql -U nexusxva -d nexusxva < backend/src/main/resources/db/demo/demo_workflows.sql
```

This creates three workflow-focused portfolios plus booking and lifecycle requests in pending, approved/confirmed and rejected states. Open ADMIN -> Workflows to inspect the nodes, or BO -> Trade Validation / Lifecycle Reporting to work the queues.

For heavier local load testing:

```bash
docker compose exec -T postgres psql -U nexusxva -d nexusxva < backend/src/main/resources/db/demo/demo_heavy_portfolios.sql
```

This creates three larger synthetic portfolios with roughly 512 option positions, 32 cash equity positions, 99 workflow-visible booking requests and 81 lifecycle requests in pending, approved/confirmed and rejected states. Use it to pressure Pricing, Stress Testing, Exposure, CVA, Delta Hedge, Admin Workflows and large table rendering.

## Run Backend And Dashboard

The simplest way to run the application is:

```bash
docker compose up --build
```

Then open `http://localhost:3000`.

For manual local development, start the backend from `backend/`:

```bash
mvn spring-boot:run
```

Start the dashboard from `frontend/`:

```bash
npm install
npm run dev
```

The dashboard opens on `http://localhost:3000`. It proxies `/nexus-api/*` to `http://localhost:8080/api/*` by default, so local browser requests do not require backend CORS changes.

To point the dashboard proxy at a different backend:

```bash
NEXUSXVA_API_BASE_URL=http://localhost:8080 npm run dev
```

## Authentication And Groups

Auth V1 is enabled in Docker with `NEXUSXVA_AUTH_ENABLED=true`. Local backend runs keep auth disabled by default so existing API workflows and tests remain simple unless auth is explicitly enabled.

Auth endpoints:

- `POST /api/auth/login`
- `GET /api/auth/me`
- `POST /api/auth/active-group`
- `POST /api/auth/logout`

Security model:

- Users are stored in `auth_user_accounts`.
- Groups are stored in `auth_groups`.
- User/group membership is stored in `auth_user_group_memberships`.
- Built-in groups are `FO`, `BO`, and `ADMIN`.
- Passwords are stored only as BCrypt hashes.
- Browser sessions use opaque random tokens in an `HttpOnly` cookie.
- The database stores only SHA-256 hashes of session tokens, not the raw cookie value.
- Mutating authenticated requests require `X-CSRF-Token`; the frontend receives it from login or `/api/auth/me`.
- One user may belong to multiple groups, but each session has one server-side `activeGroup`.

Current group intent:

- `FO`: FO Desk, Pre-Trade Analysis, Stress Testing, u-Pad booking submission, portfolios, pricing, exposure, CVA, valuation run history and report history.
- `BO`: Trade Validation, Lifecycle Reporting, Operations Reporting, preventive Trading Limits, manual EOD Control, valuation run history and report history.
- `ADMIN`: user/group administration, FO feature permissions, portfolio visibility, Operational Control, workflow monitoring, Audit Logs, valuation run history and report history.

The backend enforces the active group. Frontend navigation is not the security boundary.

## Administration V1

Admin users can manage access without changing the pricing or booking models:

- `GET /api/admin/users`
- `GET /api/admin/users/{userId}`
- `PUT /api/admin/users/{userId}/groups`
- `PUT /api/admin/users/{userId}/permissions`
- `PUT /api/admin/users/{userId}/portfolio-access`
- `GET /api/admin/portfolios`
- `GET /api/admin/workflow-map`
- `GET /api/admin/audit-events`
- `GET /api/admin/audit-events/{eventId}`
- `GET /api/admin/operational-control`
- `PUT /api/admin/operational-control`
- `GET /api/operational-control/status`

Group membership decides which area a user can enter. FO feature permissions then refine what a Front Office user can do:

- `FO_BOOK_TRADES`
- `FO_CREATE_PORTFOLIOS`
- `FO_RUN_CVA`
- `FO_RUN_WHAT_IF`
- `FO_RUN_STRESS_TEST`
- `FO_REQUEST_LIFECYCLE`

Portfolio visibility supports `ALL` or `SELECTED`. The default is permissive (`ALL` portfolios and enabled FO features) so existing development users keep working until an admin tightens access.

The workflow map is read-only. It visualizes trade booking requests across `Booked`, `Waiting BO`, `Accepted`, and `Rejected`; it does not approve or reject bookings. BO Trade Validation remains the owner of that maker-checker action.

Operational Control is global in V1. ADMIN configures the timezone, business days, trading open/close, automatic EOD enablement, EOD time and stale-market-data policy. The trading window has two independent blocking switches: one for FO trade bookings/lifecycle requests, and one for risk runs such as pricing, Pre-Trade Analysis, Stress, Delta Hedge, Exposure and CVA. Blocked actions return `409 ApiError` outside the window; disabled switches remain advisory. BO validation/corrections and ADMIN screens remain available.

Operational Control also owns **Close Checklist V1**:

- ADMIN selects the portfolios in scope.
- ADMIN enables ordered steps across `PRE_EOD`, `EOD` and `POST_EOD`.
- Step types are `FO_PNL_REPORT`, `BO_OPERATIONS_REPORT`, `BO_LIFECYCLE_REPORT`, `PORTFOLIO_PRICING`, `EXPOSURE`, `CVA` and `EOD`.
- Exposure/CVA use global defaults stored with Operational Control.
- CVA checklist runs require approved persisted `creditCurveId` and `discountCurveId`.
- BO runs the checklist manually from EOD Control through `POST /api/back-office/close-checklist/runs`.
- The scheduler runs the checklist instead of plain EOD when both EOD scheduling and Close Checklist are enabled.
- Critical failed `PRE_EOD` steps block EOD and skip later steps.

ExecuteScript V1 is a separate manual diagnostics/playbook module:

- ADMIN creates reusable templates from controlled step types.
- BO executes templates in `DRY_RUN` or `REAL_RUN`.
- Step types are `FO_PNL_REPORT`, `BO_OPERATIONS_REPORT`, `BO_LIFECYCLE_REPORT`, `PORTFOLIO_PRICING`, `EXPOSURE`, `CVA`, `EOD_VALIDATE` and `EOD_CAPTURE`.
- `DRY_RUN` stores only `execute_script_runs` / `execute_script_run_steps`; it does not create EOD closes, valuation runs or report snapshots.
- `REAL_RUN` may create `report_snapshots`, `valuation_runs` and EOD snapshots when those steps are selected.
- Critical failed steps skip the rest of the script.

ExecuteScript endpoints:

- `GET /api/admin/execute-scripts/templates`
- `POST /api/admin/execute-scripts/templates`
- `PATCH /api/admin/execute-scripts/templates/{templateId}`
- `GET /api/back-office/execute-scripts/templates`
- `POST /api/back-office/execute-scripts/runs`
- `GET /api/back-office/execute-scripts/runs`
- `GET /api/back-office/execute-scripts/runs/{runId}`

The ADMIN checkbox controls the runtime policy. Backend enforcement also has a technical guard, `NEXUSXVA_OPERATIONAL_CONTROL_ENFORCEMENT_ENABLED=true`, intended only as a controlled local/test escape hatch; production-like runs should keep the env guard enabled.

## Audit Trail And Technical Logs

NexusXVA has two separate observability layers:

- **Audit Trail**: user activity stored in PostgreSQL table `audit_events`.
- **Technical logs**: rotated files for backend debugging and support.

Audit events answer operational questions such as who logged in, who changed active group, who submitted a booking, who approved or rejected a workflow action, who changed limits, who archived a portfolio, who corrected EOD, who requested pricing/exposure/CVA/stress/pre-trade/delta hedge, and who was denied access.

ADMIN can inspect audit events from **Audit Logs** or through:

- `GET /api/admin/audit-events`
- `GET /api/admin/audit-events/{eventId}`

Supported filters are `userId`, `username`, `module`, `eventType`, `outcome`, `resourceType`, `resourceId`, `from`, `to`, `page`, and `size`.

Audit metadata is sanitized. NexusXVA does not store passwords, raw session tokens, cookies, CSRF values, raw request bodies or full responses in `audit_events`.

Technical logs are written under `backend/logs/` when running with Docker Compose:

- `backend/logs/backend/system/app.log`
- `backend/logs/backend/system/error.log`
- `backend/logs/backend/security/auth.log`
- `backend/logs/backend/integration/marketdata.log`
- `backend/logs/backend/jobs/eod.log`

The logger routing is defined in `src/main/resources/logback-spring.xml`:

- `com.nexusxva.auth` -> `security/auth.log`
- `com.nexusxva.marketdata` -> `integration/marketdata.log`
- `com.nexusxva.eod` -> `jobs/eod.log`
- root logger -> console and `system/app.log`
- error threshold appender -> `system/error.log`

The dashboard market-data refresh goes through NexusXVA at `POST /api/market-data/blemberg/refresh` before calling Blemberg, so refresh requests and incomplete refresh results are visible in `marketdata.log`. Snapshot and coverage reads remain diagnostic frontend calls.

NexusXVA also runs a Blemberg startup health probe by default, even when pricing uses the local market-data provider, because the dashboard can still use Blemberg for market-watch diagnostics. Disable it with `BLEMBERG_STARTUP_PROBE_ENABLED=false` if a local-only environment should stay quiet.

Docker Compose bind-mounts `./backend/logs:/app/logs`, so backend logs are easy to inspect from the repo and still write to `/app/logs` inside the container. Local non-Docker runs default to `logs/` from the backend working directory unless `NEXUSXVA_LOG_DIR` is overridden.

Relevant environment variables:

- `NEXUSXVA_LOG_FILE_ENABLED=true`
- `NEXUSXVA_LOG_DIR=/app/logs`
- `NEXUSXVA_LOG_RETENTION_DAYS=14`

Every API response includes `X-Correlation-Id`. The same `correlationId` is stored in audit events and written into technical logs, alongside user, active group and session when available.

## Notifications V1

NexusXVA persists user notifications so workflow events remain visible after refresh or a new session:

- `GET /api/notifications`
- `POST /api/notifications/{notificationId}/read`
- `POST /api/notifications/read-all`

FO receives notifications when BO approves or rejects trade bookings and lifecycle requests. BO receives notifications when FO submits a new booking, amend request or cancel request. Notifications belong to the user, not to the active group, so a multi-group user keeps one inbox while switching between FO, BO and ADMIN.

## Front Office Desk And Pre-Trade Analysis

FO Desk is the Front Office cockpit and the recommended first screen for FO users:

- `GET /api/front-office/desk`

It aggregates visible portfolios, the FO user's booking history, and booking counts. Reviewed or pending bookings remain visible as workflow history; pricing, exposure and CVA still use confirmed positions only.

Pre-Trade Analysis lets FO test one hypothetical European option before sending it to u-Pad and BO:

- `POST /api/front-office/what-if/european-option`

Pre-Trade Analysis is stateless. It does not create a `trade_booking_requests` row, does not create a confirmed position, and does not persist valuation results. It reuses portfolio Black-Scholes pricing and market-data pricing inputs, then returns base portfolio totals, hypothetical trade valuation, with-trade totals and incremental impact. The UI also shows Blemberg market snapshots and strike-vs-market context so FO can see whether the option is in, near, or out of the money. If FO likes the result, the UI sends the ticket terms to u-Pad for review and official BO submission.

Stress Testing lets FO run scenario matrices over confirmed positions, optionally including one hypothetical trade:

- `POST /api/front-office/stress-tests/european-options`

Stress Testing is stateless and pricing/Greeks-only. V1 shocks `spot` by relative percent and shocks `volatility`, `riskFreeRate`, and `dividendYield` by absolute basis points. The result returns base portfolio totals, optional hypothetical trade valuation, per-scenario stressed totals, scenario impact versus the confirmed base portfolio, per-position stressed values, and expired unpriceable positions. It does not run Exposure or CVA and does not persist stress results.

Delta Hedge lets FO calculate the cash-equity hedge implied by current option delta:

- `POST /api/front-office/delta-hedge/european-options`

Delta Hedge V1 is stateless. It prices confirmed option positions, adds active cash equity positions, groups delta by symbol, and suggests the stock quantity needed to reach target delta, defaulting to zero. It does not auto-book hedges; FO must book the chosen cash equity trade through u-Pad and BO validation.

## Trading Limits V1

Back Office can configure one preventive policy per active FO user:

- `GET /api/back-office/trading-limits/users`
- `GET /api/back-office/trading-limits/users/{userId}`
- `PUT /api/back-office/trading-limits/users/{userId}`
- `GET /api/trading-limits/me` for the authenticated FO user

Each policy may limit trades and notional per UTC calendar hour or day. A `null` measure is unlimited, and a missing or disabled policy never blocks. V1 notional is:

```text
abs(quantity) * strike
```

This is a preventive approximation in USD, not premium paid, cash movement, P&L, or a market valuation. Every submitted booking consumes capacity even if BO rejects it later. Limit validation and booking creation share one transaction and lock the user policy, preventing concurrent requests from jointly exceeding a configured limit.

If a portfolio is not USD, NexusXVA converts the approximate booking notional from the portfolio `baseCurrency` into USD through `FxRateService` before checking and storing limit usage. The trade terms remain unchanged; only the preventive limit usage is normalized.

Breaches return `409 ApiError` with sanitized metadata containing the limit type, maximum, current usage, requested amount, and UTC period end. No booking row is created for a breached request.

## Run Tests

```bash
mvn test
```

The current test setup covers:

- Spring application context startup
- `/api/health`
- stable validation error response shape
- PostgreSQL integration tests with Testcontainers
- Black-Scholes known-value tests for European calls and puts
- financial invariants such as put-call parity and monotonicity
- pricing API request, response, and validation behavior
- persisted portfolio metadata and confirmed European option positions
- active-group authorization plus FO submission and BO approval/rejection workflows
- preventive FO trading-limit policies, UTC usage, breach metadata, and concurrent booking enforcement
- portfolio-level Black-Scholes pricing with local market-data inputs
- Exposure V1 Monte Carlo simulation, deterministic GBM paths, and exposure aggregation
- simplified CVA V1.1 over the exposure profile
- Dashboard V1 frontend workflow for FO Desk, Pre-Trade Analysis, Stress Testing, u-Pad, portfolios, pricing, exposure and CVA flat/curve modes
- FO trade lifecycle workflow for amendment and cancellation requests over confirmed positions
- FO/BO read-only reporting over P&L snapshots, lifecycle queues and EOD coverage

The current suite has more than 150 tests, including one real Blemberg smoke test that is skipped unless explicitly enabled.

## Valuation Run History V1

Run History records portfolio and netting-set valuation executions without changing the pricing model:

- `GET /api/valuation-runs`
- `GET /api/valuation-runs/{runId}`

Filters:

- `runType=PRICING|EXPOSURE|CVA`
- `status=SUCCESS|FAILED`
- `scopeType=PORTFOLIO|NETTING_SET`
- `scopeId=<uuid>`
- `portfolioId=<uuid>`
- `limit=50`

Stored fields include scope, optional portfolio, model, valuation date, status, requesting user/group, `input_json`, `result_json`, `summary_json`, error message and timestamp. `PORTFOLIO` runs keep `portfolioId`; `NETTING_SET` runs use `scopeType=NETTING_SET` and `scopeId=<nettingSetId>`.

Important boundaries:

- Run History is audit/replay context, not official EOD close.
- Run History does not persist market data as a source of truth.
- Pricing, Exposure and CVA still recompute from current portfolio state and `marketdata` inputs.
- Failed runs are stored only when the request reaches the valuation controller and fails during calculation.

## Report Snapshot History V1

Report History records saved workstation views without changing any business calculation:

- `GET /api/report-snapshots`
- `GET /api/report-snapshots/{snapshotId}`

Filters:

- `reportType=FO_PNL_SNAPSHOT|BO_OPERATIONS|BO_LIFECYCLE`
- `limit=50`

Snapshots are created when users open:

- `GET /api/front-office/reports/desk-pnl`
- `GET /api/back-office/reports/operations`
- `GET /api/back-office/lifecycle-report`

Stored fields include report type, title, business date, scope, requesting user/group, filters JSON, full rendered result JSON, compact summary JSON and timestamp.

Important boundaries:

- Report History is a saved view/audit aid, not a source for future pricing, EOD or P&L.
- EOD snapshots remain the official close reference for Daily P&L.
- Valuation Run History remains the audit trail for pricing, exposure and CVA calculations.
- FO sees only their own report snapshots; BO sees BO report snapshots; ADMIN can review all snapshots.

## Developer Financial Docs

Conceptual financial guides for developers live in:

- Spanish: [`../docs/docs-ES/ConceptosFinancieros.md`](../docs/docs-ES/ConceptosFinancieros.md)
- System logic ES: [`../docs/docs-ES/LogicaDelSistema.md`](../docs/docs-ES/LogicaDelSistema.md)
- Recent recap ES: [`../docs/docs-ES/RecapReciente.md`](../docs/docs-ES/RecapReciente.md)
- Data model ES: [`../docs/docs-ES/DataModel.md`](../docs/docs-ES/DataModel.md)
- Auth and groups ES: [`../docs/docs-ES/AuthYGrupos.md`](../docs/docs-ES/AuthYGrupos.md)
- Demo portfolios ES: [`../docs/docs-ES/PortafoliosDemo.md`](../docs/docs-ES/PortafoliosDemo.md)
- System logic EN: [`../docs/docs-EN/SystemLogic.md`](../docs/docs-EN/SystemLogic.md)
- Recent recap EN: [`../docs/docs-EN/RecentRecap.md`](../docs/docs-EN/RecentRecap.md)
- English: [`../docs/docs-EN/FinancialConcepts.md`](../docs/docs-EN/FinancialConcepts.md)
- Demo portfolios EN: [`../docs/docs-EN/DemoPortfolios.md`](../docs/docs-EN/DemoPortfolios.md)
- Blemberg needs: [`../docs/docs-EN/BlembergNeeds.md`](../docs/docs-EN/BlembergNeeds.md)
- Blemberg contract fixtures: [`../docs/docs-EN/BlembergContractFixtures.md`](../docs/docs-EN/BlembergContractFixtures.md)
- Blemberg build spec: [`../docs/docs-EN/BlembergBuildSpec.md`](../docs/docs-EN/BlembergBuildSpec.md)
- Blemberg runtime guide: [`../docs/docs-EN/how-it-works-blemberg.md`](../docs/docs-EN/how-it-works-blemberg.md)
- NexusXVA/Blemberg integration guide: [`../docs/docs-EN/nexusxva-integration-blemberg.md`](../docs/docs-EN/nexusxva-integration-blemberg.md)
- Exposure V1 plan: [`../docs/docs-EN/ExposureV1Plan.md`](../docs/docs-EN/ExposureV1Plan.md)

## Portfolio Management

Portfolio management persists portfolios, confirmed European option positions, and confirmed cash equity positions in PostgreSQL. FO submissions remain separate until BO reviews them.

Endpoints:

```text
POST /api/portfolios
GET /api/portfolios
GET /api/portfolios/{portfolioId}
PATCH /api/portfolios/{portfolioId}
DELETE /api/portfolios/{portfolioId}
GET /api/portfolios/{portfolioId}/instruments
GET /api/portfolios/{portfolioId}/instruments/{positionId}
POST /api/portfolios/{portfolioId}/trade-bookings/european-options
POST /api/portfolios/{portfolioId}/trade-bookings/cash-equities
GET /api/trade-bookings/mine
GET /api/back-office/trade-bookings
GET /api/back-office/trade-bookings/{bookingId}
POST /api/back-office/trade-bookings/{bookingId}/approve
POST /api/back-office/trade-bookings/{bookingId}/reject
POST /api/portfolios/{portfolioId}/pricing/black-scholes
POST /api/simulations/exposure
POST /api/risk/cva
```

Create portfolio request:

```json
{
  "name": "Demo Portfolio",
  "description": "Equity option demo book",
  "baseCurrency": "USD"
}
```

Create portfolio response:

```json
{
  "id": "6f2d4637-ef84-4bc5-bca3-7c7aee54b4e5",
  "name": "Demo Portfolio",
  "description": "Equity option demo book",
  "baseCurrency": "USD",
  "createdAt": "2026-06-01T01:54:39.212Z",
  "updatedAt": "2026-06-01T01:54:39.212Z",
  "positions": []
}
```

List portfolios returns summaries with `positionCount`:

```json
[
  {
    "id": "6f2d4637-ef84-4bc5-bca3-7c7aee54b4e5",
    "name": "Demo Portfolio",
    "description": "Equity option demo book",
    "baseCurrency": "USD",
    "createdAt": "2026-06-01T01:54:39.212Z",
    "updatedAt": "2026-06-01T01:54:39.212Z",
    "positionCount": 1
  }
]
```

Submit European option booking request:

```json
{
  "underlyingSymbol": "AAPL",
  "optionType": "CALL",
  "strike": 100.0,
  "maturityDate": "2027-12-31",
  "quantity": 10.0
}
```

The response starts with `status: PENDING_VALIDATION`. It does not appear in portfolio positions until BO approves it.

Booking states:

- `PENDING_VALIDATION`: waiting for BO.
- `CONFIRMED`: approval created one confirmed position.
- `REJECTED`: no position was created; `rejectionReason` explains why.

Confirmed position fields:

- `underlyingSymbol`: normalized to uppercase.
- `optionType`: `CALL` or `PUT`.
- `strike`: must be greater than zero.
- `maturityDate`: option maturity date.
- `quantity`: can be positive or negative, but not zero.
- `lifecycleStatus`: `ACTIVE`, `CANCELLED`, or `AMENDED`.
- `createdAt` and `updatedAt`: position lifecycle timestamps.

Confirmed positions cannot be edited directly by FO. Amendments and cancellations use controlled lifecycle requests:

```text
POST /api/front-office/lifecycle/positions/{positionId}/amend
POST /api/front-office/lifecycle/positions/{positionId}/cancel
GET /api/front-office/lifecycle/mine
GET /api/front-office/lifecycle/report
GET /api/back-office/lifecycle-requests
GET /api/back-office/lifecycle-report
POST /api/back-office/lifecycle-requests/{requestId}/approve
POST /api/back-office/lifecycle-requests/{requestId}/reject
```

Lifecycle Reporting is read-only. It derives queue metrics from `trade_lifecycle_requests`: pending aging, amendments vs cancellations, average review time, and top portfolios/symbols by lifecycle activity. It does not create a second counter table.

Operations Reporting is also read-only and belongs to BO:

```text
GET /api/back-office/reports/operations
```

It derives pending booking count, pending lifecycle count, missing-today EOD portfolios, and corrected EOD runs from existing tables. No extra reporting tables or counters are created.

### Trade Economics And P&L V1

New European-option bookings may include an optional `executionPrice`, representing the premium negotiated per unit. Cash-equity bookings may include an optional `executionPrice`, representing the share execution price. It is copied into the confirmed position when BO approves the booking. Legacy positions remain valid with `executionPrice = null`.

Portfolio pricing reports:

```text
tradeValue = executionPrice * quantity
marketValue = BlackScholesUnitPrice * quantity
unrealizedPnl = marketValue - tradeValue
```

If execution price is missing, position P&L is returned as `null` and `positionsWithoutExecutionPrice` is incremented. Strike and underlying spot are never treated as the option premium.

### Cash Equity Lots, Average Cost And Realized P&L V1

Cash equity positions now keep execution lots in `cash_equity_lots`.

Current rules:

- BO approval of a cash equity booking creates an `OPENING` lot when `executionPrice` is present.
- Existing cash equity positions with execution price are backfilled with one `OPENING` lot.
- Portfolio pricing reports `averageCost`, `costBasis`, `unrealizedPnl` and `realizedPnl` for cash equity rows.
- `averageCost` is derived from opening lots and falls back to the position execution price for legacy rows.
- `costBasis = averageCost * quantity`.
- `unrealizedPnl = marketValue - costBasis`.
- A cash-equity amendment that reduces an active position and provides an execution price records an `AMENDMENT_CLOSE` lot on the original position and calculates realized P&L.
- Operational cancellation without an exit execution price does not invent realized P&L.

This is not a full accounting ledger yet. V1 keeps the current aggregate cash equity position model for pricing and delta hedge, while lots provide cost basis and first realized P&L history. Full buy/sell lots, average-cost netting across multiple executions, realized P&L on sells, and partial cancellations are later accounting slices.

### EOD Snapshots And Daily P&L

EOD closes are audited snapshots; they never overwrite trade economics or live positions.

- `GET /api/portfolios/{portfolioId}/eod/latest`
- `GET /api/portfolios/{portfolioId}/eod?limit=10`
- `POST /api/portfolios/{portfolioId}/eod/pnl`
- `POST /api/back-office/eod/run` for a global manual BO close
- `POST /api/back-office/eod/portfolios/{portfolioId}` for targeted operational recovery
- `GET /api/back-office/eod/portfolios/{portfolioId}` for BO history
- `POST /api/back-office/eod/runs/{runId}/void` to void an active close with a reason
- `POST /api/back-office/eod/runs/{runId}/recapture` to supersede and recapture the same portfolio/date

Existing positions use prior EOD market value as the Daily P&L reference. Positions created after the close use execution trade value. Missing references remain unavailable.

The normal BO and scheduler process closes all portfolios. Each portfolio runs independently and is reported as `CAPTURED`, `SKIPPED`, or `FAILED`; a failed portfolio does not roll back successful closes from other books.

Scheduled EOD is configured in ADMIN -> Operational Control, not by a fixed cron. The scheduler checks the database every minute and runs once per business date after the configured EOD time. Defaults are New York business days, `09:30` to `16:00` trading, and `17:15` EOD disabled. If Close Checklist is enabled, scheduled close executes the configured checklist; otherwise it keeps the previous plain EOD behavior.

Close Checklist endpoints:

- `POST /api/back-office/close-checklist/runs`
- `GET /api/back-office/close-checklist/runs`
- `GET /api/back-office/close-checklist/runs/{runId}`

Checklist runs are stored in `close_checklist_runs` and `close_checklist_run_steps`. Report steps create `report_snapshots`; pricing/exposure/CVA steps create `valuation_runs`; the EOD step creates normal EOD snapshots.

ExecuteScript is not scheduled and does not replace Close Checklist. It is used by BO to test close inputs, reporting and risk steps before running the formal close. `EOD_VALIDATE` checks EOD readiness without creating `portfolio_eod_runs`; `EOD_CAPTURE` creates EOD snapshots only in `REAL_RUN`.

The scheduler bean is controlled by `NEXUSXVA_EOD_SCHEDULER_ENABLED=true` and the wake-up interval by `NEXUSXVA_EOD_SCHEDULER_TICK=60000`.

The close rejects stale market data and active unpriceable positions. One `ACTIVE` portfolio/date close can exist at a time. Corrections never delete old runs: `VOIDED` and `SUPERSEDED` snapshots remain visible in history, while latest close and Daily P&L use only `ACTIVE` runs.

Approval of `CANCEL` marks the original position `CANCELLED`. Approval of `AMEND` marks the original position `AMENDED` and creates a replacement `ACTIVE` position. Pricing, exposure, CVA, pre-trade analysis and stress testing use only `ACTIVE` positions.

Portfolio removal is logical archive, not hard delete. Archived portfolios disappear from operational workflows, but reviewed booking history and EOD snapshots remain available for audit. Archive returns `409 Conflict` while pending bookings exist.

Portfolio metadata fields:

- `name`: required.
- `description`: optional, up to 500 characters.
- `baseCurrency`: three-letter currency code, normalized to uppercase, default `USD`.
- `updatedAt`: changes when portfolio metadata changes.

Portfolio positions store trade terms only. Market data inputs such as `spot`, `riskFreeRate`, `volatility`, and `dividendYield` are intentionally not persisted in portfolio positions; they belong to pricing or market-data workflows.

### Portfolio Black-Scholes Pricing

Portfolio pricing is synchronous and calculation-owned: it reads persisted positions, requests pricing inputs through the `marketdata` boundary, reuses the Black-Scholes calculator, and returns per-position plus portfolio-level totals. NexusXVA also stores an audit copy in `valuation_runs`; future calculations do not read prior pricing runs as source data.

Endpoint:

```text
POST /api/portfolios/{portfolioId}/pricing/black-scholes
```

Request:

```json
{
  "valuationDate": "2026-06-01"
}
```

If `valuationDate` is omitted, the backend uses the current UTC date.

Response shape:

```json
{
  "portfolioId": "6f2d4637-ef84-4bc5-bca3-7c7aee54b4e5",
  "valuationDate": "2026-06-01",
  "model": "BLACK_SCHOLES",
  "baseCurrency": "USD",
  "totalPrice": 1234.56,
  "totalGreeks": {
    "delta": 12.3,
    "gamma": 0.45,
    "vega": 89.1,
    "theta": -7.2,
    "rho": 14.8
  },
  "positions": [],
  "unpriceablePositions": []
}
```

V1 pricing rules:

- Results are reported in the portfolio `baseCurrency`.
- Market-data pricing inputs may arrive in another currency; NexusXVA converts monetary values through the `marketdata` FX boundary.
- The local FX provider is a deterministic development fallback for `USD`, `EUR`, `GBP`, `CAD`, `MXN`, and `JPY`.
- `delta` remains an underlying-unit Greek. Monetary Greeks (`gamma`, `vega`, `theta`, `rho`) are converted to the portfolio base currency.
- `quantity` scales price and Greeks, so negative quantities represent short exposure.
- Positions with `maturityDate <= valuationDate` are returned in `unpriceablePositions` with `UNPRICEABLE_EXPIRED` and are excluded from totals.
- Missing market-data pricing inputs return `400 Bad Request`.
- Missing FX rates return `400 Bad Request`.
- Blemberg outages return `503 Service Unavailable`.

FX V1 is deterministic and synchronous. It is suitable for portfolio totals and XVA demo flows, but it is not yet a full FX risk model: no stochastic FX paths, FX Greeks, FX options, persisted FX curves, or collateral FX haircuts are implemented.

### Blemberg Market Data Validation

NexusXVA can validate portfolio position symbols against Blemberg, the separate market-data service responsible for reference data and pricing inputs.

Configuration:

```yaml
nexusxva:
  market-data:
    provider: blemberg
    validation:
      enabled: false
    blemberg:
      base-url: http://localhost:8081
      timeout: 2s
      refresh-timeout: 60s
```

Environment overrides:

```bash
NEXUSXVA_MARKET_DATA_PROVIDER=blemberg
NEXUSXVA_MARKET_DATA_VALIDATION_ENABLED=true
BLEMBERG_BASE_URL=http://localhost:8081
BLEMBERG_TIMEOUT=2s
BLEMBERG_REFRESH_TIMEOUT=60s
```

`BLEMBERG_TIMEOUT` is intentionally short for health checks, symbol validation and pricing-input reads. The dashboard refresh button uses `BLEMBERG_REFRESH_TIMEOUT` because `POST /api/admin/market-data/refresh` can legitimately take several seconds while Blemberg updates cached provider data and handles rate limits.

Before using Blemberg locally, start that service, trigger its refresh, and verify health:

```bash
curl http://localhost:8081/actuator/health
curl -X POST http://localhost:8081/api/admin/market-data/refresh
curl http://localhost:8081/api/admin/market-data/refresh-runs
curl "http://localhost:8081/api/market-data/snapshots?symbols=AAPL,SPY,QQQ,MSFT"
curl "http://localhost:8081/api/market-data/pricing-inputs/european-option?symbol=AAPL&maturityDate=2027-06-01"
```

When NexusXVA runs from the provided Docker Compose file, `BLEMBERG_BASE_URL` defaults to `http://host.docker.internal:8081`. If both services run inside the same Docker network, override it with the Blemberg service name, for example `http://blemberg:8081`.

Local validation and pricing-input mock without Blemberg:

```bash
NEXUSXVA_MARKET_DATA_PROVIDER=local
NEXUSXVA_MARKET_DATA_VALIDATION_ENABLED=true
```

The local provider uses a fixed development watchlist for symbols such as `AAPL`, `MSFT`, `NVDA`, `JPM`, `SPY`, `QQQ`, `GLD`, and `SLV`. It validates symbols and provides temporary demo `spot`, `volatility`, `riskFreeRate`, and `dividendYield` values for portfolio pricing. These values are not persisted and must not be treated as real market data.

When validation is enabled:

- creating a European option position validates `underlyingSymbol` with `GET /api/instruments/{symbol}` on Blemberg.
- updating a position validates only when `underlyingSymbol` changes.
- portfolio pricing requests `GET /api/market-data/pricing-inputs/european-option?symbol={symbol}&maturityDate={yyyy-mm-dd}` on Blemberg.
- unknown or inactive instruments return `400 Bad Request` with `Unknown underlyingSymbol`.
- unavailable Blemberg calls return `503 Service Unavailable` with `Market data service unavailable`.

NexusXVA still persists only the symbol and trade terms. Blemberg owns instrument reference data and real pricing inputs such as spot, historical volatility, risk-free rates, and dividend yields.

Blemberg snapshot responses are diagnostic only for NexusXVA. In Blemberg V1, `GET /api/market-data/snapshots` returns:

```json
{
  "snapshots": [],
  "missingSymbols": []
}
```

Portfolio pricing and Exposure V1 do not consume raw snapshots; they consume the pricing-input endpoint. Blemberg V1 may also return `501 Not Implemented` for `GET /v3/api-docs`; NexusXVA does not depend on OpenAPI for runtime integration.

Optional real Blemberg smoke test:

```bash
RUN_REAL_BLEMBERG_SMOKE=true BLEMBERG_BASE_URL=http://localhost:8081 \
  mvn test -Dtest=BlembergRealSmokeTest
```

The real smoke test is skipped in normal `mvn test` runs unless `RUN_REAL_BLEMBERG_SMOKE=true` is set.

### Exposure Simulation V1

Exposure V1 is synchronous: it loads a persisted portfolio, requests market-data pricing inputs through the `marketdata` boundary, simulates future spot paths with a simple GBM model, reprices live European option positions with Black-Scholes across the time grid, and aggregates exposure measures. NexusXVA stores an audit copy in `valuation_runs`; it does not persist paths as reusable market/risk state.

Endpoint:

```text
POST /api/simulations/exposure
```

Request:

```json
{
  "portfolioId": "6f2d4637-ef84-4bc5-bca3-7c7aee54b4e5",
  "valuationDate": "2026-06-05",
  "horizonDays": 365,
  "timeSteps": 12,
  "paths": 1000,
  "seed": 12345,
  "pfeConfidenceLevel": 0.95
}
```

Response shape:

```json
{
  "portfolioId": "6f2d4637-ef84-4bc5-bca3-7c7aee54b4e5",
  "valuationDate": "2026-06-05",
  "model": "GBM_BLACK_SCHOLES_EXPOSURE_V1",
  "paths": 1000,
  "timeSteps": 12,
  "pfeConfidenceLevel": 0.95,
  "points": [
    {
      "date": "2026-07-05",
      "expectedExposure": 123.45,
      "expectedNegativeExposure": 12.34,
      "pfe": 456.78
    }
  ]
}
```

V1 simulation rules:

- Supports persisted European option portfolios only.
- Reports exposure values in the portfolio `baseCurrency`.
- Converts market-data currencies through the `marketdata` FX boundary using valuation-time FX rates.
- Uses `spot`, `volatility`, `riskFreeRate`, and `dividendYield` from `marketdata`.
- Uses deterministic seeds so tests and dev runs are repeatable.
- Excludes positions once `maturityDate <= futureDate`.
- Empty portfolios or all-expired portfolios return zero exposure points.
- Does not persist market data or simulated paths as reusable state; the API request/response is copied to valuation run history for audit.

### Simplified CVA V1.2

CVA V1.2 is synchronous: it reuses Exposure V1, then applies a simple credit valuation adjustment formula over expected exposure by date. NexusXVA stores an audit copy in `valuation_runs`. ADMIN can now maintain reusable credit and discount curve master data in the XVA module, while CVA result state remains valuation-run audit history rather than operational state.

Endpoint:

```text
POST /api/risk/cva
```

Request:

```json
{
  "portfolioId": "6f2d4637-ef84-4bc5-bca3-7c7aee54b4e5",
  "valuationDate": "2026-06-05",
  "horizonDays": 365,
  "timeSteps": 12,
  "paths": 1000,
  "seed": 12345,
  "pfeConfidenceLevel": 0.95,
  "lossGivenDefault": 0.6,
  "counterpartyHazardRate": 0.02,
  "discountRate": 0.05
}
```

Curve-mode request:

```json
{
  "portfolioId": "6f2d4637-ef84-4bc5-bca3-7c7aee54b4e5",
  "valuationDate": "2026-06-05",
  "horizonDays": 365,
  "timeSteps": 12,
  "paths": 1000,
  "seed": 12345,
  "pfeConfidenceLevel": 0.95,
  "lossGivenDefault": 0.6,
  "creditCurve": [
    { "date": "2026-07-05", "survivalProbability": 0.995 },
    { "date": "2026-12-05", "survivalProbability": 0.975 }
  ],
  "discountCurve": [
    { "date": "2026-07-05", "discountFactor": 0.996 },
    { "date": "2026-12-05", "discountFactor": 0.980 }
  ]
}
```

Persisted-curve request:

```json
{
  "portfolioId": "6f2d4637-ef84-4bc5-bca3-7c7aee54b4e5",
  "valuationDate": "2026-06-05",
  "horizonDays": 365,
  "timeSteps": 12,
  "paths": 1000,
  "seed": 12345,
  "pfeConfidenceLevel": 0.95,
  "lossGivenDefault": 0.6,
  "creditCurveId": "2c2db24a-c1ea-4f3e-a094-9c34de5b89f3",
  "discountCurveId": "1d33b5b2-2f52-4b63-b59a-27edc2a44c55"
}
```

Response shape:

```json
{
  "portfolioId": "6f2d4637-ef84-4bc5-bca3-7c7aee54b4e5",
  "valuationDate": "2026-06-05",
  "model": "SIMPLIFIED_CVA_V1",
  "exposureModel": "GBM_BLACK_SCHOLES_EXPOSURE_V1",
  "paths": 1000,
  "timeSteps": 12,
  "pfeConfidenceLevel": 0.95,
  "lossGivenDefault": 0.6,
  "counterpartyHazardRate": 0.02,
  "discountRate": 0.05,
  "creditMethod": "FLAT_HAZARD_RATE",
  "discountMethod": "FLAT_RATE",
  "cva": 12.34,
  "points": [
    {
      "date": "2026-07-05",
      "expectedExposure": 123.45,
      "discountFactor": 0.9959,
      "survivalProbability": 0.9984,
      "defaultProbabilityIncrement": 0.0016,
      "discountedExpectedExposure": 122.94,
      "cvaContribution": 0.12
    }
  ]
}
```

V1 CVA rules:

- Formula: `CVA = LGD * sum(discountFactor_i * expectedExposure_i * defaultProbabilityIncrement_i)`.
- If `creditCurveId` is provided, CVA loads an active persisted credit curve; if `creditCurve` is provided, CVA uses inline curve points; otherwise it uses `counterpartyHazardRate`.
- If `discountCurveId` is provided, CVA loads an active persisted discount curve; if `discountCurve` is provided, CVA uses inline curve points; otherwise it uses `discountRate`.
- A request must not provide both `creditCurveId` and inline `creditCurve`, or both `discountCurveId` and inline `discountCurve`.
- `creditCurve` points provide exactly one of `survivalProbability` or `cumulativeDefaultProbability`.
- Missing exposure dates inside a curve range use simple linear interpolation.
- Exposure dates outside a provided curve range return `400 Bad Request`.
- `lossGivenDefault` must be between `0.0` and `1.0`.
- CVA reuses Exposure V1 and reports values in the portfolio `baseCurrency`.
- European-options-only limitations still apply for the simulation leg.
- Single-portfolio CVA and netting-set CVA both write valuation run audit snapshots. Netting-set runs are stored with `scopeType=NETTING_SET`.
- Wrong-way risk, path-level netting, margining, CSA rules, dynamic collateral and persisted CVA result state are still out of scope.

### Counterparties, Netting Sets And Collateral V1

NexusXVA now has a first XVA reference-data slice:

- `GET /api/xva/counterparties`
- `POST /api/xva/counterparties`
- `PATCH /api/xva/counterparties/{counterpartyId}`
- `GET /api/xva/netting-sets`
- `POST /api/xva/netting-sets`
- `PATCH /api/xva/netting-sets/{nettingSetId}`
- `POST /api/xva/netting-sets/{nettingSetId}/portfolios`
- `DELETE /api/xva/netting-sets/{nettingSetId}/portfolios/{portfolioId}`
- `PATCH /api/xva/netting-sets/{nettingSetId}/collateral`
- `GET /api/xva/credit-curves`
- `POST /api/xva/credit-curves`
- `POST /api/xva/credit-curves/imports`
- `POST /api/xva/credit-curves/imports/market-data`
- `PATCH /api/xva/credit-curves/{curveId}`
- `POST /api/xva/credit-curves/{curveId}/approve`
- `POST /api/xva/credit-curves/{curveId}/reject`
- `GET /api/xva/discount-curves`
- `POST /api/xva/discount-curves`
- `PATCH /api/xva/discount-curves/{curveId}`
- `POST /api/xva/discount-curves/{curveId}/approve`
- `POST /api/xva/discount-curves/{curveId}/reject`

Mutating setup endpoints require an `ADMIN` active group when auth is enabled. Read endpoints default to active/operable records only; ADMIN can pass `includeInactive=true` for setup screens. FO CVA selection consumes only active counterparties, active netting sets, active approved credit curves and active approved discount curves.

Curve lifecycle V1:

- Creating a credit or discount curve creates a new `DRAFT` version.
- Draft curves can be edited, approved or rejected.
- Approving a draft makes it `APPROVED`, `active=true`, and supersedes the previous active approved version for the same credit curve name/counterparty or discount curve name/currency.
- Rejected and superseded curves remain in history and are not usable by CVA.
- Approved, rejected and superseded curves are immutable; create a new version to change curve points.
- `source` supports `MANUAL`, `IMPORT` and `MARKET_DATA`. CSV imports and Blemberg-sourced discount/credit curves create inactive drafts that require explicit ADMIN approval.
- Market credit imports use the counterparty rating and Blemberg's USD investment-grade OAS proxy. The stored lineage includes rating bucket, recovery assumption, spread, hazard proxy, observation date, source series, construction method and stale flag.

Admin setup examples:

```text
GET /api/xva/counterparties?includeInactive=true
GET /api/xva/netting-sets?includeInactive=true
```

```json
PATCH /api/xva/counterparties/{counterpartyId}
{
  "name": "Example Bank",
  "externalId": "EXBANK",
  "creditRating": "A",
  "active": true
}
```

```json
PATCH /api/xva/netting-sets/{nettingSetId}
{
  "name": "Example Bank USD CSA",
  "active": false
}
```

Netting-set CVA endpoint:

```text
POST /api/risk/cva/netting-set
```

Example request:

```json
{
  "nettingSetId": "uuid",
  "valuationDate": "2026-06-30",
  "horizonDays": 365,
  "timeSteps": 12,
  "paths": 1000,
  "seed": 12345,
  "pfeConfidenceLevel": 0.95,
  "lossGivenDefault": 0.6,
  "counterpartyHazardRate": 0.02,
  "discountRate": 0.04
}
```

V1 rules:

- A counterparty can own multiple netting sets.
- Inactive counterparties and inactive netting sets are retained for history but blocked for new netting-set CVA runs and new portfolio assignments.
- A portfolio can be assigned to only one netting set.
- Netting set and portfolio currencies must match.
- Netting-set CVA V1 supports one base currency per netting set and requires collateral currency to match that base currency.
- `collateralAmount` is a static amount in the netting set currency.
- Netting-set CVA aggregates portfolio exposure profiles, subtracts static collateral from positive expected exposure/PFE by bucket, then applies the same CVA calculator.
- This is profile-level netting, not path-level legal netting or CSA margining.

Implementation shape:

- `portfolio.domain` contains portfolio and position invariants.
- `portfolio.application` owns use cases and transaction boundaries.
- `portfolio.infrastructure` contains JPA entities and repositories.
- `portfolio.api` owns REST DTOs and the controller.
- `marketdata.application` owns the internal ports used to validate symbols and request pricing inputs.
- `marketdata.infrastructure` owns the Blemberg REST adapters and temporary local watchlist/pricing-input adapter.
- `exposure.application` owns exposure simulation orchestration.
- `cva.domain` owns the simplified CVA formula.
- `cva.application` coordinates exposure simulation and CVA calculation.
- `cva.api` owns the CVA REST contract.

## European Option Pricing

Endpoint:

```text
POST /api/pricing/european-options/black-scholes
```

Request:

```json
{
  "optionType": "CALL",
  "spot": 100.0,
  "strike": 100.0,
  "timeToMaturityYears": 1.0,
  "riskFreeRate": 0.05,
  "volatility": 0.2,
  "dividendYield": 0.0
}
```

Response:

```json
{
  "model": "BLACK_SCHOLES",
  "optionType": "CALL",
  "price": 10.4506,
  "greeks": {
    "delta": 0.6368,
    "gamma": 0.0188,
    "vega": 37.524,
    "theta": -6.414,
    "rho": 53.2325
  }
}
```

Pricing assumptions:

- Supports European `CALL` and `PUT` options.
- Uses the Black-Scholes closed-form model.
- Uses constant volatility and a constant continuously compounded risk-free rate.
- Supports optional continuous dividend yield; omitted `dividendYield` defaults to `0.0`.
- Rates and volatility are decimals: `0.05` means 5%.
- `spot`, `strike`, `timeToMaturityYears`, and `volatility` must be greater than zero.
- `riskFreeRate` may be negative, but it must be finite.
- `dividendYield` must be greater than or equal to zero.
- Exact expiry payoff at `timeToMaturityYears = 0` is out of scope for this endpoint because several Greeks are discontinuous or undefined at expiry.
- Greeks are reported per unit change: vega is per `1.0` volatility change, not per one volatility point.

Implementation shape:

- `pricing.domain` contains pure Java financial objects and formulas.
- `pricing.application` owns the pricing use case.
- `pricing.api` owns REST DTOs and the controller.
- Controllers stay thin; financial logic stays outside Spring MVC.

## Package Structure

```text
com.nexusxva
  shared
    api
    error
    validation
  instruments
    api
    application
    domain
    infrastructure
  marketdata
    api
    application
    domain
    infrastructure
  pricing
    api
    application
    domain
    infrastructure
  portfolio
    api
    application
    domain
    infrastructure
  simulation
    api
    application
    domain
    infrastructure
  exposure
    api
    application
    domain
    infrastructure
  cva
    api
    application
    domain
```

Controllers should stay thin, application services should own use-case orchestration, domain packages should contain financial concepts and invariants, and infrastructure packages should contain persistence or external adapters when those are introduced.
