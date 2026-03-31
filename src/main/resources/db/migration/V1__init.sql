CREATE TABLE orders (
    id                 UUID         PRIMARY KEY,
    external_reference VARCHAR(255),
    amount             NUMERIC(19,2),
    currency           VARCHAR(10),
    status             VARCHAR(50)  NOT NULL,
    created_at         TIMESTAMP,
    updated_at         TIMESTAMP
);

CREATE TABLE order_items (
    id          UUID         PRIMARY KEY,
    order_id    UUID         NOT NULL REFERENCES orders(id),
    description VARCHAR(255),
    quantity    INT,
    amount      NUMERIC(19,2),
    currency    VARCHAR(10)
);

CREATE TABLE payments (
    id                 UUID         PRIMARY KEY,
    gateway_payment_id VARCHAR(255),
    idempotency_key    VARCHAR(255) UNIQUE,
    amount             NUMERIC(19,2),
    currency           VARCHAR(10),
    method             VARCHAR(50),
    status             VARCHAR(50),
    received_at        TIMESTAMP,
    processed_at       TIMESTAMP
);

CREATE TABLE reconciliation_results (
    id                UUID        PRIMARY KEY,
    order_id          UUID        REFERENCES orders(id),
    payment_id        UUID        REFERENCES payments(id),
    status            VARCHAR(50) NOT NULL,
    divergence_reason TEXT,
    reconciled_at     TIMESTAMP
);