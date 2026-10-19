CREATE TABLE warehouse_dashboard_target (
    id INT NOT NULL,
    fulfillment_target_percent DECIMAL(5,2) NULL,
    packing_target_percent DECIMAL(5,2) NULL,
    loading_target_percent DECIMAL(5,2) NULL,
    supplier_qc_max_hours DECIMAL(8,2) NULL,
    supplier_lead_max_hours DECIMAL(8,2) NULL,
    picker_minimum_quantity INT NULL,
    PRIMARY KEY (id),
    CONSTRAINT chk_dashboard_target_singleton CHECK (id = 1),
    CONSTRAINT chk_dashboard_fulfillment_target CHECK (fulfillment_target_percent IS NULL OR fulfillment_target_percent BETWEEN 0 AND 100),
    CONSTRAINT chk_dashboard_packing_target CHECK (packing_target_percent IS NULL OR packing_target_percent BETWEEN 0 AND 100),
    CONSTRAINT chk_dashboard_loading_target CHECK (loading_target_percent IS NULL OR loading_target_percent BETWEEN 0 AND 100),
    CONSTRAINT chk_dashboard_qc_target CHECK (supplier_qc_max_hours IS NULL OR supplier_qc_max_hours >= 0),
    CONSTRAINT chk_dashboard_lead_target CHECK (supplier_lead_max_hours IS NULL OR supplier_lead_max_hours >= 0),
    CONSTRAINT chk_dashboard_picker_target CHECK (picker_minimum_quantity IS NULL OR picker_minimum_quantity >= 0)
);
