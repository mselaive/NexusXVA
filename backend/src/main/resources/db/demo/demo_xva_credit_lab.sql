BEGIN;

-- A compact, repeatable XVA scenario. Market curves are intentionally not seeded:
-- import them from Blemberg in XVA Setup so their source lineage remains visible.
INSERT INTO portfolios (id, name, description, base_currency, created_at, updated_at)
VALUES (
    'd1000000-0000-4000-8000-000000000001',
    'XVA Demo - Prime Broker Credit Lab',
    'Compact USD options book for learning counterparty, netting, collateral and market-sourced CVA curves.',
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

INSERT INTO portfolio_european_option_positions (
    id, portfolio_id, underlying_symbol, option_type, strike, maturity_date,
    quantity, execution_price, lifecycle_status, created_at, updated_at
)
VALUES
    ('d2000000-0000-4000-8000-000000000001', 'd1000000-0000-4000-8000-000000000001', 'AAPL', 'CALL', 220, '2027-06-18',  80, 18.40, 'ACTIVE', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    ('d2000000-0000-4000-8000-000000000002', 'd1000000-0000-4000-8000-000000000001', 'AAPL', 'PUT',  190, '2027-06-18', -45, 10.20, 'ACTIVE', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    ('d2000000-0000-4000-8000-000000000003', 'd1000000-0000-4000-8000-000000000001', 'MSFT', 'CALL', 450, '2027-09-17',  55, 24.80, 'ACTIVE', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    ('d2000000-0000-4000-8000-000000000004', 'd1000000-0000-4000-8000-000000000001', 'MSFT', 'PUT',  400, '2027-09-17',  30, 16.10, 'ACTIVE', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    ('d2000000-0000-4000-8000-000000000005', 'd1000000-0000-4000-8000-000000000001', 'SPY',  'CALL', 560, '2027-12-17',  70, 31.50, 'ACTIVE', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    ('d2000000-0000-4000-8000-000000000006', 'd1000000-0000-4000-8000-000000000001', 'SPY',  'PUT',  500, '2027-12-17',  95, 22.75, 'ACTIVE', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    ('d2000000-0000-4000-8000-000000000007', 'd1000000-0000-4000-8000-000000000001', 'QQQ',  'CALL', 500, '2028-03-17',  60, 35.20, 'ACTIVE', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    ('d2000000-0000-4000-8000-000000000008', 'd1000000-0000-4000-8000-000000000001', 'QQQ',  'PUT',  430, '2028-03-17',  50, 25.40, 'ACTIVE', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    ('d2000000-0000-4000-8000-000000000009', 'd1000000-0000-4000-8000-000000000001', 'JPM',  'CALL', 230, '2027-06-18', 120, 12.60, 'ACTIVE', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    ('d2000000-0000-4000-8000-000000000010', 'd1000000-0000-4000-8000-000000000001', 'JPM',  'PUT',  195, '2027-06-18', -70,  8.30, 'ACTIVE', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    ('d2000000-0000-4000-8000-000000000011', 'd1000000-0000-4000-8000-000000000001', 'GLD',  'CALL', 250, '2028-06-16',  90, 19.75, 'ACTIVE', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    ('d2000000-0000-4000-8000-000000000012', 'd1000000-0000-4000-8000-000000000001', 'GLD',  'PUT',  210, '2028-06-16',  65, 13.90, 'ACTIVE', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)
ON CONFLICT (id) DO UPDATE SET
    underlying_symbol = EXCLUDED.underlying_symbol,
    option_type = EXCLUDED.option_type,
    strike = EXCLUDED.strike,
    maturity_date = EXCLUDED.maturity_date,
    quantity = EXCLUDED.quantity,
    execution_price = EXCLUDED.execution_price,
    lifecycle_status = 'ACTIVE',
    updated_at = CURRENT_TIMESTAMP;

INSERT INTO counterparties (
    id, name, external_id, credit_rating, active, created_at, updated_at
)
VALUES (
    'd3000000-0000-4000-8000-000000000001',
    'Demo Prime Broker A',
    'XVA-DEMO-PB-A',
    'A',
    TRUE,
    CURRENT_TIMESTAMP,
    CURRENT_TIMESTAMP
)
ON CONFLICT (id) DO UPDATE SET
    name = EXCLUDED.name,
    external_id = EXCLUDED.external_id,
    credit_rating = EXCLUDED.credit_rating,
    active = TRUE,
    updated_at = CURRENT_TIMESTAMP;

INSERT INTO netting_sets (
    id, counterparty_id, name, base_currency, collateral_amount,
    collateral_currency, active, created_at, updated_at
)
VALUES (
    'd4000000-0000-4000-8000-000000000001',
    'd3000000-0000-4000-8000-000000000001',
    'Demo Prime Broker A - USD Netting',
    'USD',
    0,
    'USD',
    TRUE,
    CURRENT_TIMESTAMP,
    CURRENT_TIMESTAMP
)
ON CONFLICT (id) DO UPDATE SET
    counterparty_id = EXCLUDED.counterparty_id,
    name = EXCLUDED.name,
    base_currency = EXCLUDED.base_currency,
    collateral_amount = EXCLUDED.collateral_amount,
    collateral_currency = EXCLUDED.collateral_currency,
    active = TRUE,
    updated_at = CURRENT_TIMESTAMP;

DELETE FROM netting_set_portfolios
WHERE portfolio_id = 'd1000000-0000-4000-8000-000000000001';

INSERT INTO netting_set_portfolios (netting_set_id, portfolio_id, assigned_at)
VALUES (
    'd4000000-0000-4000-8000-000000000001',
    'd1000000-0000-4000-8000-000000000001',
    CURRENT_TIMESTAMP
);

COMMIT;
