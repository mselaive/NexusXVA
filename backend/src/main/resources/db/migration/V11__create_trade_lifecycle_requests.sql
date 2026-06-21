ALTER TABLE portfolio_european_option_positions
    ADD COLUMN lifecycle_status VARCHAR(16) NOT NULL DEFAULT 'ACTIVE';

ALTER TABLE portfolio_european_option_positions
    ADD CONSTRAINT chk_portfolio_positions_lifecycle_status
        CHECK (lifecycle_status IN ('ACTIVE', 'CANCELLED', 'AMENDED'));

CREATE INDEX idx_portfolio_option_positions_active
    ON portfolio_european_option_positions (portfolio_id, lifecycle_status);

CREATE TABLE trade_lifecycle_requests (
    id UUID PRIMARY KEY,
    portfolio_id UUID REFERENCES portfolios (id) ON DELETE SET NULL,
    portfolio_name VARCHAR(120) NOT NULL,
    position_id UUID REFERENCES portfolio_european_option_positions (id) ON DELETE SET NULL,
    request_type VARCHAR(16) NOT NULL CHECK (request_type IN ('AMEND', 'CANCEL')),
    status VARCHAR(32) NOT NULL CHECK (status IN ('PENDING_VALIDATION', 'APPROVED', 'REJECTED')),
    original_underlying_symbol VARCHAR(32) NOT NULL,
    original_option_type VARCHAR(16) NOT NULL CHECK (original_option_type IN ('CALL', 'PUT')),
    original_strike NUMERIC(19, 8) NOT NULL CHECK (original_strike > 0),
    original_maturity_date DATE NOT NULL,
    original_quantity NUMERIC(19, 8) NOT NULL CHECK (original_quantity <> 0),
    requested_underlying_symbol VARCHAR(32),
    requested_option_type VARCHAR(16) CHECK (requested_option_type IN ('CALL', 'PUT')),
    requested_strike NUMERIC(19, 8) CHECK (requested_strike IS NULL OR requested_strike > 0),
    requested_maturity_date DATE,
    requested_quantity NUMERIC(19, 8) CHECK (requested_quantity IS NULL OR requested_quantity <> 0),
    submitted_by_user_id UUID REFERENCES auth_user_accounts (id),
    submitted_by_username VARCHAR(120),
    submitted_by_display_name VARCHAR(160),
    submitted_at TIMESTAMPTZ NOT NULL,
    reviewed_by_user_id UUID REFERENCES auth_user_accounts (id),
    reviewed_by_username VARCHAR(120),
    reviewed_by_display_name VARCHAR(160),
    reviewed_at TIMESTAMPTZ,
    rejection_reason VARCHAR(500),
    resulting_position_id UUID REFERENCES portfolio_european_option_positions (id) ON DELETE SET NULL,
    version BIGINT NOT NULL DEFAULT 0
);

CREATE UNIQUE INDEX ux_trade_lifecycle_requests_pending_position
    ON trade_lifecycle_requests (position_id)
    WHERE status = 'PENDING_VALIDATION';

CREATE INDEX idx_trade_lifecycle_requests_status_submitted_at
    ON trade_lifecycle_requests (status, submitted_at);

CREATE INDEX idx_trade_lifecycle_requests_submitted_by
    ON trade_lifecycle_requests (submitted_by_user_id, submitted_at);

INSERT INTO auth_feature_permissions (code, group_code, name, description)
VALUES (
    'FO_REQUEST_LIFECYCLE',
    'FO',
    'Request lifecycle changes',
    'Request amendments or cancellations for confirmed positions.'
)
ON CONFLICT (code) DO NOTHING;
