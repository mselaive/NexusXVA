BEGIN;

INSERT INTO portfolios (id, name, description, base_currency, created_at, updated_at)
VALUES
    (
        'c1000000-0000-4000-8000-000000000001',
        'Heavy Demo - Mega Tech Vol Warehouse',
        'Large synthetic FO book with dense option ladders across mega-cap technology names. Built to exercise pricing, stress testing, exposure and UI rendering.',
        'USD',
        CURRENT_TIMESTAMP,
        CURRENT_TIMESTAMP
    ),
    (
        'c1000000-0000-4000-8000-000000000002',
        'Heavy Demo - Cross Asset Scenario Grid',
        'Large synthetic book mixing banks, index ETFs and duration hedges. Useful for stressing portfolio-level aggregation and scenario tables.',
        'USD',
        CURRENT_TIMESTAMP,
        CURRENT_TIMESTAMP
    ),
    (
        'c1000000-0000-4000-8000-000000000003',
        'Heavy Demo - Metals Macro Hedge Stack',
        'Large synthetic book combining metals, equity index hedges, duration and selected single names. Useful for delta hedge, EOD and XVA smoke tests.',
        'USD',
        CURRENT_TIMESTAMP,
        CURRENT_TIMESTAMP
    )
ON CONFLICT (id) DO UPDATE SET
    name = EXCLUDED.name,
    description = EXCLUDED.description,
    base_currency = EXCLUDED.base_currency,
    updated_at = CURRENT_TIMESTAMP,
    archived_at = NULL,
    archived_by_user_id = NULL,
    archive_reason = NULL;

WITH books AS (
    SELECT *
    FROM (
        VALUES
            (
                1,
                'c1000000-0000-4000-8000-000000000001'::uuid,
                ARRAY['AAPL','MSFT','NVDA','AMZN','GOOGL','META','TSLA','AVGO','ORCL','AMD']::text[]
            ),
            (
                2,
                'c1000000-0000-4000-8000-000000000002'::uuid,
                ARRAY['JPM','BAC','GS','MS','C','WFC','SPY','QQQ','DIA','IWM','VTI','TLT']::text[]
            ),
            (
                3,
                'c1000000-0000-4000-8000-000000000003'::uuid,
                ARRAY['GLD','SLV','CPER','SPY','QQQ','TLT','AAPL','NVDA','JPM','BAC']::text[]
            )
    ) AS book(portfolio_index, portfolio_id, symbols)
),
symbol_grid AS (
    SELECT
        book.portfolio_index,
        book.portfolio_id,
        symbol.symbol,
        symbol.ordinality::int AS symbol_index,
        CASE symbol.symbol
            WHEN 'AAPL' THEN 220.0
            WHEN 'MSFT' THEN 430.0
            WHEN 'NVDA' THEN 150.0
            WHEN 'AMZN' THEN 190.0
            WHEN 'GOOGL' THEN 180.0
            WHEN 'META' THEN 520.0
            WHEN 'TSLA' THEN 220.0
            WHEN 'AVGO' THEN 1600.0
            WHEN 'ORCL' THEN 140.0
            WHEN 'AMD' THEN 165.0
            WHEN 'JPM' THEN 220.0
            WHEN 'BAC' THEN 42.0
            WHEN 'GS' THEN 500.0
            WHEN 'MS' THEN 110.0
            WHEN 'C' THEN 65.0
            WHEN 'WFC' THEN 62.0
            WHEN 'SPY' THEN 540.0
            WHEN 'QQQ' THEN 470.0
            WHEN 'DIA' THEN 395.0
            WHEN 'IWM' THEN 210.0
            WHEN 'VTI' THEN 270.0
            WHEN 'TLT' THEN 92.0
            WHEN 'GLD' THEN 230.0
            WHEN 'SLV' THEN 28.0
            WHEN 'CPER' THEN 30.0
            ELSE 100.0
        END::numeric AS reference_spot
    FROM books book
    CROSS JOIN LATERAL unnest(book.symbols) WITH ORDINALITY AS symbol(symbol, ordinality)
),
option_grid AS (
    SELECT
        symbol_grid.*,
        bucket.bucket,
        option_type.option_type,
        row_number() OVER (
            PARTITION BY symbol_grid.portfolio_id
            ORDER BY symbol_grid.symbol_index, bucket.bucket, option_type.option_type
        ) AS local_row_number
    FROM symbol_grid
    CROSS JOIN generate_series(1, 8) AS bucket(bucket)
    CROSS JOIN (VALUES ('CALL'), ('PUT')) AS option_type(option_type)
),
generated_positions AS (
    SELECT
        (
            'c2000000-0000-4000-8'
            || lpad(portfolio_index::text, 3, '0')
            || '-'
            || lpad(local_row_number::text, 12, '0')
        )::uuid AS id,
        portfolio_id,
        symbol AS underlying_symbol,
        option_type,
        round(reference_spot * (0.80 + bucket * 0.055), 2) AS strike,
        (DATE '2027-03-20' + (bucket * 45))::date AS maturity_date,
        (
            CASE
                WHEN (symbol_index + bucket + CASE WHEN option_type = 'PUT' THEN 1 ELSE 0 END) % 4 = 0 THEN -1
                ELSE 1
            END
            * (12 + portfolio_index * 3 + symbol_index * 4 + bucket * 2)
        )::numeric AS quantity,
        round(reference_spot * (0.030 + bucket * 0.0035 + symbol_index * 0.0008), 2) AS execution_price
    FROM option_grid
)
INSERT INTO portfolio_european_option_positions (
    id,
    portfolio_id,
    underlying_symbol,
    option_type,
    strike,
    maturity_date,
    quantity,
    execution_price,
    lifecycle_status,
    created_at,
    updated_at
)
SELECT
    id,
    portfolio_id,
    underlying_symbol,
    option_type,
    strike,
    maturity_date,
    quantity,
    execution_price,
    'ACTIVE',
    CURRENT_TIMESTAMP,
    CURRENT_TIMESTAMP
