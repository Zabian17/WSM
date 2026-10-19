ALTER TABLE stock_count
    ADD COLUMN due_date DATE NULL AFTER created_at,
    ADD INDEX idx_stock_count_due_date (due_date, status);
