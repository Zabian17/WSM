-- ============================================================
--  GUDANGKU - Database Schema
--  Database : gudang_akhir
--  Dibuat   : 13 Mei 2026
--  Jalankan file ini di phpMyAdmin atau MySQL CLI
-- ============================================================

-- Buat database jika belum ada
CREATE DATABASE IF NOT EXISTS gudang_akhir
    CHARACTER SET utf8mb4
    COLLATE utf8mb4_unicode_ci;

USE gudang_akhir;

CREATE TABLE IF NOT EXISTS users (
    id            INT AUTO_INCREMENT PRIMARY KEY,
    full_name     VARCHAR(100)  NOT NULL,
    email         VARCHAR(150)  NOT NULL UNIQUE,
    password_hash VARCHAR(255)  NOT NULL,
    role          ENUM('admin', 'user') DEFAULT 'user',
    department    VARCHAR(100) NULL,
    profile_picture_path VARCHAR(500) NULL,
    created_at    TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at    TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS approval_routing_rule (
    id                   INT AUTO_INCREMENT PRIMARY KEY,
    requester_department VARCHAR(100) NOT NULL UNIQUE,
    approver_department  VARCHAR(100) NOT NULL,
    CONSTRAINT chk_approval_departments_differ
        CHECK (LOWER(requester_department) <> LOWER(approver_department))
);





CREATE TABLE IF NOT EXISTS barang (
    id          INT AUTO_INCREMENT PRIMARY KEY,
    kode_barang VARCHAR(20)   NOT NULL UNIQUE,
    nama_barang VARCHAR(150)  NOT NULL,
    kategori    VARCHAR(80),
    satuan      VARCHAR(20)   DEFAULT 'pcs',
    volume_cbm  DECIMAL(12,6) NULL,
    storage_type VARCHAR(40) NULL,
    stok        INT           DEFAULT 0,
    stok_min    INT           DEFAULT 0,   
    stok_max    INT           DEFAULT 0,   
    created_at  TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at  TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
);





CREATE TABLE IF NOT EXISTS supplier (
    id           INT AUTO_INCREMENT PRIMARY KEY,
    nama_supplier VARCHAR(150) NOT NULL,
    kontak        VARCHAR(20),
    email         VARCHAR(150),
    alamat        TEXT,
    created_at   TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);





CREATE TABLE IF NOT EXISTS warehouse (
    id             INT AUTO_INCREMENT PRIMARY KEY,
    warehouse_code VARCHAR(20) NOT NULL UNIQUE,
    warehouse_name VARCHAR(100) NOT NULL,
    address        VARCHAR(255) NULL,
    active         BOOLEAN NOT NULL DEFAULT TRUE
);

INSERT IGNORE INTO warehouse (id, warehouse_code, warehouse_name, active)
VALUES (1, 'WH-MAIN', 'Gudang Utama', TRUE);

CREATE TABLE IF NOT EXISTS section (
    id           INT AUTO_INCREMENT PRIMARY KEY,
    kode_section VARCHAR(10)  NOT NULL UNIQUE,
    warehouse_id INT NOT NULL DEFAULT 1,
    nama_section VARCHAR(80),
    kapasitas    INT          DEFAULT 12,  
    target_kategori VARCHAR(80) NOT NULL DEFAULT 'Semua',
    alert_threshold INT NOT NULL DEFAULT 80,
    alert_type VARCHAR(50) NOT NULL DEFAULT 'Kapasitas Penuh (>= %)',
    alert_active BOOLEAN NOT NULL DEFAULT TRUE,
    zone_code VARCHAR(40) NULL,
    storage_type VARCHAR(40) NOT NULL DEFAULT 'GENERAL',
    putaway_priority INT NOT NULL DEFAULT 50,
    created_at   TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (warehouse_id) REFERENCES warehouse(id)
);





CREATE TABLE IF NOT EXISTS rak (
    id           INT AUTO_INCREMENT PRIMARY KEY,
    section_id   INT NOT NULL,
    kode_rak     VARCHAR(10)  NOT NULL UNIQUE,
    kapasitas    INT          DEFAULT 100, 
    terisi       INT          DEFAULT 0,
    capacity_cbm DECIMAL(12,6) NULL,
    created_at   TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (section_id) REFERENCES section(id) ON DELETE CASCADE
);

CREATE TABLE IF NOT EXISTS inventory_balance (
    id           INT AUTO_INCREMENT PRIMARY KEY,
    barang_id    INT NOT NULL,
    rak_id       INT NULL,
    location_key VARCHAR(24) NOT NULL,
    quantity     INT NOT NULL DEFAULT 0,
    UNIQUE KEY uk_inventory_barang_location (barang_id, location_key),
    FOREIGN KEY (barang_id) REFERENCES barang(id),
    FOREIGN KEY (rak_id) REFERENCES rak(id)
);

CREATE TABLE IF NOT EXISTS inventory_lot_balance (
    id           INT AUTO_INCREMENT PRIMARY KEY,
    barang_id    INT NOT NULL,
    rak_id       INT NULL,
    location_key VARCHAR(24) NOT NULL,
    batch_code   VARCHAR(64) NOT NULL,
    expiry_date  DATE NULL,
    received_at  DATETIME NOT NULL,
    quantity     INT NOT NULL DEFAULT 0,
    reserved_quantity INT NOT NULL DEFAULT 0,
    UNIQUE KEY uk_lot_barang_location_batch (barang_id, location_key, batch_code),
    FOREIGN KEY (barang_id) REFERENCES barang(id),
    FOREIGN KEY (rak_id) REFERENCES rak(id)
);

CREATE TABLE IF NOT EXISTS inventory_transfer (
    id                  INT AUTO_INCREMENT PRIMARY KEY,
    transfer_code       VARCHAR(24) NOT NULL UNIQUE,
    barang_id           INT NOT NULL,
    source_rak_id       INT NULL,
    destination_rak_id  INT NOT NULL,
    quantity            INT NOT NULL,
    notes               TEXT,
    user_id             INT NOT NULL,
    created_at          TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (barang_id) REFERENCES barang(id),
    FOREIGN KEY (source_rak_id) REFERENCES rak(id),
    FOREIGN KEY (destination_rak_id) REFERENCES rak(id),
    FOREIGN KEY (user_id) REFERENCES users(id)
);

-- ============================================================
-- TABEL: transaksi
-- Mencatat setiap gerakan barang masuk dan keluar
-- ============================================================
CREATE TABLE IF NOT EXISTS transaksi (
    id            INT AUTO_INCREMENT PRIMARY KEY,
    kode_transaksi VARCHAR(20) NOT NULL UNIQUE,
    tipe          ENUM('masuk', 'keluar') NOT NULL,
    barang_id     INT NOT NULL,
    supplier_id   INT,                         
    rak_id        INT,
    jumlah        INT          NOT NULL,
    keterangan    TEXT,
    user_id       INT NOT NULL,                
    tanggal       TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (barang_id)   REFERENCES barang(id),
    FOREIGN KEY (supplier_id) REFERENCES supplier(id),
    FOREIGN KEY (rak_id)      REFERENCES rak(id),
    FOREIGN KEY (user_id)     REFERENCES users(id)
);

CREATE TABLE IF NOT EXISTS transaction_lot_allocation (
    id             INT AUTO_INCREMENT PRIMARY KEY,
    transaksi_id   INT NOT NULL,
    lot_balance_id INT NOT NULL,
    quantity       INT NOT NULL,
    FOREIGN KEY (transaksi_id) REFERENCES transaksi(id),
    FOREIGN KEY (lot_balance_id) REFERENCES inventory_lot_balance(id)
);

CREATE TABLE IF NOT EXISTS inventory_transfer_lot_allocation (
    id             INT AUTO_INCREMENT PRIMARY KEY,
    transfer_id    INT NOT NULL,
    lot_balance_id INT NOT NULL,
    quantity       INT NOT NULL,
    FOREIGN KEY (transfer_id) REFERENCES inventory_transfer(id),
    FOREIGN KEY (lot_balance_id) REFERENCES inventory_lot_balance(id)
);

CREATE TABLE IF NOT EXISTS purchase_order (
    id          INT AUTO_INCREMENT PRIMARY KEY,
    order_code  VARCHAR(24) NOT NULL UNIQUE,
    supplier_id INT NOT NULL,
    created_by  INT NOT NULL,
    status      VARCHAR(24) NOT NULL,
    reviewed_by INT NULL,
    reviewed_at DATETIME NULL,
    review_notes TEXT,
    notes       TEXT,
    currency_code CHAR(3) NOT NULL DEFAULT 'IDR',
    tender_reference VARCHAR(80) NULL,
    payment_terms VARCHAR(120) NULL,
    expected_delivery_date DATE NULL,
    created_at  DATETIME NOT NULL,
    open_line_count INT NOT NULL DEFAULT 0,
    received_quantity INT NOT NULL DEFAULT 0,
    FOREIGN KEY (supplier_id) REFERENCES supplier(id),
    FOREIGN KEY (created_by) REFERENCES users(id),
    FOREIGN KEY (reviewed_by) REFERENCES users(id)
);

CREATE TABLE IF NOT EXISTS purchase_order_line (
    id                 INT AUTO_INCREMENT PRIMARY KEY,
    purchase_order_id  INT NOT NULL,
    barang_id          INT NOT NULL,
    ordered_quantity   INT NOT NULL,
    unit_price         DECIMAL(19,4) NOT NULL DEFAULT 0,
    received_quantity  INT NOT NULL DEFAULT 0,
    pending_quantity   INT NOT NULL DEFAULT 0,
    UNIQUE KEY uk_purchase_order_item (purchase_order_id, barang_id),
    FOREIGN KEY (purchase_order_id) REFERENCES purchase_order(id),
    FOREIGN KEY (barang_id) REFERENCES barang(id)
);

CREATE TABLE IF NOT EXISTS inbound_receipt (
    id              INT AUTO_INCREMENT PRIMARY KEY,
    receipt_code    VARCHAR(24) NOT NULL UNIQUE,
    purchase_order_line_id INT NULL,
    barang_id       INT NOT NULL,
    supplier_id     INT NOT NULL,
    quantity        INT NOT NULL,
    batch_code      VARCHAR(64) NOT NULL,
    expiry_date     DATE NULL,
    notes           TEXT,
    status          VARCHAR(24) NOT NULL,
    qc_notes        TEXT,
    created_by      INT NOT NULL,
    qc_by           INT NULL,
    putaway_rak_id  INT NULL,
    created_at      DATETIME NOT NULL,
    qc_at           DATETIME NULL,
    FOREIGN KEY (barang_id) REFERENCES barang(id),
    FOREIGN KEY (supplier_id) REFERENCES supplier(id),
    CONSTRAINT fk_inbound_receipt_purchase_order_line
        FOREIGN KEY (purchase_order_line_id) REFERENCES purchase_order_line(id),
    FOREIGN KEY (created_by) REFERENCES users(id),
    FOREIGN KEY (qc_by) REFERENCES users(id),
    FOREIGN KEY (putaway_rak_id) REFERENCES rak(id)
);

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

CREATE TABLE IF NOT EXISTS delivery_order (
    id          INT AUTO_INCREMENT PRIMARY KEY,
    order_code  VARCHAR(24) NOT NULL UNIQUE,
    customer_id INT NOT NULL,
    sales_order_id INT NULL,
    created_by  INT NOT NULL,
    status      VARCHAR(24) NOT NULL,
    notes       TEXT,
    created_at  DATETIME NOT NULL,
    FOREIGN KEY (customer_id) REFERENCES customer(id),
    FOREIGN KEY (sales_order_id) REFERENCES sales_order(id),
    FOREIGN KEY (created_by) REFERENCES users(id)
);

CREATE TABLE IF NOT EXISTS delivery_order_line (
    id                 INT AUTO_INCREMENT PRIMARY KEY,
    delivery_order_id  INT NOT NULL,
    barang_id          INT NOT NULL,
    sales_order_line_id INT NULL,
    ordered_quantity   INT NOT NULL,
    shipped_quantity   INT NOT NULL DEFAULT 0,
    UNIQUE KEY uk_delivery_order_item (delivery_order_id, barang_id),
    FOREIGN KEY (delivery_order_id) REFERENCES delivery_order(id),
    FOREIGN KEY (sales_order_line_id) REFERENCES sales_order_line(id),
    FOREIGN KEY (barang_id) REFERENCES barang(id)
);

CREATE TABLE IF NOT EXISTS delivery_order_lot_reservation (
    id                    INT AUTO_INCREMENT PRIMARY KEY,
    delivery_order_line_id INT NOT NULL,
    lot_balance_id        INT NOT NULL,
    reserved_quantity     INT NOT NULL,
    picked_quantity       INT NOT NULL DEFAULT 0,
    packed_quantity       INT NOT NULL DEFAULT 0,
    loaded_quantity       INT NOT NULL DEFAULT 0,
    UNIQUE KEY uk_do_line_lot (delivery_order_line_id, lot_balance_id),
    FOREIGN KEY (delivery_order_line_id) REFERENCES delivery_order_line(id),
    FOREIGN KEY (lot_balance_id) REFERENCES inventory_lot_balance(id)
);

CREATE TABLE IF NOT EXISTS delivery_order_pick_scan (
    id                    INT AUTO_INCREMENT PRIMARY KEY,
    delivery_order_id     INT NOT NULL,
    delivery_order_line_id INT NOT NULL,
    picker_id             INT NOT NULL,
    lot_balance_id        INT NOT NULL,
    scanned_sku           VARCHAR(64) NOT NULL,
    scanned_rack          VARCHAR(10) NOT NULL,
    scanned_batch         VARCHAR(64) NOT NULL,
    scanned_expiry        DATE NULL,
    quantity              INT NOT NULL,
    scanned_at            DATETIME NOT NULL,
    INDEX idx_do_pick_scan_order_time (delivery_order_id, scanned_at),
    FOREIGN KEY (delivery_order_id) REFERENCES delivery_order(id),
    FOREIGN KEY (delivery_order_line_id) REFERENCES delivery_order_line(id),
    FOREIGN KEY (picker_id) REFERENCES users(id),
    FOREIGN KEY (lot_balance_id) REFERENCES inventory_lot_balance(id)
);

CREATE TABLE IF NOT EXISTS delivery_order_fulfillment_audit (
    id                    INT AUTO_INCREMENT PRIMARY KEY,
    delivery_order_id     INT NOT NULL,
    delivery_order_line_id INT NOT NULL,
    actor_id              INT NOT NULL,
    stage                 VARCHAR(16) NOT NULL,
    verified_quantity     INT NOT NULL,
    recorded_at           DATETIME NOT NULL,
    INDEX idx_do_fulfillment_audit_order_time (delivery_order_id, recorded_at),
    FOREIGN KEY (delivery_order_id) REFERENCES delivery_order(id),
    FOREIGN KEY (delivery_order_line_id) REFERENCES delivery_order_line(id),
    FOREIGN KEY (actor_id) REFERENCES users(id)
);

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

CREATE TABLE IF NOT EXISTS material_usage_scan (
    id                    INT AUTO_INCREMENT PRIMARY KEY,
    material_usage_id     INT NOT NULL,
    material_usage_line_id INT NOT NULL,
    issuer_id             INT NOT NULL,
    lot_balance_id        INT NOT NULL,
    scan_type             VARCHAR(10) NOT NULL,
    scanned_sku           VARCHAR(64) NOT NULL,
    scanned_rack          VARCHAR(10) NOT NULL,
    scanned_batch         VARCHAR(64) NOT NULL,
    scanned_expiry        DATE NULL,
    quantity              INT NOT NULL,
    scanned_at            DATETIME NOT NULL,
    CONSTRAINT chk_mtu_scan_quantity CHECK (quantity > 0),
    INDEX idx_mtu_scan_request_time (material_usage_id, scanned_at, id),
    FOREIGN KEY (material_usage_id) REFERENCES material_usage(id),
    FOREIGN KEY (material_usage_line_id) REFERENCES material_usage_line(id),
    FOREIGN KEY (issuer_id) REFERENCES users(id),
    FOREIGN KEY (lot_balance_id) REFERENCES inventory_lot_balance(id)
);

CREATE TABLE IF NOT EXISTS pick_task (
    id                  INT AUTO_INCREMENT PRIMARY KEY,
    task_code           VARCHAR(24) NOT NULL UNIQUE,
    delivery_order_id   INT NULL,
    material_usage_id   INT NULL,
    assigned_picker_id  INT NULL,
    assigned_section_id INT NULL,
    assigned_shift      VARCHAR(40) NOT NULL DEFAULT 'FLEXIBLE',
    status              VARCHAR(16) NOT NULL,
    created_at          DATETIME NOT NULL,
    assigned_at         DATETIME NULL,
    completed_at        DATETIME NULL,
    CONSTRAINT chk_pick_task_source CHECK (
        (delivery_order_id IS NOT NULL AND material_usage_id IS NULL)
        OR (delivery_order_id IS NULL AND material_usage_id IS NOT NULL)
    ),
    INDEX idx_pick_task_mtu_status (material_usage_id, status),
    FOREIGN KEY (delivery_order_id) REFERENCES delivery_order(id),
    FOREIGN KEY (material_usage_id) REFERENCES material_usage(id),
    FOREIGN KEY (assigned_picker_id) REFERENCES users(id),
    FOREIGN KEY (assigned_section_id) REFERENCES section(id)
);

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
    id                    INT AUTO_INCREMENT PRIMARY KEY,
    return_request_id     INT NOT NULL,
    purchase_order_line_id INT NULL,
    delivery_order_line_id INT NULL,
    barang_id             INT NOT NULL,
    rak_id                INT NOT NULL,
    batch_code            VARCHAR(64) NOT NULL,
    expiry_date           DATE NULL,
    quantity              INT NOT NULL,
    FOREIGN KEY (return_request_id) REFERENCES return_request(id),
    FOREIGN KEY (purchase_order_line_id) REFERENCES purchase_order_line(id),
    FOREIGN KEY (delivery_order_line_id) REFERENCES delivery_order_line(id),
    FOREIGN KEY (barang_id) REFERENCES barang(id),
    FOREIGN KEY (rak_id) REFERENCES rak(id)
);

