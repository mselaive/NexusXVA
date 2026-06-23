INSERT INTO auth_user_accounts (
    id, username, display_name, password_hash, active,
    created_at, updated_at, portfolio_access_mode
)
VALUES
    (
        '91000000-0000-4000-8000-000000000001',
        'fo.tech',
        'Tania Tech Trader',
        '$2y$12$bQOf3nRAAt4tC8tFX.kFMOYGgq1Q/lUJd8q6vVFBce5tpq4vMg2Wy',
        TRUE, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 'SELECTED'
    ),
    (
        '91000000-0000-4000-8000-000000000002',
        'fo.macro',
        'Marco Macro Trader',
        '$2y$12$bQOf3nRAAt4tC8tFX.kFMOYGgq1Q/lUJd8q6vVFBce5tpq4vMg2Wy',
        TRUE, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 'SELECTED'
    ),
    (
        '91000000-0000-4000-8000-000000000003',
        'bo.pnl',
        'Paula P&L Control',
        '$2y$12$QmE3inlo8v.lbO5/7Ii2h.jcCGwQy7toBZoy.UZnHOJeQ3urJfWxC',
        TRUE, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 'ALL'
    )
ON CONFLICT (username) DO NOTHING;

INSERT INTO auth_user_group_memberships (user_id, group_id)
SELECT u.id, g.id
FROM auth_user_accounts u
JOIN auth_groups g ON g.code = 'FO'
WHERE u.username IN ('fo.tech', 'fo.macro')
ON CONFLICT DO NOTHING;

INSERT INTO auth_user_group_memberships (user_id, group_id)
SELECT u.id, g.id
FROM auth_user_accounts u
JOIN auth_groups g ON g.code = 'BO'
WHERE u.username = 'bo.pnl'
ON CONFLICT DO NOTHING;

INSERT INTO portfolios (id, name, description, base_currency, created_at, updated_at)
VALUES
    (
        'a1000000-0000-4000-8000-000000000001',
        'P&L Demo - Tech Options',
        'Small tech option book with execution premiums and a prior EOD close.',
        'USD', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
    ),
    (
        'a1000000-0000-4000-8000-000000000002',
        'P&L Demo - US Banks',
        'Small US bank volatility book for checking long and short option P&L.',
        'USD', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
    ),
    (
        'a1000000-0000-4000-8000-000000000003',
        'P&L Demo - Macro Hedges',
        'ETF option hedge book with a prior EOD close for daily P&L testing.',
        'USD', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
    )
ON CONFLICT (id) DO NOTHING;

