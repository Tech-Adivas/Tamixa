-- Token usage per story for cost and observability.
CREATE TABLE IF NOT EXISTS story_token_usage (
    id BIGSERIAL PRIMARY KEY,
    story_id BIGINT NOT NULL REFERENCES stories(id) ON DELETE CASCADE,
    prompt_tokens INT NOT NULL DEFAULT 0,
    completion_tokens INT NOT NULL DEFAULT 0,
    total_tokens INT NOT NULL DEFAULT 0,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_story_token_usage_story_id ON story_token_usage(story_id);

-- Ensure status column accepts new enum values (PostgreSQL stores as VARCHAR; no change needed if already STRING).
-- REQUESTED, GENERATING, MODERATION_CHECK are new; existing rows remain PENDING/PROCESSING/READY/FAILED.
