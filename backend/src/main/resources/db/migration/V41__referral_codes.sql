-- Referral codes for subscription discounts: shop name, shortcode, offer percentage, expiration.
CREATE TABLE IF NOT EXISTS referral_codes (
    id BIGSERIAL PRIMARY KEY,
    shortcode VARCHAR(32) NOT NULL,
    shop_name VARCHAR(255) NOT NULL,
    offer_percent INT NOT NULL CHECK (offer_percent >= 0 AND offer_percent <= 100),
    expires_at TIMESTAMPTZ NOT NULL,
    active BOOLEAN NOT NULL DEFAULT TRUE,
    stripe_coupon_id VARCHAR(255),
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    CONSTRAINT uq_referral_codes_shortcode UNIQUE (shortcode)
);

CREATE INDEX idx_referral_codes_shortcode ON referral_codes(shortcode);
CREATE INDEX idx_referral_codes_active_expires ON referral_codes(active, expires_at);
