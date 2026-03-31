ALTER TABLE orders
    ADD CONSTRAINT uq_orders_external_reference UNIQUE (external_reference);