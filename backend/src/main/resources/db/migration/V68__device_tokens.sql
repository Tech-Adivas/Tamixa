-- Device tokens for push notifications (FCM/APNs)
CREATE TABLE device_tokens (
    id BIGSERIAL PRIMARY KEY,
    parent_id BIGINT NOT NULL REFERENCES parents(id) ON DELETE CASCADE,
    token VARCHAR(512) NOT NULL UNIQUE,
    platform VARCHAR(20) NOT NULL CHECK (platform IN ('ANDROID', 'IOS')),
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    last_used_at TIMESTAMP NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_device_token_parent_id ON device_tokens(parent_id);
CREATE INDEX idx_device_token_token ON device_tokens(token);

COMMENT ON TABLE device_tokens IS 'Push notification device tokens (FCM for Android, APNs for iOS)';
COMMENT ON COLUMN device_tokens.token IS 'FCM registration token or APNs device token';
COMMENT ON COLUMN device_tokens.last_used_at IS 'Updated when token is re-registered or used for push';
