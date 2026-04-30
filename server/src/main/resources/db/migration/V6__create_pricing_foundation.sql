CREATE TABLE pricing.price_books (
    id UUID PRIMARY KEY,
    code VARCHAR(64) NOT NULL,
    name VARCHAR(160) NOT NULL,
    currency VARCHAR(3) NOT NULL,
    active BOOLEAN NOT NULL DEFAULT TRUE,
    valid_from DATE NOT NULL,
    valid_until DATE,
    created_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL,
    version BIGINT NOT NULL DEFAULT 0,
    CONSTRAINT uq_price_books_code UNIQUE (code),
    CONSTRAINT chk_price_books_currency
        CHECK (currency = UPPER(currency)),
    CONSTRAINT chk_price_books_validity
        CHECK (valid_until IS NULL OR valid_until >= valid_from)
);

CREATE INDEX idx_price_books_active
    ON pricing.price_books (active);

CREATE INDEX idx_price_books_validity
    ON pricing.price_books (valid_from, valid_until);

CREATE TABLE pricing.price_rules (
    id UUID PRIMARY KEY,
    price_book_id UUID NOT NULL REFERENCES pricing.price_books (id),
    rule_code VARCHAR(64) NOT NULL,
    description VARCHAR(240),
    sku_id UUID NOT NULL REFERENCES catalog.skus (id),
    retailer_id UUID REFERENCES partner.retailers (id),
    delivery_zone VARCHAR(32),
    min_quantity NUMERIC(12, 3) NOT NULL DEFAULT 0,
    unit_price NUMERIC(12, 2) NOT NULL,
    priority INTEGER NOT NULL DEFAULT 0,
    active BOOLEAN NOT NULL DEFAULT TRUE,
    valid_from DATE NOT NULL,
    valid_until DATE,
    created_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL,
    version BIGINT NOT NULL DEFAULT 0,
    CONSTRAINT uq_price_rules_rule_code UNIQUE (rule_code),
    CONSTRAINT chk_price_rules_min_quantity
        CHECK (min_quantity >= 0),
    CONSTRAINT chk_price_rules_unit_price
        CHECK (unit_price > 0),
    CONSTRAINT chk_price_rules_validity
        CHECK (valid_until IS NULL OR valid_until >= valid_from)
);

CREATE INDEX idx_price_rules_price_book_id
    ON pricing.price_rules (price_book_id);

CREATE INDEX idx_price_rules_sku_id
    ON pricing.price_rules (sku_id);

CREATE INDEX idx_price_rules_retailer_id
    ON pricing.price_rules (retailer_id);

CREATE INDEX idx_price_rules_delivery_zone
    ON pricing.price_rules (delivery_zone);

CREATE INDEX idx_price_rules_active_lookup
    ON pricing.price_rules (sku_id, active, valid_from, valid_until);
