package com.mycompany.tugas_akhir.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

@Entity
@Table(name = "warehouse_zone_rule",
        uniqueConstraints = @UniqueConstraint(name = "uk_warehouse_zone_rule", columnNames = {"operation", "zone_code"}))
public class WarehouseZoneRule {
    public enum Operation {
        DELIVERY_ORDER_EXCLUSION("Delivery Order"),
        SLOC_TRANSFER_EXCLUSION("Transfer SLOC"),
        BARCODE_TRANSFER_EXCLUSION("Transfer barcode"),
        STO_PICKLIST_ZONE("Stock Transfer picklist"),
        PURCHASE_RETURN_PICKLIST_ZONE("Purchase Return picklist");

        private final String displayName;

        Operation(String displayName) {
            this.displayName = displayName;
        }

        public String getDisplayName() { return displayName; }

        public boolean isPicklist() {
            return this == STO_PICKLIST_ZONE || this == PURCHASE_RETURN_PICKLIST_ZONE;
        }
    }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 40)
    private Operation operation;

    @Column(name = "zone_code", nullable = false, length = 40)
    private String zoneCode;

    protected WarehouseZoneRule() {}

    public WarehouseZoneRule(Operation operation, String zoneCode) {
        this.operation = operation;
        this.zoneCode = zoneCode;
    }

    public Integer getId() { return id; }
    public Operation getOperation() { return operation; }
    public String getZoneCode() { return zoneCode; }
}
