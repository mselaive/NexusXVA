CREATE TABLE execute_script_templates (
    id UUID PRIMARY KEY,
    name VARCHAR(120) NOT NULL,
    description VARCHAR(500),
    active BOOLEAN NOT NULL DEFAULT TRUE,
    default_parameters_json JSONB NOT NULL DEFAULT '{}'::jsonb,
    created_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL,
    updated_by_user_id UUID
);

CREATE TABLE execute_script_template_steps (
    id UUID PRIMARY KEY,
    template_id UUID NOT NULL REFERENCES execute_script_templates (id) ON DELETE CASCADE,
    step_type VARCHAR(40) NOT NULL,
    step_order INTEGER NOT NULL,
    critical BOOLEAN NOT NULL DEFAULT FALSE,
    enabled BOOLEAN NOT NULL DEFAULT TRUE,
    parameters_json JSONB NOT NULL DEFAULT '{}'::jsonb,
    CONSTRAINT chk_execute_script_template_step_type CHECK (step_type IN (
        'FO_PNL_REPORT',
        'BO_OPERATIONS_REPORT',
        'BO_LIFECYCLE_REPORT',
        'PORTFOLIO_PRICING',
        'EXPOSURE',
        'CVA',
        'EOD_VALIDATE',
        'EOD_CAPTURE'
    )),
    CONSTRAINT ux_execute_script_template_step_order UNIQUE (template_id, step_order)
);

CREATE TABLE execute_script_runs (
    id UUID PRIMARY KEY,
    template_id UUID REFERENCES execute_script_templates (id) ON DELETE SET NULL,
    template_name VARCHAR(120) NOT NULL,
    mode VARCHAR(20) NOT NULL,
    business_date DATE NOT NULL,
    status VARCHAR(20) NOT NULL,
    started_at TIMESTAMPTZ NOT NULL,
    completed_at TIMESTAMPTZ,
    requested_by_user_id UUID,
    message VARCHAR(1000),
    input_json JSONB NOT NULL DEFAULT '{}'::jsonb,
    CONSTRAINT chk_execute_script_run_mode CHECK (mode IN ('DRY_RUN', 'REAL_RUN')),
    CONSTRAINT chk_execute_script_run_status CHECK (status IN ('RUNNING', 'COMPLETED', 'FAILED', 'PARTIAL'))
);

CREATE TABLE execute_script_run_steps (
    id UUID PRIMARY KEY,
    run_id UUID NOT NULL REFERENCES execute_script_runs (id) ON DELETE CASCADE,
    step_type VARCHAR(40) NOT NULL,
    step_order INTEGER NOT NULL,
    critical BOOLEAN NOT NULL,
    status VARCHAR(20) NOT NULL,
    started_at TIMESTAMPTZ,
    completed_at TIMESTAMPTZ,
    message VARCHAR(1000),
    output_json JSONB NOT NULL DEFAULT '{}'::jsonb,
    CONSTRAINT chk_execute_script_run_step_type CHECK (step_type IN (
        'FO_PNL_REPORT',
        'BO_OPERATIONS_REPORT',
        'BO_LIFECYCLE_REPORT',
        'PORTFOLIO_PRICING',
        'EXPOSURE',
        'CVA',
        'EOD_VALIDATE',
        'EOD_CAPTURE'
    )),
    CONSTRAINT chk_execute_script_run_step_status CHECK (status IN ('PENDING', 'RUNNING', 'COMPLETED', 'FAILED', 'SKIPPED'))
);

CREATE INDEX idx_execute_script_templates_active
    ON execute_script_templates (active, updated_at DESC);

CREATE INDEX idx_execute_script_runs_started_at
    ON execute_script_runs (started_at DESC);

CREATE INDEX idx_execute_script_run_steps_run_order
    ON execute_script_run_steps (run_id, step_order);
