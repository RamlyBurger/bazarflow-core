ALTER TABLE inventory.stock_movements
    DROP CONSTRAINT chk_stock_movements_type;

ALTER TABLE inventory.stock_movements
    ADD CONSTRAINT chk_stock_movements_type
        CHECK (movement_type IN ('RECEIVE', 'RESERVE', 'DISPATCH'));

ALTER TABLE ordering.orders
    DROP CONSTRAINT chk_orders_status;

ALTER TABLE ordering.orders
    ADD CONSTRAINT chk_orders_status
        CHECK (status IN ('DRAFT', 'SUBMITTED', 'ACCEPTED', 'DELIVERED', 'DELIVERY_FAILED', 'CANCELLED'));

ALTER TABLE ordering.order_status_history
    DROP CONSTRAINT chk_order_status_history_from_status;

ALTER TABLE ordering.order_status_history
    ADD CONSTRAINT chk_order_status_history_from_status
        CHECK (
            from_status IS NULL
            OR from_status IN ('DRAFT', 'SUBMITTED', 'ACCEPTED', 'DELIVERED', 'DELIVERY_FAILED', 'CANCELLED')
        );

ALTER TABLE ordering.order_status_history
    DROP CONSTRAINT chk_order_status_history_to_status;

ALTER TABLE ordering.order_status_history
    ADD CONSTRAINT chk_order_status_history_to_status
        CHECK (to_status IN ('DRAFT', 'SUBMITTED', 'ACCEPTED', 'DELIVERED', 'DELIVERY_FAILED', 'CANCELLED'));

ALTER TABLE fulfillment.dispatch_jobs
    ADD COLUMN completed_at TIMESTAMPTZ,
    ADD COLUMN failed_at TIMESTAMPTZ,
    ADD COLUMN failure_reason VARCHAR(160);

ALTER TABLE fulfillment.dispatch_jobs
    ADD CONSTRAINT chk_dispatch_jobs_outcome
        CHECK (
            (
                status = 'COMPLETED'
                AND completed_at IS NOT NULL
                AND failed_at IS NULL
                AND failure_reason IS NULL
            )
            OR (
                status = 'FAILED'
                AND completed_at IS NULL
                AND failed_at IS NOT NULL
                AND failure_reason IS NOT NULL
            )
            OR (
                status IN ('PLANNED', 'IN_PROGRESS')
                AND completed_at IS NULL
                AND failed_at IS NULL
                AND failure_reason IS NULL
            )
        );
