-- Story slideshow ("visuals"): a few AI pictures per library story, shown as timed camera shots during narration.
-- Pictures are planned on the source-language narration text and shared by all languages and voices; shot start
-- times are computed at playback from each voice's word timings. New tables only; no existing table changes.
-- Rollback: DROP TABLE story_visual_shots, story_visual_images, story_visual_sets.

CREATE TABLE IF NOT EXISTS story_visual_sets (
    id                   BIGSERIAL PRIMARY KEY,
    story_id             BIGINT       NOT NULL UNIQUE REFERENCES library_stories (id) ON DELETE CASCADE,
    -- PLANNING | GENERATING | READY | FAILED
    status               VARCHAR(20)  NOT NULL DEFAULT 'PLANNING',
    source_language      VARCHAR(10)  NOT NULL,
    -- Word count of the source text the shots' start_word values refer to.
    source_word_count    INT          NOT NULL DEFAULT 0,
    -- Characters with appearance/outfits as planned (JSON), used for every picture prompt.
    characters_json      TEXT,
    character_sheet_path VARCHAR(512),
    last_error           VARCHAR(1000),
    -- Children only see the slideshow once an admin has approved it.
    approved_at          TIMESTAMP WITH TIME ZONE,
    created_at           TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT now(),
    updated_at           TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT now()
);

CREATE TABLE IF NOT EXISTS story_visual_images (
    id            BIGSERIAL PRIMARY KEY,
    set_id        BIGINT       NOT NULL REFERENCES story_visual_sets (id) ON DELETE CASCADE,
    image_key     VARCHAR(20)  NOT NULL,
    story_moment  TEXT,
    prompt        TEXT         NOT NULL,
    -- [{"name":"Mia","region":"left"}, ...] so the app can zoom/pan to each subject.
    subjects_json TEXT,
    image_path    VARCHAR(512),
    -- PENDING | READY | FAILED
    status        VARCHAR(20)  NOT NULL DEFAULT 'PENDING',
    last_error    VARCHAR(1000),
    created_at    TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT now(),
    updated_at    TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT now(),
    CONSTRAINT uq_story_visual_images_key UNIQUE (set_id, image_key)
);

CREATE TABLE IF NOT EXISTS story_visual_shots (
    id              BIGSERIAL PRIMARY KEY,
    set_id          BIGINT       NOT NULL REFERENCES story_visual_sets (id) ON DELETE CASCADE,
    shot_index      INT          NOT NULL,
    image_key       VARCHAR(20)  NOT NULL,
    narration_start TEXT         NOT NULL,
    -- Index (0-based) of the first word of this shot in the source text.
    start_word      INT          NOT NULL,
    -- wide | medium | close_up
    camera          VARCHAR(20)  NOT NULL,
    -- left | center | right | top | bottom | top_left | ... ; NULL = whole picture
    focus           VARCHAR(20),
    -- none | slow_zoom_in | slow_zoom_out | pan_left | pan_right | pan_up | pan_down
    movement        VARCHAR(20)  NOT NULL,
    CONSTRAINT uq_story_visual_shots_index UNIQUE (set_id, shot_index)
);

CREATE INDEX IF NOT EXISTS idx_story_visual_images_set ON story_visual_images (set_id);
CREATE INDEX IF NOT EXISTS idx_story_visual_shots_set ON story_visual_shots (set_id);
