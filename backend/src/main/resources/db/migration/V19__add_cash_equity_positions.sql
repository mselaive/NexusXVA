CREATE TABLE portfolio_cash_equity_positions (
    id UUID PRIMARY KEY,
    portfolio_id UUID NOT NULL REFERENCES portfolios (id) ON DELETE CASCADE,
    underlying_symbol VARCHAR(32) NOT NULL,
    quantity NUMERIC(19, 8) NOT NULL CHECK (quantity <> 0),
    execution_price NUMERIC(19, 8) CHECK (execution_price IS NULL OR execution_price >= 0),
    lifecycle_status VARCHAR(16) NOT NULL DEFAULT 'ACTIVE' CHECK (lifecycle_status IN ('ACTIVE', 'CANCELLED', 'AMENDED')),
    created_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL
);

CREATE INDEX idx_cash_equity_positions_portfolio
    ON portfolio_cash_equity_positions (portfolio_id);

CREATE INDEX idx_cash_equity_positions_portfolio_symbol
    ON portfolio_cash_equity_positions (portfolio_id, underlying_symbol);

CREATE INDEX idx_cash_equity_positions_portfolio_status
    ON portfolio_cash_equity_positions (portfolio_id, lifecycle_status);

ALTER TABLE trade_booking_requests
    DROP CONSTRAINT IF EXISTS trade_booking_requests_instrument_type_check;

ALTER TABLE trade_booking_requests
    DROP CONSTRAINT IF EXISTS trade_booking_requests_option_type_check;

ALTER TABLE trade_booking_requests
    DROP CONSTRAINT IF EXISTS trade_booking_requests_strike_check;

ALTER TABLE trade_booking_requests
    DROP CONSTRAINT IF EXISTS chk_trade_booking_type;

ALTER TABLE trade_booking_requests
    DROP CONSTRAINT IF EXISTS chk_trade_booking_strategy;

ALTER TABLE trade_booking_requests
    ALTER COLUMN option_type DROP NOT NULL;

ALTER TABLE trade_booking_requests
    ALTER COLUMN strike DROP NOT NULL;

ALTER TABLE trade_booking_requests
    ALTER COLUMN maturity_date DROP NOT NULL;

ALTER TABLE trade_booking_requests
    ADD CONSTRAINT chk_trade_booking_instrument_type
        CHECK (instrument_type IN ('EUROPEAN_OPTION', 'CASH_EQUITY'));

ALTER TABLE trade_booking_requests
    ADD CONSTRAINT chk_trade_booking_type
        CHECK (booking_type IN ('SINGLE_OPTION', 'OPTION_STRATEGY', 'CASH_EQUITY'));

ALTER TABLE trade_booking_requests
    ADD CONSTRAINT chk_trade_booking_product_terms
        CHECK (
            (
                instrument_type = 'EUROPEAN_OPTION'
                AND booking_type IN ('SINGLE_OPTION', 'OPTION_STRATEGY')
                AND option_type IN ('CALL', 'PUT')
                AND strike > 0
                AND maturity_date IS NOT NULL
            )
            OR
            (
                instrument_type = 'CASH_EQUITY'
                AND booking_type = 'CASH_EQUITY'
                AND option_type IS NULL
                AND strike IS NULL
                AND maturity_date IS NULL
                AND strategy_id IS NULL
                AND strategy_type IS NULL
                AND strategy_name IS NULL
                AND strategy_legs_json IS NULL
            )
        );

ALTER TABLE trade_booking_requests
    ADD CONSTRAINT chk_trade_booking_strategy
        CHECK (
            (booking_type = 'SINGLE_OPTION' AND strategy_legs_json IS NULL)
            OR
            (booking_type = 'OPTION_STRATEGY' AND strategy_id IS NOT NULL AND strategy_type IS NOT NULL AND strategy_legs_json IS NOT NULL)
            OR
            (booking_type = 'CASH_EQUITY' AND strategy_legs_json IS NULL)
        );
