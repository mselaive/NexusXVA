CREATE TABLE trading_limit_policies (
    user_id UUID PRIMARY KEY REFERENCES auth_user_accounts (id) ON DELETE CASCADE,
    max_trades_per_hour INTEGER CHECK (max_trades_per_hour > 0),
    max_trades_per_day INTEGER CHECK (max_trades_per_day > 0),
    max_notional_per_hour NUMERIC(19, 8) CHECK (max_notional_per_hour > 0),
    max_notional_per_day NUMERIC(19, 8) CHECK (max_notional_per_day > 0),
    notional_currency VARCHAR(3) NOT NULL DEFAULT 'USD' CHECK (notional_currency = 'USD'),
    active BOOLEAN NOT NULL,
    created_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL,
    updated_by_user_id UUID REFERENCES auth_user_accounts (id) ON DELETE SET NULL,
    updated_by_username VARCHAR(120),
    updated_by_display_name VARCHAR(160),
    version BIGINT NOT NULL DEFAULT 0,
    CHECK (
        max_trades_per_hour IS NULL
        OR max_trades_per_day IS NULL
        OR max_trades_per_day >= max_trades_per_hour
    ),
    CHECK (
        max_notional_per_hour IS NULL
        OR max_notional_per_day IS NULL
        OR max_notional_per_day >= max_notional_per_hour
    )
);

CREATE INDEX idx_trading_limit_policies_active
    ON trading_limit_policies (active);

CREATE INDEX idx_trade_booking_requests_user_submitted_at
    ON trade_booking_requests (submitted_by_user_id, submitted_at);