FROM generated_positions
ON CONFLICT (id) DO UPDATE SET
    portfolio_id = EXCLUDED.portfolio_id,
    underlying_symbol = EXCLUDED.underlying_symbol,
    option_type = EXCLUDED.option_type,
    strike = EXCLUDED.strike,
    maturity_date = EXCLUDED.maturity_date,
    quantity = EXCLUDED.quantity,
    execution_price = EXCLUDED.execution_price,
    lifecycle_status = EXCLUDED.lifecycle_status,
    updated_at = CURRENT_TIMESTAMP;

WITH books AS (
    SELECT *
    FROM (
        VALUES
            (
                1,
                'c1000000-0000-4000-8000-000000000001'::uuid,
                ARRAY['AAPL','MSFT','NVDA','AMZN','GOOGL','META','TSLA','AVGO','ORCL','AMD']::text[]
            ),
            (
                2,
                'c1000000-0000-4000-8000-000000000002'::uuid,
                ARRAY['JPM','BAC','GS','MS','C','WFC','SPY','QQQ','DIA','IWM','VTI','TLT']::text[]
            ),
            (
                3,
                'c1000000-0000-4000-8000-000000000003'::uuid,
                ARRAY['GLD','SLV','CPER','SPY','QQQ','TLT','AAPL','NVDA','JPM','BAC']::text[]
            )
    ) AS book(portfolio_index, portfolio_id, symbols)
),
cash_grid AS (
    SELECT
        book.portfolio_index,
        book.portfolio_id,
        symbol.symbol,
        symbol.ordinality::int AS symbol_index,
        CASE symbol.symbol
            WHEN 'AAPL' THEN 220.0
            WHEN 'MSFT' THEN 430.0
            WHEN 'NVDA' THEN 150.0
            WHEN 'AMZN' THEN 190.0
            WHEN 'GOOGL' THEN 180.0
            WHEN 'META' THEN 520.0
            WHEN 'TSLA' THEN 220.0
            WHEN 'AVGO' THEN 1600.0
            WHEN 'ORCL' THEN 140.0
            WHEN 'AMD' THEN 165.0
            WHEN 'JPM' THEN 220.0
            WHEN 'BAC' THEN 42.0
            WHEN 'GS' THEN 500.0
            WHEN 'MS' THEN 110.0
            WHEN 'C' THEN 65.0
            WHEN 'WFC' THEN 62.0
            WHEN 'SPY' THEN 540.0
            WHEN 'QQQ' THEN 470.0
            WHEN 'DIA' THEN 395.0
            WHEN 'IWM' THEN 210.0
            WHEN 'VTI' THEN 270.0
            WHEN 'TLT' THEN 92.0
            WHEN 'GLD' THEN 230.0
            WHEN 'SLV' THEN 28.0
            WHEN 'CPER' THEN 30.0
            ELSE 100.0
        END::numeric AS reference_spot
    FROM books book
    CROSS JOIN LATERAL unnest(book.symbols) WITH ORDINALITY AS symbol(symbol, ordinality)
)
INSERT INTO portfolio_cash_equity_positions (
    id,
    portfolio_id,
    underlying_symbol,
    quantity,
    execution_price,
    lifecycle_status,
    created_at,
    updated_at
)
SELECT
    (
        'c3000000-0000-4000-8'
        || lpad(portfolio_index::text, 3, '0')
        || '-'
        || lpad(symbol_index::text, 12, '0')
    )::uuid,
    portfolio_id,
    symbol,
    (
        CASE WHEN (portfolio_index + symbol_index) % 3 = 0 THEN -1 ELSE 1 END
        * (75 + symbol_index * 15)
    )::numeric,
    round(reference_spot * (0.985 + portfolio_index * 0.004), 2),
    'ACTIVE',
    CURRENT_TIMESTAMP,
    CURRENT_TIMESTAMP
FROM cash_grid
ON CONFLICT (id) DO UPDATE SET
    portfolio_id = EXCLUDED.portfolio_id,
    underlying_symbol = EXCLUDED.underlying_symbol,
    quantity = EXCLUDED.quantity,
    execution_price = EXCLUDED.execution_price,
    lifecycle_status = EXCLUDED.lifecycle_status,
    updated_at = CURRENT_TIMESTAMP;

