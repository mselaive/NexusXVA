ALTER TABLE trade_booking_requests
    ADD COLUMN execution_price NUMERIC(19, 8);

ALTER TABLE trade_booking_requests
    ADD CONSTRAINT chk_trade_booking_execution_price
        CHECK (execution_price IS NULL OR execution_price >= 0);

ALTER TABLE portfolio_european_option_positions
    ADD COLUMN execution_price NUMERIC(19, 8);

ALTER TABLE portfolio_european_option_positions
    ADD CONSTRAINT chk_portfolio_position_execution_price
        CHECK (execution_price IS NULL OR execution_price >= 0);
