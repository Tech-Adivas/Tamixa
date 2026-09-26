-- Per-locale full interactive graph JSON (optional). When null, clients use library_stories.interactive_graph.
ALTER TABLE story_translations ADD COLUMN IF NOT EXISTS interactive_graph TEXT;
