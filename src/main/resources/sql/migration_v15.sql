USE gudang_akhir;

ALTER TABLE pick_task
    MODIFY COLUMN delivery_order_id INT NULL,
    ADD COLUMN material_usage_id INT NULL AFTER delivery_order_id,
    ADD INDEX idx_pick_task_mtu_status (material_usage_id, status),
    ADD CONSTRAINT chk_pick_task_source CHECK (
        (delivery_order_id IS NOT NULL AND material_usage_id IS NULL)
        OR (delivery_order_id IS NULL AND material_usage_id IS NOT NULL)
    ),
    ADD CONSTRAINT fk_pick_task_material_usage
        FOREIGN KEY (material_usage_id) REFERENCES material_usage(id);

INSERT IGNORE INTO pick_task (task_code, material_usage_id, status, created_at)
SELECT CONCAT('MTP-', request.id), request.id, 'OPEN',
       COALESCE(request.reviewed_at, request.created_at)
FROM material_usage request
WHERE request.status IN ('APPROVED', 'PARTIALLY_ISSUED');
