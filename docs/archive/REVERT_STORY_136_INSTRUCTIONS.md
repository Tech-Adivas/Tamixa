# Instructions to Revert Story #136

## Context
You want to revert story #136 back to its previous state before the recent changes were applied.

## Option 1: Using Admin UI (Recommended)

1. **Open Admin Dashboard**:
   - Navigate to `http://localhost:3001` (or your admin URL)
   - Log in with your admin credentials

2. **Find Story #136**:
   - Go to **Dashboard** → **Stories**
   - Search for story ID `136` or browse the list

3. **Check Current State**:
   - Click on story #136 to view details
   - Note the current:
     - Title
     - Content
     - Interactive graph (if present)
     - Word count
     - Status

4. **Revert Options**:

   ### A. If you have a backup:
   - Copy the previous content from your backup
   - Click **Edit**
   - Paste the original content
   - Remove or restore the original interactive graph
   - Click **Save draft**

   ### B. If you want to regenerate from scratch:
   - Click **Edit**
   - Click **Move to draft** (if in review)
   - Click **Invalidate content** to clear translations
   - Re-enter the original story text
   - Click **Save draft**
   - Click **Regenerate & sync all languages** when ready

   ### C. If you want to restore a soft-deleted version:
   - Use the restore endpoint (see Option 2 below)

## Option 2: Using Admin API

### Prerequisites
- Backend running at `http://localhost:8080`
- Valid admin authentication token

### Steps

1. **Get Authentication Token**:
```bash
# Login to get token
curl -X POST http://localhost:8080/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{"email":"your-admin@email.com","password":"your-password"}'
```

2. **View Current Story State**:
```bash
curl -X GET http://localhost:8080/api/admin/stories/136 \
  -H "Authorization: Bearer YOUR_TOKEN" \
  -H "Content-Type: application/json"
```

3. **Update Story Content** (if you have the original):
```bash
curl -X PUT http://localhost:8080/api/admin/stories/136 \
  -H "Authorization: Bearer YOUR_TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "title": "Original Title",
    "content": "Original story content...",
    "interactiveGraph": null,
    "status": "DRAFT"
  }'
```

4. **Invalidate and Regenerate** (if needed):
```bash
# Invalidate existing translations
curl -X POST http://localhost:8080/api/admin/stories/136/invalidate-content \
  -H "Authorization: Bearer YOUR_TOKEN"

# Trigger regeneration
curl -X POST http://localhost:8080/api/admin/stories/136/trigger-pipeline \
  -H "Authorization: Bearer YOUR_TOKEN" \
  -d '{"translationOnly": true}'
```

## Option 3: Direct Database Access (Advanced)

⚠️ **Warning**: Only use if you're comfortable with SQL and have database backups.

### Prerequisites
- PostgreSQL client installed
- Database running and accessible
- Database credentials from `.env`

### Steps

1. **Connect to Database**:
```bash
psql "jdbc:postgresql://localhost:5432/araro_kids" -U postgres
```

2. **Check Current State**:
```sql
-- View story details
SELECT id, title, language, status, word_count, 
       LENGTH(content) as content_length,
       LENGTH(interactive_graph) as graph_length,
       created_at, updated_at
FROM library_stories 
WHERE id = 136;

-- View translations
SELECT id, language, status, word_count,
       LENGTH(content) as content_length,
       LENGTH(interactive_graph) as graph_length
FROM story_translations 
WHERE master_story_id = 136;
```

3. **Backup Current State** (IMPORTANT):
```sql
-- Create backup table
CREATE TABLE library_stories_backup_136 AS 
SELECT * FROM library_stories WHERE id = 136;

CREATE TABLE story_translations_backup_136 AS 
SELECT * FROM story_translations WHERE master_story_id = 136;
```

4. **Revert Changes**:

   ### If you have the original content:
   ```sql
   -- Update master story
   UPDATE library_stories 
   SET 
     content = 'Your original story content here...',
     interactive_graph = NULL,  -- or original graph JSON
     word_count = 500,  -- original word count
     status = 'DRAFT',
     updated_at = NOW()
   WHERE id = 136;

   -- Clear translations to force regeneration
   DELETE FROM story_translations WHERE master_story_id = 136;
   ```

   ### If you want to restore from a previous backup:
   ```sql
   -- Assuming you have a backup from before
   UPDATE library_stories 
   SET 
     content = (SELECT content FROM library_stories_backup WHERE id = 136),
     interactive_graph = (SELECT interactive_graph FROM library_stories_backup WHERE id = 136),
     word_count = (SELECT word_count FROM library_stories_backup WHERE id = 136),
     updated_at = NOW()
   WHERE id = 136;
   ```

5. **Clear Related Data**:
```sql
-- Clear narration audio
DELETE FROM narration_audio 
WHERE translation_id IN (
  SELECT id FROM story_translations WHERE master_story_id = 136
);

-- Clear narration scripts
DELETE FROM narration_scripts 
WHERE translation_id IN (
  SELECT id FROM story_translations WHERE master_story_id = 136
);

-- Reset translation status
UPDATE story_translations 
SET status = 'PENDING', last_error = NULL 
WHERE master_story_id = 136;
```

## Option 4: Undo Recent Changes (If Just Applied)

If you just ran "Regenerate & Sync All languages" and want to undo:

1. **Stop the Pipeline** (if still running):
   - In Admin UI, check pipeline status
   - Wait for it to complete or manually stop

2. **Clear Generated Content**:
   ```bash
   # Using admin API
   curl -X POST http://localhost:8080/api/admin/stories/136/invalidate-content \
     -H "Authorization: Bearer YOUR_TOKEN"
   ```

3. **Restore Original Content**:
   - Edit the story in Admin UI
   - Replace with original text
   - Remove interactive graph if it was added
   - Save as draft

## What to Revert

Based on the fixes applied, here's what might have changed:

1. **Interactive Graph**: 
   - If story #136 is interactive, translations now have the graph copied
   - To revert: Remove `interactive_graph` from translations

2. **Content Length**:
   - If regenerated, segments now have 100-150 words each (500-750+ total)
   - To revert: Replace with original shorter content

3. **Translation Status**:
   - Translations may have been regenerated
   - To revert: Clear translations and regenerate from original

## Verification After Revert

1. **Check Story Details**:
   - Word count matches original
   - Content matches original
   - Interactive graph status correct

2. **Check Translations**:
   - All languages have correct content
   - Interactive graph present/absent as expected

3. **Test in Mobile App**:
   - Story plays correctly
   - Interactive choices work (if applicable)
   - All languages work

## Need Help?

If you need the original content for story #136, check:
- Git history (if story was committed)
- Database backups
- Admin audit logs
- Your local notes or documentation

## Recommended Approach

**For quickest revert**:
1. Use Admin UI (Option 1)
2. Edit story #136
3. Replace content with original
4. Save as draft
5. Regenerate if needed

This is the safest and most straightforward method.