INSERT INTO trade_booking_requests (
    id,
    portfolio_id,
    portfolio_name,
    instrument_type,
    underlying_symbol,
    option_type,
    strike,
    maturity_date,
    quantity,
    status,
    submitted_by_user_id,
    submitted_by_username,
    submitted_by_display_name,
    submitted_at,
    reviewed_by_user_id,
    reviewed_by_username,
    reviewed_by_display_name,
    reviewed_at,
    rejection_reason,
    confirmed_position_id,
    execution_price,
    booking_type,
    booking_notional
)
VALUES
    ('c4000000-0000-4000-8000-000000000001', 'c1000000-0000-4000-8000-000000000001', 'Heavy Demo - Mega Tech Vol Warehouse', 'EUROPEAN_OPTION', 'AAPL', 'CALL', 240.00000000, '2027-12-17', 75.00000000, 'PENDING_VALIDATION', (SELECT id FROM auth_user_accounts WHERE username = 'fo.tech'), 'fo.tech', 'Tania Tech Trader', CURRENT_TIMESTAMP - INTERVAL '20 minutes', NULL, NULL, NULL, NULL, NULL, NULL, 21.00000000, 'SINGLE_OPTION', 18000.00000000),
    ('c4000000-0000-4000-8000-000000000002', 'c1000000-0000-4000-8000-000000000001', 'Heavy Demo - Mega Tech Vol Warehouse', 'EUROPEAN_OPTION', 'NVDA', 'PUT', 135.00000000, '2027-09-17', -110.00000000, 'CONFIRMED', (SELECT id FROM auth_user_accounts WHERE username = 'fo.tech'), 'fo.tech', 'Tania Tech Trader', CURRENT_TIMESTAMP - INTERVAL '4 hours', (SELECT id FROM auth_user_accounts WHERE username = 'bo.pnl'), 'bo.pnl', 'Paula P&L Control', CURRENT_TIMESTAMP - INTERVAL '3 hours', NULL, 'c2000000-0000-4000-8001-000000000033', 8.50000000, 'SINGLE_OPTION', 14850.00000000),
    ('c4000000-0000-4000-8000-000000000003', 'c1000000-0000-4000-8000-000000000001', 'Heavy Demo - Mega Tech Vol Warehouse', 'EUROPEAN_OPTION', 'TSLA', 'CALL', 310.00000000, '2028-03-17', 60.00000000, 'REJECTED', (SELECT id FROM auth_user_accounts WHERE username = 'fo.tech'), 'fo.tech', 'Tania Tech Trader', CURRENT_TIMESTAMP - INTERVAL '1 day', (SELECT id FROM auth_user_accounts WHERE username = 'bo.pnl'), 'bo.pnl', 'Paula P&L Control', CURRENT_TIMESTAMP - INTERVAL '23 hours', 'Demo rejection: concentration too high for tech warehouse.', NULL, 18.00000000, 'SINGLE_OPTION', 18600.00000000),

    ('c4000000-0000-4000-8000-000000000004', 'c1000000-0000-4000-8000-000000000002', 'Heavy Demo - Cross Asset Scenario Grid', 'EUROPEAN_OPTION', 'SPY', 'PUT', 500.00000000, '2027-06-18', 120.00000000, 'PENDING_VALIDATION', (SELECT id FROM auth_user_accounts WHERE username = 'fo.macro'), 'fo.macro', 'Marco Macro Trader', CURRENT_TIMESTAMP - INTERVAL '50 minutes', NULL, NULL, NULL, NULL, NULL, NULL, 19.50000000, 'SINGLE_OPTION', 60000.00000000),
    ('c4000000-0000-4000-8000-000000000005', 'c1000000-0000-4000-8000-000000000002', 'Heavy Demo - Cross Asset Scenario Grid', 'EUROPEAN_OPTION', 'TLT', 'CALL', 100.00000000, '2027-12-17', 250.00000000, 'CONFIRMED', (SELECT id FROM auth_user_accounts WHERE username = 'fo.macro'), 'fo.macro', 'Marco Macro Trader', CURRENT_TIMESTAMP - INTERVAL '5 hours', (SELECT id FROM auth_user_accounts WHERE username = 'bo.pnl'), 'bo.pnl', 'Paula P&L Control', CURRENT_TIMESTAMP - INTERVAL '4 hours', NULL, 'c2000000-0000-4000-8002-000000000177', 4.25000000, 'SINGLE_OPTION', 25000.00000000),
    ('c4000000-0000-4000-8000-000000000006', 'c1000000-0000-4000-8000-000000000002', 'Heavy Demo - Cross Asset Scenario Grid', 'EUROPEAN_OPTION', 'BAC', 'PUT', 36.00000000, '2027-09-17', -500.00000000, 'REJECTED', (SELECT id FROM auth_user_accounts WHERE username = 'fo.macro'), 'fo.macro', 'Marco Macro Trader', CURRENT_TIMESTAMP - INTERVAL '2 days', (SELECT id FROM auth_user_accounts WHERE username = 'bo.pnl'), 'bo.pnl', 'Paula P&L Control', CURRENT_TIMESTAMP - INTERVAL '47 hours', 'Demo rejection: wrong sign for requested macro hedge.', NULL, 2.10000000, 'SINGLE_OPTION', 18000.00000000),

    ('c4000000-0000-4000-8000-000000000007', 'c1000000-0000-4000-8000-000000000003', 'Heavy Demo - Metals Macro Hedge Stack', 'EUROPEAN_OPTION', 'GLD', 'CALL', 255.00000000, '2028-01-21', 160.00000000, 'PENDING_VALIDATION', (SELECT id FROM auth_user_accounts WHERE username = 'fo.macro'), 'fo.macro', 'Marco Macro Trader', CURRENT_TIMESTAMP - INTERVAL '75 minutes', NULL, NULL, NULL, NULL, NULL, NULL, 15.50000000, 'SINGLE_OPTION', 40800.00000000),
    ('c4000000-0000-4000-8000-000000000008', 'c1000000-0000-4000-8000-000000000003', 'Heavy Demo - Metals Macro Hedge Stack', 'EUROPEAN_OPTION', 'CPER', 'CALL', 33.00000000, '2027-12-17', 900.00000000, 'CONFIRMED', (SELECT id FROM auth_user_accounts WHERE username = 'fo.macro'), 'fo.macro', 'Marco Macro Trader', CURRENT_TIMESTAMP - INTERVAL '7 hours', (SELECT id FROM auth_user_accounts WHERE username = 'bo.pnl'), 'bo.pnl', 'Paula P&L Control', CURRENT_TIMESTAMP - INTERVAL '6 hours', NULL, 'c2000000-0000-4000-8003-000000000033', 2.70000000, 'SINGLE_OPTION', 29700.00000000),
    ('c4000000-0000-4000-8000-000000000009', 'c1000000-0000-4000-8000-000000000003', 'Heavy Demo - Metals Macro Hedge Stack', 'EUROPEAN_OPTION', 'SLV', 'PUT', 24.00000000, '2027-06-18', -700.00000000, 'REJECTED', (SELECT id FROM auth_user_accounts WHERE username = 'fo.macro'), 'fo.macro', 'Marco Macro Trader', CURRENT_TIMESTAMP - INTERVAL '3 days', (SELECT id FROM auth_user_accounts WHERE username = 'bo.pnl'), 'bo.pnl', 'Paula P&L Control', CURRENT_TIMESTAMP - INTERVAL '70 hours', 'Demo rejection: metals desk asked to resubmit as spread.', NULL, 1.90000000, 'SINGLE_OPTION', 16800.00000000)
