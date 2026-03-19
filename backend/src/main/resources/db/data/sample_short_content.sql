-- Sample short content for local dev. Run manually after migrations:
-- psql $DATABASE_URL -f backend/src/main/resources/db/data/sample_short_content.sql

-- Run once; re-running will create duplicate rows unless you truncate first.
INSERT INTO short_content (type, content, answer, language, status) VALUES
('RIDDLE', 'What has hands but cannot clap?', 'A clock', 'ta', 'PUBLISHED'),
('THOUGHT_FOR_THE_DAY', 'Small steps every day lead to big dreams.', NULL, 'ta', 'PUBLISHED'),
('PROVERB', 'Unity is strength.', NULL, 'ta', 'PUBLISHED'),
('TONGUE_TWISTER', 'She sells seashells by the seashore.', NULL, 'en', 'PUBLISHED');
