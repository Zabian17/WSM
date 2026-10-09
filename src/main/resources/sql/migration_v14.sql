USE gudang_akhir;

ALTER TABLE users
    ADD COLUMN department VARCHAR(100) NULL AFTER role;

CREATE TABLE IF NOT EXISTS approval_routing_rule (
    id                   INT AUTO_INCREMENT PRIMARY KEY,
    requester_department VARCHAR(100) NOT NULL UNIQUE,
    approver_department  VARCHAR(100) NOT NULL,
    CONSTRAINT chk_approval_departments_differ
        CHECK (LOWER(requester_department) <> LOWER(approver_department))
);
