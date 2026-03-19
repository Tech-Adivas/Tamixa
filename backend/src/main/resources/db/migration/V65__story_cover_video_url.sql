-- Animated GIF cover for generated stories (Sora image-to-video). Served via /api/v1/covers/generated_cover_videos/{id}.gif
ALTER TABLE stories ADD COLUMN IF NOT EXISTS cover_video_url VARCHAR(512);
