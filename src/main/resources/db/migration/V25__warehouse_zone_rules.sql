CREATE TABLE warehouse_zone_rule (
    id INT AUTO_INCREMENT PRIMARY KEY,
    operation VARCHAR(40) NOT NULL,
    zone_code VARCHAR(40) NOT NULL,
    CONSTRAINT uk_warehouse_zone_rule UNIQUE (operation, zone_code)
);
