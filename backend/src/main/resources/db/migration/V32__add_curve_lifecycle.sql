ALTER TABLE credit_curves
    ADD COLUMN version INTEGER NOT NULL DEFAULT 1,
    ADD COLUMN status VARCHAR(24) NOT NULL DEFAULT 'APPROVED',
    ADD COLUMN source VARCHAR(40) NOT NULL DEFAULT 'MANUAL',
    ADD COLUMN submitted_at TIMESTAMPTZ,
    ADD COLUMN approved_at TIMESTAMPTZ,
    ADD COLUMN approved_by_user_id UUID,
    ADD COLUMN rejection_reason VARCHAR(500);

ALTER TABLE discount_curves
    ADD COLUMN version INTEGER NOT NULL DEFAULT 1,
    ADD COLUMN status VARCHAR(24) NOT NULL DEFAULT 'APPROVED',
    ADD COLUMN source VARCHAR(40) NOT NULL DEFAULT 'MANUAL',
    ADD COLUMN submitted_at TIMESTAMPTZ,
    ADD COLUMN approved_at TIMESTAMPTZ,
    ADD COLUMN approved_by_user_id UUID,
    ADD COLUMN rejection_reason VARCHAR(500);

UPDATE credit_curves SET approved_at = updated_at WHERE active = TRUE;
UPDATE discount_curves SET approved_at = updated_at WHERE active = TRUE;

ALTER TABLE credit_curves
    ADD CONSTRAINT chk_credit_curves_status CHECK (status IN ('DRAFT', 'APPROVED', 'REJECTED', 'SUPERSEDED')),
    ADD CONSTRAINT chk_credit_curves_source CHECK (source IN ('MANUAL', 'IMPORT', 'MARKET_DATA'));

ALTER TABLE discount_curves
    ADD CONSTRAINT chk_discount_curves_status CHECK (status IN ('DRAFT', 'APPROVED', 'REJECTED', 'SUPERSEDED')),
    ADD CONSTRAINT chk_discount_curves_source CHECK (source IN ('MANUAL', 'IMPORT', 'MARKET_DATA'));

ALTER TABLE credit_curves
    DROP CONSTRAINT uk_credit_curves_counterparty_name;

ALTER TABLE discount_curves
    DROP CONSTRAINT uk_discount_curves_name_currency;

ALTER TABLE credit_curves
    ADD CONSTRAINT uk_credit_curves_counterparty_name_version UNIQUE (counterparty_id, name, version);

ALTER TABLE discount_curves
    ADD CONSTRAINT uk_discount_curves_name_currency_version UNIQUE (name, currency, version);

CREATE UNIQUE INDEX ux_credit_curves_active_approved_name
    ON credit_curves (counterparty_id, name)
    WHERE active = TRUE AND status = 'APPROVED';

CREATE UNIQUE INDEX ux_discount_curves_active_approved_name
    ON discount_curves (name, currency)
    WHERE active = TRUE AND status = 'APPROVED';

CREATE INDEX idx_credit_curves_lifecycle ON credit_curves (status, source, updated_at DESC);
CREATE INDEX idx_discount_curves_lifecycle ON discount_curves (status, source, updated_at DESC);
