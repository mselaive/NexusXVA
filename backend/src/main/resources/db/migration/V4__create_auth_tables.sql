CREATE TABLE auth_user_accounts (
    id UUID PRIMARY KEY,
    username VARCHAR(80) NOT NULL UNIQUE,
    display_name VARCHAR(140) NOT NULL,
    password_hash VARCHAR(120) NOT NULL,
    active BOOLEAN NOT NULL,
    created_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL,
    last_login_at TIMESTAMPTZ
);

CREATE TABLE auth_groups (
    id UUID PRIMARY KEY,
    code VARCHAR(40) NOT NULL UNIQUE,
    name VARCHAR(120) NOT NULL,
    description VARCHAR(500),
    system_group BOOLEAN NOT NULL,
    created_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL
);

CREATE TABLE auth_user_group_memberships (
    user_id UUID NOT NULL REFERENCES auth_user_accounts (id) ON DELETE CASCADE,
    group_id UUID NOT NULL REFERENCES auth_groups (id) ON DELETE CASCADE,
    PRIMARY KEY (user_id, group_id)
);

CREATE TABLE auth_sessions (
    id UUID PRIMARY KEY,
    user_id UUID NOT NULL REFERENCES auth_user_accounts (id) ON DELETE CASCADE,
    token_hash CHAR(64) NOT NULL UNIQUE,
    csrf_token VARCHAR(80) NOT NULL,
    created_at TIMESTAMPTZ NOT NULL,
    expires_at TIMESTAMPTZ NOT NULL,
    revoked_at TIMESTAMPTZ
);

CREATE INDEX idx_auth_sessions_user_id
    ON auth_sessions (user_id);

CREATE INDEX idx_auth_sessions_expires_at
    ON auth_sessions (expires_at);

INSERT INTO auth_groups (id, code, name, description, system_group, created_at, updated_at)
VALUES
    ('00000000-0000-0000-0000-0000000000a1', 'FO', 'Front Office', 'Trading and front-office users who can book and value trades.', TRUE, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    ('00000000-0000-0000-0000-0000000000b1', 'BO', 'Back Office', 'Operations and back-office users who review booked trades and lifecycle data.', TRUE, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    ('00000000-0000-0000-0000-0000000000c1', 'ADMIN', 'Admin', 'Administrators who can manage users, groups and platform settings.', TRUE, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP);
