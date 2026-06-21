CREATE TABLE user_notifications (
    id UUID PRIMARY KEY,
    recipient_user_id UUID NOT NULL REFERENCES auth_user_accounts (id) ON DELETE CASCADE,
    notification_type VARCHAR(80) NOT NULL,
    title VARCHAR(160) NOT NULL,
    message VARCHAR(800) NOT NULL,
    link_path VARCHAR(300),
    created_at TIMESTAMPTZ NOT NULL,
    read_at TIMESTAMPTZ
);

CREATE INDEX idx_user_notifications_recipient_created
    ON user_notifications (recipient_user_id, created_at DESC);

CREATE INDEX idx_user_notifications_unread
    ON user_notifications (recipient_user_id, read_at)
    WHERE read_at IS NULL;
