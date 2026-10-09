USE gudang_akhir;

CREATE TABLE IF NOT EXISTS warehouse (
    id             INT AUTO_INCREMENT PRIMARY KEY,
    warehouse_code VARCHAR(20) NOT NULL UNIQUE,
    warehouse_name VARCHAR(100) NOT NULL,
    address        VARCHAR(255) NULL,
    active         BOOLEAN NOT NULL DEFAULT TRUE
);

INSERT INTO warehouse (warehouse_code, warehouse_name, active)
SELECT 'WH-MAIN', 'Gudang Utama', TRUE
WHERE NOT EXISTS (SELECT 1 FROM warehouse WHERE warehouse_code = 'WH-MAIN');

SET @has_section_warehouse = (
    SELECT COUNT(*) FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = 'gudang_akhir' AND TABLE_NAME = 'section'
      AND COLUMN_NAME = 'warehouse_id'
);
SET @sql = IF(@has_section_warehouse = 0,
    'ALTER TABLE section ADD COLUMN warehouse_id INT NULL AFTER kode_section',
    'SELECT ''section warehouse_id already exists'' AS info');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @default_warehouse_id = (
    SELECT id FROM warehouse WHERE warehouse_code = 'WH-MAIN' LIMIT 1
);
UPDATE section SET warehouse_id = @default_warehouse_id WHERE warehouse_id IS NULL;

SET @has_section_warehouse_fk = (
    SELECT COUNT(*) FROM information_schema.KEY_COLUMN_USAGE
    WHERE CONSTRAINT_SCHEMA = 'gudang_akhir' AND TABLE_NAME = 'section'
      AND COLUMN_NAME = 'warehouse_id' AND REFERENCED_TABLE_NAME = 'warehouse'
);
SET @sql = IF(@has_section_warehouse_fk = 0,
    'ALTER TABLE section ADD CONSTRAINT fk_section_warehouse FOREIGN KEY (warehouse_id) REFERENCES warehouse(id)',
    'SELECT ''section warehouse foreign key already exists'' AS info');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

ALTER TABLE section MODIFY warehouse_id INT NOT NULL;

CREATE TABLE IF NOT EXISTS stock_transfer_order (
    id                      INT AUTO_INCREMENT PRIMARY KEY,
    order_code              VARCHAR(24) NOT NULL UNIQUE,
    source_warehouse_id     INT NOT NULL,
    destination_warehouse_id INT NOT NULL,
    created_by              INT NOT NULL,
    reviewed_by             INT NULL,
    status                  VARCHAR(20) NOT NULL,
    reason                  TEXT NOT NULL,
    review_notes            TEXT,
    created_at              DATETIME NOT NULL,
    reviewed_at             DATETIME NULL,
    dispatched_at           DATETIME NULL,
    received_at             DATETIME NULL,
    FOREIGN KEY (source_warehouse_id) REFERENCES warehouse(id),
    FOREIGN KEY (destination_warehouse_id) REFERENCES warehouse(id),
    FOREIGN KEY (created_by) REFERENCES users(id),
    FOREIGN KEY (reviewed_by) REFERENCES users(id)
);

CREATE TABLE IF NOT EXISTS stock_transfer_order_line (
    id                       INT AUTO_INCREMENT PRIMARY KEY,
    stock_transfer_order_id  INT NOT NULL,
    barang_id                INT NOT NULL,
    source_rak_id            INT NOT NULL,
    destination_rak_id       INT NOT NULL,
    quantity                 INT NOT NULL,
    FOREIGN KEY (stock_transfer_order_id) REFERENCES stock_transfer_order(id),
    FOREIGN KEY (barang_id) REFERENCES barang(id),
    FOREIGN KEY (source_rak_id) REFERENCES rak(id),
    FOREIGN KEY (destination_rak_id) REFERENCES rak(id)
);

CREATE TABLE IF NOT EXISTS stock_transfer_order_lot (
    id                          INT AUTO_INCREMENT PRIMARY KEY,
    stock_transfer_order_line_id INT NOT NULL,
    batch_code                  VARCHAR(64) NOT NULL,
    expiry_date                 DATE NULL,
    quantity                    INT NOT NULL,
    FOREIGN KEY (stock_transfer_order_line_id) REFERENCES stock_transfer_order_line(id)
);
