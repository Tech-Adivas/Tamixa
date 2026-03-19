-- Monetization plan: add STARTER tier (INR), update PREMIUM/FAMILY to INR (paise).
INSERT INTO subscription_tiers (
    name, price_monthly, price_yearly, max_children, max_voices, max_avatar_videos,
    max_soundscapes, allows_voice_cloning, allows_avatar_video, allows_soundscapes,
    allows_family_sharing, analytics_enabled
) VALUES
    ('STARTER', 9900, 79900, 1, 0, 0, 0, false, false, false, false, false)
ON CONFLICT (name) DO NOTHING;

-- Update PREMIUM and FAMILY to INR (paise): ₹299/₹2399, ₹549/₹4399
UPDATE subscription_tiers SET price_monthly = 29900, price_yearly = 239900 WHERE name = 'PREMIUM';
UPDATE subscription_tiers SET price_monthly = 54900, price_yearly = 439900 WHERE name = 'FAMILY';
