# V99: Unified Library Story Status Migration

## Overview

This migration implements the unified `LibraryStoryStatus` system that replaces the confusing separation between story status and pipeline status. It's part of the Tamixa Premium UX Overhaul initiative (Phase 1, Task 4).

## What Changed

### Before (Old System)
- Single `status` column with values: `DRAFT`, `PUBLISHED`, `PROCESSING`, `READY`, `CHANGES_REQUESTED`, `REJECTED`
- Confusing semantics: `PUBLISHED` meant "in review queue", not "live on app"
- No clear distinction between translation, content review, audio generation, and audio review phases

### After (New System)
- Unified `status` column with comprehensive workflow states
- Clear progression: `DRAFT` → `SUBMITTED` → `TRANSLATING` → `CONTENT_REVIEW` → `APPROVED` → `AUDIO_GENERATING` → `AUDIO_REVIEW` → `PUBLISHED`
- Legacy status preserved in `legacy_status` column for audit trail

## New Status Values

| Status | Description | Phase |
|--------|-------------|-------|
| `DRAFT` | Being edited, not submitted | Draft |
| `SUBMITTED` | Submitted for review, pipeline starting | Translation |
| `TRANSLATING` | Pipeline running (translation + rewrite) | Translation |
| `TRANSLATION_FAILED` | Pipeline failed, needs retry | Translation |
| `CONTENT_REVIEW` | All languages ready, awaiting human review | Content Review |
| `CHANGES_REQUESTED` | Reviewer requested changes | Content Review |
| `REJECTED` | Story rejected | Content Review |
| `APPROVED` | Content approved, ready for audio | Audio Generation |
| `AUDIO_GENERATING` | TTS pipeline running | Audio Generation |
| `AUDIO_FAILED` | Audio generation failed | Audio Generation |
| `AUDIO_REVIEW` | Audio ready, awaiting approval | Audio Review |
| `PUBLISHED` | Live on app | Published |

## Migration Mapping

The migration automatically maps old status values to new ones:

| Old Status | New Status | Rationale |
|------------|------------|-----------|
| `DRAFT` | `DRAFT` | No change |
| `PUBLISHED` | `CONTENT_REVIEW` | Old "PUBLISHED" meant "in review queue" |
| `PROCESSING` | `TRANSLATING` | Pipeline running |
| `READY` | `APPROVED` | Content approved, ready for audio |
| `CHANGES_REQUESTED` | `CHANGES_REQUESTED` | No change |
| `REJECTED` | `REJECTED` | No change |

## Database Changes

1. **New Column**: `status` (VARCHAR(30)) - unified status system
2. **Preserved Column**: `legacy_status` (VARCHAR(20)) - old status values for audit
3. **New Indexes**:
   - `idx_library_stories_status` - status queries with deleted_at filter
   - `idx_library_stories_status_narration` - status + narration approval queries
   - `idx_library_stories_status_created` - status + created_at for ordering

## Backward Compatibility

- The `legacy_status` column preserves original status values
- Existing queries using status will continue to work after code updates
- No data loss during migration

## Rollback

If rollback is needed, use the `V99__unified_library_story_status_ROLLBACK.sql` script:

```bash
# Connect to database
psql $DATABASE_URL

# Run rollback script
\i backend/src/main/resources/db/migration/V99__unified_library_story_status_ROLLBACK.sql
```

**Warning**: Rollback will lose any new status values (SUBMITTED, TRANSLATING, etc.) and map them back to old values.

## Next Steps

After this migration:

1. **Task 5**: Enhance `story_translations` table with per-language status tracking
2. **Task 6**: Implement `LibraryStoryStatus` enum in Kotlin backend
3. **Task 7**: Create parallel translation pipeline service
4. **Tasks 8-10**: Add story management, audio generation, and audio review APIs
5. **Tasks 11-15**: Update admin dashboard UI to use new status system

## Testing

To test the migration:

```bash
# Validate migration
./gradlew :backend:flywayValidate -Ptamixa.backendOnly=true

# Check migration status
./gradlew :backend:flywayInfo -Ptamixa.backendOnly=true

# Run migration (if not already applied)
./gradlew :backend:flywayMigrate -Ptamixa.backendOnly=true
```

## References

- **Spec**: `.kiro/specs/tamixa-premium-ux-overhaul/`
- **Requirements**: Requirement 11 (Admin Story Management Workflow Clarity)
- **Design**: `ENHANCEMENTS.md` Section 2 (Multi-Language Story Creation Workflow Improvements)
- **Tasks**: Phase 1, Task 4

## Author

Generated as part of Tamixa Premium UX Overhaul initiative.

