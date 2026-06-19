# Trade Validation

Use these rules when changing FO/BO booking workflows.

- FO submission creates `PENDING_VALIDATION`, never a portfolio position.
- BO approval creates exactly one confirmed position in the same transaction.
- BO rejection requires a reason and creates no position.
- Pricing, exposure, and CVA read confirmed positions only.
- A session has one backend-persisted active group; UI visibility is not authorization.
- Repeated or concurrent review attempts return `409`.
- Confirmed positions are immutable until amendment/cancellation workflows exist.
- Keep portfolio, maker, and reviewer snapshots in booking history for audit readability.
