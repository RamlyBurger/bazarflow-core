CREATE TABLE inventory.inventory_lots (
    id UUID PRIMARY KEY,
    sku_id UUID NOT NULL REFERENCES catalog.skus (id),
    lot_code VARCHAR(64) NOT NULL,
    warehouse_code VARCHAR(32) NOT NULL,
    received_quantity NUMERIC(12, 3) NOT NULL,
    available_quantity NUMERIC(12, 3) NOT NULL,
    reserved_quantity NUMERIC(12, 3) NOT NULL,
    dispatched_quantity NUMERIC(12, 3) NOT NULL,
    expiry_date DATE NOT NULL,
    status VARCHAR(24) NOT NULL,
    received_at TIMESTAMPTZ NOT NULL,
    created_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL,
    version BIGINT NOT NULL DEFAULT 0,
    CONSTRAINT uq_inventory_lots_sku_lot_code
        UNIQUE (sku_id, lot_code),
    CONSTRAINT chk_inventory_lots_status
        CHECK (status IN ('AVAILABLE', 'QUARANTINED', 'EXPIRED')),
    CONSTRAINT chk_inventory_lots_quantities_non_negative
        CHECK (
            received_quantity > 0
            AND available_quantity >= 0
            AND reserved_quantity >= 0
            AND dispatched_quantity >= 0
        ),
    CONSTRAINT chk_inventory_lots_quantity_balance
        CHECK (available_quantity + reserved_quantity + dispatched_quantity <= received_quantity)
);

CREATE INDEX idx_inventory_lots_sku_id
    ON inventory.inventory_lots (sku_id);

CREATE INDEX idx_inventory_lots_expiry_date
    ON inventory.inventory_lots (expiry_date);

CREATE INDEX idx_inventory_lots_available
    ON inventory.inventory_lots (sku_id, expiry_date)
    WHERE status = 'AVAILABLE' AND available_quantity > 0;

CREATE TABLE inventory.stock_movements (
    id UUID PRIMARY KEY,
    lot_id UUID NOT NULL REFERENCES inventory.inventory_lots (id),
    movement_type VARCHAR(32) NOT NULL,
    quantity_delta NUMERIC(12, 3) NOT NULL,
    reason VARCHAR(160) NOT NULL,
    occurred_at TIMESTAMPTZ NOT NULL,
    CONSTRAINT chk_stock_movements_type
        CHECK (movement_type IN ('RECEIVE')),
    CONSTRAINT chk_stock_movements_quantity_delta
        CHECK (quantity_delta <> 0)
);

CREATE INDEX idx_stock_movements_lot_id
    ON inventory.stock_movements (lot_id);

CREATE INDEX idx_stock_movements_occurred_at
    ON inventory.stock_movements (occurred_at);
