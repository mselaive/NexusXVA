ALTER TABLE auth_user_accounts
    ADD COLUMN portfolio_access_mode VARCHAR(16) NOT NULL DEFAULT 'ALL';

ALTER TABLE auth_user_accounts
    ADD CONSTRAINT chk_auth_user_portfolio_access_mode
        CHECK (portfolio_access_mode IN ('ALL', 'SELECTED'));

CREATE TABLE auth_feature_permissions (
    code VARCHAR(80) PRIMARY KEY,
    group_code VARCHAR(40) NOT NULL REFERENCES auth_groups (code),
    name VARCHAR(140) NOT NULL,
    description VARCHAR(500) NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE auth_user_feature_permission_overrides (
    user_id UUID NOT NULL REFERENCES auth_user_accounts (id) ON DELETE CASCADE,
    permission_code VARCHAR(80) NOT NULL REFERENCES auth_feature_permissions (code) ON DELETE CASCADE,
    enabled BOOLEAN NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL,
    updated_by_user_id UUID REFERENCES auth_user_accounts (id) ON DELETE SET NULL,
    updated_by_username VARCHAR(120),
    updated_by_display_name VARCHAR(160),
    PRIMARY KEY (user_id, permission_code)
);

CREATE TABLE auth_user_portfolio_access (
    user_id UUID NOT NULL REFERENCES auth_user_accounts (id) ON DELETE CASCADE,
    portfolio_id UUID NOT NULL REFERENCES portfolios (id) ON DELETE CASCADE,
    granted_at TIMESTAMPTZ NOT NULL,
    granted_by_user_id UUID REFERENCES auth_user_accounts (id) ON DELETE SET NULL,
    granted_by_username VARCHAR(120),
    granted_by_display_name VARCHAR(160),
    PRIMARY KEY (user_id, portfolio_id)
);

CREATE INDEX idx_auth_user_portfolio_access_portfolio
    ON auth_user_portfolio_access (portfolio_id);

INSERT INTO auth_feature_permissions (code, group_code, name, description)
VALUES
    ('FO_BOOK_TRADES', 'FO', 'Book trades', 'Submit trade bookings from u-Pad for Back Office validation.'),
    ('FO_CREATE_PORTFOLIOS', 'FO', 'Create portfolios', 'Create new portfolio books.'),
    ('FO_RUN_CVA', 'FO', 'Run CVA', 'Run CVA calculations for visible portfolios.');
