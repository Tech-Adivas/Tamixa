-- Subscription and billing schema for Araro (Stripe + Razorpay).
-- Parent table 'parents' must exist (FK reference).

CREATE TABLE IF NOT EXISTS subscriptions (
    id BIGSERIAL PRIMARY KEY,
    parent_id BIGINT NOT NULL REFERENCES parents(id) ON DELETE CASCADE,
    plan VARCHAR(32) NOT NULL,
    status VARCHAR(32) NOT NULL,
    provider VARCHAR(16) NOT NULL,
    external_subscription_id VARCHAR(255),
    external_customer_id VARCHAR(255),
    current_period_start TIMESTAMPTZ,
    current_period_end TIMESTAMPTZ,
    trial_end TIMESTAMPTZ,
    cancel_at_period_end BOOLEAN NOT NULL DEFAULT FALSE,
    canceled_at TIMESTAMPTZ,
    past_due_at TIMESTAMPTZ,
    max_children INT NOT NULL DEFAULT 1,
    voice_premium BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    CONSTRAINT uq_subscriptions_parent_id UNIQUE (parent_id)
);

CREATE INDEX idx_subscriptions_parent_id ON subscriptions(parent_id);
CREATE INDEX idx_subscriptions_external ON subscriptions(provider, external_subscription_id);
CREATE INDEX idx_subscriptions_status ON subscriptions(status);

CREATE TABLE IF NOT EXISTS subscription_events (
    id BIGSERIAL PRIMARY KEY,
    subscription_id BIGINT NOT NULL REFERENCES subscriptions(id) ON DELETE CASCADE,
    event_type VARCHAR(64) NOT NULL,
    previous_status VARCHAR(32),
    new_status VARCHAR(32),
    provider VARCHAR(16) NOT NULL,
    external_event_id VARCHAR(255),
    payload TEXT,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_subscription_events_subscription_id ON subscription_events(subscription_id);
CREATE INDEX idx_subscription_events_created_at ON subscription_events(created_at);
CREATE UNIQUE INDEX idx_subscription_events_external ON subscription_events(provider, external_event_id) WHERE external_event_id IS NOT NULL;

CREATE TABLE IF NOT EXISTS billing_audit_log (
    id BIGSERIAL PRIMARY KEY,
    parent_id BIGINT NOT NULL REFERENCES parents(id) ON DELETE CASCADE,
    subscription_id BIGINT REFERENCES subscriptions(id) ON DELETE SET NULL,
    event_type VARCHAR(48) NOT NULL,
    provider VARCHAR(16) NOT NULL,
    external_id VARCHAR(255),
    amount_minor BIGINT,
    currency VARCHAR(8),
    invoice_id VARCHAR(255),
    details TEXT,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_billing_audit_parent_id ON billing_audit_log(parent_id);
CREATE INDEX idx_billing_audit_subscription_id ON billing_audit_log(subscription_id);
CREATE INDEX idx_billing_audit_created_at ON billing_audit_log(created_at);
CREATE INDEX idx_billing_audit_event_type ON billing_audit_log(event_type);

-- Idempotency: one row per provider+event_id so webhooks are processed at most once.
CREATE TABLE IF NOT EXISTS webhook_idempotency (
    id BIGSERIAL PRIMARY KEY,
    provider VARCHAR(16) NOT NULL,
    event_id VARCHAR(255) NOT NULL,
    processed_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    CONSTRAINT uq_webhook_idempotency UNIQUE (provider, event_id)
);

CREATE INDEX idx_webhook_idempotency_provider_event ON webhook_idempotency(provider, event_id);
