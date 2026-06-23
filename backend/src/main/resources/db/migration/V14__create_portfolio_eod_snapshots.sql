CREATE TABLE portfolio_eod_runs (
    id UUID PRIMARY KEY,
    portfolio_id UUID NOT NULL REFERENCES portfolios (id) ON DELETE CASCADE,
    business_date DATE NOT NULL,
    base_currency CHAR(3) NOT NULL,
    total_market_value NUMERIC(24, 8) NOT NULL,
    total_trade_value NUMERIC(24, 8) NOT NULL,
    total_unrealized_pnl NUMERIC(24, 8) NOT NULL,
    positions_without_execution_price INTEGER NOT NULL,
    captured_at TIMESTAMPTZ NOT NULL,
    source VARCHAR(32) NOT NULL,
    UNIQUE (portfolio_id, business_date)
);

CREATE TABLE portfolio_position_eod_snapshots (
    run_id UUID NOT NULL REFERENCES portfolio_eod_runs (id) ON DELETE CASCADE,
    position_id UUID,
    underlying_symbol VARCHAR(32) NOT NULL,
    quantity NUMERIC(19, 8) NOT NULL,
    unit_price NUMERIC(24, 8) NOT NULL,
    market_value NUMERIC(24, 8) NOT NULL,
    execution_price NUMERIC(19, 8),
    trade_value NUMERIC(24, 8),
    unrealized_pnl NUMERIC(24, 8),
    market_data_as_of TIMESTAMPTZ NOT NULL,
    market_data_source VARCHAR(80) NOT NULL,
    stale BOOLEAN NOT NULL,
    PRIMARY KEY (run_id, position_id)
);

CREATE INDEX idx_portfolio_eod_runs_portfolio_date
    ON portfolio_eod_runs (portfolio_id, business_date DESC);
