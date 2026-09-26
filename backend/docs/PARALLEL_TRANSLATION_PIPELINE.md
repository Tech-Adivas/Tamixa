# Parallel Translation Pipeline Service

## Overview

The Parallel Translation Pipeline Service processes story translations for multiple languages concurrently, enabling efficient multi-language content delivery for the Tamixa platform.

## Features

- **Parallel Processing**: Translates stories into 6 languages (Tamil, English, Hindi, Telugu, Kannada, Malayalam) simultaneously using Kotlin coroutines
- **Independent Language Status**: Each language translation has its own status tracking and error handling
- **Graceful Failure Handling**: Failures in one language don't block others; partial success is supported
- **Retry Logic**: Automatic retry mechanism with configurable max retries (default: 3)
- **Timeout Management**: Per-language and total pipeline timeouts prevent indefinite hangs
- **Status Tracking**: Real-time status polling for admin dashboard integration

## Architecture

### Components

1. **ParallelTranslationPipelineService** (`application/pipeline/`)
   - Core service implementing parallel translation logic
   - Uses Kotlin coroutines for concurrent processing
   - Implements `TranslationPipelinePort` interface

2. **TranslationPipelinePort** (`application/port/`)
   - Port interface defining the contract for pipeline operations
   - Follows hexagonal architecture pattern

3. **TranslationPipelineController** (`api/library/`)
   - REST controller exposing pipeline endpoints
   - Secured with `@PreAuthorize` for ADMIN/CONTENT_MANAGER roles

### Data Flow

```
Story Submission (DRAFT → SUBMITTED)
         ↓
Pipeline Triggered (SUBMITTED → TRANSLATING)
         ↓
Parallel Language Processing
    ├─ Tamil (ta)
    ├─ English (en)
    ├─ Hindi (hi)
    ├─ Telugu (te)
    ├─ Kannada (kn)
    └─ Malayalam (ml)
         ↓
Status Update
    ├─ All Success → CONTENT_REVIEW
    └─ Any Failure → TRANSLATION_FAILED
```

## API Endpoints

### 1. Trigger Pipeline

**Endpoint**: `POST /api/v1/library/stories/{id}/pipeline/trigger`

**Description**: Triggers the translation pipeline for a story, processing all configured languages in parallel.

**Authorization**: Requires `ADMIN`, `CONTENT_MANAGER`, or `SUPER_ADMIN` role

**Request Body** (optional):
```json
{
  "forceRetranslate": false
}
```

**Response** (200 OK - All languages succeeded):
```json
{
  "storyId": 123,
  "success": true,
  "message": "Pipeline triggered successfully for all languages",
  "languageResults": [
    {
      "language": "ta",
      "success": true,
      "error": null,
      "translationId": 456
    },
    {
      "language": "en",
      "success": true,
      "error": null,
      "translationId": 457
    }
  ],
  "totalDurationMs": 45000
}
```

**Response** (207 Multi-Status - Partial success):
```json
{
  "storyId": 123,
  "success": false,
  "message": "Pipeline completed with some failures",
  "languageResults": [
    {
      "language": "ta",
      "success": true,
      "error": null,
      "translationId": 456
    },
    {
      "language": "en",
      "success": false,
      "error": "Translation timeout: 5min",
      "translationId": 457
    }
  ],
  "totalDurationMs": 300000
}
```

**Error Responses**:
- `400 Bad Request`: Story not found
- `409 Conflict`: Invalid status transition (e.g., story already published)
- `500 Internal Server Error`: Pipeline execution failed

### 2. Get Pipeline Status

**Endpoint**: `GET /api/v1/library/stories/{id}/pipeline/status`

**Description**: Gets the current pipeline status for a story across all languages. Used by admin dashboard for status polling (every 3 seconds).

**Authorization**: Requires `ADMIN`, `CONTENT_MANAGER`, or `SUPER_ADMIN` role

