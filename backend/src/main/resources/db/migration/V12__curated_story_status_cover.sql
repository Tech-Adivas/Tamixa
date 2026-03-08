-- Add status (DRAFT/PUBLISHED) and cover_image_url for content ops workflow
ALTER TABLE curated_stories
    ADD COLUMN IF NOT EXISTS status VARCHAR(20) NOT NULL DEFAULT 'DRAFT',
    ADD COLUMN IF NOT EXISTS cover_image_url VARCHAR(512);

CREATE INDEX IF NOT EXISTS idx_curated_stories_status ON curated_stories(status);
