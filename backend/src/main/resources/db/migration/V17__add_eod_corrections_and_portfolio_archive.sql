ALTER TABLE portfolios
    ADD COLUMN archived_at TIMESTAMPTZ,
    ADD COLUMN archived_by_user_id UUID REFERENCES auth_user_accounts (id) ON DELETE SET NULL,
    ADD COLUMN archive_reason VARCHAR(500);

ALTER TABLE portfolio_eod_runs
    ADD COLUMN status VARCHAR(16) NOT NULL DEFAULT 'ACTIVE',
    ADD COLUMN voided_at TIMESTAMPTZ,
    ADD COLUMN voided_by_user_id UUID REFERENCES auth_user_accounts (id) ON DELETE SET NULL,
    ADD COLUMN void_reason VARCHAR(500),
    ADD COLUMN correction_of_run_id UUID REFERENCES portfolio_eod_runs (id) ON DELETE SET NULL;

ALTER TABLE portfolio_eod_runs
    ADD CONSTRAINT chk_portfolio_eod_runs_status
    CHECK (status IN ('ACTIVE', 'VOIDED', 'SUPERSEDED'));

ALTER TABLE portfolio_eod_runs
    DROP CONSTRAINT IF EXISTS portfolio_eod_runs_portfolio_id_business_date_key;

ALTER TABLE portfolio_eod_runs
    DROP CONSTRAINT IF EXISTS portfolio_eod_runs_portfolio_id_fkey;

ALTER TABLE portfolio_eod_runs
    ADD CONSTRAINT portfolio_eod_runs_portfolio_id_fkey
    FOREIGN KEY (portfolio_id) REFERENCES portfolios (id) ON DELETE RESTRICT;

CREATE UNIQUE INDEX ux_portfolio_eod_runs_active_portfolio_date
    ON portfolio_eod_runs (portfolio_id, business_date)
    WHERE status = 'ACTIVE';

CREATE INDEX idx_portfolios_archived_at
    ON portfolios (archived_at);
