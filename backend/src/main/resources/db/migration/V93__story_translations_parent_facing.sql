-- Per-locale parent-facing metadata (content note, speak-along, discussion prompts).
-- Populated by the translation pipeline and optional admin payloads; playback prefers these over master when set.

ALTER TABLE story_translations
    ADD COLUMN IF NOT EXISTS parent_content_note TEXT,
    ADD COLUMN IF NOT EXISTS speak_along_prompt VARCHAR(500),
    ADD COLUMN IF NOT EXISTS parent_discussion_prompts JSONB;

-- Copy from master for rows whose translation language matches the library story language (typically primary catalog).
UPDATE story_translations st
SET parent_content_note = ls.parent_content_note,
    speak_along_prompt = ls.speak_along_prompt,
    parent_discussion_prompts = ls.parent_discussion_prompts
FROM library_stories ls
WHERE st.master_story_id = ls.id
  AND lower(trim(st.language)) = lower(trim(ls.language));
