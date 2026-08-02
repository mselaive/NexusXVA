ALTER TABLE credit_curves
    ADD COLUMN source_as_of TIMESTAMPTZ,
    ADD COLUMN source_reference VARCHAR(240),
    ADD COLUMN source_series_id VARCHAR(80),
    ADD COLUMN construction_method VARCHAR(120),
    ADD COLUMN source_stale BOOLEAN NOT NULL DEFAULT FALSE,
    ADD COLUMN source_currency VARCHAR(3),
    ADD COLUMN source_credit_rating VARCHAR(16),
    ADD COLUMN source_rating_bucket VARCHAR(8),
    ADD COLUMN source_recovery_rate NUMERIC(12, 10),
    ADD COLUMN source_spread NUMERIC(18, 10),
    ADD COLUMN source_spread_unit VARCHAR(16),
    ADD COLUMN source_hazard_rate NUMERIC(18, 10),
    ADD COLUMN source_observation_date DATE,
    ADD COLUMN market_proxy BOOLEAN NOT NULL DEFAULT FALSE;

CREATE INDEX idx_credit_curves_source_as_of
    ON credit_curves (source, source_as_of DESC);
