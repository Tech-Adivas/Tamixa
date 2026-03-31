-- DORA metrics event store for deployment and incident tracking.
CREATE TABLE IF NOT EXISTS dora_metric_events (
    id BIGSERIAL PRIMARY KEY,
    event_type VARCHAR(24) NOT NULL,
    service_name VARCHAR(80) NOT NULL,
    environment VARCHAR(40) NOT NULL,
    deployment_status VARCHAR(20),
    incident_status VARCHAR(20),
    change_id VARCHAR(120),
    change_started_at TIMESTAMPTZ,
    deployed_at TIMESTAMPTZ,
    incident_started_at TIMESTAMPTZ,
    restored_at TIMESTAMPTZ,
    caused_by_change BOOLEAN NOT NULL DEFAULT TRUE,
    summary VARCHAR(500),
    created_by VARCHAR(255) NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_dora_metric_events_created_at
    ON dora_metric_events(created_at DESC);

CREATE INDEX IF NOT EXISTS idx_dora_metric_events_event_type
    ON dora_metric_events(event_type);

CREATE INDEX IF NOT EXISTS idx_dora_metric_events_service_env
    ON dora_metric_events(service_name, environment);

CREATE INDEX IF NOT EXISTS idx_dora_metric_events_deployed_at
    ON dora_metric_events(deployed_at DESC);

CREATE INDEX IF NOT EXISTS idx_dora_metric_events_incident_started_at
    ON dora_metric_events(incident_started_at DESC);