CREATE TABLE IF NOT EXISTS stock_transfer_order (
    id                     INT AUTO_INCREMENT PRIMARY KEY,
    order_code             VARCHAR(24) NOT NULL UNIQUE,
    source_warehouse_id    INT NOT NULL,
    destination_warehouse_id INT NOT NULL,
    created_by             INT NOT NULL,
    reviewed_by            INT NULL,
    assigned_picker_id     INT NULL,
    status                 VARCHAR(40) NOT NULL,
    reason                 TEXT NOT NULL,
    review_notes           TEXT,
    created_at             DATETIME NOT NULL,
    reviewed_at             DATETIME NULL,
    assigned_at             DATETIME NULL,
    dispatched_at          DATETIME NULL,
    received_at             DATETIME NULL,
    FOREIGN KEY (source_warehouse_id) REFERENCES warehouse(id),
    FOREIGN KEY (destination_warehouse_id) REFERENCES warehouse(id),
    FOREIGN KEY (created_by) REFERENCES users(id),
    FOREIGN KEY (reviewed_by) REFERENCES users(id),
    FOREIGN KEY (assigned_picker_id) REFERENCES users(id)
);

CREATE TABLE IF NOT EXISTS stock_transfer_order_line (
    id                      INT AUTO_INCREMENT PRIMARY KEY,
    stock_transfer_order_id INT NOT NULL,
    barang_id               INT NOT NULL,
    source_rak_id           INT NOT NULL,
    destination_rak_id      INT NOT NULL,
    quantity                INT NOT NULL,
    FOREIGN KEY (stock_transfer_order_id) REFERENCES stock_transfer_order(id),
    FOREIGN KEY (barang_id) REFERENCES barang(id),
    FOREIGN KEY (source_rak_id) REFERENCES rak(id),
    FOREIGN KEY (destination_rak_id) REFERENCES rak(id)
);

