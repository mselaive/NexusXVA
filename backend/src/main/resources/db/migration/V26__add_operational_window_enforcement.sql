ALTER TABLE operational_control_settings
    ADD COLUMN enforce_operational_window BOOLEAN NOT NULL DEFAULT TRUE;
