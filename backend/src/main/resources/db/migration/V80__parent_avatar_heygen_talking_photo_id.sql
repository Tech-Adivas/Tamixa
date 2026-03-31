-- Persist HeyGen talking photo ID per avatar so identical avatar images can reuse
-- the same provider-side talking photo and avoid duplicate creation.
ALTER TABLE parent_avatar
    ADD COLUMN IF NOT EXISTS heygen_talking_photo_id VARCHAR(128);
