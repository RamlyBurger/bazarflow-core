CREATE TABLE fulfillment.pick_waves (
    id UUID PRIMARY KEY,
    wave_number VARCHAR(48) NOT NULL,
    delivery_zone VARCHAR(32) NOT NULL,
    delivery_date DATE NOT NULL,
    status VARCHAR(24) NOT NULL,
    order_count INTEGER NOT NULL,
    line_count INTEGER NOT NULL,
    planned_at TIMESTAMPTZ NOT NULL,
    created_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL,
    version BIGINT NOT NULL DEFAULT 0,
    CONSTRAINT uq_pick_waves_wave_number UNIQUE (wave_number),
    CONSTRAINT chk_pick_waves_status
        CHECK (status IN ('OPEN', 'LOCKED', 'COMPLETED', 'CANCELLED')),
    CONSTRAINT chk_pick_waves_counts
        CHECK (order_count >= 0 AND line_count >= 0)
);

CREATE INDEX idx_pick_waves_delivery
    ON fulfillment.pick_waves (delivery_date, delivery_zone);

CREATE INDEX idx_pick_waves_status
    ON fulfillment.pick_waves (status);

CREATE TABLE fulfillment.dispatch_jobs (
    id UUID PRIMARY KEY,
    pick_wave_id UUID NOT NULL REFERENCES fulfillment.pick_waves (id) ON DELETE CASCADE,
    order_id UUID NOT NULL REFERENCES ordering.orders (id),
    order_number VARCHAR(32) NOT NULL,
    retailer_id UUID NOT NULL REFERENCES partner.retailers (id),
    outlet_id UUID NOT NULL REFERENCES partner.outlets (id),
    delivery_zone VARCHAR(32) NOT NULL,
    requested_delivery_date DATE NOT NULL,
    delivery_window_start TIME NOT NULL,
    delivery_window_end TIME NOT NULL,
    status VARCHAR(24) NOT NULL,
    sla_at_risk BOOLEAN NOT NULL,
    sla_risk_reason VARCHAR(120),
    planned_at TIMESTAMPTZ NOT NULL,
    created_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL,
    version BIGINT NOT NULL DEFAULT 0,
    CONSTRAINT uq_dispatch_jobs_order_id UNIQUE (order_id),
    CONSTRAINT chk_dispatch_jobs_status
        CHECK (status IN ('PLANNED', 'IN_PROGRESS', 'COMPLETED', 'FAILED')),
    CONSTRAINT chk_dispatch_jobs_delivery_window
        CHECK (delivery_window_start < delivery_window_end),
    CONSTRAINT chk_dispatch_jobs_sla_reason
        CHECK ((sla_at_risk = FALSE AND sla_risk_reason IS NULL) OR (sla_at_risk = TRUE AND sla_risk_reason IS NOT NULL))
);

CREATE INDEX idx_dispatch_jobs_pick_wave_id
    ON fulfillment.dispatch_jobs (pick_wave_id);

CREATE INDEX idx_dispatch_jobs_status
    ON fulfillment.dispatch_jobs (status);

CREATE INDEX idx_dispatch_jobs_sla_risk
    ON fulfillment.dispatch_jobs (sla_at_risk, requested_delivery_date, delivery_window_end);
