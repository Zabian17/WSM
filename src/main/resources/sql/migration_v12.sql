USE gudang_akhir;

SET @has_sto_picker = (
    SELECT COUNT(*) FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = 'gudang_akhir' AND TABLE_NAME = 'stock_transfer_order'
      AND COLUMN_NAME = 'assigned_picker_id'
);
SET @sql = IF(@has_sto_picker = 0,
    'ALTER TABLE stock_transfer_order ADD COLUMN assigned_picker_id INT NULL AFTER reviewed_by, ADD COLUMN assigned_at DATETIME NULL AFTER reviewed_at',
    'SELECT ''STO picker columns already exist'' AS info');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @sto_status_length = (
    SELECT CHARACTER_MAXIMUM_LENGTH FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = 'gudang_akhir' AND TABLE_NAME = 'stock_transfer_order'
      AND COLUMN_NAME = 'status'
);
SET @sql = IF(@sto_status_length < 40,
    'ALTER TABLE stock_transfer_order MODIFY COLUMN status VARCHAR(40) NOT NULL',
    'SELECT ''STO status column already supports partial states'' AS info');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @has_sto_picker_fk = (
    SELECT COUNT(*) FROM information_schema.KEY_COLUMN_USAGE
    WHERE CONSTRAINT_SCHEMA = 'gudang_akhir' AND TABLE_NAME = 'stock_transfer_order'
      AND COLUMN_NAME = 'assigned_picker_id' AND REFERENCED_TABLE_NAME = 'users'
);
SET @sql = IF(@has_sto_picker_fk = 0,
    'ALTER TABLE stock_transfer_order ADD CONSTRAINT fk_sto_assigned_picker FOREIGN KEY (assigned_picker_id) REFERENCES users(id)',
    'SELECT ''STO picker foreign key already exists'' AS info');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

CREATE TABLE IF NOT EXISTS stock_transfer_order_scan (
    id                           INT AUTO_INCREMENT PRIMARY KEY,
    stock_transfer_order_id      INT NOT NULL,
    stock_transfer_order_line_id INT NOT NULL,
    picker_id                    INT NOT NULL,
    lot_balance_id               INT NULL,
    source_scan_id               INT NULL,
    scan_type                    VARCHAR(10) NOT NULL,
    scanned_sku                  VARCHAR(64) NOT NULL,
    scanned_rack                 VARCHAR(10) NOT NULL,
    scanned_batch                VARCHAR(64) NOT NULL,
    scanned_expiry               DATE NULL,
    quantity                     INT NOT NULL,
    scanned_at                   DATETIME NOT NULL,
    CONSTRAINT chk_sto_scan_quantity CHECK (quantity > 0),
    INDEX idx_sto_scan_order_time (stock_transfer_order_id, scanned_at, id),
    INDEX idx_sto_scan_source (source_scan_id),
    FOREIGN KEY (stock_transfer_order_id) REFERENCES stock_transfer_order(id),
    FOREIGN KEY (stock_transfer_order_line_id) REFERENCES stock_transfer_order_line(id),
    FOREIGN KEY (picker_id) REFERENCES users(id),
    FOREIGN KEY (lot_balance_id) REFERENCES inventory_lot_balance(id),
    FOREIGN KEY (source_scan_id) REFERENCES stock_transfer_order_scan(id)
);
