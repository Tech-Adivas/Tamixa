-- Parental controls per child (usage limits, content filters)
CREATE TABLE parental_control (
    id BIGSERIAL PRIMARY KEY,
    child_id BIGINT NOT NULL REFERENCES children(id) ON DELETE CASCADE UNIQUE,
    daily_story_limit INT,
    blocked_themes TEXT,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_parental_control_child ON parental_control(child_id);

-- Data export jobs for GDPR/DPDP
CREATE TABLE data_export_job (
    id BIGSERIAL PRIMARY KEY,
    parent_id BIGINT NOT NULL REFERENCES parents(id) ON DELETE CASCADE,
    status VARCHAR(20) NOT NULL DEFAULT 'pending',
    requested_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    completed_at TIMESTAMPTZ,
    download_url TEXT,
    expires_at TIMESTAMPTZ
);

CREATE INDEX idx_data_export_job_parent ON data_export_job(parent_id);
CREATE INDEX idx_data_export_job_status ON data_export_job(status);
