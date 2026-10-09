USE gudang_akhir;

SET @has_shipped_quantity = (
    SELECT COUNT(*) FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = 'gudang_akhir'
      AND TABLE_NAME = 'delivery_order_line'
      AND COLUMN_NAME = 'shipped_quantity'
);
SET @sql = IF(@has_shipped_quantity = 0,
    'ALTER TABLE delivery_order_line ADD COLUMN shipped_quantity INT NOT NULL DEFAULT 0 AFTER ordered_quantity',
    'SELECT ''shipped_quantity already exists'' AS info');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @has_packed_quantity = (
    SELECT COUNT(*) FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = 'gudang_akhir'
      AND TABLE_NAME = 'delivery_order_lot_reservation'
      AND COLUMN_NAME = 'packed_quantity'
);
SET @sql = IF(@has_packed_quantity = 0,
    'ALTER TABLE delivery_order_lot_reservation ADD COLUMN packed_quantity INT NOT NULL DEFAULT 0 AFTER picked_quantity',
    'SELECT ''packed_quantity already exists'' AS info');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @has_loaded_quantity = (
    SELECT COUNT(*) FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = 'gudang_akhir'
      AND TABLE_NAME = 'delivery_order_lot_reservation'
      AND COLUMN_NAME = 'loaded_quantity'
);
SET @sql = IF(@has_loaded_quantity = 0,
    'ALTER TABLE delivery_order_lot_reservation ADD COLUMN loaded_quantity INT NOT NULL DEFAULT 0 AFTER packed_quantity',
    'SELECT ''loaded_quantity already exists'' AS info');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

CREATE TABLE IF NOT EXISTS delivery_order_pick_scan (
    id                     INT AUTO_INCREMENT PRIMARY KEY,
    delivery_order_id      INT NOT NULL,
    delivery_order_line_id INT NOT NULL,
    picker_id              INT NOT NULL,
    lot_balance_id         INT NOT NULL,
    scanned_sku            VARCHAR(64) NOT NULL,
    scanned_rack           VARCHAR(10) NOT NULL,
    scanned_batch          VARCHAR(64) NOT NULL,
    quantity               INT NOT NULL,
    scanned_at             DATETIME NOT NULL,
    INDEX idx_do_pick_scan_order_time (delivery_order_id, scanned_at),
    FOREIGN KEY (delivery_order_id) REFERENCES delivery_order(id),
    FOREIGN KEY (delivery_order_line_id) REFERENCES delivery_order_line(id),
    FOREIGN KEY (picker_id) REFERENCES users(id),
    FOREIGN KEY (lot_balance_id) REFERENCES inventory_lot_balance(id)
);
