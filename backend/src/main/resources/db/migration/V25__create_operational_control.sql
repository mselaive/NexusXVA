CREATE TABLE operational_control_settings (
    id SMALLINT PRIMARY KEY DEFAULT 1,
    timezone VARCHAR(80) NOT NULL,
    business_days VARCHAR(80) NOT NULL,
    trading_open_time TIME NOT NULL,
    trading_close_time TIME NOT NULL,
    eod_enabled BOOLEAN NOT NULL,
    eod_run_time TIME NOT NULL,
    eod_allow_stale_market_data BOOLEAN NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL,
    updated_by_user_id UUID REFERENCES auth_user_accounts (id) ON DELETE SET NULL,
    version BIGINT NOT NULL DEFAULT 0,
    CONSTRAINT chk_operational_control_singleton CHECK (id = 1),
    CONSTRAINT chk_operational_control_window CHECK (trading_open_time < trading_close_time),
    CONSTRAINT chk_operational_control_eod_after_close CHECK (eod_run_time > trading_close_time)
);

INSERT INTO operational_control_settings (
    id,
    timezone,
    business_days,
    trading_open_time,
    trading_close_time,
    eod_enabled,
    eod_run_time,
    eod_allow_stale_market_data,
    updated_at,
    updated_by_user_id,
    version
)
VALUES (
    1,
    'America/New_York',
    'MONDAY,TUESDAY,WEDNESDAY,THURSDAY,FRIDAY',
    TIME '09:30',
    TIME '16:00',
    FALSE,
    TIME '17:15',
    FALSE,
    NOW(),
    NULL,
    0
);

CREATE TABLE eod_scheduler_runs (
    id UUID PRIMARY KEY,
    business_date DATE NOT NULL,
    started_at TIMESTAMPTZ NOT NULL,
    completed_at TIMESTAMPTZ,
    status VARCHAR(20) NOT NULL,
    captured INTEGER NOT NULL DEFAULT 0,
    skipped INTEGER NOT NULL DEFAULT 0,
    failed INTEGER NOT NULL DEFAULT 0,
    message VARCHAR(500),
    CONSTRAINT chk_eod_scheduler_runs_status CHECK (status IN ('RUNNING', 'COMPLETED', 'FAILED', 'SKIPPED'))
);

CREATE UNIQUE INDEX ux_eod_scheduler_runs_business_date
    ON eod_scheduler_runs (business_date);

CREATE INDEX idx_eod_scheduler_runs_started_at
    ON eod_scheduler_runs (started_at DESC);