CREATE TABLE IF NOT EXISTS stock_transfer_order_lot (
    id                         INT AUTO_INCREMENT PRIMARY KEY,
    stock_transfer_order_line_id INT NOT NULL,
    batch_code                 VARCHAR(64) NOT NULL,
    expiry_date                DATE NULL,
    quantity                   INT NOT NULL,
    FOREIGN KEY (stock_transfer_order_line_id) REFERENCES stock_transfer_order_line(id)
);

CREATE TABLE IF NOT EXISTS stock_transfer_order_scan (
    id                          INT AUTO_INCREMENT PRIMARY KEY,
    stock_transfer_order_id     INT NOT NULL,
    stock_transfer_order_line_id INT NOT NULL,
    picker_id                   INT NOT NULL,
    lot_balance_id              INT NULL,
    source_scan_id              INT NULL,
    scan_type                   VARCHAR(10) NOT NULL,
    scanned_sku                 VARCHAR(64) NOT NULL,
    scanned_rack                VARCHAR(10) NOT NULL,
    scanned_batch               VARCHAR(64) NOT NULL,
    scanned_expiry              DATE NULL,
    quantity                    INT NOT NULL,
    scanned_at                  DATETIME NOT NULL,
    CONSTRAINT chk_sto_scan_quantity CHECK (quantity > 0),
    INDEX idx_sto_scan_order_time (stock_transfer_order_id, scanned_at, id),
    INDEX idx_sto_scan_source (source_scan_id),
    FOREIGN KEY (stock_transfer_order_id) REFERENCES stock_transfer_order(id),
    FOREIGN KEY (stock_transfer_order_line_id) REFERENCES stock_transfer_order_line(id),
    FOREIGN KEY (picker_id) REFERENCES users(id),
    FOREIGN KEY (lot_balance_id) REFERENCES inventory_lot_balance(id),
    FOREIGN KEY (source_scan_id) REFERENCES stock_transfer_order_scan(id)
);

