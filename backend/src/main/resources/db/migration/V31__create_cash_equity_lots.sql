CREATE EXTENSION IF NOT EXISTS pgcrypto;

CREATE TABLE cash_equity_lots (
    id UUID PRIMARY KEY,
    position_id UUID NOT NULL REFERENCES portfolio_cash_equity_positions (id) ON DELETE CASCADE,
    lot_type VARCHAR(32) NOT NULL,
    quantity NUMERIC(19, 8) NOT NULL,
    execution_price NUMERIC(19, 8),
    average_cost NUMERIC(19, 8),
    realized_pnl NUMERIC(24, 8) NOT NULL DEFAULT 0,
    executed_at TIMESTAMPTZ NOT NULL,
    created_at TIMESTAMPTZ NOT NULL,
    CONSTRAINT chk_cash_equity_lots_type CHECK (lot_type IN ('OPENING', 'AMENDMENT_CLOSE', 'CANCELLATION')),
    CONSTRAINT chk_cash_equity_lots_quantity CHECK (quantity <> 0),
    CONSTRAINT chk_cash_equity_lots_execution_price CHECK (execution_price IS NULL OR execution_price >= 0),
    CONSTRAINT chk_cash_equity_lots_average_cost CHECK (average_cost IS NULL OR average_cost >= 0)
);

CREATE INDEX idx_cash_equity_lots_position
    ON cash_equity_lots (position_id, executed_at);

INSERT INTO cash_equity_lots (
    id, position_id, lot_type, quantity, execution_price, average_cost, realized_pnl, executed_at, created_at
)
SELECT
    gen_random_uuid(),
    id,
    'OPENING',
    quantity,
    execution_price,
    execution_price,
    0,
    created_at,
    created_at
FROM portfolio_cash_equity_positions
WHERE execution_price IS NOT NULL;
