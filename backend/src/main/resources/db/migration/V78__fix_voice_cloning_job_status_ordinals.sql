-- Fix voice_cloning_jobs.status: native query wrote enum ordinals (0,1,2,3)
-- instead of names (PENDING,PROCESSING,READY,FAILED). Map them back.
UPDATE voice_cloning_jobs SET status = 'PENDING'    WHERE status = '0';
UPDATE voice_cloning_jobs SET status = 'PROCESSING' WHERE status = '1';
UPDATE voice_cloning_jobs SET status = 'READY'      WHERE status = '2';
UPDATE voice_cloning_jobs SET status = 'FAILED'     WHERE status = '3';
