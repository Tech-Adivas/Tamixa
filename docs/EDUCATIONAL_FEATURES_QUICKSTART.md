# Educational Features - Quick Start Guide

Get up and running with reading levels, quizzes, and streaks in 15 minutes.

## Prerequisites

- Backend running: `./gradlew :backend:bootRun`
- PostgreSQL with Tamixa database
- Authenticated parent user

## 1. Database Setup (1 min)

The migration runs automatically on startup:
```bash
# Verify migration ran
SELECT * FROM reading_levels;
SELECT * FROM quizzes;
SELECT * FROM reading_streaks;
```

## 2. Test Reading Levels (3 min)

```bash
# Get or create reading level for child
curl -X GET http://localhost:8080/api/v1/reading-levels/123 \
  -H "Authorization: Bearer YOUR_JWT_TOKEN"

# Response:
{
  "childId": 123,
  "level": 1,
  "updatedAt": "2026-03-20T10:00:00Z"
}

# Get assessment history
curl -X GET http://localhost:8080/api/v1/reading-levels/123/history \
  -H "Authorization: Bearer YOUR_JWT_TOKEN"
```

## 3. Test Quizzes (5 min)

```bash
# Get quiz for a story
curl -X GET http://localhost:8080/api/v1/quizzes/stories/456 \
  -H "Authorization: Bearer YOUR_JWT_TOKEN"

# Response:
{
  "id": 1,
  "storyId": 456,
  "questions": [
    {
      "id": "q1",
      "text": "Who is the main character?",
      "type": "SHORT_ANSWER",
      "options": null,
      "explanation": "The story is about Alex."
    }
  ],
  "difficultyLevel": 2,
  "createdAt": "2026-03-20T10:00:00Z"
}

# Submit quiz answers
curl -X POST http://localhost:8080/api/v1/quizzes/1/submit?childId=123 \
  -H "Authorization: Bearer YOUR_JWT_TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "answers": {
      "q1": "Alex",
      "q2": "Adventure",
      "q3": "True"
    }
  }'

# Response:
{
  "quizId": 1,
  "score": 100,
  "maxScore": 100,
  "percentage": 100,
  "completedAt": "2026-03-20T10:05:00Z"
}

# Get quiz results for child
curl -X GET "http://localhost:8080/api/v1/quizzes/results?childId=123&limit=20" \
  -H "Authorization: Bearer YOUR_JWT_TOKEN"
```

## 4. Test Reading Streaks (3 min)

```bash
# Get current streak
curl -X GET http://localhost:8080/api/v1/reading-streaks/123 \
  -H "Authorization: Bearer YOUR_JWT_TOKEN"

# Response:
{
  "childId": 123,
  "currentStreak": 0,
  "longestStreak": 0,
  "lastReadAt": null,
  "isActive": false
}

# Record a story read
curl -X POST http://localhost:8080/api/v1/reading-streaks/123/record-read \
  -H "Authorization: Bearer YOUR_JWT_TOKEN"

# Response:
{
  "childId": 123,
  "currentStreak": 1,
  "longestStreak": 1,
  "lastReadAt": "2026-03-20T10:10:00Z",
  "isActive": true
}

# Get streak stats
curl -X GET http://localhost:8080/api/v1/reading-streaks/123/stats \
  -H "Authorization: Bearer YOUR_JWT_TOKEN"

# Response:
{
  "currentStreak": 1,
  "longestStreak": 1,
  "lastReadAt": "2026-03-20T10:10:00Z",
  "isActive": true
}
```

## 5. Integration Flow (3 min)

**Typical user journey:**

```
1. Parent creates child profile
   → ReadingLevelService.getOrCreateReadingLevel(childId)
   → Returns level 1 (default)

2. Child reads a story
   → StoryService.completeStory(storyId, childId)
   → ReadingStreakService.recordStoryRead(childId)
   → Streak increments to 1

3. Quiz is presented
   → QuizService.getOrGenerateQuizForStory(story)
   → Returns 3 questions

4. Child submits quiz
   → QuizService.submitQuizResponse(quizId, childId, answers)
   → Score calculated: 80/100
   → ReadingLevelService.assessAndUpdateLevel(childId, 80, 100, quizId)
   → Level stays at 1 (need 90%+ to increase)

5. Parent views dashboard
   → GET /api/v1/reading-levels/123
   → GET /api/v1/reading-streaks/123/stats
   → GET /api/v1/quizzes/results?childId=123
```

## 6. Common Tasks

### Add a new vocabulary word

```kotlin
// In VocabularyService (to be implemented)
val word = VocabularyWord(
    id = 0,
    word = "adventure",
    definition = "An exciting or unusual experience",
    language = "en",
    difficultyLevel = 2,
    exampleSentence = "The story was full of adventure.",
    createdAt = Instant.now()
)
vocabularyRepository.save(word)
```

### Mark a word as learned

