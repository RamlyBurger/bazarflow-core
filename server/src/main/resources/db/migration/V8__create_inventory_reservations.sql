ALTER TABLE inventory.stock_movements
    DROP CONSTRAINT chk_stock_movements_type;

ALTER TABLE inventory.stock_movements
    ADD CONSTRAINT chk_stock_movements_type
        CHECK (movement_type IN ('RECEIVE', 'RESERVE'));

CREATE TABLE inventory.reservations (
    id UUID PRIMARY KEY,
    order_id UUID NOT NULL REFERENCES ordering.orders (id),
    required_by_date DATE NOT NULL,
    status VARCHAR(24) NOT NULL,
    reserved_at TIMESTAMPTZ NOT NULL,
    expires_at TIMESTAMPTZ NOT NULL,
    created_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL,
    version BIGINT NOT NULL DEFAULT 0,
    CONSTRAINT uq_reservations_order_id UNIQUE (order_id),
    CONSTRAINT chk_reservations_status
        CHECK (status IN ('ACTIVE', 'RELEASED', 'CONSUMED')),
    CONSTRAINT chk_reservations_expiry
        CHECK (expires_at > reserved_at)
);

CREATE INDEX idx_reservations_order_id
    ON inventory.reservations (order_id);

CREATE INDEX idx_reservations_status
    ON inventory.reservations (status);

CREATE INDEX idx_reservations_expires_at
    ON inventory.reservations (expires_at);

CREATE TABLE inventory.reservation_lines (
    id UUID PRIMARY KEY,
    reservation_id UUID NOT NULL REFERENCES inventory.reservations (id) ON DELETE CASCADE,
    lot_id UUID NOT NULL REFERENCES inventory.inventory_lots (id),
    sku_id UUID NOT NULL REFERENCES catalog.skus (id),
    lot_code VARCHAR(64) NOT NULL,
    warehouse_code VARCHAR(32) NOT NULL,
    quantity NUMERIC(12, 3) NOT NULL,
    expiry_date DATE NOT NULL,
    created_at TIMESTAMPTZ NOT NULL,
    CONSTRAINT uq_reservation_lines_reservation_lot
        UNIQUE (reservation_id, lot_id),
    CONSTRAINT chk_reservation_lines_quantity
        CHECK (quantity > 0)
);

CREATE INDEX idx_reservation_lines_reservation_id
    ON inventory.reservation_lines (reservation_id);

CREATE INDEX idx_reservation_lines_lot_id
    ON inventory.reservation_lines (lot_id);

CREATE INDEX idx_reservation_lines_sku_id
    ON inventory.reservation_lines (sku_id);
