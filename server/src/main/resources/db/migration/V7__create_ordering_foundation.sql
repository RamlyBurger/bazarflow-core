CREATE TABLE ordering.orders (
    id UUID PRIMARY KEY,
    order_number VARCHAR(32) NOT NULL,
    retailer_id UUID NOT NULL REFERENCES partner.retailers (id),
    outlet_id UUID NOT NULL REFERENCES partner.outlets (id),
    delivery_zone VARCHAR(32) NOT NULL,
    requested_delivery_date DATE NOT NULL,
    status VARCHAR(32) NOT NULL,
    currency VARCHAR(3) NOT NULL,
    subtotal NUMERIC(12, 2) NOT NULL,
    submitted_at TIMESTAMPTZ,
    created_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL,
    version BIGINT NOT NULL DEFAULT 0,
    CONSTRAINT uq_orders_order_number UNIQUE (order_number),
    CONSTRAINT chk_orders_status
        CHECK (status IN ('DRAFT', 'SUBMITTED', 'ACCEPTED', 'CANCELLED')),
    CONSTRAINT chk_orders_subtotal
        CHECK (subtotal >= 0)
);

CREATE INDEX idx_orders_retailer_id
    ON ordering.orders (retailer_id);

CREATE INDEX idx_orders_outlet_id
    ON ordering.orders (outlet_id);

CREATE INDEX idx_orders_status
    ON ordering.orders (status);

CREATE INDEX idx_orders_created_at
    ON ordering.orders (created_at);

CREATE TABLE ordering.order_lines (
    id UUID PRIMARY KEY,
    order_id UUID NOT NULL REFERENCES ordering.orders (id) ON DELETE CASCADE,
    line_number INTEGER NOT NULL,
    sku_id UUID NOT NULL REFERENCES catalog.skus (id),
    quantity NUMERIC(12, 3) NOT NULL,
    unit_price NUMERIC(12, 2) NOT NULL,
    line_total NUMERIC(12, 2) NOT NULL,
    created_at TIMESTAMPTZ NOT NULL,
    CONSTRAINT uq_order_lines_order_line_number
        UNIQUE (order_id, line_number),
    CONSTRAINT chk_order_lines_quantity
        CHECK (quantity > 0),
    CONSTRAINT chk_order_lines_prices
        CHECK (unit_price > 0 AND line_total >= 0)
);

CREATE INDEX idx_order_lines_order_id
    ON ordering.order_lines (order_id);

CREATE INDEX idx_order_lines_sku_id
    ON ordering.order_lines (sku_id);

CREATE TABLE ordering.order_status_history (
    id UUID PRIMARY KEY,
    order_id UUID NOT NULL REFERENCES ordering.orders (id) ON DELETE CASCADE,
    from_status VARCHAR(32),
    to_status VARCHAR(32) NOT NULL,
    reason VARCHAR(160) NOT NULL,
    changed_by VARCHAR(120) NOT NULL,
    changed_at TIMESTAMPTZ NOT NULL,
    CONSTRAINT chk_order_status_history_from_status
        CHECK (from_status IS NULL OR from_status IN ('DRAFT', 'SUBMITTED', 'ACCEPTED', 'CANCELLED')),
    CONSTRAINT chk_order_status_history_to_status
        CHECK (to_status IN ('DRAFT', 'SUBMITTED', 'ACCEPTED', 'CANCELLED'))
);

CREATE INDEX idx_order_status_history_order_id
    ON ordering.order_status_history (order_id, changed_at);

CREATE TABLE ordering.idempotency_records (
    id UUID PRIMARY KEY,
    idempotency_key VARCHAR(120) NOT NULL,
    command_type VARCHAR(64) NOT NULL,
    target_id UUID NOT NULL,
    response_order_id UUID NOT NULL REFERENCES ordering.orders (id),
    status_code INTEGER NOT NULL,
    created_at TIMESTAMPTZ NOT NULL,
    CONSTRAINT uq_idempotency_records_command
        UNIQUE (command_type, target_id, idempotency_key)
);

CREATE INDEX idx_idempotency_records_target
    ON ordering.idempotency_records (target_id);
