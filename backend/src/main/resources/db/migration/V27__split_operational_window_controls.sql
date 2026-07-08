ALTER TABLE operational_control_settings
    ADD COLUMN block_trade_bookings_outside_window BOOLEAN NOT NULL DEFAULT TRUE,
    ADD COLUMN block_risk_runs_outside_window BOOLEAN NOT NULL DEFAULT TRUE;

UPDATE operational_control_settings
SET block_trade_bookings_outside_window = enforce_operational_window,
    block_risk_runs_outside_window = enforce_operational_window;
