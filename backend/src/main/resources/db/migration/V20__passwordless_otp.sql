-- Add short_code to magic_link_token for mobile passwordless (enter code from email)
ALTER TABLE magic_link_token ADD COLUMN short_code VARCHAR(6);

-- OTP for phone-based login
CREATE TABLE otp_verification (
    id BIGSERIAL PRIMARY KEY,
    phone VARCHAR(20) NOT NULL,
    code VARCHAR(6) NOT NULL,
    expires_at TIMESTAMPTZ NOT NULL,
    used_at TIMESTAMPTZ,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_otp_verification_phone ON otp_verification(phone);
CREATE INDEX idx_otp_verification_expires ON otp_verification(expires_at);

-- Optional phone for parents (nullable, for OTP login)
ALTER TABLE parents ADD COLUMN phone VARCHAR(20) UNIQUE;
