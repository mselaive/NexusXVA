ALTER TABLE valuation_runs
    ADD COLUMN scope_type VARCHAR(32),
    ADD COLUMN scope_id UUID,
    ADD COLUMN scope_name_snapshot VARCHAR(255);

UPDATE valuation_runs
SET scope_type = 'PORTFOLIO',
    scope_id = portfolio_id,
    scope_name_snapshot = portfolio_name_snapshot;

ALTER TABLE valuation_runs
    ALTER COLUMN portfolio_id DROP NOT NULL,
    ALTER COLUMN scope_type SET NOT NULL,
    ALTER COLUMN scope_id SET NOT NULL;

ALTER TABLE valuation_runs
    ADD CONSTRAINT chk_valuation_runs_scope_type CHECK (scope_type IN ('PORTFOLIO', 'NETTING_SET'));

CREATE INDEX idx_valuation_runs_scope_created
    ON valuation_runs (scope_type, scope_id, created_at DESC);
