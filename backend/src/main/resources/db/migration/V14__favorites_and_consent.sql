-- Parent/child favorites for stories (curated or generated)
CREATE TABLE favorite_story (
    id BIGSERIAL PRIMARY KEY,
    parent_id BIGINT NOT NULL REFERENCES parents(id) ON DELETE CASCADE,
    story_id BIGINT NOT NULL,
    story_source VARCHAR(20) NOT NULL DEFAULT 'generated',
    child_id BIGINT REFERENCES children(id) ON DELETE CASCADE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    UNIQUE(parent_id, story_id)
);

CREATE INDEX idx_favorite_story_parent ON favorite_story(parent_id);
CREATE INDEX idx_favorite_story_child ON favorite_story(child_id);

-- Parent consent for compliance (DPDP, COPPA, GDPR)
CREATE TABLE parent_consent (
    id BIGSERIAL PRIMARY KEY,
    parent_id BIGINT NOT NULL REFERENCES parents(id) ON DELETE CASCADE,
    consent_type VARCHAR(50) NOT NULL,
    version INT NOT NULL DEFAULT 1,
    granted_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    ip_address VARCHAR(45),
    user_agent TEXT
);

CREATE INDEX idx_parent_consent_parent ON parent_consent(parent_id);
CREATE INDEX idx_parent_consent_type ON parent_consent(consent_type);
