CREATE TABLE audit.audit_events (
    id UUID PRIMARY KEY,
    source_module VARCHAR(64) NOT NULL,
    aggregate_type VARCHAR(64) NOT NULL,
    aggregate_id UUID NOT NULL,
    event_type VARCHAR(96) NOT NULL,
    message VARCHAR(240) NOT NULL,
    actor VARCHAR(120) NOT NULL,
    correlation_id VARCHAR(120),
    details TEXT NOT NULL DEFAULT '{}',
    occurred_at TIMESTAMPTZ NOT NULL,
    created_at TIMESTAMPTZ NOT NULL,
    CONSTRAINT chk_audit_events_source_module
        CHECK (length(trim(source_module)) > 0),
    CONSTRAINT chk_audit_events_aggregate_type
        CHECK (length(trim(aggregate_type)) > 0),
    CONSTRAINT chk_audit_events_event_type
        CHECK (length(trim(event_type)) > 0),
    CONSTRAINT chk_audit_events_message
        CHECK (length(trim(message)) > 0),
    CONSTRAINT chk_audit_events_actor
        CHECK (length(trim(actor)) > 0)
);

CREATE INDEX idx_audit_events_aggregate
    ON audit.audit_events (aggregate_type, aggregate_id, occurred_at, id);

CREATE INDEX idx_audit_events_occurred_at
    ON audit.audit_events (occurred_at DESC, id DESC);

CREATE INDEX idx_audit_events_event_type
    ON audit.audit_events (event_type);
