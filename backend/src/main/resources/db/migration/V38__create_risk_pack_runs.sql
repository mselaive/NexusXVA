CREATE TABLE risk_pack_runs (
    id UUID PRIMARY KEY,
    portfolio_id UUID NOT NULL REFERENCES portfolios(id),
    valuation_date DATE NOT NULL,
    status VARCHAR(20) NOT NULL,
    requested_by_user_id UUID REFERENCES auth_user_accounts(id) ON DELETE SET NULL,
    requested_by_username VARCHAR(120),
    requested_by_group VARCHAR(30),
    portfolio_updated_at TIMESTAMPTZ NOT NULL,
    market_data_as_of TIMESTAMPTZ,
    configuration_json JSONB NOT NULL,
    queued_at TIMESTAMPTZ NOT NULL,
    started_at TIMESTAMPTZ,
    completed_at TIMESTAMPTZ,
    error_message VARCHAR(500),
    CONSTRAINT ck_risk_pack_run_status CHECK (status IN ('QUEUED', 'RUNNING', 'SUCCESS', 'PARTIAL', 'FAILED'))
);

CREATE TABLE risk_pack_run_components (
    id UUID PRIMARY KEY,
    run_id UUID NOT NULL REFERENCES risk_pack_runs(id) ON DELETE CASCADE,
    component_type VARCHAR(20) NOT NULL,
    status VARCHAR(20) NOT NULL,
    started_at TIMESTAMPTZ,
    completed_at TIMESTAMPTZ,
    output_json JSONB,
    error_message VARCHAR(500),
    duration_ms BIGINT,
    CONSTRAINT uk_risk_pack_component UNIQUE (run_id, component_type),
    CONSTRAINT ck_risk_pack_component_type CHECK (component_type IN ('PRICING', 'STRESS', 'VAR', 'EXPOSURE', 'CVA')),
    CONSTRAINT ck_risk_pack_component_status CHECK (status IN ('PENDING', 'RUNNING', 'SUCCESS', 'FAILED', 'SKIPPED'))
);

CREATE UNIQUE INDEX uk_risk_pack_active_portfolio
    ON risk_pack_runs(portfolio_id)
    WHERE status IN ('QUEUED', 'RUNNING');
CREATE INDEX idx_risk_pack_portfolio_created ON risk_pack_runs(portfolio_id, queued_at DESC);
CREATE INDEX idx_risk_pack_component_run ON risk_pack_run_components(run_id);
