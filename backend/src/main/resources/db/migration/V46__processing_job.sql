-- Generic processing job table for AI workflow tracking.
-- Supports story polish, TTS generation, voice cloning, avatar video, etc.
-- Used for admin UX (AI progress) and job status APIs.

CREATE TABLE IF NOT EXISTS processing_job (
    id BIGSERIAL PRIMARY KEY,
    job_type VARCHAR(50) NOT NULL,
    resource_type VARCHAR(32) NOT NULL,
    resource_id VARCHAR(128) NOT NULL,
    status VARCHAR(30) NOT NULL DEFAULT 'PENDING',
    progress INT NOT NULL DEFAULT 0,
    started_at TIMESTAMPTZ,
    finished_at TIMESTAMPTZ,
    error_message TEXT,
    metadata TEXT,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_processing_job_resource ON processing_job(resource_type, resource_id);
CREATE INDEX idx_processing_job_status ON processing_job(status);
CREATE INDEX idx_processing_job_created ON processing_job(created_at);
CREATE INDEX idx_processing_job_job_type ON processing_job(job_type);

COMMENT ON TABLE processing_job IS 'Generic AI/workflow job tracking for admin UX and status APIs';
