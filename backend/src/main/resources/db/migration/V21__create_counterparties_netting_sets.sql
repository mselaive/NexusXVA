CREATE TABLE counterparties (
    id UUID PRIMARY KEY,
    name VARCHAR(160) NOT NULL,
    external_id VARCHAR(80),
    credit_rating VARCHAR(40),
    active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL,
    CONSTRAINT uk_counterparties_name UNIQUE (name),
    CONSTRAINT uk_counterparties_external_id UNIQUE (external_id)
);

CREATE TABLE netting_sets (
    id UUID PRIMARY KEY,
    counterparty_id UUID NOT NULL REFERENCES counterparties (id),
    name VARCHAR(160) NOT NULL,
    base_currency CHAR(3) NOT NULL DEFAULT 'USD',
    collateral_amount NUMERIC(20, 6) NOT NULL DEFAULT 0,
    collateral_currency CHAR(3) NOT NULL DEFAULT 'USD',
    active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL,
    CONSTRAINT uk_netting_sets_counterparty_name UNIQUE (counterparty_id, name),
    CONSTRAINT chk_netting_sets_base_currency CHECK (base_currency ~ '^[A-Z]{3}$'),
    CONSTRAINT chk_netting_sets_collateral_currency CHECK (collateral_currency ~ '^[A-Z]{3}$'),
    CONSTRAINT chk_netting_sets_collateral_amount CHECK (collateral_amount >= 0)
);

CREATE TABLE netting_set_portfolios (
    netting_set_id UUID NOT NULL REFERENCES netting_sets (id) ON DELETE CASCADE,
    portfolio_id UUID NOT NULL REFERENCES portfolios (id),
    assigned_at TIMESTAMPTZ NOT NULL,
    PRIMARY KEY (netting_set_id, portfolio_id),
    CONSTRAINT uk_netting_set_portfolio UNIQUE (portfolio_id)
);

CREATE INDEX idx_netting_sets_counterparty ON netting_sets (counterparty_id);
CREATE INDEX idx_netting_set_portfolios_portfolio ON netting_set_portfolios (portfolio_id);