```kotlin
// In VocabularyService (to be implemented)
val childVocab = ChildVocabulary(
    id = 0,
    childId = 123,
    wordId = 1,
    masteryLevel = ChildVocabulary.MASTERY_FAMILIAR,
    learnedAt = Instant.now()
)
vocabularyRepository.save(childVocab)
```

### Create a teacher account

```kotlin
// In TeacherService (to be implemented)
val teacher = Teacher(
    id = 0,
    parentId = 456,
    schoolName = "Lincoln Elementary",
    verifiedAt = null,
    createdAt = Instant.now()
)
teacherRepository.save(teacher)
```

### Create a classroom

```kotlin
// In ClassroomService (to be implemented)
val classroom = Classroom(
    id = 0,
    teacherId = 1,
    name = "Grade 3 - English",
    code = "ABC123XYZ",
    subject = "English",
    gradeLevel = "3",
    createdAt = Instant.now()
)
classroomRepository.save(classroom)
```

## 7. Debugging

### Check if migration ran

```sql
SELECT * FROM information_schema.tables 
WHERE table_name IN ('reading_levels', 'quizzes', 'reading_streaks');
```

### View reading level for a child

```sql
SELECT * FROM reading_levels WHERE child_id = 123;
```

### View quiz responses

```sql
SELECT * FROM quiz_responses WHERE child_id = 123 ORDER BY completed_at DESC;
```

### View reading streak

```sql
SELECT * FROM reading_streaks WHERE child_id = 123;
```

### Check logs for errors

```bash
# Look for educational feature logs
grep -i "reading\|quiz\|streak" logs/tamixa.log
```

## 8. Next Steps

### For Mobile Developers
1. Create `ReadingLevelScreen` composable
2. Create `QuizScreen` composable
3. Create `StreakScreen` composable
4. Integrate with `StoryViewModel`
5. Call streak endpoint after story completion

### For Web Developers
1. Create `ReadingLevels.tsx` page
2. Create `Quizzes.tsx` page
3. Create `Streaks.tsx` page
4. Add to parent dashboard
5. Display charts and progress

### For Backend Developers
1. Implement `VocabularyService` and API
2. Implement `TeacherService` and API
3. Add integration tests
4. Add unit tests
5. Seed vocabulary data

## 9. API Reference

| Endpoint | Method | Purpose |
|----------|--------|---------|
| `/reading-levels/{childId}` | GET | Get child's reading level |
| `/reading-levels/{childId}/history` | GET | Get level change history |
| `/quizzes/stories/{storyId}` | GET | Get quiz for story |
| `/quizzes/{quizId}/submit` | POST | Submit quiz answers |
| `/quizzes/results` | GET | Get quiz results for child |
| `/reading-streaks/{childId}` | GET | Get reading streak |
| `/reading-streaks/{childId}/record-read` | POST | Record story read |
| `/reading-streaks/{childId}/stats` | GET | Get streak statistics |

## 10. Performance Tips

- **Cache reading levels** in Redis (TTL: 1 hour)
- **Index frequently queried columns:** `child_id`, `quiz_id`, `story_id`
- **Paginate quiz results** (default 20 per page)
- **Use lazy loading** for entity relationships
- **Monitor API response times** (target: <500ms)

## 11. Security Reminders

✅ All endpoints require `@PreAuthorize("hasRole('PARENT')")`  
✅ Parent can only access their own children's data  
✅ Quiz answers validated server-side  
✅ No PII in logs  
✅ Input validation on all DTOs  

## 12. Troubleshooting

**Q: Migration not running**
- A: Check PostgreSQL is running and database exists
- A: Check Flyway is enabled in `application.yml`

**Q: 401 Unauthorized on API calls**
- A: Ensure JWT token is valid and not expired
- A: Check Authorization header format: `Bearer YOUR_TOKEN`

**Q: Quiz not generating**
- A: Check story exists and is accessible by parent
- A: Check `QuizService.generateQuizForStory()` logs

**Q: Streak not incrementing**
- A: Check `ReadingStreakService.recordStoryRead()` is called
- A: Verify child_id is correct
- A: Check database for existing streak record

**Q: Reading level not updating**
- A: Quiz score must be ≥90% to increase level
- A: Quiz score must be <60% to decrease level
- A: Check `ReadingLevelService.assessAndUpdateLevel()` logs

## 13. Support

For issues or questions:
1. Check `docs/EDUCATIONAL_FEATURES_GUIDE.md` for detailed docs
2. Review `EDUCATIONAL_FEATURES_SUMMARY.md` for architecture
3. Check logs: `grep -i "reading\|quiz\|streak" logs/tamixa.log`
4. Run tests: `./gradlew :backend:test -Ptamixa.backendOnly=true`

---

**Last Updated:** March 20, 2026  
**Status:** Ready for Phase 1.5 (Vocabulary & Teacher Services)
