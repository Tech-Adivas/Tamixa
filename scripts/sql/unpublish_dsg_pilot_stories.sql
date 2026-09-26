-- Change DSG Pilot stories back to DRAFT status
-- This will hide them from the library and search results

-- Before: Check current status
SELECT id, title, status 
FROM library_stories 
WHERE title LIKE '%DSG%' OR title LIKE '%Pilot%'
ORDER BY id;

-- Update to DRAFT
UPDATE library_stories 
SET status = 'DRAFT' 
WHERE title LIKE '%DSG%' OR title LIKE '%Pilot%';

-- After: Verify the change
SELECT id, title, status 
FROM library_stories 
WHERE title LIKE '%DSG%' OR title LIKE '%Pilot%'
ORDER BY id;

-- Verify only non-DSG stories are now published
SELECT COUNT(*) as published_count, 
       COUNT(CASE WHEN title LIKE '%DSG%' THEN 1 END) as dsg_published_count
FROM library_stories 
WHERE status = 'PUBLISHED';