ON CONFLICT (id) DO UPDATE SET
    portfolio_id = EXCLUDED.portfolio_id,
    portfolio_name = EXCLUDED.portfolio_name,
    instrument_type = EXCLUDED.instrument_type,
    underlying_symbol = EXCLUDED.underlying_symbol,
    option_type = EXCLUDED.option_type,
    strike = EXCLUDED.strike,
    maturity_date = EXCLUDED.maturity_date,
    quantity = EXCLUDED.quantity,
    status = EXCLUDED.status,
    reviewed_by_user_id = EXCLUDED.reviewed_by_user_id,
    reviewed_by_username = EXCLUDED.reviewed_by_username,
    reviewed_by_display_name = EXCLUDED.reviewed_by_display_name,
    reviewed_at = EXCLUDED.reviewed_at,
    rejection_reason = EXCLUDED.rejection_reason,
    confirmed_position_id = EXCLUDED.confirmed_position_id,
    execution_price = EXCLUDED.execution_price,
    booking_type = EXCLUDED.booking_type,
    booking_notional = EXCLUDED.booking_notional;

WITH heavy_books AS (
    SELECT *
    FROM (
        VALUES
            (1, 'c1000000-0000-4000-8000-000000000001'::uuid, 'Heavy Demo - Mega Tech Vol Warehouse', 'fo.tech', 'Tania Tech Trader', ARRAY['AAPL','MSFT','NVDA','AMZN','GOOGL','META','TSLA','AVGO','ORCL','AMD']::text[]),
            (2, 'c1000000-0000-4000-8000-000000000002'::uuid, 'Heavy Demo - Cross Asset Scenario Grid', 'fo.macro', 'Marco Macro Trader', ARRAY['JPM','BAC','GS','MS','C','WFC','SPY','QQQ','DIA','IWM','VTI','TLT']::text[]),
            (3, 'c1000000-0000-4000-8000-000000000003'::uuid, 'Heavy Demo - Metals Macro Hedge Stack', 'fo.macro', 'Marco Macro Trader', ARRAY['GLD','SLV','CPER','SPY','QQQ','TLT','AAPL','NVDA','JPM','BAC']::text[])
    ) AS book(portfolio_index, portfolio_id, portfolio_name, maker_username, maker_display_name, symbols)
),
generated_bookings AS (
    SELECT
        (
            'c6000000-0000-4000-8'
            || lpad(book.portfolio_index::text, 3, '0')
            || '-'
            || lpad(seq.seq::text, 12, '0')
        )::uuid AS id,
        book.portfolio_id,
        book.portfolio_name,
        book.maker_username,
        book.maker_display_name,
        book.symbols[((seq.seq - 1) % array_length(book.symbols, 1)) + 1] AS symbol,
        CASE WHEN seq.seq % 2 = 0 THEN 'PUT' ELSE 'CALL' END AS option_type,
        round((85 + book.portfolio_index * 12 + seq.seq * 4.75)::numeric, 2) AS strike,
        (DATE '2027-03-20' + (seq.seq * 14))::date AS maturity_date,
        (CASE WHEN seq.seq % 5 = 0 THEN -1 ELSE 1 END * (10 + seq.seq * 3))::numeric AS quantity,
        CASE
            WHEN seq.seq % 3 = 1 THEN 'PENDING_VALIDATION'
            WHEN seq.seq % 3 = 2 THEN 'CONFIRMED'
            ELSE 'REJECTED'
        END AS status,
        seq.seq
    FROM heavy_books book
    CROSS JOIN generate_series(1, 30) AS seq(seq)
)
INSERT INTO trade_booking_requests (
    id,
    portfolio_id,
    portfolio_name,
    instrument_type,
    underlying_symbol,
    option_type,
    strike,
    maturity_date,
    quantity,
    status,
    submitted_by_user_id,
    submitted_by_username,
    submitted_by_display_name,
    submitted_at,
    reviewed_by_user_id,
    reviewed_by_username,
    reviewed_by_display_name,
    reviewed_at,
    rejection_reason,
    confirmed_position_id,
    execution_price,
    booking_type,
    booking_notional
)
SELECT
    booking.id,
    booking.portfolio_id,
    booking.portfolio_name,
    'EUROPEAN_OPTION',
    booking.symbol,
    booking.option_type,
    booking.strike,
    booking.maturity_date,
    booking.quantity,
    booking.status,
    maker.id,
    booking.maker_username,
    booking.maker_display_name,
    CURRENT_TIMESTAMP - (booking.seq || ' hours')::interval,
    CASE WHEN booking.status IN ('CONFIRMED', 'REJECTED') THEN reviewer.id ELSE NULL END,
    CASE WHEN booking.status IN ('CONFIRMED', 'REJECTED') THEN 'bo.pnl' ELSE NULL END,
    CASE WHEN booking.status IN ('CONFIRMED', 'REJECTED') THEN 'Paula P&L Control' ELSE NULL END,
    CASE WHEN booking.status IN ('CONFIRMED', 'REJECTED') THEN CURRENT_TIMESTAMP - ((booking.seq - 1) || ' hours')::interval ELSE NULL END,
    CASE WHEN booking.status = 'REJECTED' THEN 'Heavy demo rejection for workflow load testing.' ELSE NULL END,
    CASE
        WHEN booking.status = 'CONFIRMED' THEN (
            'c2000000-0000-4000-8'
            || lpad((CASE WHEN booking.portfolio_id = 'c1000000-0000-4000-8000-000000000001'::uuid THEN 1 WHEN booking.portfolio_id = 'c1000000-0000-4000-8000-000000000002'::uuid THEN 2 ELSE 3 END)::text, 3, '0')
            || '-'
            || lpad(((booking.seq % 80) + 1)::text, 12, '0')
        )::uuid
        ELSE NULL
    END,
    round((booking.strike * 0.04)::numeric, 2),
    'SINGLE_OPTION',
    abs(booking.quantity) * booking.strike