**Response** (200 OK):
```json
{
  "storyId": 123,
  "languageStatuses": [
    {
      "language": "ta",
      "status": "COMPLETED",
      "displayName": "Completed",
      "isFailed": false,
      "isTerminal": true
    },
    {
      "language": "en",
      "status": "TRANSLATING",
      "displayName": "Translating",
      "isFailed": false,
      "isTerminal": false
    },
    {
      "language": "hi",
      "status": "TRANSLATION_FAILED",
      "displayName": "Translation failed",
      "isFailed": true,
      "isTerminal": true
    }
  ]
}
```

### 3. Retry Failed Translations

**Endpoint**: `POST /api/v1/library/stories/{id}/pipeline/retry`

**Description**: Retries failed translations for a story. Only retries translations that haven't exceeded max retry count.

**Authorization**: Requires `ADMIN`, `CONTENT_MANAGER`, or `SUPER_ADMIN` role

**Response** (200 OK):
```json
{
  "storyId": 123,
  "success": true,
  "message": "Retried 2 language(s)",
  "retriedLanguages": ["en", "hi"]
}
```

## Configuration

Configuration is managed through `application.yml` under `app.translationPipeline`:

```yaml
app:
  translationPipeline:
    sourceLanguage: ta                    # Source language (Tamil)
    targetLanguages: en,hi,te,kn,ml       # Comma-separated target languages
    languageTimeoutMinutes: 5             # Timeout per language
    totalTimeoutMinutes: 60               # Total pipeline timeout
    maxRetries: 3                         # Max retry attempts per language
    translationTimeoutMs: 60000           # Translation API timeout
    parallelism: 4                        # Parallel processing threads
```

## Status Workflow

### Library Story Status

The pipeline integrates with the unified `LibraryStoryStatus` enum:

1. **DRAFT** → Story being edited
2. **SUBMITTED** → Submitted for review, pipeline starting
3. **TRANSLATING** → Pipeline running (translation + rewrite for all languages)
4. **TRANSLATION_FAILED** → Pipeline failed, needs retry
5. **CONTENT_REVIEW** → All languages ready, awaiting human review
6. **APPROVED** → Content approved, ready for audio generation
7. **AUDIO_GENERATING** → TTS pipeline running
8. **AUDIO_REVIEW** → Audio ready, awaiting review
9. **PUBLISHED** → Live on app

### Translation Pipeline Status

Each language translation has its own `TranslationPipelineStatus`:

- **PENDING** → Not yet started
- **TRANSLATING** → Translation in progress
- **TRANSLATION_FAILED** → Translation failed
- **REWRITING** → Rewrite/paraphrase in progress
- **REWRITE_FAILED** → Rewrite failed
- **AWAITING_AUDIO** → Translation complete, waiting for audio generation
- **TTS_PROCESSING** → Audio generation in progress
- **TTS_FAILED** → Audio generation failed
- **COMPLETED** → Fully processed

## Error Handling

### Retry Logic

- Each language translation tracks its retry count
- Failed translations are automatically retried up to `maxRetries` (default: 3)
- After max retries, translation status is set to `TRANSLATION_FAILED`
- Retry count is reset when pipeline is manually retriggered

### Timeout Handling

1. **Per-Language Timeout**: Each language has `languageTimeoutMinutes` (default: 5 min)
   - If exceeded, that language is marked as failed
   - Other languages continue processing

2. **Total Pipeline Timeout**: Entire pipeline has `totalTimeoutMinutes` (default: 60 min)
   - If exceeded, all remaining languages are marked as failed
   - Prevents indefinite hangs

### Partial Success

- Pipeline supports partial success (some languages succeed, others fail)
- Story status is set to `TRANSLATION_FAILED` if any language fails
- Admin can retry failed languages without reprocessing successful ones

## Admin Dashboard Integration

### Status Polling

The admin dashboard should poll the status endpoint every 3 seconds while pipeline is active:

