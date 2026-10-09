USE gudang_akhir;

CREATE TABLE IF NOT EXISTS material_usage_scan (
    id                     INT AUTO_INCREMENT PRIMARY KEY,
    material_usage_id      INT NOT NULL,
    material_usage_line_id INT NOT NULL,
    issuer_id              INT NOT NULL,
    lot_balance_id         INT NOT NULL,
    scan_type              VARCHAR(10) NOT NULL,
    scanned_sku            VARCHAR(64) NOT NULL,
    scanned_rack           VARCHAR(10) NOT NULL,
    scanned_batch          VARCHAR(64) NOT NULL,
    scanned_expiry         DATE NULL,
    quantity               INT NOT NULL,
    scanned_at             DATETIME NOT NULL,
    CONSTRAINT chk_mtu_scan_quantity CHECK (quantity > 0),
    INDEX idx_mtu_scan_request_time (material_usage_id, scanned_at, id),
    FOREIGN KEY (material_usage_id) REFERENCES material_usage(id),
    FOREIGN KEY (material_usage_line_id) REFERENCES material_usage_line(id),
    FOREIGN KEY (issuer_id) REFERENCES users(id),
    FOREIGN KEY (lot_balance_id) REFERENCES inventory_lot_balance(id)
);
