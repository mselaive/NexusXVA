# Trading Limits

Use these rules when changing preventive FO booking controls.

- One optional policy belongs to one active FO user.
- Missing or disabled policy means `UNLIMITED`; null measures are individually unlimited.
- V1 measures trades and `abs(quantity) * strike` USD notional per UTC calendar hour/day.
- Derive usage from `trade_booking_requests`; do not persist mutable usage counters.
- Every created booking consumes capacity even if BO later rejects it.
- A breached request returns `409` with sanitized metadata and creates no booking.
- Lock the policy and create the booking in one transaction to enforce concurrency safely.
- Active notional controls reject non-USD portfolios until FX exists.
- Do not present controlled notional as premium, cash, P&L, market value, or a full risk limit.
