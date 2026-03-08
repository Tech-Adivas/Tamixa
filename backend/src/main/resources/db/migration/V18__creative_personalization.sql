-- Story cover/illustration (AI-generated)
ALTER TABLE stories ADD COLUMN IF NOT EXISTS cover_image_url VARCHAR(512);

-- Character builder (extend child profile)
ALTER TABLE children ADD COLUMN IF NOT EXISTS favorite_color VARCHAR(50);
ALTER TABLE children ADD COLUMN IF NOT EXISTS favorite_animal VARCHAR(100);
ALTER TABLE children ADD COLUMN IF NOT EXISTS character_traits VARCHAR(500);
ALTER TABLE children ADD COLUMN IF NOT EXISTS avatar_choice VARCHAR(50);
