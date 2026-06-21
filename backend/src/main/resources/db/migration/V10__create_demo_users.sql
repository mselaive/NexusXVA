INSERT INTO auth_user_accounts (
    id,
    username,
    display_name,
    password_hash,
    active,
    created_at,
    updated_at,
    portfolio_access_mode
)
VALUES
    (
        '90000000-0000-4000-8000-000000000001',
        'fo.trader',
        'Fiona Trader',
        '$2y$12$bQOf3nRAAt4tC8tFX.kFMOYGgq1Q/lUJd8q6vVFBce5tpq4vMg2Wy',
        TRUE,
        CURRENT_TIMESTAMP,
        CURRENT_TIMESTAMP,
        'ALL'
    ),
    (
        '90000000-0000-4000-8000-000000000002',
        'bo.ops',
        'Bruno Operations',
        '$2y$12$QmE3inlo8v.lbO5/7Ii2h.jcCGwQy7toBZoy.UZnHOJeQ3urJfWxC',
        TRUE,
        CURRENT_TIMESTAMP,
        CURRENT_TIMESTAMP,
        'ALL'
    ),
    (
        '90000000-0000-4000-8000-000000000003',
        'raul',
        'Raul Multi Group',
        '$2y$12$4cfzoMsv6LlNWkgHHYozReM2wMhGmf6QGEw1SS7TZ8xY7r9IllIEm',
        TRUE,
        CURRENT_TIMESTAMP,
        CURRENT_TIMESTAMP,
        'ALL'
    )
ON CONFLICT (username) DO NOTHING;

INSERT INTO auth_user_group_memberships (user_id, group_id)
SELECT u.id, g.id
FROM auth_user_accounts u
JOIN auth_groups g ON g.code = 'FO'
WHERE u.username = 'fo.trader'
ON CONFLICT DO NOTHING;

INSERT INTO auth_user_group_memberships (user_id, group_id)
SELECT u.id, g.id
FROM auth_user_accounts u
JOIN auth_groups g ON g.code = 'BO'
WHERE u.username = 'bo.ops'
ON CONFLICT DO NOTHING;

INSERT INTO auth_user_group_memberships (user_id, group_id)
SELECT u.id, g.id
FROM auth_user_accounts u
JOIN auth_groups g ON g.code IN ('FO', 'BO', 'ADMIN')
WHERE u.username = 'raul'
ON CONFLICT DO NOTHING;
