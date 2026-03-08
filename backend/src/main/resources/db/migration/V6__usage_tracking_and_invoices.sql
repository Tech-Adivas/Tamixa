-- Usage tracking: server-side enforcement of FREE (stories/month) and voice generations.
CREATE TABLE IF NOT EXISTS usage_tracking (
    id BIGSERIAL PRIMARY KEY,
    parent_id BIGINT NOT NULL REFERENCES parents(id) ON DELETE CASCADE,
    month VARCHAR(7) NOT NULL,
    stories_generated INT NOT NULL DEFAULT 0,
    voice_generations INT NOT NULL DEFAULT 0,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    CONSTRAINT uq_usage_tracking_parent_month UNIQUE (parent_id, month)
);
CREATE INDEX idx_usage_tracking_parent_month ON usage_tracking(parent_id, month);

-- Invoices: link to subscription for financial audit.
CREATE TABLE IF NOT EXISTS invoices (
    id BIGSERIAL PRIMARY KEY,
    provider_invoice_id VARCHAR(255) NOT NULL,
    parent_id BIGINT NOT NULL REFERENCES parents(id) ON DELETE CASCADE,
    subscription_id BIGINT REFERENCES subscriptions(id) ON DELETE SET NULL,
    amount_minor BIGINT NOT NULL,
    currency VARCHAR(8) NOT NULL,
    status VARCHAR(32) NOT NULL DEFAULT 'PENDING',
    paid_at TIMESTAMPTZ,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    CONSTRAINT uq_invoices_provider_invoice_id UNIQUE (provider_invoice_id)
);
CREATE INDEX idx_invoices_parent_id ON invoices(parent_id);
CREATE INDEX idx_invoices_subscription_id ON invoices(subscription_id);
CREATE INDEX idx_invoices_provider_invoice_id ON invoices(provider_invoice_id);
