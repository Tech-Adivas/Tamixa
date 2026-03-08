-- Store S3/GCS object key for completed data exports; download URL is generated on-demand (presigned).
ALTER TABLE data_export_job ADD COLUMN IF NOT EXISTS storage_key TEXT;
