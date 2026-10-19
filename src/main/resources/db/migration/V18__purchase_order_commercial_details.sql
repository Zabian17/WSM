ALTER TABLE purchase_order
    ADD COLUMN currency_code CHAR(3) NOT NULL DEFAULT 'IDR' AFTER notes,
    ADD COLUMN tender_reference VARCHAR(80) NULL AFTER currency_code,
    ADD COLUMN payment_terms VARCHAR(120) NULL AFTER tender_reference,
    ADD COLUMN expected_delivery_date DATE NULL AFTER payment_terms;

ALTER TABLE purchase_order_line
    ADD COLUMN unit_price DECIMAL(19,4) NOT NULL DEFAULT 0 AFTER ordered_quantity;
