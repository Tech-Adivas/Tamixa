-- Tamixa AI Control Plane: governance schema (source of truth in PostgreSQL).
-- See docs/CONTROL_PLANE_ARCHITECTURE.md

CREATE TABLE ai_projects (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    name VARCHAR(120) NOT NULL,
    code VARCHAR(50) NOT NULL UNIQUE,
    status VARCHAR(30) NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE TABLE ai_workflows (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    project_id UUID NOT NULL REFERENCES ai_projects (id) ON DELETE CASCADE,
    workflow_key VARCHAR(120) NOT NULL,
    name VARCHAR(150) NOT NULL,
    description TEXT,
    category VARCHAR(50) NOT NULL,
    status VARCHAR(30) NOT NULL,
    version INTEGER NOT NULL,
    definition_json JSONB NOT NULL,
    requires_human_approval BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    UNIQUE (workflow_key)
);

CREATE TABLE ai_workflow_steps (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    workflow_id UUID NOT NULL REFERENCES ai_workflows (id) ON DELETE CASCADE,
    step_key VARCHAR(120) NOT NULL,
    step_type VARCHAR(50) NOT NULL,
    step_order INTEGER NOT NULL,
    config_json JSONB NOT NULL,
    is_required BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    UNIQUE (workflow_id, step_key),
    UNIQUE (workflow_id, step_order)
);

CREATE TABLE ai_prompt_assets (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    project_id UUID NOT NULL REFERENCES ai_projects (id) ON DELETE CASCADE,
    asset_key VARCHAR(150) NOT NULL,
    asset_type VARCHAR(40) NOT NULL,
    name VARCHAR(150) NOT NULL,
    description TEXT,
    language_code VARCHAR(20),
    scope VARCHAR(30) NOT NULL,
    status VARCHAR(30) NOT NULL,
    current_version INTEGER NOT NULL,
    owner_team VARCHAR(80),
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    UNIQUE (project_id, asset_key)
);

CREATE TABLE ai_prompt_asset_versions (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    asset_id UUID NOT NULL REFERENCES ai_prompt_assets (id) ON DELETE CASCADE,
    version INTEGER NOT NULL,
    content TEXT NOT NULL,
    metadata_json JSONB,
    checksum VARCHAR(128) NOT NULL,
    created_by VARCHAR(120) NOT NULL,
    approved_by VARCHAR(120),
    approval_status VARCHAR(30) NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    UNIQUE (asset_id, version)
);

CREATE TABLE ai_language_profiles (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    language_code VARCHAR(20) NOT NULL UNIQUE,
    language_name VARCHAR(100) NOT NULL,
    script_name VARCHAR(50) NOT NULL,
    supports_generation BOOLEAN NOT NULL,
    supports_translation BOOLEAN NOT NULL,
    supports_transliteration BOOLEAN NOT NULL,
    supports_tts BOOLEAN NOT NULL,
    supports_voice_clone BOOLEAN NOT NULL,
    preferred_provider_rules JSONB,
    quality_thresholds JSONB,
    moderation_profile_key VARCHAR(120),
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE TABLE ai_model_providers (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    provider_key VARCHAR(80) NOT NULL UNIQUE,
    name VARCHAR(120) NOT NULL,
    provider_type VARCHAR(40) NOT NULL,
    status VARCHAR(30) NOT NULL,
    capabilities_json JSONB NOT NULL,
    health_status VARCHAR(30) NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE TABLE ai_models (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    provider_id UUID NOT NULL REFERENCES ai_model_providers (id) ON DELETE RESTRICT,
    model_key VARCHAR(120) NOT NULL UNIQUE,
    display_name VARCHAR(120) NOT NULL,
    task_family VARCHAR(50) NOT NULL,
    context_window INTEGER,
    max_output_tokens INTEGER,
    pricing_json JSONB,
    quality_tier VARCHAR(20) NOT NULL,
    status VARCHAR(30) NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE TABLE ai_routing_policies (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    policy_key VARCHAR(120) NOT NULL UNIQUE,
    name VARCHAR(120) NOT NULL,
    task_type VARCHAR(50) NOT NULL,
    language_code VARCHAR(20),
    audience_type VARCHAR(30),
    quality_tier VARCHAR(20),
    budget_tier VARCHAR(20),
    rule_json JSONB NOT NULL,
    status VARCHAR(30) NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE TABLE ai_policy_rules (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    rule_key VARCHAR(120) NOT NULL UNIQUE,
    name VARCHAR(150) NOT NULL,
    rule_type VARCHAR(50) NOT NULL,
    scope VARCHAR(30) NOT NULL,
    severity VARCHAR(20) NOT NULL,
    condition_json JSONB NOT NULL,
    action_json JSONB NOT NULL,
    status VARCHAR(30) NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE TABLE ai_workflow_runs (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    project_id UUID NOT NULL REFERENCES ai_projects (id) ON DELETE RESTRICT,
    workflow_id UUID NOT NULL REFERENCES ai_workflows (id) ON DELETE RESTRICT,
    entity_type VARCHAR(50) NOT NULL,
    entity_id VARCHAR(120),
    request_source VARCHAR(40) NOT NULL,
    status VARCHAR(30) NOT NULL,
    current_step_key VARCHAR(120),
    requested_by VARCHAR(120),
    input_json JSONB NOT NULL,
    output_json JSONB,
    context_snapshot_json JSONB,
    started_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    completed_at TIMESTAMPTZ,
    failed_at TIMESTAMPTZ,
    error_code VARCHAR(80),
    error_message TEXT
);

CREATE TABLE ai_workflow_step_runs (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    workflow_run_id UUID NOT NULL REFERENCES ai_workflow_runs (id) ON DELETE CASCADE,
    step_key VARCHAR(120) NOT NULL,
    status VARCHAR(30) NOT NULL,
    provider_id UUID REFERENCES ai_model_providers (id) ON DELETE SET NULL,
    model_id UUID REFERENCES ai_models (id) ON DELETE SET NULL,
    prompt_asset_version_ids JSONB,
    input_json JSONB,
    output_json JSONB,
    validation_json JSONB,
    evaluation_json JSONB,
    retry_count INTEGER NOT NULL DEFAULT 0,
    started_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    completed_at TIMESTAMPTZ,
    error_code VARCHAR(80),
    error_message TEXT
);

CREATE TABLE ai_evaluation_profiles (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    profile_key VARCHAR(120) NOT NULL UNIQUE,
    name VARCHAR(150) NOT NULL,
    task_type VARCHAR(50) NOT NULL,
    language_code VARCHAR(20),
    rubric_json JSONB NOT NULL,
    thresholds_json JSONB NOT NULL,
    status VARCHAR(30) NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE TABLE ai_evaluation_results (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    workflow_step_run_id UUID NOT NULL REFERENCES ai_workflow_step_runs (id) ON DELETE CASCADE,
    profile_id UUID NOT NULL REFERENCES ai_evaluation_profiles (id) ON DELETE RESTRICT,
    overall_score NUMERIC(5, 2) NOT NULL,
    dimension_scores_json JSONB NOT NULL,
    decision VARCHAR(30) NOT NULL,
    notes TEXT,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE TABLE ai_human_review_queue (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    workflow_run_id UUID NOT NULL REFERENCES ai_workflow_runs (id) ON DELETE CASCADE,
    review_type VARCHAR(50) NOT NULL,
    priority VARCHAR(20) NOT NULL,
    status VARCHAR(30) NOT NULL,
    reason_codes JSONB,
    assigned_to VARCHAR(120),
    resolution VARCHAR(30),
    resolution_notes TEXT,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    resolved_at TIMESTAMPTZ
);

CREATE TABLE ai_cost_events (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    workflow_step_run_id UUID NOT NULL REFERENCES ai_workflow_step_runs (id) ON DELETE CASCADE,
    provider_id UUID NOT NULL REFERENCES ai_model_providers (id) ON DELETE RESTRICT,
    model_id UUID REFERENCES ai_models (id) ON DELETE SET NULL,
    input_tokens INTEGER DEFAULT 0,
    output_tokens INTEGER DEFAULT 0,
    cached_input_tokens INTEGER DEFAULT 0,
    latency_ms INTEGER,
    estimated_cost NUMERIC(12, 6) NOT NULL,
    currency VARCHAR(10) NOT NULL DEFAULT 'USD',
    created_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE TABLE ai_audit_events (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    event_type VARCHAR(60) NOT NULL,
    actor_type VARCHAR(30) NOT NULL,
    actor_id VARCHAR(120),
    entity_type VARCHAR(60) NOT NULL,
    entity_id VARCHAR(120) NOT NULL,
    action VARCHAR(60) NOT NULL,
    details_json JSONB,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE INDEX idx_ai_workflow_runs_project_status ON ai_workflow_runs (project_id, status, started_at DESC);
CREATE INDEX idx_ai_workflow_runs_workflow ON ai_workflow_runs (workflow_id, started_at DESC);
CREATE INDEX idx_ai_workflow_step_runs_run ON ai_workflow_step_runs (workflow_run_id, started_at DESC);
CREATE INDEX idx_ai_cost_events_created ON ai_cost_events (created_at DESC);
CREATE INDEX idx_ai_audit_events_created ON ai_audit_events (created_at DESC);
CREATE INDEX idx_ai_audit_events_entity ON ai_audit_events (entity_type, entity_id);

-- Default product project for single-tenant Tamixa (adjust for multi-tenant).
INSERT INTO ai_projects (name, code, status)
VALUES ('Tamixa', 'tamixa', 'ACTIVE');
