-- Educational Features: Reading Levels, Quizzes, Vocabulary, Streaks, Teachers

-- Reading Levels
CREATE TABLE reading_levels (
    id BIGSERIAL PRIMARY KEY,
    child_id BIGINT NOT NULL REFERENCES children(id) ON DELETE CASCADE,
    level INT NOT NULL DEFAULT 1,
    updated_at TIMESTAMP NOT NULL DEFAULT NOW(),
    UNIQUE(child_id)
);
CREATE INDEX idx_reading_levels_child_id ON reading_levels(child_id);

-- Reading Level Assessments (history)
CREATE TABLE reading_level_assessments (
    id BIGSERIAL PRIMARY KEY,
    child_id BIGINT NOT NULL REFERENCES children(id) ON DELETE CASCADE,
    quiz_id BIGINT,
    old_level INT NOT NULL,
    new_level INT NOT NULL,
    assessed_at TIMESTAMP NOT NULL DEFAULT NOW()
);
CREATE INDEX idx_reading_level_assessments_child_id ON reading_level_assessments(child_id);

-- Quizzes (per story)
CREATE TABLE quizzes (
    id BIGSERIAL PRIMARY KEY,
    story_id BIGINT NOT NULL REFERENCES stories(id) ON DELETE CASCADE,
    questions JSONB NOT NULL,
    difficulty_level INT NOT NULL DEFAULT 1,
    created_at TIMESTAMP NOT NULL DEFAULT NOW()
);
CREATE INDEX idx_quizzes_story_id ON quizzes(story_id);

-- Quiz Responses (child answers)
CREATE TABLE quiz_responses (
    id BIGSERIAL PRIMARY KEY,
    quiz_id BIGINT NOT NULL REFERENCES quizzes(id) ON DELETE CASCADE,
    child_id BIGINT NOT NULL REFERENCES children(id) ON DELETE CASCADE,
    answers JSONB NOT NULL,
    score INT NOT NULL,
    max_score INT NOT NULL DEFAULT 100,
    completed_at TIMESTAMP NOT NULL DEFAULT NOW()
);
CREATE INDEX idx_quiz_responses_quiz_id ON quiz_responses(quiz_id);
CREATE INDEX idx_quiz_responses_child_id ON quiz_responses(child_id);

-- Vocabulary Words (global dictionary)
CREATE TABLE vocabulary_words (
    id BIGSERIAL PRIMARY KEY,
    word VARCHAR(255) NOT NULL,
    definition TEXT NOT NULL,
    language VARCHAR(10) NOT NULL,
    difficulty_level INT NOT NULL DEFAULT 1,
    example_sentence TEXT,
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    UNIQUE(word, language)
);
CREATE INDEX idx_vocabulary_words_language ON vocabulary_words(language);
CREATE INDEX idx_vocabulary_words_difficulty ON vocabulary_words(difficulty_level);

-- Child Vocabulary Progress
CREATE TABLE child_vocabulary (
    id BIGSERIAL PRIMARY KEY,
    child_id BIGINT NOT NULL REFERENCES children(id) ON DELETE CASCADE,
    word_id BIGINT NOT NULL REFERENCES vocabulary_words(id) ON DELETE CASCADE,
    mastery_level INT NOT NULL DEFAULT 0,
    learned_at TIMESTAMP NOT NULL DEFAULT NOW(),
    UNIQUE(child_id, word_id)
);
CREATE INDEX idx_child_vocabulary_child_id ON child_vocabulary(child_id);
CREATE INDEX idx_child_vocabulary_word_id ON child_vocabulary(word_id);

-- Reading Streaks
CREATE TABLE reading_streaks (
    id BIGSERIAL PRIMARY KEY,
    child_id BIGINT NOT NULL REFERENCES children(id) ON DELETE CASCADE,
    current_streak INT NOT NULL DEFAULT 0,
    longest_streak INT NOT NULL DEFAULT 0,
    last_read_at TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT NOW(),
    UNIQUE(child_id)
);
CREATE INDEX idx_reading_streaks_child_id ON reading_streaks(child_id);

-- Teachers (parent role extension)
CREATE TABLE teachers (
    id BIGSERIAL PRIMARY KEY,
    parent_id BIGINT NOT NULL REFERENCES parents(id) ON DELETE CASCADE,
    school_name VARCHAR(255),
    verified_at TIMESTAMP,
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    UNIQUE(parent_id)
);
CREATE INDEX idx_teachers_parent_id ON teachers(parent_id);

-- Classrooms
CREATE TABLE classrooms (
    id BIGSERIAL PRIMARY KEY,
    teacher_id BIGINT NOT NULL REFERENCES teachers(id) ON DELETE CASCADE,
    name VARCHAR(255) NOT NULL,
    code VARCHAR(10) NOT NULL UNIQUE,
    subject VARCHAR(100),
    grade_level VARCHAR(50),
    created_at TIMESTAMP NOT NULL DEFAULT NOW()
);
CREATE INDEX idx_classrooms_teacher_id ON classrooms(teacher_id);
CREATE INDEX idx_classrooms_code ON classrooms(code);

-- Classroom Students
CREATE TABLE classroom_students (
    id BIGSERIAL PRIMARY KEY,
    classroom_id BIGINT NOT NULL REFERENCES classrooms(id) ON DELETE CASCADE,
    child_id BIGINT NOT NULL REFERENCES children(id) ON DELETE CASCADE,
    joined_at TIMESTAMP NOT NULL DEFAULT NOW(),
    UNIQUE(classroom_id, child_id)
);
CREATE INDEX idx_classroom_students_classroom_id ON classroom_students(classroom_id);
CREATE INDEX idx_classroom_students_child_id ON classroom_students(child_id);

-- Story Tags (curriculum alignment)
CREATE TABLE story_tags (
    id BIGSERIAL PRIMARY KEY,
    story_id BIGINT NOT NULL REFERENCES stories(id) ON DELETE CASCADE,
    tag VARCHAR(100) NOT NULL,
    category VARCHAR(50) NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT NOW()
);
CREATE INDEX idx_story_tags_story_id ON story_tags(story_id);
CREATE INDEX idx_story_tags_category ON story_tags(category);
CREATE INDEX idx_story_tags_tag ON story_tags(tag);
