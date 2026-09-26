# V100 Migration: Enhanced Story Translations Pipeline Tracking

## Overview
This migration enhances the `story_translations` table with granular per-language status tracking for the multi-stage content pipeline. It supports the Premium UX Overhaul initiative (`.kiro/specs/tamixa-premium-ux-overhaul/ENHANCEMENTS.md Section 2`) which introduces parallel language processing and detailed pipeline visibility.

## Business Context
The enhanced workflow separates the content creation pipeline into distinct phases:
1. **Translation Pipeline**: Translate content from Tamil (source) to target languages
2. **Rewrite Pipeline**: Rewrite translated content for Tamixa storytelling style
3. **Content Review**: Human review and approval of translated content
4. **Audio Pipeline**: TTS generation for approved content
5. **Audio Review**: Human review and approval of generated audio

Previously, these stages were tracked using a single `status` column with combined states (e.g., `TRANSLATING`, `REWRITING`, `TTS_PROCESSING`). The new schema provides independent tracking for each pipeline stage, enabling:
- **Parallel processing**: All 6 languages can be processed simultaneously
- **Granular visibility**: Track each stage independently per language
- **Better error handling**: Retry logic per pipeline stage
- **Quality gates**: Human review checkpoints with reviewer tracking

## Schema Changes

### New Columns Added

#### Translation Pipeline
- `translation_status` VARCHAR(50) DEFAULT 'PENDING' - Status: PENDING, IN_PROGRESS, COMPLETED, FAILED
- `translation_started_at` TIMESTAMP - When translation began
- `translation_completed_at` TIMESTAMP - When translation finished
- `translation_error` TEXT - Error message if translation failed
- `translation_retry_count` INT DEFAULT 0 - Number of retry attempts

#### Rewrite Pipeline
- `rewrite_status` VARCHAR(50) DEFAULT 'PENDING' - Status: PENDING, IN_PROGRESS, COMPLETED, FAILED
- `rewrite_started_at` TIMESTAMP - When rewrite began
- `rewrite_completed_at` TIMESTAMP - When rewrite finished
- `rewrite_error` TEXT - Error message if rewrite failed
- `rewrite_retry_count` INT DEFAULT 0 - Number of retry attempts

#### Content Review
- `content_reviewed_at` TIMESTAMP - When content was reviewed
- `content_reviewed_by` VARCHAR(255) - Reviewer identifier (admin email/username)
- `content_review_notes` TEXT - Review notes and feedback

#### Audio Pipeline
- `audio_status` VARCHAR(50) DEFAULT 'PENDING' - Status: PENDING, IN_PROGRESS, COMPLETED, FAILED
- `audio_started_at` TIMESTAMP - When audio generation began
- `audio_completed_at` TIMESTAMP - When audio generation finished
- `audio_file_url` TEXT - URL to generated audio file (S3/CDN)
- `audio_duration_seconds` INT - Duration of generated audio
- `audio_error` TEXT - Error message if audio generation failed
- `audio_retry_count` INT DEFAULT 0 - Number of retry attempts

#### Audio Review
- `audio_reviewed_at` TIMESTAMP - When audio was reviewed
- `audio_reviewed_by` VARCHAR(255) - Reviewer identifier (admin email/username)
- `audio_review_notes` TEXT - Audio review notes and feedback

#### Metadata
- `updated_at` TIMESTAMP DEFAULT CURRENT_TIMESTAMP - Last modification timestamp

### New Indexes
- `idx_story_translations_translation_status` - Query by translation status
- `idx_story_translations_rewrite_status` - Query by rewrite status
- `idx_story_translations_audio_status` - Query by audio status
- `idx_story_translations_content_reviewed` - Partial index for reviewed content
- `idx_story_translations_audio_reviewed` - Partial index for reviewed audio
- `idx_story_translations_master_language` - Composite index for common joins

## Backward Compatibility

### Existing Columns Preserved
The migration uses `ADD COLUMN IF NOT EXISTS` to safely add new columns without affecting existing data. The following existing columns are **preserved**:
- `status` - Legacy combined status (kept for backward compatibility)
- `retry_count` - Legacy retry count (kept for backward compatibility)
- `last_error` - Legacy error tracking (kept for backward compatibility)
- `narration_approved_at` - Existing approval timestamp

### Migration Strategy
1. **Non-breaking**: All new columns are nullable or have defaults
2. **Additive**: No existing columns are modified or removed
3. **Safe rollback**: Rollback script provided (V100_ROLLBACK.sql)
4. **Zero downtime**: Can be applied to production without service interruption

### Data Migration
No data migration is required. Existing rows will have:
- New status columns set to 'PENDING' (default)
- New timestamp columns set to NULL
- New retry count columns set to 0 (default)

Applications can gradually migrate to use the new columns while the legacy `status` column continues to work.

## Performance Considerations

### Index Strategy
- **Status indexes**: Enable fast filtering by pipeline stage status
- **Partial indexes**: Reduce index size for review timestamps (only indexed when NOT NULL)
- **Composite index**: Optimize common query pattern (master_story_id + language)