```javascript
// Example polling implementation
const pollPipelineStatus = async (storyId) => {
  const response = await fetch(`/api/v1/library/stories/${storyId}/pipeline/status`);
  const data = await response.json();
  
  // Update UI with language statuses
  updateLanguageStatusBadges(data.languageStatuses);
  
  // Check if pipeline is still running
  const isRunning = data.languageStatuses.some(
    status => !status.isTerminal
  );
  
  if (isRunning) {
    // Continue polling
    setTimeout(() => pollPipelineStatus(storyId), 3000);
  }
};
```

### Preventing Edits During Processing

The admin dashboard should disable editing when story status is `TRANSLATING`:

```javascript
const canEdit = (story) => {
  return story.status !== 'TRANSLATING' && 
         story.status !== 'AUDIO_GENERATING';
};
```

## Performance Considerations

### Parallel Processing

- Uses Kotlin coroutines for lightweight concurrency
- Each language runs in its own coroutine
- Coroutines are more efficient than threads (lower memory overhead)
- Configured parallelism limit prevents resource exhaustion

### Caching

- Translation results are cached by content hash
- Cache prevents redundant API calls on retries
- Cache TTL is configurable (default: 24 hours)

### Resource Management

- Translation service uses connection pooling for HTTP clients
- Timeouts prevent resource leaks from hanging requests
- Coroutine cancellation ensures proper cleanup

## Monitoring and Logging

### Structured Logging

All log messages include context:

```kotlin
log.info("Processing language={} for storyId={}", language, storyId)
log.error("Translation failed: id={}, storyId={}, language={}", 
    translation.id, storyId, language, e)
```

### Key Metrics to Monitor

1. **Pipeline Duration**: `totalDurationMs` in response
2. **Language Success Rate**: Percentage of successful language translations
3. **Retry Count**: Average retries per translation
4. **Timeout Frequency**: How often timeouts occur
5. **Partial Success Rate**: Stories with some but not all languages successful

### Log Levels

- **INFO**: Pipeline start/completion, status transitions
- **WARN**: Partial success, retry attempts
- **ERROR**: Failures, timeouts, exceptions

## Testing

### Manual Testing

1. Create a draft story in admin dashboard
2. Submit for review to trigger pipeline
3. Monitor status via polling endpoint
4. Verify all languages are processed
5. Test retry functionality for failed translations

### Integration Testing

```kotlin
// Example integration test
@Test
fun `should process all languages in parallel`() = runBlocking {
    val storyId = createTestStory()
    val result = pipelineService.triggerPipeline(storyId)
    
    assertTrue(result.success)
    assertEquals(6, result.languageResults.size)
    assertTrue(result.languageResults.all { it.success })
}
```

## Troubleshooting

### Common Issues

1. **Pipeline Stuck in TRANSLATING**
   - Check if timeout exceeded
   - Verify translation service is responding
   - Use retry endpoint to restart

2. **All Languages Failing**
   - Check translation API credentials
   - Verify network connectivity
   - Review error messages in logs

3. **Partial Failures**
   - Check per-language error messages
   - Verify language-specific configuration
   - Use retry endpoint for failed languages

4. **Slow Performance**
   - Increase `parallelism` setting
   - Check translation API rate limits
   - Review timeout settings

## Future Enhancements

1. **Selective Language Processing**: Allow triggering pipeline for specific languages only
2. **Priority Queuing**: Process high-priority stories first
3. **Batch Processing**: Process multiple stories in a single pipeline run
4. **Webhook Notifications**: Notify external systems when pipeline completes
5. **Metrics Dashboard**: Real-time pipeline performance metrics
6. **A/B Testing**: Compare different translation providers

## References

- Requirements: `.kiro/specs/tamixa-premium-ux-overhaul/requirements.md` (Requirement 3)
- Design: `.kiro/specs/tamixa-premium-ux-overhaul/design.md` (Task 7)
- Status Enum: `backend/src/main/kotlin/com/tamixa/domain/LibraryStoryStatus.kt`
- Translation Service: `backend/src/main/kotlin/com/tamixa/application/translation/TranslationService.kt`
