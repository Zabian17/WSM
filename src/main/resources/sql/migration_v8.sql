USE gudang_akhir;

SET @has_do_pick_scanned_expiry = (
    SELECT COUNT(*) FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = 'gudang_akhir' AND TABLE_NAME = 'delivery_order_pick_scan'
      AND COLUMN_NAME = 'scanned_expiry'
);
SET @sql = IF(@has_do_pick_scanned_expiry = 0,
    'ALTER TABLE delivery_order_pick_scan ADD COLUMN scanned_expiry DATE NULL AFTER scanned_batch',
    'SELECT ''delivery order pick scan scanned_expiry already exists'' AS info');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

CREATE TABLE IF NOT EXISTS pick_task (
    id                  INT AUTO_INCREMENT PRIMARY KEY,
    task_code           VARCHAR(24) NOT NULL UNIQUE,
    delivery_order_id   INT NOT NULL,
    assigned_picker_id  INT NULL,
    assigned_section_id INT NULL,
    status              VARCHAR(16) NOT NULL,
    created_at          DATETIME NOT NULL,
    assigned_at         DATETIME NULL,
    completed_at        DATETIME NULL,
    FOREIGN KEY (delivery_order_id) REFERENCES delivery_order(id),
    FOREIGN KEY (assigned_picker_id) REFERENCES users(id),
    FOREIGN KEY (assigned_section_id) REFERENCES section(id)
);

CREATE TABLE IF NOT EXISTS delivery_order_fulfillment_audit (
    id                     INT AUTO_INCREMENT PRIMARY KEY,
    delivery_order_id      INT NOT NULL,
    delivery_order_line_id INT NOT NULL,
    actor_id               INT NOT NULL,
    stage                  VARCHAR(16) NOT NULL,
    verified_quantity      INT NOT NULL,
    recorded_at            DATETIME NOT NULL,
    INDEX idx_do_fulfillment_audit_order_time (delivery_order_id, recorded_at),
    FOREIGN KEY (delivery_order_id) REFERENCES delivery_order(id),
    FOREIGN KEY (delivery_order_line_id) REFERENCES delivery_order_line(id),
    FOREIGN KEY (actor_id) REFERENCES users(id)
);
