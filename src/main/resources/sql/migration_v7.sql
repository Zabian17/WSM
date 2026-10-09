USE gudang_akhir;

SET @has_po_reviewed_by = (
    SELECT COUNT(*) FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = 'gudang_akhir' AND TABLE_NAME = 'purchase_order'
      AND COLUMN_NAME = 'reviewed_by'
);
SET @sql = IF(@has_po_reviewed_by = 0,
    'ALTER TABLE purchase_order ADD COLUMN reviewed_by INT NULL AFTER status',
    'SELECT ''purchase order reviewed_by already exists'' AS info');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @has_po_reviewed_at = (
    SELECT COUNT(*) FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = 'gudang_akhir' AND TABLE_NAME = 'purchase_order'
      AND COLUMN_NAME = 'reviewed_at'
);
SET @sql = IF(@has_po_reviewed_at = 0,
    'ALTER TABLE purchase_order ADD COLUMN reviewed_at DATETIME NULL AFTER reviewed_by',
    'SELECT ''purchase order reviewed_at already exists'' AS info');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @has_po_review_notes = (
    SELECT COUNT(*) FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = 'gudang_akhir' AND TABLE_NAME = 'purchase_order'
      AND COLUMN_NAME = 'review_notes'
);
SET @sql = IF(@has_po_review_notes = 0,
    'ALTER TABLE purchase_order ADD COLUMN review_notes TEXT NULL AFTER reviewed_at',
    'SELECT ''purchase order review_notes already exists'' AS info');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @has_po_reviewer_fk = (
    SELECT COUNT(*) FROM information_schema.KEY_COLUMN_USAGE
    WHERE CONSTRAINT_SCHEMA = 'gudang_akhir' AND TABLE_NAME = 'purchase_order'
      AND COLUMN_NAME = 'reviewed_by' AND REFERENCED_TABLE_NAME = 'users'
);
SET @sql = IF(@has_po_reviewer_fk = 0,
    'ALTER TABLE purchase_order ADD CONSTRAINT fk_purchase_order_reviewer FOREIGN KEY (reviewed_by) REFERENCES users(id)',
    'SELECT ''purchase order reviewer foreign key already exists'' AS info');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

UPDATE purchase_order SET status = 'APPROVED' WHERE status IN ('OPEN', 'PARTIALLY_RECEIVED');

CREATE TABLE IF NOT EXISTS sales_order (
    id          INT AUTO_INCREMENT PRIMARY KEY,
    order_code  VARCHAR(24) NOT NULL UNIQUE,
    customer_id INT NOT NULL,
    created_by  INT NOT NULL,
    status      VARCHAR(24) NOT NULL,
    notes       TEXT,
    created_at  DATETIME NOT NULL,
    FOREIGN KEY (customer_id) REFERENCES customer(id),
    FOREIGN KEY (created_by) REFERENCES users(id)
);

CREATE TABLE IF NOT EXISTS sales_order_line (
    id                 INT AUTO_INCREMENT PRIMARY KEY,
    sales_order_id     INT NOT NULL,
    barang_id          INT NOT NULL,
    ordered_quantity   INT NOT NULL,
    converted_quantity INT NOT NULL DEFAULT 0,
    UNIQUE KEY uk_sales_order_item (sales_order_id, barang_id),
    FOREIGN KEY (sales_order_id) REFERENCES sales_order(id),
    FOREIGN KEY (barang_id) REFERENCES barang(id)
);

SET @has_do_sales_order = (
    SELECT COUNT(*) FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = 'gudang_akhir' AND TABLE_NAME = 'delivery_order'
      AND COLUMN_NAME = 'sales_order_id'
);
SET @sql = IF(@has_do_sales_order = 0,
    'ALTER TABLE delivery_order ADD COLUMN sales_order_id INT NULL AFTER customer_id',
    'SELECT ''delivery order sales_order_id already exists'' AS info');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @has_do_so_fk = (
    SELECT COUNT(*) FROM information_schema.KEY_COLUMN_USAGE
    WHERE CONSTRAINT_SCHEMA = 'gudang_akhir' AND TABLE_NAME = 'delivery_order'
      AND COLUMN_NAME = 'sales_order_id' AND REFERENCED_TABLE_NAME = 'sales_order'
);
SET @sql = IF(@has_do_so_fk = 0,
    'ALTER TABLE delivery_order ADD CONSTRAINT fk_delivery_order_sales_order FOREIGN KEY (sales_order_id) REFERENCES sales_order(id)',
    'SELECT ''delivery order sales order foreign key already exists'' AS info');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @has_do_line_sales_order_line = (
    SELECT COUNT(*) FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = 'gudang_akhir' AND TABLE_NAME = 'delivery_order_line'
      AND COLUMN_NAME = 'sales_order_line_id'
);
SET @sql = IF(@has_do_line_sales_order_line = 0,
    'ALTER TABLE delivery_order_line ADD COLUMN sales_order_line_id INT NULL AFTER barang_id',
    'SELECT ''delivery order line sales_order_line_id already exists'' AS info');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @has_do_line_so_fk = (
    SELECT COUNT(*) FROM information_schema.KEY_COLUMN_USAGE
    WHERE CONSTRAINT_SCHEMA = 'gudang_akhir' AND TABLE_NAME = 'delivery_order_line'
      AND COLUMN_NAME = 'sales_order_line_id' AND REFERENCED_TABLE_NAME = 'sales_order_line'
);
SET @sql = IF(@has_do_line_so_fk = 0,
    'ALTER TABLE delivery_order_line ADD CONSTRAINT fk_delivery_order_line_sales_order_line FOREIGN KEY (sales_order_line_id) REFERENCES sales_order_line(id)',
    'SELECT ''delivery order line sales order line foreign key already exists'' AS info');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;
