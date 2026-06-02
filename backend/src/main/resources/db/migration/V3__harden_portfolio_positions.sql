ALTER TABLE portfolio_european_option_positions
    ADD COLUMN updated_at TIMESTAMPTZ;

UPDATE portfolio_european_option_positions
SET updated_at = created_at
WHERE updated_at IS NULL;

ALTER TABLE portfolio_european_option_positions
    ALTER COLUMN updated_at SET NOT NULL;

CREATE INDEX idx_portfolio_option_positions_portfolio_symbol
    ON portfolio_european_option_positions (portfolio_id, underlying_symbol);

CREATE INDEX idx_portfolio_option_positions_portfolio_maturity
    ON portfolio_european_option_positions (portfolio_id, maturity_date);
