package com.mycompany.tugas_akhir.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.math.BigDecimal;

@Entity
@Table(name = "warehouse_dashboard_target")
public class WarehouseDashboardTarget {
    @Id
    private Integer id = 1;

    @Column(name = "fulfillment_target_percent", precision = 5, scale = 2)
    private BigDecimal fulfillmentPercent;
    @Column(name = "packing_target_percent", precision = 5, scale = 2)
    private BigDecimal packingPercent;
    @Column(name = "loading_target_percent", precision = 5, scale = 2)
    private BigDecimal loadingPercent;
    @Column(name = "supplier_qc_max_hours", precision = 8, scale = 2)
    private BigDecimal supplierQcMaxHours;
    @Column(name = "supplier_lead_max_hours", precision = 8, scale = 2)
    private BigDecimal supplierLeadMaxHours;
    @Column(name = "picker_minimum_quantity")
    private Integer pickerMinimumQuantity;

    protected WarehouseDashboardTarget() {}

    public WarehouseDashboardTarget(BigDecimal fulfillmentPercent, BigDecimal packingPercent,
                                    BigDecimal loadingPercent, BigDecimal supplierQcMaxHours,
                                    BigDecimal supplierLeadMaxHours, Integer pickerMinimumQuantity) {
        this.fulfillmentPercent = fulfillmentPercent;
        this.packingPercent = packingPercent;
        this.loadingPercent = loadingPercent;
        this.supplierQcMaxHours = supplierQcMaxHours;
        this.supplierLeadMaxHours = supplierLeadMaxHours;
        this.pickerMinimumQuantity = pickerMinimumQuantity;
    }

    public Integer getId() { return id; }
    public BigDecimal getFulfillmentPercent() { return fulfillmentPercent; }
    public BigDecimal getPackingPercent() { return packingPercent; }
    public BigDecimal getLoadingPercent() { return loadingPercent; }
    public BigDecimal getSupplierQcMaxHours() { return supplierQcMaxHours; }
    public BigDecimal getSupplierLeadMaxHours() { return supplierLeadMaxHours; }
    public Integer getPickerMinimumQuantity() { return pickerMinimumQuantity; }
}
