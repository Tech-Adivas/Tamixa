-- Subscription state machine: align legacy status values with new enum (TRIAL, GRACE_PERIOD).
-- Financial audit: add reason to subscription_events; add webhook_events table for audit and replay protection.

UPDATE subscriptions SET status = 'TRIAL' WHERE status = 'TRIALING';
UPDATE subscriptions SET status = 'GRACE_PERIOD' WHERE status = 'IN_GRACE_PERIOD';

UPDATE subscription_events SET previous_status = 'TRIAL' WHERE previous_status = 'TRIALING';
UPDATE subscription_events SET new_status = 'TRIAL' WHERE new_status = 'TRIALING';
UPDATE subscription_events SET previous_status = 'GRACE_PERIOD' WHERE previous_status = 'IN_GRACE_PERIOD';
UPDATE subscription_events SET new_status = 'GRACE_PERIOD' WHERE new_status = 'IN_GRACE_PERIOD';

ALTER TABLE subscription_events ADD COLUMN IF NOT EXISTS reason TEXT;

-- Webhook event audit: every webhook logged; duplicate event_id rejected via idempotency table.
-- Raw payload stored encrypted for dispute resolution and replay prevention.
CREATE TABLE IF NOT EXISTS webhook_events (
    id BIGSERIAL PRIMARY KEY,
    provider VARCHAR(16) NOT NULL,
    event_id VARCHAR(255) NOT NULL,
    event_type VARCHAR(128) NOT NULL,
    processed_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    status VARCHAR(32) NOT NULL DEFAULT 'PROCESSED',
    raw_payload_encrypted TEXT,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    CONSTRAINT uq_webhook_events_provider_event_id UNIQUE (provider, event_id)
);
CREATE INDEX idx_webhook_events_provider_event ON webhook_events(provider, event_id);
CREATE INDEX idx_webhook_events_processed_at ON webhook_events(processed_at);
