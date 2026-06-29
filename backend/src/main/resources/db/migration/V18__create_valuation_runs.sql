CREATE TABLE valuation_runs (
    id UUID PRIMARY KEY,
    portfolio_id UUID NOT NULL,
    portfolio_name_snapshot VARCHAR(255),
    run_type VARCHAR(24) NOT NULL,
    model VARCHAR(100) NOT NULL,
    valuation_date DATE,
    status VARCHAR(16) NOT NULL,
    requested_by_user_id UUID REFERENCES auth_user_accounts (id) ON DELETE SET NULL,
    requested_by_username VARCHAR(120),
    requested_by_display_name VARCHAR(160),
    active_group_code VARCHAR(40),
    input_json JSONB NOT NULL,
    result_json JSONB,
    summary_json JSONB,
    error_message VARCHAR(1000),
    created_at TIMESTAMPTZ NOT NULL,
    CONSTRAINT chk_valuation_runs_type CHECK (run_type IN ('PRICING', 'EXPOSURE', 'CVA')),
    CONSTRAINT chk_valuation_runs_status CHECK (status IN ('SUCCESS', 'FAILED'))
);

CREATE INDEX idx_valuation_runs_portfolio_created
    ON valuation_runs (portfolio_id, created_at DESC);

CREATE INDEX idx_valuation_runs_type_created
    ON valuation_runs (run_type, created_at DESC);

CREATE INDEX idx_valuation_runs_requested_by_created
    ON valuation_runs (requested_by_user_id, created_at DESC);