FROM generated_bookings booking
LEFT JOIN auth_user_accounts maker ON maker.username = booking.maker_username
LEFT JOIN auth_user_accounts reviewer ON reviewer.username = 'bo.pnl'
ON CONFLICT (id) DO UPDATE SET
    portfolio_id = EXCLUDED.portfolio_id,
    portfolio_name = EXCLUDED.portfolio_name,
    instrument_type = EXCLUDED.instrument_type,
    underlying_symbol = EXCLUDED.underlying_symbol,
    option_type = EXCLUDED.option_type,
    strike = EXCLUDED.strike,
    maturity_date = EXCLUDED.maturity_date,
    quantity = EXCLUDED.quantity,
    status = EXCLUDED.status,
    reviewed_by_user_id = EXCLUDED.reviewed_by_user_id,
    reviewed_by_username = EXCLUDED.reviewed_by_username,
    reviewed_by_display_name = EXCLUDED.reviewed_by_display_name,
    reviewed_at = EXCLUDED.reviewed_at,
    rejection_reason = EXCLUDED.rejection_reason,
    confirmed_position_id = EXCLUDED.confirmed_position_id,
    execution_price = EXCLUDED.execution_price,
    booking_type = EXCLUDED.booking_type,
    booking_notional = EXCLUDED.booking_notional;

