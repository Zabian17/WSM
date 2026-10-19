CREATE TABLE material_usage_reservation (
    id INT NOT NULL AUTO_INCREMENT,
    material_usage_line_id INT NOT NULL,
    lot_balance_id INT NOT NULL,
    reserved_quantity INT NOT NULL,
    issued_quantity INT NOT NULL DEFAULT 0,
    PRIMARY KEY (id),
    CONSTRAINT uk_mtu_line_lot UNIQUE (material_usage_line_id, lot_balance_id),
    CONSTRAINT fk_mtu_reservation_line FOREIGN KEY (material_usage_line_id)
        REFERENCES material_usage_line (id) ON DELETE CASCADE,
    CONSTRAINT fk_mtu_reservation_lot FOREIGN KEY (lot_balance_id)
        REFERENCES inventory_lot_balance (id),
    CONSTRAINT chk_mtu_reserved_quantity CHECK (reserved_quantity > 0),
    CONSTRAINT chk_mtu_issued_quantity CHECK (issued_quantity >= 0 AND issued_quantity <= reserved_quantity)
);
