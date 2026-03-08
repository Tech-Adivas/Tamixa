-- Medium priority features: Voice cloning, Avatar video, Soundscapes, Subscription tiers

-- Soundscapes table
CREATE TABLE IF NOT EXISTS soundscapes (
    id BIGSERIAL PRIMARY KEY,
    name VARCHAR(255) NOT NULL,
    category VARCHAR(50) NOT NULL,
    duration_seconds INT NOT NULL,
    audio_url VARCHAR(500) NOT NULL,
    description VARCHAR(1000),
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_soundscapes_category ON soundscapes(category);
CREATE INDEX IF NOT EXISTS idx_soundscapes_name ON soundscapes(name);

-- Soundscapes usage table
CREATE TABLE IF NOT EXISTS soundscapes_usage (
    id BIGSERIAL PRIMARY KEY,
    parent_id BIGINT NOT NULL,
    story_id BIGINT,
    soundscape_id BIGINT NOT NULL,
    usage_count INT NOT NULL DEFAULT 1,
    last_used_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    FOREIGN KEY (parent_id) REFERENCES parents(id) ON DELETE CASCADE,
    FOREIGN KEY (story_id) REFERENCES stories(id) ON DELETE SET NULL,
    FOREIGN KEY (soundscape_id) REFERENCES soundscapes(id) ON DELETE CASCADE,
    UNIQUE(parent_id, soundscape_id)
);

CREATE INDEX IF NOT EXISTS idx_soundscapes_usage_parent ON soundscapes_usage(parent_id);
CREATE INDEX IF NOT EXISTS idx_soundscapes_usage_story ON soundscapes_usage(story_id);

-- Voice cloning jobs table
CREATE TABLE IF NOT EXISTS voice_cloning_jobs (
    id BIGSERIAL PRIMARY KEY,
    parent_id BIGINT NOT NULL,
    audio_storage_path VARCHAR(500) NOT NULL,
    audio_file_size_bytes BIGINT NOT NULL,
    voice_name VARCHAR(255) NOT NULL,
    eleven_labs_voice_id VARCHAR(255),
    status VARCHAR(50) NOT NULL,
    error_message VARCHAR(1000),
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    completed_at TIMESTAMP WITH TIME ZONE,
    FOREIGN KEY (parent_id) REFERENCES parents(id) ON DELETE CASCADE
);

CREATE INDEX IF NOT EXISTS idx_voice_cloning_jobs_parent ON voice_cloning_jobs(parent_id);
CREATE INDEX IF NOT EXISTS idx_voice_cloning_jobs_status ON voice_cloning_jobs(status);

-- Subscription tiers table
CREATE TABLE IF NOT EXISTS subscription_tiers (
    id BIGSERIAL PRIMARY KEY,
    name VARCHAR(100) NOT NULL UNIQUE,
    price_monthly BIGINT NOT NULL,
    price_yearly BIGINT NOT NULL,
    max_children INT NOT NULL,
    max_voices INT NOT NULL,
    max_avatar_videos INT NOT NULL,
    max_soundscapes INT NOT NULL,
    allows_voice_cloning BOOLEAN NOT NULL,
    allows_avatar_video BOOLEAN NOT NULL,
    allows_soundscapes BOOLEAN NOT NULL,
    allows_family_sharing BOOLEAN NOT NULL,
    analytics_enabled BOOLEAN NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW()
);

-- Insert default tiers
INSERT INTO subscription_tiers (
    name, price_monthly, price_yearly, max_children, max_voices, max_avatar_videos,
    max_soundscapes, allows_voice_cloning, allows_avatar_video, allows_soundscapes,
    allows_family_sharing, analytics_enabled
) VALUES
    ('FREE', 0, 0, 1, 0, 0, 0, false, false, false, false, false),
    ('PREMIUM', 999, 9999, 3, 3, 10, 20, true, true, true, false, true),
    ('FAMILY', 1999, 19999, 10, 10, 50, 100, true, true, true, true, true)
ON CONFLICT (name) DO NOTHING;