CREATE TABLE IF NOT EXISTS stock_count (
    id          INT AUTO_INCREMENT PRIMARY KEY,
    count_code  VARCHAR(24) NOT NULL UNIQUE,
    cycle_code  VARCHAR(24) NULL,
    cycle_type  VARCHAR(16) NOT NULL DEFAULT 'AD_HOC',
    rak_id      INT NOT NULL,
    created_by  INT NOT NULL,
    counted_by  INT NULL,
    posted_by   INT NULL,
    status      VARCHAR(16) NOT NULL,
    created_at  DATETIME NOT NULL,
    due_date    DATE NULL,
    posted_at   DATETIME NULL,
    FOREIGN KEY (rak_id) REFERENCES rak(id),
    FOREIGN KEY (created_by) REFERENCES users(id),
    FOREIGN KEY (counted_by) REFERENCES users(id),
    FOREIGN KEY (posted_by) REFERENCES users(id),
    INDEX idx_stock_count_rack_status (rak_id, status),
    INDEX idx_stock_count_cycle_code (cycle_code),
    INDEX idx_stock_count_due_date (due_date, status)
);

CREATE TABLE IF NOT EXISTS stock_count_line (
    id                 INT AUTO_INCREMENT PRIMARY KEY,
    stock_count_id     INT NOT NULL,
    barang_id          INT NOT NULL,
    assigned_user_id   INT NULL,
    expected_quantity  INT NOT NULL,
    counted_quantity   INT NULL,
    counted_by         INT NULL,
    UNIQUE KEY uk_stock_count_barang (stock_count_id, barang_id),
    FOREIGN KEY (stock_count_id) REFERENCES stock_count(id),
    FOREIGN KEY (barang_id) REFERENCES barang(id),
    FOREIGN KEY (assigned_user_id) REFERENCES users(id),
    FOREIGN KEY (counted_by) REFERENCES users(id),
    INDEX idx_stock_count_line_assignment (assigned_user_id, counted_quantity)
);

