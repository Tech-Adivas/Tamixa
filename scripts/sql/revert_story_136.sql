-- Revert Story #136: Remove incorrect interactive graph
-- Story: "The Day the Colors Ran Away" - a linear children's story about colors
-- Issue: Has an incorrect interactive graph about civic survival/traffic stops

-- Backup current state first
CREATE TABLE IF NOT EXISTS library_stories_backup_story_136 AS 
SELECT * FROM library_stories WHERE id = 136;

CREATE TABLE IF NOT EXISTS story_translations_backup_story_136 AS 
SELECT * FROM story_translations WHERE master_story_id = 136;

-- Remove the incorrect interactive graph from master story
UPDATE library_stories 
SET 
  interactive_graph = NULL,
  updated_at = NOW()
WHERE id = 136;

-- Remove interactive graph from all translations
UPDATE story_translations 
SET 
  interactive_graph = NULL,
  updated_at = NOW()
WHERE master_story_id = 136;

-- Verify the changes
SELECT 
  id, 
  title, 
  LENGTH(content) as content_length,
  word_count,
  interactive_graph IS NULL as graph_removed,
  status
FROM library_stories 
WHERE id = 136;

SELECT 
  id,
  language,
  LENGTH(content) as content_length,
  word_count,
  interactive_graph IS NULL as graph_removed,
  status
FROM story_translations 
WHERE master_story_id = 136
ORDER BY language;
