-- Enterprise monetization: fraud protection, revenue intelligence, audit safety.
-- Financial safety: all changes are additive; existing subscription logic unchanged.

-- 1) Device fingerprint tracking: one device per account for trial eligibility.
CREATE TABLE IF NOT EXISTS devices (
    id BIGSERIAL PRIMARY KEY,
    parent_id BIGINT NOT NULL REFERENCES parents(id) ON DELETE CASCADE,
    device_hash VARCHAR(64) NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);
CREATE INDEX idx_devices_parent_id ON devices(parent_id);
CREATE INDEX idx_devices_hash_parent ON devices(device_hash, parent_id);
CREATE UNIQUE INDEX idx_devices_parent_hash ON devices(parent_id, device_hash);

-- 2) Trial abuse: track trial usage by parent (one trial per account).
ALTER TABLE subscriptions ADD COLUMN IF NOT EXISTS trial_used BOOLEAN NOT NULL DEFAULT FALSE;

-- 3) Trial usage tracking: device+email combination for abuse detection.
CREATE TABLE IF NOT EXISTS trial_usage (
    id BIGSERIAL PRIMARY KEY,
    parent_id BIGINT NOT NULL REFERENCES parents(id) ON DELETE CASCADE,
    device_hash VARCHAR(64) NOT NULL,
    email_hash VARCHAR(64) NOT NULL,
    used_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);
CREATE INDEX idx_trial_usage_device_email ON trial_usage(device_hash, email_hash);
CREATE INDEX idx_trial_usage_parent ON trial_usage(parent_id);

-- 4) Revenue intelligence: daily snapshot for MRR, churn, conversion.
CREATE TABLE IF NOT EXISTS revenue_snapshots (
    id BIGSERIAL PRIMARY KEY,
    snapshot_date DATE NOT NULL UNIQUE,
    mrr_minor BIGINT NOT NULL DEFAULT 0,
    active_subscriptions INT NOT NULL DEFAULT 0,
    churn_rate DECIMAL(5,4) NOT NULL DEFAULT 0,
    trial_conversion_rate DECIMAL(5,4) NOT NULL DEFAULT 0,
    arpu_minor BIGINT NOT NULL DEFAULT 0,
    stories_free INT NOT NULL DEFAULT 0,
    stories_premium INT NOT NULL DEFAULT 0,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);
CREATE INDEX idx_revenue_snapshots_date ON revenue_snapshots(snapshot_date);

-- 5) Subscription reconciliation: compare DB vs provider status for audit.
CREATE TABLE IF NOT EXISTS subscription_reconciliation_log (
    id BIGSERIAL PRIMARY KEY,
    subscription_id BIGINT NOT NULL REFERENCES subscriptions(id) ON DELETE CASCADE,
    provider_status VARCHAR(32) NOT NULL,
    db_status VARCHAR(32) NOT NULL,
    mismatch_flag BOOLEAN NOT NULL DEFAULT FALSE,
    checked_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);
CREATE INDEX idx_reconciliation_subscription ON subscription_reconciliation_log(subscription_id);
CREATE INDEX idx_reconciliation_checked_at ON subscription_reconciliation_log(checked_at);
CREATE INDEX idx_reconciliation_mismatch ON subscription_reconciliation_log(mismatch_flag) WHERE mismatch_flag = TRUE;
