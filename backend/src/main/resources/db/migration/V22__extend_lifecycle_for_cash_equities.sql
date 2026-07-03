ALTER TABLE trade_lifecycle_requests
    ADD COLUMN instrument_type VARCHAR(32) NOT NULL DEFAULT 'EUROPEAN_OPTION',
    ADD COLUMN original_execution_price NUMERIC(19, 8),
    ADD COLUMN requested_execution_price NUMERIC(19, 8);

ALTER TABLE trade_lifecycle_requests
    ALTER COLUMN original_option_type DROP NOT NULL,
    ALTER COLUMN original_strike DROP NOT NULL,
    ALTER COLUMN original_maturity_date DROP NOT NULL;

ALTER TABLE trade_lifecycle_requests
    ADD CONSTRAINT chk_trade_lifecycle_instrument_type
        CHECK (instrument_type IN ('EUROPEAN_OPTION', 'CASH_EQUITY'));

ALTER TABLE trade_lifecycle_requests
    ADD CONSTRAINT chk_trade_lifecycle_product_terms
        CHECK (
            (
                instrument_type = 'EUROPEAN_OPTION'
                AND original_option_type IN ('CALL', 'PUT')
                AND original_strike > 0
                AND original_maturity_date IS NOT NULL
                AND (
                    request_type = 'CANCEL'
                    OR (
                        requested_option_type IN ('CALL', 'PUT')
                        AND requested_strike > 0
                        AND requested_maturity_date IS NOT NULL
                    )
                )
            )
            OR
            (
                instrument_type = 'CASH_EQUITY'
                AND original_option_type IS NULL
                AND original_strike IS NULL
                AND original_maturity_date IS NULL
                AND requested_option_type IS NULL
                AND requested_strike IS NULL
                AND requested_maturity_date IS NULL
            )
        );