### Query Patterns Optimized
```sql
-- Find all translations pending translation
SELECT * FROM story_translations WHERE translation_status = 'PENDING';

-- Find all translations ready for content review (translation + rewrite complete)
SELECT * FROM story_translations 
WHERE translation_status = 'COMPLETED' 
  AND rewrite_status = 'COMPLETED' 
  AND content_reviewed_at IS NULL;

-- Find all translations ready for audio generation (content approved)
SELECT * FROM story_translations 
WHERE content_reviewed_at IS NOT NULL 
  AND audio_status = 'PENDING';

-- Get all translations for a story with their pipeline status
SELECT * FROM story_translations 
WHERE master_story_id = ? 
ORDER BY language;
```

### Storage Impact
- **Per row overhead**: ~500 bytes (25 new columns × ~20 bytes average)
- **Index overhead**: ~6 indexes × row count × ~50 bytes
- **Estimated total**: For 10,000 translations: ~5MB data + ~3MB indexes = ~8MB total

## Rollback Procedure

If rollback is needed:

```bash
# 1. Stop application services
systemctl stop tamixa-backend

# 2. Create backup
pg_dump -h localhost -U tamixa_user -d tamixa_db -t story_translations > story_translations_backup.sql

# 3. Apply rollback migration
psql -h localhost -U tamixa_user -d tamixa_db -f V100__enhance_story_translations_pipeline_tracking_ROLLBACK.sql

# 4. Verify rollback
psql -h localhost -U tamixa_user -d tamixa_db -c "\d story_translations"

# 5. Restart application
systemctl start tamixa-backend
```

**WARNING**: Rollback will result in data loss for all pipeline tracking information stored in the new columns.

## Testing Checklist

### Pre-Migration
- [ ] Backup production database
- [ ] Test migration on staging environment
- [ ] Verify existing queries still work
- [ ] Check application logs for errors

### Post-Migration
- [ ] Verify all new columns exist: `\d story_translations`
- [ ] Verify all indexes created: `\di story_translations*`
- [ ] Test new query patterns for performance
- [ ] Verify existing application functionality
- [ ] Monitor error logs for 24 hours

### Validation Queries
```sql
-- Verify schema changes
SELECT column_name, data_type, column_default, is_nullable
FROM information_schema.columns
WHERE table_name = 'story_translations'
  AND column_name LIKE '%status%' OR column_name LIKE '%reviewed%'
ORDER BY ordinal_position;

-- Verify indexes
SELECT indexname, indexdef
FROM pg_indexes
WHERE tablename = 'story_translations'
  AND indexname LIKE 'idx_story_translations_%'
ORDER BY indexname;

-- Check existing data (should all have defaults)
SELECT 
    COUNT(*) as total_rows,
    COUNT(CASE WHEN translation_status = 'PENDING' THEN 1 END) as pending_translation,
    COUNT(CASE WHEN rewrite_status = 'PENDING' THEN 1 END) as pending_rewrite,
    COUNT(CASE WHEN audio_status = 'PENDING' THEN 1 END) as pending_audio
FROM story_translations;
```

## Application Integration

### Kotlin Entity Updates Required
Update `StoryTranslationEntity` to include new fields:

```kotlin
@Entity
@Table(name = "story_translations")
data class StoryTranslationEntity(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long = 0,
    
    // ... existing fields ...
    
    // Translation pipeline
    @Column(name = "translation_status")
    val translationStatus: String = "PENDING",
    
    @Column(name = "translation_started_at")
    val translationStartedAt: Instant? = null,
    
    @Column(name = "translation_completed_at")
    val translationCompletedAt: Instant? = null,
    
    @Column(name = "translation_error")
    val translationError: String? = null,
    
    @Column(name = "translation_retry_count")
    val translationRetryCount: Int = 0,
    
    // Rewrite pipeline
    @Column(name = "rewrite_status")
    val rewriteStatus: String = "PENDING",
    
    // ... (similar for other fields)
    
    @Column(name = "updated_at")
    val updatedAt: Instant = Instant.now()
)
```

### Service Layer Updates
Update translation pipeline services to use new granular status tracking:

```kotlin
// Before (legacy)
translationEntity.status = "TRANSLATING"

// After (granular)
translationEntity.translationStatus = "IN_PROGRESS"
translationEntity.translationStartedAt = Instant.now()
```

## Related Documentation
- Spec: `.kiro/specs/tamixa-premium-ux-overhaul/ENHANCEMENTS.md`
- Design: `.kiro/specs/tamixa-premium-ux-overhaul/design.md`
- Tasks: `.kiro/specs/tamixa-premium-ux-overhaul/tasks.md`
- Previous migration: `V27__story_translation_status_state_machine.sql`

## Migration Metadata
- **Version**: V100
- **Author**: Kiro AI (spec-task-execution subagent)
- **Date**: 2025-01-04
- **Spec**: tamixa-premium-ux-overhaul
- **Task**: 5 - Database - Enhance story_translations table
- **Estimated Duration**: < 1 second (additive schema changes only)
- **Downtime Required**: None (zero-downtime migration)

