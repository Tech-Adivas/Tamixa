-- Human verification: mark when a narrated story was approved for final delivery.
ALTER TABLE curated_stories
    ADD COLUMN IF NOT EXISTS narration_approved_at TIMESTAMP;