INSERT INTO trade_lifecycle_requests (
    id,
    portfolio_id,
    portfolio_name,
    position_id,
    request_type,
    status,
    original_underlying_symbol,
    original_option_type,
    original_strike,
    original_maturity_date,
    original_quantity,
    requested_underlying_symbol,
    requested_option_type,
    requested_strike,
    requested_maturity_date,
    requested_quantity,
    submitted_by_user_id,
    submitted_by_username,
    submitted_by_display_name,
    submitted_at,
    reviewed_by_user_id,
    reviewed_by_username,
    reviewed_by_display_name,
    reviewed_at,
    rejection_reason,
    resulting_position_id
)
VALUES
    ('c5000000-0000-4000-8000-000000000001', 'c1000000-0000-4000-8000-000000000001', 'Heavy Demo - Mega Tech Vol Warehouse', 'c2000000-0000-4000-8001-000000000005', 'AMEND', 'PENDING_VALIDATION', 'AAPL', 'CALL', 212.30000000, '2027-08-02', 21.00000000, 'AAPL', 'CALL', 225.00000000, '2027-09-17', 35.00000000, (SELECT id FROM auth_user_accounts WHERE username = 'fo.tech'), 'fo.tech', 'Tania Tech Trader', CURRENT_TIMESTAMP - INTERVAL '25 minutes', NULL, NULL, NULL, NULL, NULL, NULL),
    ('c5000000-0000-4000-8000-000000000002', 'c1000000-0000-4000-8000-000000000001', 'Heavy Demo - Mega Tech Vol Warehouse', 'c2000000-0000-4000-8001-000000000033', 'AMEND', 'APPROVED', 'NVDA', 'CALL', 128.25000000, '2027-05-04', 25.00000000, 'NVDA', 'CALL', 140.00000000, '2027-09-17', 40.00000000, (SELECT id FROM auth_user_accounts WHERE username = 'fo.tech'), 'fo.tech', 'Tania Tech Trader', CURRENT_TIMESTAMP - INTERVAL '9 hours', (SELECT id FROM auth_user_accounts WHERE username = 'bo.pnl'), 'bo.pnl', 'Paula P&L Control', CURRENT_TIMESTAMP - INTERVAL '8 hours', NULL, 'c2000000-0000-4000-8001-000000000034'),
    ('c5000000-0000-4000-8000-000000000003', 'c1000000-0000-4000-8000-000000000001', 'Heavy Demo - Mega Tech Vol Warehouse', 'c2000000-0000-4000-8001-000000000081', 'CANCEL', 'REJECTED', 'META', 'CALL', 444.60000000, '2027-05-04', 37.00000000, NULL, NULL, NULL, NULL, NULL, (SELECT id FROM auth_user_accounts WHERE username = 'fo.tech'), 'fo.tech', 'Tania Tech Trader', CURRENT_TIMESTAMP - INTERVAL '15 hours', (SELECT id FROM auth_user_accounts WHERE username = 'bo.pnl'), 'bo.pnl', 'Paula P&L Control', CURRENT_TIMESTAMP - INTERVAL '14 hours', 'Demo rejection: position remains required for hedge coverage.', NULL),

    ('c5000000-0000-4000-8000-000000000004', 'c1000000-0000-4000-8000-000000000002', 'Heavy Demo - Cross Asset Scenario Grid', 'c2000000-0000-4000-8002-000000000097', 'CANCEL', 'PENDING_VALIDATION', 'SPY', 'CALL', 461.70000000, '2027-05-04', 43.00000000, NULL, NULL, NULL, NULL, NULL, (SELECT id FROM auth_user_accounts WHERE username = 'fo.macro'), 'fo.macro', 'Marco Macro Trader', CURRENT_TIMESTAMP - INTERVAL '45 minutes', NULL, NULL, NULL, NULL, NULL, NULL),
    ('c5000000-0000-4000-8000-000000000005', 'c1000000-0000-4000-8000-000000000002', 'Heavy Demo - Cross Asset Scenario Grid', 'c2000000-0000-4000-8002-000000000177', 'CANCEL', 'APPROVED', 'TLT', 'CALL', 78.66000000, '2027-05-04', 63.00000000, NULL, NULL, NULL, NULL, NULL, (SELECT id FROM auth_user_accounts WHERE username = 'fo.macro'), 'fo.macro', 'Marco Macro Trader', CURRENT_TIMESTAMP - INTERVAL '1 day', (SELECT id FROM auth_user_accounts WHERE username = 'bo.pnl'), 'bo.pnl', 'Paula P&L Control', CURRENT_TIMESTAMP - INTERVAL '23 hours', NULL, NULL),
    ('c5000000-0000-4000-8000-000000000006', 'c1000000-0000-4000-8000-000000000002', 'Heavy Demo - Cross Asset Scenario Grid', 'c2000000-0000-4000-8002-000000000017', 'AMEND', 'REJECTED', 'BAC', 'CALL', 35.91000000, '2027-05-04', 27.00000000, 'BAC', 'CALL', 44.00000000, '2027-12-17', 90.00000000, (SELECT id FROM auth_user_accounts WHERE username = 'fo.macro'), 'fo.macro', 'Marco Macro Trader', CURRENT_TIMESTAMP - INTERVAL '2 days', (SELECT id FROM auth_user_accounts WHERE username = 'bo.pnl'), 'bo.pnl', 'Paula P&L Control', CURRENT_TIMESTAMP - INTERVAL '47 hours', 'Demo rejection: requested size needs desk limit review.', NULL),

    ('c5000000-0000-4000-8000-000000000007', 'c1000000-0000-4000-8000-000000000003', 'Heavy Demo - Metals Macro Hedge Stack', 'c2000000-0000-4000-8003-000000000001', 'AMEND', 'PENDING_VALIDATION', 'GLD', 'CALL', 196.65000000, '2027-05-04', 27.00000000, 'GLD', 'CALL', 245.00000000, '2027-12-17', 120.00000000, (SELECT id FROM auth_user_accounts WHERE username = 'fo.macro'), 'fo.macro', 'Marco Macro Trader', CURRENT_TIMESTAMP - INTERVAL '70 minutes', NULL, NULL, NULL, NULL, NULL, NULL),
    ('c5000000-0000-4000-8000-000000000008', 'c1000000-0000-4000-8000-000000000003', 'Heavy Demo - Metals Macro Hedge Stack', 'c2000000-0000-4000-8003-000000000033', 'AMEND', 'APPROVED', 'CPER', 'CALL', 25.65000000, '2027-05-04', 35.00000000, 'CPER', 'CALL', 33.00000000, '2027-12-17', 450.00000000, (SELECT id FROM auth_user_accounts WHERE username = 'fo.macro'), 'fo.macro', 'Marco Macro Trader', CURRENT_TIMESTAMP - INTERVAL '10 hours', (SELECT id FROM auth_user_accounts WHERE username = 'bo.pnl'), 'bo.pnl', 'Paula P&L Control', CURRENT_TIMESTAMP - INTERVAL '9 hours', NULL, 'c2000000-0000-4000-8003-000000000034'),
    ('c5000000-0000-4000-8000-000000000009', 'c1000000-0000-4000-8000-000000000003', 'Heavy Demo - Metals Macro Hedge Stack', 'c2000000-0000-4000-8003-000000000017', 'CANCEL', 'REJECTED', 'SLV', 'CALL', 23.94000000, '2027-05-04', 31.00000000, NULL, NULL, NULL, NULL, NULL, (SELECT id FROM auth_user_accounts WHERE username = 'fo.macro'), 'fo.macro', 'Marco Macro Trader', CURRENT_TIMESTAMP - INTERVAL '16 hours', (SELECT id FROM auth_user_accounts WHERE username = 'bo.pnl'), 'bo.pnl', 'Paula P&L Control', CURRENT_TIMESTAMP - INTERVAL '15 hours', 'Demo rejection: keep silver leg until next rebalance.', NULL)
