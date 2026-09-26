# Search Troubleshooting - "The Deceptive Web" Not Found

## Issue
User searched for "web" but story "The Deceptive Web" was not appearing in search results.

## Root Cause
The story status is **DRAFT**, not **PUBLISHED**. The search query correctly filters by `status = 'PUBLISHED'` to prevent unapproved stories from appearing in parent/child search results.

**CRITICAL FINDING**: There are **ZERO published stories** in the database!
- Total stories: 129
- Published: 0 ❌
- Draft: 85
- NULL status: 44

This means **ALL stories are currently unsearchable** because none have been published.

## Database Findings

### Master Story (library_stories)
```
ID: 86
Title: The Deceptive Web
Status: DRAFT ❌ (should be PUBLISHED)
Language: ta (Tamil)
Narration Approved: 2026-04-10 20:00:00 ✅
```

### English Translation (story_translations)
```
ID: 30
Master Story ID: 86
Language: en
Title: The Deceptive Web
Content: [Warm tone] It was a pleasant evening...
Narration Approved: NULL ❌
```

## Solution

### Option 1: Publish Individual Story (Quick Test)
To make "The Deceptive Web" searchable:

1. **Change story status to PUBLISHED** in the admin panel
   - Navigate to Stories → Find "The Deceptive Web"
   - Change status from DRAFT to PUBLISHED
   - Save changes

2. **Optionally approve the English narration** (if audio exists)
   - This will set `narration_approved_at` on the translation record
   - Required if you want the English audio to be playable

### Option 2: Bulk Publish Stories (Recommended for Production)
Since there are 0 published stories, you may want to bulk publish approved stories:

```sql
-- Review stories that have approved narration but are still DRAFT
SELECT id, title, status, narration_approved_at 
FROM library_stories 
WHERE narration_approved_at IS NOT NULL 
AND status = 'DRAFT';

-- Bulk publish stories with approved narration (REVIEW FIRST!)
UPDATE library_stories 
SET status = 'PUBLISHED' 
WHERE narration_approved_at IS NOT NULL 
AND status = 'DRAFT';
```

**WARNING**: Only publish stories that have been reviewed and approved for children!

## Search Query Behavior (Correct)

The search correctly filters by:
- ✅ `status = 'PUBLISHED'` - Only approved stories
- ✅ Searches in translation `title`, `content`, and `moral` fields
- ✅ Language-agnostic - finds stories with translations in the requested language
- ✅ Case-insensitive matching

## Verification Query

After changing status to PUBLISHED, verify with:

```sql
SELECT ls.id, ls.title, ls.status, st.language, st.title as translation_title
FROM library_stories ls
LEFT JOIN story_translations st ON ls.id = st.master_story_id
WHERE ls.id = 86;
```

Expected result:
- `ls.status` should be `PUBLISHED`
- English translation (language='en') should exist

## Next Steps

1. User should update story status in admin panel
2. Test search again with "web" query
3. Story should now appear in search results
