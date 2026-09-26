-- Interactive Stories Content Review Query
-- This script helps you review all interactive stories and their content

-- 1. List all interactive stories with key metadata
SELECT 
    id,
    title,
    category,
    theme,
    status,
    language,
    age,
    word_count,
    reading_time_minutes,
    LENGTH(interactive_graph) as graph_json_size,
    LENGTH(content) as content_length,
    post_story_mission IS NOT NULL as has_mission,
    parent_discussion_prompts IS NOT NULL as has_discussion_prompts,
    parent_content_note IS NOT NULL as has_content_note,
    narration_approved_at IS NOT NULL as narration_approved,
    created_at
FROM library_stories
WHERE interactive_graph IS NOT NULL
ORDER BY category, title;

-- 2. Count interactive stories by category
SELECT 
    category,
    COUNT(*) as story_count,
    COUNT(CASE WHEN status = 'PUBLISHED' THEN 1 END) as published_count,
    COUNT(CASE WHEN status = 'DRAFT' THEN 1 END) as draft_count,
    COUNT(CASE WHEN narration_approved_at IS NOT NULL THEN 1 END) as narration_approved_count
FROM library_stories
WHERE interactive_graph IS NOT NULL
GROUP BY category
ORDER BY story_count DESC;

-- 3. Get detailed content for a specific story (replace ID)
-- Example: Get story ID 100
SELECT 
    id,
    title,
    content,
    moral,
    interactive_graph,
    post_story_mission,
    parent_discussion_prompts,
    parent_content_note,
    speak_along_prompt
FROM library_stories
WHERE id = 100;  -- Replace with actual story ID

-- 4. Find stories missing parent-facing content
SELECT 
    id,
    title,
    category,
    post_story_mission IS NULL as missing_mission,
    parent_discussion_prompts IS NULL as missing_prompts,
    parent_content_note IS NULL as missing_note
FROM library_stories
WHERE interactive_graph IS NOT NULL
  AND (
    post_story_mission IS NULL 
    OR parent_discussion_prompts IS NULL 
    OR parent_content_note IS NULL
  )
ORDER BY category, title;

-- 5. Check interactive graph structure (sample)
-- This shows the first 500 characters of each interactive_graph
SELECT 
    id,
    title,
    LEFT(interactive_graph, 500) as graph_preview
FROM library_stories
WHERE interactive_graph IS NOT NULL
ORDER BY id
LIMIT 10;

-- 6. Find stories ready for review (DRAFT with complete content)
SELECT 
    id,
    title,
    category,
    word_count,
    LENGTH(interactive_graph) as graph_size
FROM library_stories
WHERE interactive_graph IS NOT NULL
  AND status = 'DRAFT'
  AND post_story_mission IS NOT NULL
  AND parent_discussion_prompts IS NOT NULL
  AND content IS NOT NULL
ORDER BY category, title;

-- 7. Export story for detailed review (replace ID)
-- Copy this output to a text file for review
\echo '\n=== STORY CONTENT REVIEW ==='
\echo 'Story ID: [Replace with ID]'
\echo ''
SELECT 
    'Title: ' || title || E'\n' ||
    'Category: ' || category || E'\n' ||
    'Theme: ' || theme || E'\n' ||
    'Status: ' || status || E'\n' ||
    'Age: ' || age || E'\n' ||
    'Word Count: ' || word_count || E'\n' ||
    'Reading Time: ' || reading_time_minutes || ' minutes' || E'\n\n' ||
    '=== CONTENT ===' || E'\n' ||
    content || E'\n\n' ||
    '=== MORAL ===' || E'\n' ||
    COALESCE(moral, '[No moral]') || E'\n\n' ||
    '=== POST-STORY MISSION ===' || E'\n' ||
    COALESCE(post_story_mission, '[No mission]') || E'\n\n' ||
    '=== PARENT CONTENT NOTE ===' || E'\n' ||
    COALESCE(parent_content_note, '[No note]') || E'\n\n' ||
    '=== DISCUSSION PROMPTS ===' || E'\n' ||
    COALESCE(parent_discussion_prompts::text, '[No prompts]') as full_review
FROM library_stories
WHERE id = 100;  -- Replace with actual story ID