ON CONFLICT (id) DO UPDATE SET
    portfolio_id = EXCLUDED.portfolio_id,
    portfolio_name = EXCLUDED.portfolio_name,
    position_id = EXCLUDED.position_id,
    request_type = EXCLUDED.request_type,
    status = EXCLUDED.status,
    original_underlying_symbol = EXCLUDED.original_underlying_symbol,
    original_option_type = EXCLUDED.original_option_type,
    original_strike = EXCLUDED.original_strike,
    original_maturity_date = EXCLUDED.original_maturity_date,
    original_quantity = EXCLUDED.original_quantity,
    requested_underlying_symbol = EXCLUDED.requested_underlying_symbol,
    requested_option_type = EXCLUDED.requested_option_type,
    requested_strike = EXCLUDED.requested_strike,
    requested_maturity_date = EXCLUDED.requested_maturity_date,
    requested_quantity = EXCLUDED.requested_quantity,
    reviewed_by_user_id = EXCLUDED.reviewed_by_user_id,
    reviewed_by_username = EXCLUDED.reviewed_by_username,
    reviewed_by_display_name = EXCLUDED.reviewed_by_display_name,
    reviewed_at = EXCLUDED.reviewed_at,
    rejection_reason = EXCLUDED.rejection_reason,
    resulting_position_id = EXCLUDED.resulting_position_id;

WITH heavy_books AS (
    SELECT *
    FROM (
        VALUES
            (1, 'c1000000-0000-4000-8000-000000000001'::uuid, 'Heavy Demo - Mega Tech Vol Warehouse', 'fo.tech', 'Tania Tech Trader', ARRAY['AAPL','MSFT','NVDA','AMZN','GOOGL','META','TSLA','AVGO','ORCL','AMD']::text[]),
            (2, 'c1000000-0000-4000-8000-000000000002'::uuid, 'Heavy Demo - Cross Asset Scenario Grid', 'fo.macro', 'Marco Macro Trader', ARRAY['JPM','BAC','GS','MS','C','WFC','SPY','QQQ','DIA','IWM','VTI','TLT']::text[]),
            (3, 'c1000000-0000-4000-8000-000000000003'::uuid, 'Heavy Demo - Metals Macro Hedge Stack', 'fo.macro', 'Marco Macro Trader', ARRAY['GLD','SLV','CPER','SPY','QQQ','TLT','AAPL','NVDA','JPM','BAC']::text[])
    ) AS book(portfolio_index, portfolio_id, portfolio_name, maker_username, maker_display_name, symbols)
),
generated_lifecycle AS (
    SELECT
        (
            'c7000000-0000-4000-8'
            || lpad(book.portfolio_index::text, 3, '0')
            || '-'
            || lpad(seq.seq::text, 12, '0')
        )::uuid AS id,
        book.portfolio_index,
        book.portfolio_id,
        book.portfolio_name,
        book.maker_username,
        book.maker_display_name,
        book.symbols[((seq.seq - 1) % array_length(book.symbols, 1)) + 1] AS symbol,
        seq.seq,
        CASE WHEN seq.seq % 2 = 0 THEN 'CANCEL' ELSE 'AMEND' END AS request_type,
        CASE
            WHEN seq.seq % 3 = 1 THEN 'PENDING_VALIDATION'
            WHEN seq.seq % 3 = 2 THEN 'APPROVED'
            ELSE 'REJECTED'
        END AS status,
        (
            'c2000000-0000-4000-8'
            || lpad(book.portfolio_index::text, 3, '0')
            || '-'
            || lpad((40 + seq.seq)::text, 12, '0')
        )::uuid AS position_id
    FROM heavy_books book
    CROSS JOIN generate_series(1, 24) AS seq(seq)
)
INSERT INTO trade_lifecycle_requests (
    id,
    portfolio_id,
    portfolio_name,
    position_id,
    request_type,
    status,
    original_underlying_symbol,
    original_option_type,
    original_strike,
    original_maturity_date,
    original_quantity,
    requested_underlying_symbol,
    requested_option_type,
    requested_strike,
    requested_maturity_date,
    requested_quantity,
    submitted_by_user_id,
    submitted_by_username,
    submitted_by_display_name,
    submitted_at,
    reviewed_by_user_id,
    reviewed_by_username,
    reviewed_by_display_name,
    reviewed_at,
    rejection_reason,
    resulting_position_id
)
SELECT
    lifecycle.id,
    lifecycle.portfolio_id,
    lifecycle.portfolio_name,
    lifecycle.position_id,
    lifecycle.request_type,
    lifecycle.status,
    lifecycle.symbol,
    CASE WHEN lifecycle.seq % 2 = 0 THEN 'PUT' ELSE 'CALL' END,
    round((90 + lifecycle.portfolio_index * 8 + lifecycle.seq * 3.25)::numeric, 2),
    (DATE '2027-03-20' + (lifecycle.seq * 20))::date,
    (12 + lifecycle.seq * 2)::numeric,
    CASE WHEN lifecycle.request_type = 'AMEND' THEN lifecycle.symbol ELSE NULL END,
    CASE WHEN lifecycle.request_type = 'AMEND' THEN CASE WHEN lifecycle.seq % 2 = 0 THEN 'PUT' ELSE 'CALL' END ELSE NULL END,
    CASE WHEN lifecycle.request_type = 'AMEND' THEN round((96 + lifecycle.portfolio_index * 8 + lifecycle.seq * 3.50)::numeric, 2) ELSE NULL END,
    CASE WHEN lifecycle.request_type = 'AMEND' THEN (DATE '2027-06-18' + (lifecycle.seq * 20))::date ELSE NULL END,
    CASE WHEN lifecycle.request_type = 'AMEND' THEN (18 + lifecycle.seq * 3)::numeric ELSE NULL END,
    maker.id,
    lifecycle.maker_username,
    lifecycle.maker_display_name,
    CURRENT_TIMESTAMP - (lifecycle.seq || ' hours')::interval,
    CASE WHEN lifecycle.status IN ('APPROVED', 'REJECTED') THEN reviewer.id ELSE NULL END,
    CASE WHEN lifecycle.status IN ('APPROVED', 'REJECTED') THEN 'bo.pnl' ELSE NULL END,
    CASE WHEN lifecycle.status IN ('APPROVED', 'REJECTED') THEN 'Paula P&L Control' ELSE NULL END,
    CASE WHEN lifecycle.status IN ('APPROVED', 'REJECTED') THEN CURRENT_TIMESTAMP - ((lifecycle.seq - 1) || ' hours')::interval ELSE NULL END,
    CASE WHEN lifecycle.status = 'REJECTED' THEN 'Heavy demo lifecycle rejection for workflow load testing.' ELSE NULL END,
    CASE
        WHEN lifecycle.status = 'APPROVED' AND lifecycle.request_type = 'AMEND' THEN (
            'c2000000-0000-4000-8'
            || lpad(lifecycle.portfolio_index::text, 3, '0')
            || '-'
            || lpad((90 + lifecycle.seq)::text, 12, '0')
        )::uuid
        ELSE NULL
    END
