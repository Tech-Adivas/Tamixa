-- Soft "life bar" counters per child (author-defined deltas roll up to four pillars). No percentages in API contract.
CREATE TABLE IF NOT EXISTS child_life_skill_soft_counters (
    child_id BIGINT PRIMARY KEY REFERENCES children(id) ON DELETE CASCADE,
    wisdom INT NOT NULL DEFAULT 0,
    social INT NOT NULL DEFAULT 0,
    money INT NOT NULL DEFAULT 0,
    balance INT NOT NULL DEFAULT 0,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT chk_life_skill_wisdom CHECK (wisdom >= -100000 AND wisdom <= 100000),
    CONSTRAINT chk_life_skill_social CHECK (social >= -100000 AND social <= 100000),
    CONSTRAINT chk_life_skill_money CHECK (money >= -100000 AND money <= 100000),
    CONSTRAINT chk_life_skill_balance CHECK (balance >= -100000 AND balance <= 100000)
);
