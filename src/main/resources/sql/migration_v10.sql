USE gudang_akhir;

CREATE TABLE IF NOT EXISTS return_request (
    id           INT AUTO_INCREMENT PRIMARY KEY,
    return_code  VARCHAR(24) NOT NULL UNIQUE,
    return_type  VARCHAR(16) NOT NULL,
    status       VARCHAR(20) NOT NULL,
    reason       TEXT NOT NULL,
    created_by   INT NOT NULL,
    reviewed_by  INT NULL,
    review_notes TEXT,
    created_at   DATETIME NOT NULL,
    reviewed_at  DATETIME NULL,
    posted_at    DATETIME NULL,
    FOREIGN KEY (created_by) REFERENCES users(id),
    FOREIGN KEY (reviewed_by) REFERENCES users(id)
);

CREATE TABLE IF NOT EXISTS return_request_line (
    id                     INT AUTO_INCREMENT PRIMARY KEY,
    return_request_id      INT NOT NULL,
    purchase_order_line_id INT NULL,
    delivery_order_line_id INT NULL,
    barang_id              INT NOT NULL,
    rak_id                 INT NOT NULL,
    batch_code             VARCHAR(64) NOT NULL,
    expiry_date            DATE NULL,
    quantity               INT NOT NULL,
    FOREIGN KEY (return_request_id) REFERENCES return_request(id),
    FOREIGN KEY (purchase_order_line_id) REFERENCES purchase_order_line(id),
    FOREIGN KEY (delivery_order_line_id) REFERENCES delivery_order_line(id),
    FOREIGN KEY (barang_id) REFERENCES barang(id),
    FOREIGN KEY (rak_id) REFERENCES rak(id)
);
