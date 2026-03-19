-- Track when share_clip_downloaded analytics event was emitted (idempotency).
ALTER TABLE shareable_clips ADD COLUMN IF NOT EXISTS download_analytics_emitted_at TIMESTAMPTZ;

COMMENT ON COLUMN shareable_clips.download_analytics_emitted_at IS 'When share_clip_downloaded analytics event was emitted (once per clip)';
