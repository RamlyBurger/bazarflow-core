CREATE TABLE catalog.products (
    id UUID PRIMARY KEY,
    name VARCHAR(160) NOT NULL,
    category VARCHAR(80) NOT NULL,
    description VARCHAR(500),
    metadata JSONB NOT NULL DEFAULT '{}'::jsonb,
    created_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL,
    version BIGINT NOT NULL DEFAULT 0
);

CREATE UNIQUE INDEX uq_products_name_ci
    ON catalog.products (LOWER(name));

CREATE INDEX idx_products_category
    ON catalog.products (category);

CREATE INDEX idx_products_metadata
    ON catalog.products USING gin (metadata jsonb_path_ops);

CREATE TABLE catalog.skus (
    id UUID PRIMARY KEY,
    product_id UUID NOT NULL REFERENCES catalog.products (id),
    sku_code VARCHAR(64) NOT NULL,
    name VARCHAR(160) NOT NULL,
    unit_of_measure VARCHAR(24) NOT NULL,
    storage_class VARCHAR(24) NOT NULL,
    shelf_life_days INTEGER,
    barcode VARCHAR(64),
    status VARCHAR(24) NOT NULL,
    created_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL,
    version BIGINT NOT NULL DEFAULT 0,
    CONSTRAINT uq_skus_sku_code UNIQUE (sku_code),
    CONSTRAINT chk_skus_storage_class
        CHECK (storage_class IN ('FROZEN', 'CHILLED', 'AMBIENT')),
    CONSTRAINT chk_skus_status
        CHECK (status IN ('ACTIVE', 'DISCONTINUED')),
    CONSTRAINT chk_skus_shelf_life_days
        CHECK (shelf_life_days IS NULL OR shelf_life_days >= 0)
);

CREATE INDEX idx_skus_product_id
    ON catalog.skus (product_id);

CREATE INDEX idx_skus_status
    ON catalog.skus (status);

CREATE INDEX idx_skus_storage_class
    ON catalog.skus (storage_class);
