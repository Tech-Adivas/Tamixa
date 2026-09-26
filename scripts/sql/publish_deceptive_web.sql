-- Publish "The Deceptive Web" story for search testing
-- This will make the story searchable in the mobile app

-- Before: Check current status
SELECT id, title, status, narration_approved_at 
FROM library_stories 
WHERE id = 86;

-- Publish the story
UPDATE library_stories 
SET status = 'PUBLISHED' 
WHERE id = 86;

-- After: Verify the change
SELECT id, title, status, narration_approved_at 
FROM library_stories 
WHERE id = 86;

-- Check English translation
SELECT id, master_story_id, language, title, narration_approved_at 
FROM story_translations 
WHERE master_story_id = 86 AND language = 'en';

-- Test search query (should now return 1 result)
SELECT 
    st.id,
    st.master_story_id,
    st.language,
    st.title,
    ls.status
FROM story_translations st
JOIN library_stories ls ON st.master_story_id = ls.id
WHERE ls.status = 'PUBLISHED'
  AND st.language = 'en'
  AND (
    LOWER(st.title) LIKE '%web%'
    OR LOWER(st.content) LIKE '%web%'
    OR LOWER(st.moral) LIKE '%web%'
  );
