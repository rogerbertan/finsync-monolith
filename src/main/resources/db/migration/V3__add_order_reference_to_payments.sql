ALTER TABLE payments ADD COLUMN order_external_reference VARCHAR(255) NOT NULL DEFAULT '';
ALTER TABLE payments ALTER COLUMN order_external_reference DROP DEFAULT;