INSERT INTO portfolio_european_option_positions (
    id, portfolio_id, underlying_symbol, option_type, strike,
    maturity_date, quantity, execution_price, lifecycle_status, created_at, updated_at
)
VALUES
    ('a2000000-0000-4000-8000-000000000001', 'a1000000-0000-4000-8000-000000000001', 'AAPL', 'CALL', 220, '2027-06-18', 50, 22, 'ACTIVE', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    ('a2000000-0000-4000-8000-000000000002', 'a1000000-0000-4000-8000-000000000001', 'MSFT', 'PUT', 430, '2027-09-17', 30, 18, 'ACTIVE', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    ('a2000000-0000-4000-8000-000000000003', 'a1000000-0000-4000-8000-000000000001', 'NVDA', 'CALL', 160, '2027-06-18', -40, 14, 'ACTIVE', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),

    ('a2000000-0000-4000-8000-000000000004', 'a1000000-0000-4000-8000-000000000002', 'JPM', 'CALL', 240, '2027-06-18', 80, 15, 'ACTIVE', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    ('a2000000-0000-4000-8000-000000000005', 'a1000000-0000-4000-8000-000000000002', 'BAC', 'PUT', 42, '2027-09-17', -100, 4, 'ACTIVE', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    ('a2000000-0000-4000-8000-000000000006', 'a1000000-0000-4000-8000-000000000002', 'GS', 'CALL', 520, '2027-12-17', 20, 35, 'ACTIVE', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),

    ('a2000000-0000-4000-8000-000000000007', 'a1000000-0000-4000-8000-000000000003', 'SPY', 'PUT', 560, '2027-06-18', 40, 28, 'ACTIVE', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    ('a2000000-0000-4000-8000-000000000008', 'a1000000-0000-4000-8000-000000000003', 'QQQ', 'CALL', 520, '2027-09-17', -25, 24, 'ACTIVE', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    ('a2000000-0000-4000-8000-000000000009', 'a1000000-0000-4000-8000-000000000003', 'GLD', 'CALL', 260, '2027-12-17', 60, 12, 'ACTIVE', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)
ON CONFLICT (id) DO NOTHING;

INSERT INTO auth_user_portfolio_access (
    user_id, portfolio_id, granted_at,
    granted_by_username, granted_by_display_name
)
SELECT u.id, p.id, CURRENT_TIMESTAMP, 'system', 'Demo Data Migration'
FROM auth_user_accounts u
JOIN portfolios p ON (
    (u.username = 'fo.tech' AND p.id IN (
        'a1000000-0000-4000-8000-000000000001',
        'a1000000-0000-4000-8000-000000000002'
    ))
    OR
    (u.username = 'fo.macro' AND p.id IN (
        'a1000000-0000-4000-8000-000000000002',
        'a1000000-0000-4000-8000-000000000003'
    ))
)
ON CONFLICT DO NOTHING;

INSERT INTO portfolio_eod_runs (
    id, portfolio_id, business_date, base_currency,
    total_market_value, total_trade_value, total_unrealized_pnl,
    positions_without_execution_price, captured_at, source
)
VALUES
    ('a3000000-0000-4000-8000-000000000001', 'a1000000-0000-4000-8000-000000000001', CURRENT_DATE - 1, 'USD', 1220, 1080, 140, 0, CURRENT_TIMESTAMP, 'DEMO_SEED'),
    ('a3000000-0000-4000-8000-000000000002', 'a1000000-0000-4000-8000-000000000002', CURRENT_DATE - 1, 'USD', 1650, 1500, 150, 0, CURRENT_TIMESTAMP, 'DEMO_SEED'),
    ('a3000000-0000-4000-8000-000000000003', 'a1000000-0000-4000-8000-000000000003', CURRENT_DATE - 1, 'USD', 1330, 1240, 90, 0, CURRENT_TIMESTAMP, 'DEMO_SEED')
ON CONFLICT (portfolio_id, business_date) DO NOTHING;

INSERT INTO portfolio_position_eod_snapshots (
    run_id, position_id, underlying_symbol, quantity,
    unit_price, market_value, execution_price, trade_value, unrealized_pnl,
    market_data_as_of, market_data_source, stale
)
VALUES
    ('a3000000-0000-4000-8000-000000000001', 'a2000000-0000-4000-8000-000000000001', 'AAPL', 50, 24.5, 1225, 22, 1100, 125, CURRENT_TIMESTAMP, 'DEMO_SEED', FALSE),
    ('a3000000-0000-4000-8000-000000000001', 'a2000000-0000-4000-8000-000000000002', 'MSFT', 30, 16.5, 495, 18, 540, -45, CURRENT_TIMESTAMP, 'DEMO_SEED', FALSE),
    ('a3000000-0000-4000-8000-000000000001', 'a2000000-0000-4000-8000-000000000003', 'NVDA', -40, 12.5, -500, 14, -560, 60, CURRENT_TIMESTAMP, 'DEMO_SEED', FALSE),

    ('a3000000-0000-4000-8000-000000000002', 'a2000000-0000-4000-8000-000000000004', 'JPM', 80, 17, 1360, 15, 1200, 160, CURRENT_TIMESTAMP, 'DEMO_SEED', FALSE),
    ('a3000000-0000-4000-8000-000000000002', 'a2000000-0000-4000-8000-000000000005', 'BAC', -100, 3.5, -350, 4, -400, 50, CURRENT_TIMESTAMP, 'DEMO_SEED', FALSE),
    ('a3000000-0000-4000-8000-000000000002', 'a2000000-0000-4000-8000-000000000006', 'GS', 20, 32, 640, 35, 700, -60, CURRENT_TIMESTAMP, 'DEMO_SEED', FALSE),

    ('a3000000-0000-4000-8000-000000000003', 'a2000000-0000-4000-8000-000000000007', 'SPY', 40, 30, 1200, 28, 1120, 80, CURRENT_TIMESTAMP, 'DEMO_SEED', FALSE),
    ('a3000000-0000-4000-8000-000000000003', 'a2000000-0000-4000-8000-000000000008', 'QQQ', -25, 26, -650, 24, -600, -50, CURRENT_TIMESTAMP, 'DEMO_SEED', FALSE),
    ('a3000000-0000-4000-8000-000000000003', 'a2000000-0000-4000-8000-000000000009', 'GLD', 60, 13, 780, 12, 720, 60, CURRENT_TIMESTAMP, 'DEMO_SEED', FALSE)
ON CONFLICT DO NOTHING;
