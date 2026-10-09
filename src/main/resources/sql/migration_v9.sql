USE gudang_akhir;

CREATE TABLE IF NOT EXISTS material_usage (
    id           INT AUTO_INCREMENT PRIMARY KEY,
    request_code VARCHAR(24) NOT NULL UNIQUE,
    created_by   INT NOT NULL,
    reviewed_by  INT NULL,
    status       VARCHAR(24) NOT NULL,
    reason       TEXT NOT NULL,
    review_notes TEXT,
    created_at   DATETIME NOT NULL,
    reviewed_at  DATETIME NULL,
    issued_at    DATETIME NULL,
    FOREIGN KEY (created_by) REFERENCES users(id),
    FOREIGN KEY (reviewed_by) REFERENCES users(id)
);

CREATE TABLE IF NOT EXISTS material_usage_line (
    id                   INT AUTO_INCREMENT PRIMARY KEY,
    material_usage_id    INT NOT NULL,
    barang_id            INT NOT NULL,
    requested_quantity   INT NOT NULL,
    UNIQUE KEY uk_material_usage_item (material_usage_id, barang_id),
    FOREIGN KEY (material_usage_id) REFERENCES material_usage(id),
    FOREIGN KEY (barang_id) REFERENCES barang(id)
);
