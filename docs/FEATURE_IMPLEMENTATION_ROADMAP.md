# Tamixa Educational Features - Implementation Roadmap

**Goal:** Expand Tamixa from kids' stories to school students with personalized learning, engagement tracking, and teacher tools.

**Timeline:** 4 weeks (phased rollout)

---

## Phase 1: Foundation (Week 1-2) — Reading Levels & Quizzes

### 1.1 Reading Level System
- **Backend:** ReadingLevelService, ReadingLevelEntity, ReadingLevelAssessmentEntity
- **Mobile:** ReadingLevelScreen, ReadingLevelViewModel
- **Web:** ReadingLevelDashboard.tsx
- **Impact:** Enables personalized story recommendations

### 1.2 Story Quizzes
- **Backend:** QuizService, QuizEntity, QuizResponseEntity
- **Mobile:** QuizScreen, QuizViewModel
- **Web:** QuizManagement.tsx (admin), QuizResults.tsx (parent)
- **Impact:** Measures comprehension, drives engagement

### 1.3 Vocabulary Tracker
- **Backend:** VocabularyService, VocabularyWordEntity, ChildVocabularyEntity
- **Mobile:** VocabularyScreen, VocabularyViewModel
- **Web:** VocabularyDashboard.tsx
- **Impact:** Tracks learning outcomes, builds word mastery

---

## Phase 2: Engagement (Week 2-3) — Gamification & Streaks

### 2.1 Reading Streaks
- **Backend:** ReadingStreakService, ReadingStreakEntity
- **Mobile:** StreakWidget, StreakScreen
- **Web:** StreakDashboard.tsx
- **Impact:** Daily engagement, habit formation

### 2.2 Achievement Badges
- **Backend:** Extend AchievementService with new badge types
- **Mobile:** AchievementBadgeScreen
- **Web:** AchievementDashboard.tsx
- **Impact:** Motivation, social sharing

### 2.3 Leaderboards (Optional)
- **Backend:** LeaderboardService
- **Mobile:** LeaderboardScreen
- **Web:** LeaderboardDashboard.tsx
- **Impact:** Friendly competition (opt-in)

---

## Phase 3: School Integration (Week 3-4) — Teacher Dashboard

### 3.1 Teacher Accounts & Classrooms
- **Backend:** TeacherService, ClassroomEntity, ClassroomStudentEntity
- **Web:** TeacherDashboard, ClassroomManagement
- **Impact:** School adoption, bulk management

### 3.2 Curriculum Alignment
- **Backend:** CurriculumService, StoryTagEntity
- **Mobile:** CurriculumBrowseScreen
- **Web:** CurriculumManagement.tsx
- **Impact:** Subject-based discovery

### 3.3 Class Analytics
- **Backend:** ClassAnalyticsService
- **Web:** ClassAnalyticsDashboard.tsx
- **Impact:** Teacher insights, progress tracking

---

## Database Schema Changes

### New Tables