FROM generated_lifecycle lifecycle
LEFT JOIN auth_user_accounts maker ON maker.username = lifecycle.maker_username
LEFT JOIN auth_user_accounts reviewer ON reviewer.username = 'bo.pnl'
ON CONFLICT (id) DO UPDATE SET
    portfolio_id = EXCLUDED.portfolio_id,
    portfolio_name = EXCLUDED.portfolio_name,
    position_id = EXCLUDED.position_id,
    request_type = EXCLUDED.request_type,
    status = EXCLUDED.status,
    original_underlying_symbol = EXCLUDED.original_underlying_symbol,
    original_option_type = EXCLUDED.original_option_type,
    original_strike = EXCLUDED.original_strike,
    original_maturity_date = EXCLUDED.original_maturity_date,
    original_quantity = EXCLUDED.original_quantity,
    requested_underlying_symbol = EXCLUDED.requested_underlying_symbol,
    requested_option_type = EXCLUDED.requested_option_type,
    requested_strike = EXCLUDED.requested_strike,
    requested_maturity_date = EXCLUDED.requested_maturity_date,
    requested_quantity = EXCLUDED.requested_quantity,
    reviewed_by_user_id = EXCLUDED.reviewed_by_user_id,
    reviewed_by_username = EXCLUDED.reviewed_by_username,
    reviewed_by_display_name = EXCLUDED.reviewed_by_display_name,
    reviewed_at = EXCLUDED.reviewed_at,
    rejection_reason = EXCLUDED.rejection_reason,
    resulting_position_id = EXCLUDED.resulting_position_id;

UPDATE portfolio_european_option_positions
SET lifecycle_status = 'AMENDED', updated_at = CURRENT_TIMESTAMP
WHERE id IN (
    'c2000000-0000-4000-8001-000000000033',
    'c2000000-0000-4000-8003-000000000033'
);

UPDATE portfolio_european_option_positions
SET lifecycle_status = 'CANCELLED', updated_at = CURRENT_TIMESTAMP
WHERE id = 'c2000000-0000-4000-8002-000000000177';

INSERT INTO auth_user_portfolio_access (
    user_id,
    portfolio_id,
    granted_at,
    granted_by_username,
    granted_by_display_name
)
SELECT u.id, p.id, CURRENT_TIMESTAMP, 'system', 'Heavy Demo Seed'
FROM auth_user_accounts u
JOIN portfolios p ON (
    (u.username = 'fo.tech' AND p.id IN (
        'c1000000-0000-4000-8000-000000000001',
        'c1000000-0000-4000-8000-000000000002'
    ))
    OR
    (u.username = 'fo.macro' AND p.id IN (
        'c1000000-0000-4000-8000-000000000002',
        'c1000000-0000-4000-8000-000000000003'
    ))
)
ON CONFLICT DO NOTHING;

COMMIT;
