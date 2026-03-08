-- Admin audit trail: in-app store for admin actions (flag, approve, reject, suspend, refund, delete).
CREATE TABLE IF NOT EXISTS admin_audit (
    id BIGSERIAL PRIMARY KEY,
    admin_email VARCHAR(255) NOT NULL,
    action VARCHAR(100) NOT NULL,
    resource_type VARCHAR(50) NOT NULL,
    resource_id VARCHAR(100),
    details TEXT,
    trace_id VARCHAR(100),
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_admin_audit_created_at ON admin_audit(created_at DESC);
CREATE INDEX idx_admin_audit_action ON admin_audit(action);
CREATE INDEX idx_admin_audit_resource ON admin_audit(resource_type, resource_id);
