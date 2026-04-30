CREATE TABLE partner.retailers (
    id UUID PRIMARY KEY,
    legal_name VARCHAR(160) NOT NULL,
    trading_name VARCHAR(120),
    registration_number VARCHAR(64),
    credit_status VARCHAR(32) NOT NULL,
    created_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL,
    version BIGINT NOT NULL DEFAULT 0,
    CONSTRAINT chk_retailers_credit_status
        CHECK (credit_status IN ('ACTIVE', 'ON_HOLD', 'BLOCKED')),
    CONSTRAINT uq_retailers_registration_number
        UNIQUE (registration_number)
);

CREATE INDEX idx_retailers_credit_status
    ON partner.retailers (credit_status);

CREATE TABLE partner.outlets (
    id UUID PRIMARY KEY,
    retailer_id UUID NOT NULL REFERENCES partner.retailers (id),
    name VARCHAR(120) NOT NULL,
    delivery_zone VARCHAR(32) NOT NULL,
    address_line_1 VARCHAR(180) NOT NULL,
    city VARCHAR(80) NOT NULL,
    state VARCHAR(80) NOT NULL,
    postal_code VARCHAR(20) NOT NULL,
    delivery_window_start TIME NOT NULL,
    delivery_window_end TIME NOT NULL,
    active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL,
    version BIGINT NOT NULL DEFAULT 0,
    CONSTRAINT chk_outlets_delivery_window
        CHECK (delivery_window_start < delivery_window_end)
);

CREATE INDEX idx_outlets_retailer_id
    ON partner.outlets (retailer_id);

CREATE INDEX idx_outlets_delivery_zone
    ON partner.outlets (delivery_zone);
