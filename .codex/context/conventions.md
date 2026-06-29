# Development Conventions

## General

- Prefer simple, explicit designs.
- Keep changes scoped to the current feature.
- Document important tradeoffs.
- Do not introduce infrastructure before there is a concrete need.
- Treat financial simplifications as explicit assumptions.

## Java

- Use Java 21 or newer.
- Prefer immutable value objects for domain concepts where practical.
- Use `BigDecimal` for persisted monetary amounts when exact decimal representation matters.
- Use `double` for numerical analytics and Monte Carlo calculations when appropriate, with tests and documented tolerance.
- Keep pure pricing formulas framework-independent.
- Use meaningful domain names.

## Spring Boot

- Controllers should be thin.
- Application services own use-case orchestration.
- Domain services own financial behavior.
- Repositories should be infrastructure details.
- Request and response DTOs should be explicit.
- Validation should happen at API boundaries and in domain constructors/factories where needed.

## Testing

- Financial formulas require unit tests.
- Use deterministic seeds for Monte Carlo tests.
- Use Testcontainers for database integration tests.
- Avoid mocking pure domain objects.
- Prefer property-style tests for invariants.
- Include exact-value tests for known pricing examples.

## Persistence

- Keep schema understandable.
- Use explicit identifiers.
- Avoid premature inheritance hierarchies in database tables.
- Avoid storing core business concepts only as opaque JSON.
- Use migrations when the backend project is established.
- Persisted positions should store trade terms, not market data inputs such as spot, volatility, or rates, unless a feature explicitly changes that boundary.
- External instrument validation must go through the `marketdata` application boundary; portfolio code should not call Blemberg or other providers directly.
- Portfolio pricing inputs must also go through the `marketdata` application boundary; do not read provider data directly from portfolio code.
- Blemberg snapshots are diagnostic/cache data only in NexusXVA; pricing and exposure should consume Blemberg pricing inputs, not raw snapshots.
- Treat Blemberg V1 `GET /v3/api-docs` returning `501 Not Implemented` as expected; runtime integration must not depend on OpenAPI.
- Pricing, exposure, and CVA now write audit snapshots to `valuation_runs` when run through their main APIs. Future calculations must still recompute from portfolio state and `marketdata`; do not read old valuation runs as source data.
- CVA V1 should consume exposure profiles through `exposure.application`; do not duplicate simulation logic in `cva`.
- CVA credit and discount curves are request-scoped for now; do not persist counterparties, curves, or CVA runs until explicitly planned.
- CVA curve inputs remain request-scoped. `valuation_runs` may store the submitted request/response for audit, but it must not become counterparty, curve, or market-data master data.

## API Design

- Use stable URLs under `/api`.
- Prefer nouns for resources and explicit action endpoints for calculations.
- Validate inputs and return clear errors.
- Do not expose internal entity shapes directly.
- Do not expose framework-internal exception messages, class names, or stack details in API error responses.

## Authentication

- Store passwords only as strong one-way hashes such as BCrypt; never store or log raw passwords.
- Browser sessions should use opaque random tokens in `HttpOnly` cookies.
- Store only hashes of session tokens in the database.
- Keep user/group membership relational and explicit; do not encode core authorization state only inside opaque JSON.
- Auth groups include `FO`, `BO`, and `ADMIN`; a session persists one active group and backend authorization must enforce it.
- Mutating authenticated requests should carry CSRF protection when cookie sessions are used.
- Frontend route filtering is UX only and must never replace backend group authorization.
- ADMIN manages group memberships, FO feature permissions, and portfolio visibility; ADMIN workflow maps are read-only monitoring, not BO approval actions.
- FO feature permissions currently gate trade booking, portfolio creation, CVA execution, Pre-Trade Analysis, Stress Testing, and lifecycle requests. Keep adding explicit permission codes instead of burying new checks in UI-only logic.
- Portfolio visibility is enforced by the backend and must be checked by portfolio views, trade booking, pricing, exposure, and CVA.
- Trade booking requests are separate from confirmed positions. Pending or rejected bookings must never enter pricing, exposure, or CVA.
- Pre-Trade Analysis is stateless and must not create booking requests, confirmed positions, market data, or stored valuation runs. If FO wants to book after analysis, send the ticket terms to `u-Pad`.
- Stress Testing is stateless and pricing/Greeks-only in V1. It may include one hypothetical trade, but must not create bookings, confirmed positions, market data, exposure runs, CVA runs, or persisted stress results.
- Confirmed positions are changed only through lifecycle requests. Risk workflows must use only `ACTIVE` positions and treat `CANCELLED`/`AMENDED` as history.
- User notifications are persisted in NexusXVA and belong to the user, not the active group. Workflow events should notify the maker/checker users, but notifications must not replace backend workflow state.
- Option `executionPrice` means premium per unit. Never substitute strike, spot, notional, or theoretical price for missing execution economics; return P&L as unavailable.
- EOD snapshots are audited per portfolio/business date and must not overwrite trades or positions. Corrections use `VOIDED`/`SUPERSEDED` runs with reasons; never physically delete EOD history. Daily P&L and latest close use only `ACTIVE` EOD runs, falling back to execution value only for positions absent from that close.
- Manual EOD capture belongs to BO. FO may read EOD/Daily P&L, while scheduled EOD runs as a system process.
- The normal EOD operation is a global batch over active portfolios. Process books independently and report `CAPTURED`, `SKIPPED`, or `FAILED`; one failed book must not roll back successful closes for other portfolios.
- Portfolios should be archived, not hard-deleted. Archived portfolios must disappear from operational workflows while preserving booking, lifecycle, and EOD history.
- Trading-limit usage is derived from submitted booking history; do not maintain a second mutable counter.
- Trading-limit enforcement and booking creation must share a transaction and lock the policy row.
- V1 controlled notional is `abs(quantity) * strike` in USD. Never describe it as premium, cash spent, P&L, or market value.
- Rejected bookings still consume the submission period; only bookings blocked before creation consume nothing.

## Documentation

- Every major feature should have:
  - Planner specification.
  - API notes.
  - Test plan.
  - Relevant architecture decision if tradeoffs are important.
- When conceptual docs exist under `docs/`, link them from relevant README or feature docs.
- Keep docs aligned with explicit financial assumptions such as expiry behavior, rate conventions, and unsupported model inputs.
- Treat `docs/docs-ES/LogicaDelSistema.md` and `docs/docs-EN/SystemLogic.md` as the living system-logic narrative and update them after meaningful vertical slices.

## AI Agent Usage

- The Planner Agent designs first.
- The Reviewer Agent reviews design before implementation for non-trivial features.
- The Programmer Agent implements the approved spec.
- The Tester Agent validates correctness and coverage.
- The Optimizer Agent runs only after working code and tests exist.
- The Reviewer Agent gives final merge guidance.