-- ============================================================
-- DATA AWAL (Seed)
-- ============================================================





INSERT IGNORE INTO section (kode_section, nama_section, kapasitas) VALUES
('A', 'Section A', 12),
('B', 'Section B', 12),
('C', 'Section C', 12);


INSERT IGNORE INTO rak (section_id, kode_rak, kapasitas, terisi) VALUES
(1, 'A-01', 100, 95),
(1, 'A-02', 100, 40),
(1, 'A-03', 100, 20),
(1, 'A-04', 100, 10),
(1, 'A-05', 100, 0),
(1, 'A-06', 100, 0),
(1, 'A-07', 100, 0),
(1, 'A-08', 100, 0),
(1, 'A-09', 100, 0),
(1, 'A-10', 100, 0),
(1, 'A-11', 100, 0),
(1, 'A-12', 100, 0);


INSERT IGNORE INTO rak (section_id, kode_rak, kapasitas, terisi) VALUES
(2, 'B-01', 100, 100),
(2, 'B-02', 100, 80),
(2, 'B-03', 100, 60),
(2, 'B-04', 100, 50),
(2, 'B-05', 100, 0),
(2, 'B-06', 100, 0),
(2, 'B-07', 100, 0),
(2, 'B-08', 100, 0),
(2, 'B-09', 100, 0),
(2, 'B-10', 100, 0),
(2, 'B-11', 100, 0),
(2, 'B-12', 100, 0);


