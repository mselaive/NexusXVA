CREATE TABLE credit_curves (
    id UUID PRIMARY KEY,
    counterparty_id UUID NOT NULL REFERENCES counterparties (id),
    name VARCHAR(160) NOT NULL,
    curve_type VARCHAR(40) NOT NULL,
    active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL,
    CONSTRAINT uk_credit_curves_counterparty_name UNIQUE (counterparty_id, name),
    CONSTRAINT chk_credit_curves_type CHECK (curve_type IN ('SURVIVAL_PROBABILITY', 'CUMULATIVE_DEFAULT_PROBABILITY'))
);

CREATE TABLE credit_curve_points (
    credit_curve_id UUID NOT NULL REFERENCES credit_curves (id) ON DELETE CASCADE,
    point_date DATE NOT NULL,
    survival_probability DOUBLE PRECISION,
    cumulative_default_probability DOUBLE PRECISION,
    PRIMARY KEY (credit_curve_id, point_date),
    CONSTRAINT chk_credit_curve_points_survival CHECK (survival_probability IS NULL OR (survival_probability >= 0.0 AND survival_probability <= 1.0)),
    CONSTRAINT chk_credit_curve_points_cumulative CHECK (cumulative_default_probability IS NULL OR (cumulative_default_probability >= 0.0 AND cumulative_default_probability <= 1.0)),
    CONSTRAINT chk_credit_curve_points_one_value CHECK (
        (survival_probability IS NOT NULL AND cumulative_default_probability IS NULL)
        OR (survival_probability IS NULL AND cumulative_default_probability IS NOT NULL)
    )
);

CREATE TABLE discount_curves (
    id UUID PRIMARY KEY,
    name VARCHAR(160) NOT NULL,
    currency CHAR(3) NOT NULL,
    active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL,
    CONSTRAINT uk_discount_curves_name_currency UNIQUE (name, currency),
    CONSTRAINT chk_discount_curves_currency CHECK (currency ~ '^[A-Z]{3}$')
);

CREATE TABLE discount_curve_points (
    discount_curve_id UUID NOT NULL REFERENCES discount_curves (id) ON DELETE CASCADE,
    point_date DATE NOT NULL,
    discount_factor DOUBLE PRECISION NOT NULL,
    PRIMARY KEY (discount_curve_id, point_date),
    CONSTRAINT chk_discount_curve_points_factor CHECK (discount_factor >= 0.0 AND discount_factor <= 1.0)
);

CREATE INDEX idx_credit_curves_counterparty ON credit_curves (counterparty_id, active);
CREATE INDEX idx_discount_curves_currency ON discount_curves (currency, active);
