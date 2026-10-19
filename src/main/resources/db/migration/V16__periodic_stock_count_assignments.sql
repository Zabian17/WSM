ALTER TABLE stock_count
    ADD COLUMN cycle_code VARCHAR(24) NULL AFTER count_code,
    ADD COLUMN cycle_type VARCHAR(16) NOT NULL DEFAULT 'AD_HOC' AFTER cycle_code,
    ADD INDEX idx_stock_count_cycle_code (cycle_code);

ALTER TABLE stock_count_line
    ADD COLUMN assigned_user_id INT NULL AFTER barang_id,
    ADD COLUMN counted_by INT NULL AFTER counted_quantity,
    ADD CONSTRAINT fk_stock_count_line_assigned_user FOREIGN KEY (assigned_user_id) REFERENCES users(id),
    ADD CONSTRAINT fk_stock_count_line_counted_by FOREIGN KEY (counted_by) REFERENCES users(id),
    ADD INDEX idx_stock_count_line_assignment (assigned_user_id, counted_quantity);