INSERT IGNORE INTO rak (section_id, kode_rak, kapasitas, terisi) VALUES
(3, 'C-01', 100, 45),
(3, 'C-02', 100, 100),
(3, 'C-03', 100, 100),
(3, 'C-04', 100, 100),
(3, 'C-05', 100, 100),
(3, 'C-06', 100, 100),
(3, 'C-07', 100, 100),
(3, 'C-08', 100, 100),
(3, 'C-09', 100, 100),
(3, 'C-10', 100, 100),
(3, 'C-11', 100, 100),
(3, 'C-12', 100, 100);


INSERT IGNORE INTO supplier (nama_supplier, kontak, email) VALUES
('PT. Satu',  '08111111111', 'satu@example.com'),
('PT. Dua',   '08222222222', 'dua@example.com'),
('PT. Tiga',  '08333333333', 'tiga@example.com'),
('PT. Empat', '08444444444', 'empat@example.com');


INSERT IGNORE INTO barang (kode_barang, nama_barang, kategori, satuan, stok, stok_min, stok_max) VALUES
('BRG-001', 'Barang A', 'Umum', 'pcs', 500, 50, 100),
('BRG-002', 'Barang B', 'Umum', 'pcs', 300, 30, 60),
('BRG-003', 'Barang C', 'Umum', 'pcs', 150, 20, 40),
('BRG-004', 'Barang D', 'Umum', 'pcs', 200, 25, 50),
('BRG-005', 'Barang E', 'Umum', 'pcs', 769, 100, 200);






INSERT IGNORE INTO users (full_name, email, password_hash, role) VALUES
('Test User', 'testuser@example.com', 'Testing123', 'user');
