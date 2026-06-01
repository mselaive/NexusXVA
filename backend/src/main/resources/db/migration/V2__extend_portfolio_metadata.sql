ALTER TABLE portfolios
    ADD COLUMN description VARCHAR(500),
    ADD COLUMN base_currency VARCHAR(3) NOT NULL DEFAULT 'USD',
    ADD COLUMN updated_at TIMESTAMPTZ;

UPDATE portfolios
SET updated_at = created_at
WHERE updated_at IS NULL;

ALTER TABLE portfolios
    ALTER COLUMN updated_at SET NOT NULL,
    ADD CONSTRAINT chk_portfolios_base_currency_format
        CHECK (base_currency ~ '^[A-Z]{3}$');
