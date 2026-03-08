-- Child achievements/badges
CREATE TABLE child_achievement (
    id BIGSERIAL PRIMARY KEY,
    child_id BIGINT NOT NULL REFERENCES children(id) ON DELETE CASCADE,
    achievement_type VARCHAR(50) NOT NULL,
    earned_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    metadata JSONB
);

CREATE INDEX idx_child_achievement_child ON child_achievement(child_id);

-- Reading goals per child
CREATE TABLE reading_goal (
    id BIGSERIAL PRIMARY KEY,
    child_id BIGINT NOT NULL REFERENCES children(id) ON DELETE CASCADE,
    goal_type VARCHAR(30) NOT NULL DEFAULT 'stories_per_week',
    target_value INT NOT NULL,
    current_value INT NOT NULL DEFAULT 0,
    period_start DATE NOT NULL,
    period_end DATE NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    UNIQUE(child_id, goal_type, period_start)
);

CREATE INDEX idx_reading_goal_child ON reading_goal(child_id);

-- Feedback from parents
CREATE TABLE story_feedback (
    id BIGSERIAL PRIMARY KEY,
    parent_id BIGINT NOT NULL REFERENCES parents(id) ON DELETE CASCADE,
    story_id BIGINT,
    story_source VARCHAR(20) DEFAULT 'generated',
    rating INT CHECK (rating >= 1 AND rating <= 5),
    comment TEXT,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_story_feedback_parent ON story_feedback(parent_id);
CREATE INDEX idx_story_feedback_story ON story_feedback(story_id);
