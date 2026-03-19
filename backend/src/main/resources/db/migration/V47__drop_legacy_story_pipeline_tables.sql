-- Drop legacy pipeline tables replaced by story_translations + story_narration_audio + processing_job
DROP TABLE IF EXISTS story_processing_status CASCADE;
DROP TABLE IF EXISTS story_audio CASCADE;
