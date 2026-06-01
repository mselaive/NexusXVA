CREATE TABLE portfolios (
    id UUID PRIMARY KEY,
    name VARCHAR(120) NOT NULL,
    created_at TIMESTAMPTZ NOT NULL
);

CREATE TABLE portfolio_european_option_positions (
    id UUID PRIMARY KEY,
    portfolio_id UUID NOT NULL REFERENCES portfolios (id) ON DELETE CASCADE,
    underlying_symbol VARCHAR(32) NOT NULL,
    option_type VARCHAR(16) NOT NULL CHECK (option_type IN ('CALL', 'PUT')),
    strike NUMERIC(19, 8) NOT NULL CHECK (strike > 0),
    maturity_date DATE NOT NULL,
    quantity NUMERIC(19, 8) NOT NULL CHECK (quantity <> 0),
    created_at TIMESTAMPTZ NOT NULL
);

CREATE INDEX idx_portfolio_option_positions_portfolio_id
    ON portfolio_european_option_positions (portfolio_id);
