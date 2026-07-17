ALTER TABLE operational_control_settings
    ADD COLUMN close_checklist_enabled BOOLEAN NOT NULL DEFAULT FALSE,
    ADD COLUMN close_checklist_portfolio_ids JSONB NOT NULL DEFAULT '[]'::jsonb,
    ADD COLUMN close_checklist_steps JSONB NOT NULL DEFAULT '[
      {"phase":"PRE_EOD","stepType":"BO_OPERATIONS_REPORT","enabled":true,"critical":true,"order":10},
      {"phase":"PRE_EOD","stepType":"BO_LIFECYCLE_REPORT","enabled":true,"critical":false,"order":20},
      {"phase":"EOD","stepType":"EOD","enabled":true,"critical":true,"order":30},
      {"phase":"POST_EOD","stepType":"PORTFOLIO_PRICING","enabled":true,"critical":false,"order":40},
      {"phase":"POST_EOD","stepType":"EXPOSURE","enabled":false,"critical":false,"order":50},
      {"phase":"POST_EOD","stepType":"CVA","enabled":false,"critical":false,"order":60},
      {"phase":"POST_EOD","stepType":"FO_PNL_REPORT","enabled":true,"critical":false,"order":70}
    ]'::jsonb,
    ADD COLUMN close_checklist_risk_defaults JSONB NOT NULL DEFAULT '{
      "horizonDays":365,
      "timeSteps":12,
      "paths":1000,
      "seed":12345,
      "pfeConfidenceLevel":0.95,
      "lossGivenDefault":0.6,
      "creditCurveId":null,
      "discountCurveId":null
    }'::jsonb;

CREATE TABLE close_checklist_runs (
    id UUID PRIMARY KEY,
    business_date DATE NOT NULL,
    source VARCHAR(40) NOT NULL,
    status VARCHAR(20) NOT NULL,
    started_at TIMESTAMPTZ NOT NULL,
    completed_at TIMESTAMPTZ,
    requested_by_user_id UUID REFERENCES auth_user_accounts (id) ON DELETE SET NULL,
    message VARCHAR(1000),
    config_json JSONB NOT NULL,
    CONSTRAINT chk_close_checklist_run_status CHECK (status IN ('RUNNING', 'COMPLETED', 'FAILED', 'PARTIAL'))
);

CREATE TABLE close_checklist_run_steps (
    id UUID PRIMARY KEY,
    run_id UUID NOT NULL REFERENCES close_checklist_runs (id) ON DELETE CASCADE,
    phase VARCHAR(20) NOT NULL,
    step_type VARCHAR(40) NOT NULL,
    step_order INTEGER NOT NULL,
    critical BOOLEAN NOT NULL,
    status VARCHAR(20) NOT NULL,
    started_at TIMESTAMPTZ,
    completed_at TIMESTAMPTZ,
    message VARCHAR(1000),
    output_json JSONB NOT NULL DEFAULT '{}'::jsonb,
    CONSTRAINT chk_close_checklist_step_phase CHECK (phase IN ('PRE_EOD', 'EOD', 'POST_EOD')),
    CONSTRAINT chk_close_checklist_step_type CHECK (step_type IN ('FO_PNL_REPORT', 'BO_OPERATIONS_REPORT', 'BO_LIFECYCLE_REPORT', 'PORTFOLIO_PRICING', 'EXPOSURE', 'CVA', 'EOD')),
    CONSTRAINT chk_close_checklist_step_status CHECK (status IN ('PENDING', 'RUNNING', 'COMPLETED', 'FAILED', 'SKIPPED'))
);

CREATE UNIQUE INDEX ux_close_checklist_scheduled_business_date
    ON close_checklist_runs (business_date)
    WHERE source = 'SCHEDULED';

CREATE INDEX idx_close_checklist_runs_started_at
    ON close_checklist_runs (started_at DESC);

CREATE INDEX idx_close_checklist_run_steps_run_order
    ON close_checklist_run_steps (run_id, step_order);
