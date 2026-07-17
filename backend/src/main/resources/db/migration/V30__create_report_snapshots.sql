CREATE TABLE report_snapshots (
    id UUID PRIMARY KEY,
    report_type VARCHAR(60) NOT NULL,
    title VARCHAR(180) NOT NULL,
    business_date DATE,
    scope_type VARCHAR(40) NOT NULL,
    scope_id UUID,
    scope_name_snapshot VARCHAR(255),
    requested_by_user_id UUID,
    requested_by_username VARCHAR(80),
    requested_by_display_name VARCHAR(120),
    active_group_code VARCHAR(20),
    filters_json JSONB NOT NULL,
    result_json JSONB NOT NULL,
    summary_json JSONB NOT NULL,
    created_at TIMESTAMPTZ NOT NULL,
    CONSTRAINT chk_report_snapshots_type CHECK (report_type IN ('FO_PNL_SNAPSHOT', 'BO_OPERATIONS', 'BO_LIFECYCLE')),
    CONSTRAINT chk_report_snapshots_scope CHECK (scope_type IN ('USER', 'BACK_OFFICE', 'PORTFOLIO', 'GLOBAL'))
);

CREATE INDEX idx_report_snapshots_type_created
    ON report_snapshots (report_type, created_at DESC);

CREATE INDEX idx_report_snapshots_requested_by_created
    ON report_snapshots (requested_by_user_id, created_at DESC);

CREATE INDEX idx_report_snapshots_scope_created
    ON report_snapshots (scope_type, scope_id, created_at DESC);
