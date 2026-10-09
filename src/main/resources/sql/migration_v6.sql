USE gudang_akhir;

CREATE TABLE IF NOT EXISTS stock_count (
    id          INT AUTO_INCREMENT PRIMARY KEY,
    count_code  VARCHAR(24) NOT NULL UNIQUE,
    rak_id      INT NOT NULL,
    created_by  INT NOT NULL,
    counted_by  INT NULL,
    posted_by   INT NULL,
    status      VARCHAR(16) NOT NULL,
    created_at  DATETIME NOT NULL,
    posted_at   DATETIME NULL,
    FOREIGN KEY (rak_id) REFERENCES rak(id),
    FOREIGN KEY (created_by) REFERENCES users(id),
    FOREIGN KEY (counted_by) REFERENCES users(id),
    FOREIGN KEY (posted_by) REFERENCES users(id),
    INDEX idx_stock_count_rack_status (rak_id, status)
);

CREATE TABLE IF NOT EXISTS stock_count_line (
    id                 INT AUTO_INCREMENT PRIMARY KEY,
    stock_count_id     INT NOT NULL,
    barang_id          INT NOT NULL,
    expected_quantity  INT NOT NULL,
    counted_quantity   INT NULL,
    UNIQUE KEY uk_stock_count_barang (stock_count_id, barang_id),
    FOREIGN KEY (stock_count_id) REFERENCES stock_count(id),
    FOREIGN KEY (barang_id) REFERENCES barang(id)
);