```sql
-- Reading Levels
CREATE TABLE reading_levels (
  id BIGSERIAL PRIMARY KEY,
  child_id BIGINT NOT NULL REFERENCES children(id),
  level INT NOT NULL DEFAULT 1,
  updated_at TIMESTAMP NOT NULL DEFAULT NOW()
);

-- Quizzes
CREATE TABLE quizzes (
  id BIGSERIAL PRIMARY KEY,
  story_id BIGINT NOT NULL REFERENCES stories(id),
  questions JSONB NOT NULL,
  difficulty_level INT NOT NULL,
  created_at TIMESTAMP NOT NULL DEFAULT NOW()
);

CREATE TABLE quiz_responses (
  id BIGSERIAL PRIMARY KEY,
  quiz_id BIGINT NOT NULL REFERENCES quizzes(id),
  child_id BIGINT NOT NULL REFERENCES children(id),
  answers JSONB NOT NULL,
  score INT NOT NULL,
  completed_at TIMESTAMP NOT NULL DEFAULT NOW()
);

-- Vocabulary
CREATE TABLE vocabulary_words (
  id BIGSERIAL PRIMARY KEY,
  word VARCHAR(255) NOT NULL,
  definition TEXT NOT NULL,
  language VARCHAR(10) NOT NULL,
  difficulty_level INT NOT NULL,
  created_at TIMESTAMP NOT NULL DEFAULT NOW()
);

CREATE TABLE child_vocabulary (
  id BIGSERIAL PRIMARY KEY,
  child_id BIGINT NOT NULL REFERENCES children(id),
  word_id BIGINT NOT NULL REFERENCES vocabulary_words(id),
  mastery_level INT NOT NULL DEFAULT 0,
  learned_at TIMESTAMP NOT NULL DEFAULT NOW()
);

-- Reading Streaks
CREATE TABLE reading_streaks (
  id BIGSERIAL PRIMARY KEY,
  child_id BIGINT NOT NULL REFERENCES children(id),
  current_streak INT NOT NULL DEFAULT 0,
  longest_streak INT NOT NULL DEFAULT 0,
  last_read_at TIMESTAMP,
  updated_at TIMESTAMP NOT NULL DEFAULT NOW()
);

-- Teachers & Classrooms
CREATE TABLE teachers (
  id BIGSERIAL PRIMARY KEY,
  parent_id BIGINT NOT NULL REFERENCES parents(id),
  school_name VARCHAR(255),
  verified_at TIMESTAMP,
  created_at TIMESTAMP NOT NULL DEFAULT NOW()
);

CREATE TABLE classrooms (
  id BIGSERIAL PRIMARY KEY,
  teacher_id BIGINT NOT NULL REFERENCES teachers(id),
  name VARCHAR(255) NOT NULL,
  code VARCHAR(10) NOT NULL UNIQUE,
  created_at TIMESTAMP NOT NULL DEFAULT NOW()
);

CREATE TABLE classroom_students (
  id BIGSERIAL PRIMARY KEY,
  classroom_id BIGINT NOT NULL REFERENCES classrooms(id),
  child_id BIGINT NOT NULL REFERENCES children(id),
  joined_at TIMESTAMP NOT NULL DEFAULT NOW()
);
```

---

## API Endpoints (Phase 1)

### Reading Levels
- `GET /api/v1/reading-levels/{childId}` - Get child's reading level
- `POST /api/v1/reading-levels/{childId}/assess` - Assess and update level

### Quizzes
- `GET /api/v1/stories/{storyId}/quiz` - Get quiz for story
- `POST /api/v1/quizzes/{quizId}/submit` - Submit quiz answers
- `GET /api/v1/quizzes/results` - Get quiz results for child

### Vocabulary
- `GET /api/v1/vocabulary/words` - Get vocabulary words
- `POST /api/v1/vocabulary/learn` - Mark word as learned
- `GET /api/v1/vocabulary/progress` - Get vocabulary progress

---

## Success Metrics

| Feature | Metric | Target |
|---------|--------|--------|
| **Reading Levels** | % of users with assigned level | 80% |
| **Quizzes** | Avg quiz completion rate | 60% |
| **Vocabulary** | Avg words learned per child | 50/month |
| **Streaks** | % with 7+ day streak | 40% |
| **Teacher Adoption** | # of classrooms created | 100 in Q1 |

---

## Implementation Order

1. **Backend entities & migrations** (Day 1)
2. **Backend services & controllers** (Day 2-3)
3. **Mobile screens & ViewModels** (Day 3-4)
4. **Web pages & components** (Day 4-5)
5. **Integration & testing** (Day 5-6)
6. **Admin dashboard updates** (Day 6-7)

---

## Rollout Strategy

- **Week 1:** Beta with 10% of users (reading levels + quizzes)
- **Week 2:** Expand to 50% (add vocabulary)
- **Week 3:** Full rollout (add streaks + badges)
- **Week 4:** Teacher beta (classroom management)

---

## Risks & Mitigations

| Risk | Mitigation |
|------|-----------|
| Quiz generation quality | Use OpenAI with prompt engineering |
| Teacher adoption friction | Provide classroom code + invite links |
| Data migration | Backfill reading levels from story history |
| Performance | Index quiz_responses, cache leaderboards |

