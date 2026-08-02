ALTER TABLE discount_curves
    ADD COLUMN source_as_of TIMESTAMPTZ,
    ADD COLUMN source_reference VARCHAR(240),
    ADD COLUMN construction_method VARCHAR(120),
    ADD COLUMN source_stale BOOLEAN NOT NULL DEFAULT FALSE;

CREATE INDEX idx_discount_curves_source_as_of
    ON discount_curves (source, source_as_of DESC);
