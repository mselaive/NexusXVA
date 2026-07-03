ALTER TABLE portfolio_position_eod_snapshots
    ADD COLUMN instrument_type VARCHAR(32) NOT NULL DEFAULT 'EUROPEAN_OPTION';

ALTER TABLE portfolio_position_eod_snapshots
    ADD CONSTRAINT chk_portfolio_position_eod_instrument_type
        CHECK (instrument_type IN ('EUROPEAN_OPTION', 'CASH_EQUITY'));

CREATE INDEX idx_portfolio_position_eod_snapshots_instrument_type
    ON portfolio_position_eod_snapshots (run_id, instrument_type);
