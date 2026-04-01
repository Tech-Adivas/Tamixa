# Educational Features Implementation Guide

This guide documents the new educational features added to Tamixa to support school students and personalized learning.

## Overview

The educational features module adds:
- **Reading Levels** - Adaptive difficulty tracking
- **Story Quizzes** - Comprehension assessment
- **Vocabulary Tracking** - Word mastery progression
- **Reading Streaks** - Engagement gamification
- **Teacher Dashboard** - School integration (Phase 2)

## Architecture

### Backend Structure

```
backend/src/main/kotlin/com/tamixa/
├── domain/
│   ├── ReadingLevel.kt
│   ├── Quiz.kt
│   ├── Vocabulary.kt
│   ├── ReadingStreak.kt
│   └── Teacher.kt
├── application/
│   ├── port/
│   │   ├── ReadingLevelRepositoryPort.kt
│   │   └── QuizRepositoryPort.kt
│   └── service/
│       ├── ReadingLevelService.kt
│       ├── QuizService.kt
│       ├── ReadingStreakService.kt
│       └── VocabularyService.kt
├── infrastructure/
│   ├── persistence/
│   │   ├── ReadingLevelEntity.kt
│   │   ├── QuizEntity.kt
│   │   ├── VocabularyEntity.kt
│   │   ├── ReadingStreakEntity.kt
│   │   ├── TeacherEntity.kt
│   │   └── *JpaRepository.kt
│   └── adapter/
│       ├── ReadingLevelRepositoryAdapter.kt
│       └── QuizRepositoryAdapter.kt
└── api/
    └── education/
        ├── ReadingLevelController.kt
        ├── QuizController.kt
        ├── ReadingStreakController.kt
        └── dto/
            ├── ReadingLevelDto.kt
            ├── QuizDto.kt
            └── ReadingStreakDto.kt
```

### Database Schema

**Reading Levels**
```sql
reading_levels (id, child_id, level, updated_at)
reading_level_assessments (id, child_id, quiz_id, old_level, new_level, assessed_at)
```

**Quizzes**
```sql
quizzes (id, story_id, questions, difficulty_level, created_at)
quiz_responses (id, quiz_id, child_id, answers, score, max_score, completed_at)
```

**Vocabulary**
```sql
vocabulary_words (id, word, definition, language, difficulty_level, example_sentence, created_at)
child_vocabulary (id, child_id, word_id, mastery_level, learned_at)
```

**Reading Streaks**
```sql
reading_streaks (id, child_id, current_streak, longest_streak, last_read_at, updated_at)
```

**Teachers & Classrooms** (Phase 2)
```sql
teachers (id, parent_id, school_name, verified_at, created_at)
classrooms (id, teacher_id, name, code, subject, grade_level, created_at)
classroom_students (id, classroom_id, child_id, joined_at)
```

## API Endpoints

### Reading Levels

```
GET /api/v1/reading-levels/{childId}
  Response: { childId, level, updatedAt }

GET /api/v1/reading-levels/{childId}/history
  Response: { assessments: [{ childId, oldLevel, newLevel, assessedAt }] }
```

### Quizzes

```
GET /api/v1/quizzes/stories/{storyId}
  Response: { id, storyId, questions: [{ id, text, type, options, explanation }], difficultyLevel, createdAt }

POST /api/v1/quizzes/{quizId}/submit
  Body: { answers: { questionId: "answer" } }
  Response: { quizId, score, maxScore, percentage, completedAt }

GET /api/v1/quizzes/results?childId=123&limit=20
  Response: { content: [QuizResult], totalElements, totalPages, page, first, last }
```

### Reading Streaks

```
GET /api/v1/reading-streaks/{childId}
  Response: { childId, currentStreak, longestStreak, lastReadAt, isActive }

POST /api/v1/reading-streaks/{childId}/record-read
  Response: { childId, currentStreak, longestStreak, lastReadAt, isActive }

GET /api/v1/reading-streaks/{childId}/stats
  Response: { currentStreak, longestStreak, lastReadAt, isActive }
```

## Service Layer

### ReadingLevelService

```kotlin
fun getOrCreateReadingLevel(childId: Long): ReadingLevel
fun assessAndUpdateLevel(childId: Long, quizScore: Int, maxScore: Int, quizId: Long?): ReadingLevel
fun getAssessmentHistory(childId: Long): List<ReadingLevelAssessment>
```

**Logic:**
- Levels range from 1-10
- Level increases when quiz score ≥ 90%
- Level decreases when quiz score < 60%
- Assessment history tracks all level changes

### QuizService

```kotlin
fun generateQuizForStory(story: Story): Quiz
fun getOrGenerateQuizForStory(story: Story): Quiz
fun submitQuizResponse(quizId: Long, childId: Long, answers: Map<String, String>): QuizResponse
fun getQuizResultsForChild(childId: Long, limit: Int): List<QuizResponse>
```

