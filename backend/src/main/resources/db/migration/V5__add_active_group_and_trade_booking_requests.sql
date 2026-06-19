ALTER TABLE auth_sessions
    ADD COLUMN active_group_code VARCHAR(32);

ALTER TABLE auth_sessions
    ADD CONSTRAINT fk_auth_sessions_active_group
        FOREIGN KEY (active_group_code) REFERENCES auth_groups (code);

CREATE TABLE trade_booking_requests (
    id UUID PRIMARY KEY,
    portfolio_id UUID REFERENCES portfolios (id) ON DELETE SET NULL,
    portfolio_name VARCHAR(120) NOT NULL,
    instrument_type VARCHAR(32) NOT NULL CHECK (instrument_type = 'EUROPEAN_OPTION'),
    underlying_symbol VARCHAR(32) NOT NULL,
    option_type VARCHAR(16) NOT NULL CHECK (option_type IN ('CALL', 'PUT')),
    strike NUMERIC(19, 8) NOT NULL CHECK (strike > 0),
    maturity_date DATE NOT NULL,
    quantity NUMERIC(19, 8) NOT NULL CHECK (quantity <> 0),
    status VARCHAR(32) NOT NULL CHECK (status IN ('PENDING_VALIDATION', 'CONFIRMED', 'REJECTED')),
    submitted_by_user_id UUID REFERENCES auth_user_accounts (id),
    submitted_by_username VARCHAR(120),
    submitted_by_display_name VARCHAR(160),
    submitted_at TIMESTAMPTZ NOT NULL,
    reviewed_by_user_id UUID REFERENCES auth_user_accounts (id),
    reviewed_by_username VARCHAR(120),
    reviewed_by_display_name VARCHAR(160),
    reviewed_at TIMESTAMPTZ,
    rejection_reason VARCHAR(500),
    confirmed_position_id UUID REFERENCES portfolio_european_option_positions (id) ON DELETE SET NULL,
    version BIGINT NOT NULL DEFAULT 0
);

CREATE INDEX idx_trade_booking_requests_status_submitted_at
    ON trade_booking_requests (status, submitted_at);

CREATE INDEX idx_trade_booking_requests_portfolio_id
    ON trade_booking_requests (portfolio_id);

CREATE INDEX idx_trade_booking_requests_submitted_by
    ON trade_booking_requests (submitted_by_user_id, submitted_at);

