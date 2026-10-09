USE gudang_akhir;

CREATE TABLE IF NOT EXISTS purchase_order (
    id          INT AUTO_INCREMENT PRIMARY KEY,
    order_code  VARCHAR(24) NOT NULL UNIQUE,
    supplier_id INT NOT NULL,
    created_by  INT NOT NULL,
    status      VARCHAR(24) NOT NULL,
    notes       TEXT,
    created_at  DATETIME NOT NULL,
    open_line_count INT NOT NULL DEFAULT 0,
    received_quantity INT NOT NULL DEFAULT 0,
    FOREIGN KEY (supplier_id) REFERENCES supplier(id),
    FOREIGN KEY (created_by) REFERENCES users(id)
);

CREATE TABLE IF NOT EXISTS purchase_order_line (
    id                 INT AUTO_INCREMENT PRIMARY KEY,
    purchase_order_id  INT NOT NULL,
    barang_id          INT NOT NULL,
    ordered_quantity   INT NOT NULL,
    received_quantity  INT NOT NULL DEFAULT 0,
    pending_quantity   INT NOT NULL DEFAULT 0,
    UNIQUE KEY uk_purchase_order_item (purchase_order_id, barang_id),
    FOREIGN KEY (purchase_order_id) REFERENCES purchase_order(id),
    FOREIGN KEY (barang_id) REFERENCES barang(id)
);

SET @has_open_line_count = (
    SELECT COUNT(*) FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = 'gudang_akhir'
      AND TABLE_NAME = 'purchase_order'
      AND COLUMN_NAME = 'open_line_count'
);
SET @sql = IF(@has_open_line_count = 0,
    'ALTER TABLE purchase_order ADD COLUMN open_line_count INT NOT NULL DEFAULT 0',
    'SELECT ''open_line_count already exists'' AS info');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @has_received_quantity = (
    SELECT COUNT(*) FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = 'gudang_akhir'
      AND TABLE_NAME = 'purchase_order'
      AND COLUMN_NAME = 'received_quantity'
);
SET @sql = IF(@has_received_quantity = 0,
    'ALTER TABLE purchase_order ADD COLUMN received_quantity INT NOT NULL DEFAULT 0',
    'SELECT ''purchase order received_quantity already exists'' AS info');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @has_purchase_order_line_id = (
    SELECT COUNT(*) FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = 'gudang_akhir'
      AND TABLE_NAME = 'inbound_receipt'
      AND COLUMN_NAME = 'purchase_order_line_id'
);
SET @sql = IF(@has_purchase_order_line_id = 0,
    'ALTER TABLE inbound_receipt ADD COLUMN purchase_order_line_id INT NULL AFTER receipt_code',
    'SELECT ''purchase_order_line_id already exists'' AS info');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @has_purchase_order_line_fk = (
    SELECT COUNT(*) FROM information_schema.KEY_COLUMN_USAGE
    WHERE CONSTRAINT_SCHEMA = 'gudang_akhir'
      AND TABLE_NAME = 'inbound_receipt'
      AND COLUMN_NAME = 'purchase_order_line_id'
      AND REFERENCED_TABLE_NAME = 'purchase_order_line'
      AND REFERENCED_COLUMN_NAME = 'id'
);
SET @sql = IF(@has_purchase_order_line_fk = 0,
    'ALTER TABLE inbound_receipt ADD CONSTRAINT fk_inbound_receipt_purchase_order_line FOREIGN KEY (purchase_order_line_id) REFERENCES purchase_order_line(id)',
    'SELECT ''purchase order receipt foreign key already exists'' AS info');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;