**Logic:**
- Auto-generates 3 questions per story (simplified)
- Difficulty based on child's age
- Scoring: (correct_answers / total_questions) * 100
- Triggers reading level assessment on submission

### ReadingStreakService

```kotlin
fun getOrCreateStreak(childId: Long): ReadingStreak
fun recordStoryRead(childId: Long): ReadingStreak
fun getStreakStats(childId: Long): Map<String, Any>
```

**Logic:**
- Streak increments by 1 for each day with a story read
- Streak resets if > 1 day passes without reading
- Tracks current and longest streaks
- `isActive` = true if read within last 24 hours

## Integration Points

### With Story Service

When a story is completed:
1. Call `ReadingStreakService.recordStoryRead(childId)` to update streak
2. Call `QuizService.getOrGenerateQuizForStory(story)` to create quiz
3. Display quiz to child

### With Mobile App

**Screens to add:**
- `ReadingLevelScreen` - Show current level and progression
- `QuizScreen` - Display and submit quiz
- `StreakScreen` - Show streak stats and badges
- `VocabularyScreen` - Word learning cards

**ViewModels:**
- `ReadingLevelViewModel` - Fetch and display levels
- `QuizViewModel` - Quiz logic and submission
- `StreakViewModel` - Streak tracking

### With Web App

**Pages to add:**
- `ReadingLevels.tsx` - Parent dashboard for child levels
- `Quizzes.tsx` - Quiz history and results
- `Streaks.tsx` - Streak tracking
- `Vocabulary.tsx` - Vocabulary progress

## Security Considerations

- ✅ All endpoints require `@PreAuthorize("hasRole('PARENT')")`
- ✅ Parent can only access their own children's data
- ✅ Quiz answers validated server-side
- ✅ No PII in logs
- ✅ Input validation on all DTOs

## Testing

### Unit Tests

```kotlin
// ReadingLevelService
- testGetOrCreateReadingLevel()
- testAssessAndUpdateLevelIncrease()
- testAssessAndUpdateLevelDecrease()
- testAssessmentHistory()

// QuizService
- testGenerateQuizForStory()
- testSubmitQuizResponse()
- testCalculateScore()

// ReadingStreakService
- testRecordStoryRead()
- testStreakReset()
- testIsStreakActive()
```

### Integration Tests

```kotlin
// ReadingLevelControllerTest
- testGetReadingLevel()
- testGetReadingLevelHistory()

// QuizControllerTest
- testGetQuizForStory()
- testSubmitQuiz()
- testGetQuizResults()

// ReadingStreakControllerTest
- testGetStreak()
- testRecordStoryRead()
- testGetStreakStats()
```

## Performance Considerations

- **Indexes:** `reading_levels(child_id)`, `quiz_responses(child_id)`, `reading_streaks(child_id)`
- **Caching:** Cache reading levels in Redis (TTL: 1 hour)
- **Pagination:** Quiz results paginated (default 20 per page)
- **Lazy Loading:** Use `FetchType.LAZY` for entity relationships

## Future Enhancements

### Phase 2: Teacher Dashboard
- Teacher accounts and classroom management
- Bulk student assignment
- Class-level analytics
- Curriculum alignment

### Phase 3: Advanced Features
- AI-powered quiz generation (OpenAI)
- Vocabulary extraction from stories
- Adaptive difficulty based on performance
- Social leaderboards (opt-in)
- Achievement badges

### Phase 4: Analytics
- Reading level progression charts
- Quiz performance trends
- Vocabulary mastery heatmaps
- Engagement metrics

## Deployment Checklist

- [ ] Run database migration: `V69__add_educational_features.sql`
- [ ] Deploy backend with new services and controllers
- [ ] Update mobile app with new screens
- [ ] Update web app with new pages
- [ ] Update admin dashboard with education metrics
- [ ] Test end-to-end: story → quiz → level update → streak
- [ ] Monitor performance and error rates
- [ ] Gather user feedback

## Monitoring & Observability

**Metrics to track:**
- Quiz completion rate
- Average quiz score by age group
- Reading streak distribution
- Level progression velocity
- API response times

**Logs to monitor:**
- Quiz generation failures
- Level assessment anomalies
- Streak calculation errors

**Alerts:**
- High quiz failure rate (> 50%)
- Slow quiz submission (> 2s)
- Database query timeouts

## References

- [Naming Conventions](NAMING_CONVENTIONS.md)
- [API Design Guide](.cursor/rules/api-design-agent.mdc)
- [Database Migrations Guide](.cursor/rules/database-migrations-agent.mdc)
- [Security Guidelines](.cursor/rules/security-agent.mdc)
