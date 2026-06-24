ALTER TABLE trade_booking_requests
    ADD COLUMN booking_type VARCHAR(32) NOT NULL DEFAULT 'SINGLE_OPTION';

ALTER TABLE trade_booking_requests
    ADD CONSTRAINT chk_trade_booking_type
        CHECK (booking_type IN ('SINGLE_OPTION', 'OPTION_STRATEGY'));

ALTER TABLE trade_booking_requests
    ADD COLUMN strategy_id UUID;

ALTER TABLE trade_booking_requests
    ADD COLUMN strategy_type VARCHAR(32);

ALTER TABLE trade_booking_requests
    ADD COLUMN strategy_name VARCHAR(120);

ALTER TABLE trade_booking_requests
    ADD COLUMN strategy_legs_json TEXT;

ALTER TABLE trade_booking_requests
    ADD COLUMN confirmed_position_ids_json TEXT;

ALTER TABLE trade_booking_requests
    ADD COLUMN booking_notional NUMERIC(19, 8);

UPDATE trade_booking_requests
SET booking_notional = abs(quantity) * strike
WHERE booking_notional IS NULL;

ALTER TABLE trade_booking_requests
    ADD CONSTRAINT chk_trade_booking_strategy
        CHECK (
            (booking_type = 'SINGLE_OPTION' AND strategy_legs_json IS NULL)
            OR
            (booking_type = 'OPTION_STRATEGY' AND strategy_id IS NOT NULL AND strategy_type IS NOT NULL AND strategy_legs_json IS NOT NULL)
        );

ALTER TABLE trade_booking_requests
    ADD CONSTRAINT chk_trade_booking_notional
        CHECK (booking_notional IS NULL OR booking_notional >= 0);

CREATE INDEX idx_trade_booking_requests_strategy_id
    ON trade_booking_requests (strategy_id);

ALTER TABLE portfolio_european_option_positions
    ADD COLUMN strategy_id UUID;

ALTER TABLE portfolio_european_option_positions
    ADD COLUMN strategy_type VARCHAR(32);

ALTER TABLE portfolio_european_option_positions
    ADD COLUMN strategy_name VARCHAR(120);

ALTER TABLE portfolio_european_option_positions
    ADD COLUMN strategy_leg_index INTEGER;

CREATE INDEX idx_portfolio_option_positions_strategy
    ON portfolio_european_option_positions (portfolio_id, strategy_id);
