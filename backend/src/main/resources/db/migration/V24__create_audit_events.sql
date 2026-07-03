CREATE TABLE audit_events (
    id UUID PRIMARY KEY,
    occurred_at TIMESTAMPTZ NOT NULL,
    event_type VARCHAR(80) NOT NULL,
    module VARCHAR(80) NOT NULL,
    action VARCHAR(120) NOT NULL,
    outcome VARCHAR(20) NOT NULL,
    actor_user_id UUID,
    username VARCHAR(120),
    display_name VARCHAR(200),
    active_group VARCHAR(30),
    session_id UUID,
    http_method VARCHAR(12),
    path VARCHAR(500),
    status_code INTEGER,
    resource_type VARCHAR(80),
    resource_id VARCHAR(120),
    correlation_id VARCHAR(80),
    ip_address VARCHAR(80),
    user_agent VARCHAR(500),
    message VARCHAR(500),
    metadata_json JSONB,
    CONSTRAINT chk_audit_events_outcome CHECK (outcome IN ('SUCCESS', 'FAILURE', 'DENIED'))
);

CREATE INDEX idx_audit_events_occurred_at
    ON audit_events (occurred_at DESC);

CREATE INDEX idx_audit_events_actor_occurred_at
    ON audit_events (actor_user_id, occurred_at DESC);

CREATE INDEX idx_audit_events_module_occurred_at
    ON audit_events (module, occurred_at DESC);

CREATE INDEX idx_audit_events_resource
    ON audit_events (resource_type, resource_id);

CREATE INDEX idx_audit_events_correlation_id
    ON audit_events (correlation_id);
