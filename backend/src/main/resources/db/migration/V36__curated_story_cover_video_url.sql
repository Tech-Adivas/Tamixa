-- Cover video (MP4) from Sora image-to-video; served via /api/v1/covers/curated_cover_videos/{id}.mp4
ALTER TABLE curated_stories
    ADD COLUMN IF NOT EXISTS cover_video_url VARCHAR(512);
