-- Add title and moral to stories for structured AI story output.
ALTER TABLE stories ADD COLUMN IF NOT EXISTS title VARCHAR(255);
ALTER TABLE stories ADD COLUMN IF NOT EXISTS moral TEXT;
