package com.mycompany.tugas_akhir.model;

import jakarta.persistence.*;

@Entity
@Table(name = "material_usage_reservation", uniqueConstraints =
        @UniqueConstraint(name = "uk_mtu_line_lot", columnNames = {"material_usage_line_id", "lot_balance_id"}))
public class MaterialUsageReservation {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @ManyToOne(fetch = FetchType.EAGER, optional = false)
    @JoinColumn(name = "material_usage_line_id", nullable = false)
    private MaterialUsageLine materialUsageLine;

    @ManyToOne(fetch = FetchType.EAGER, optional = false)
    @JoinColumn(name = "lot_balance_id", nullable = false)
    private InventoryLotBalance lotBalance;

    @Column(name = "reserved_quantity", nullable = false)
    private Integer reservedQuantity;

    @Column(name = "issued_quantity", nullable = false)
    private Integer issuedQuantity = 0;

    protected MaterialUsageReservation() {}

    public MaterialUsageReservation(MaterialUsageLine materialUsageLine,
                                    InventoryLotBalance lotBalance, Integer reservedQuantity) {
        this.materialUsageLine = materialUsageLine;
        this.lotBalance = lotBalance;
        this.reservedQuantity = reservedQuantity;
    }

    public Integer getId() { return id; }
    public MaterialUsageLine getMaterialUsageLine() { return materialUsageLine; }
    public InventoryLotBalance getLotBalance() { return lotBalance; }
    public Integer getReservedQuantity() { return reservedQuantity; }
    public Integer getIssuedQuantity() { return issuedQuantity == null ? 0 : issuedQuantity; }
    public Integer getRemainingQuantity() { return reservedQuantity - getIssuedQuantity(); }
    public void setIssuedQuantity(Integer issuedQuantity) { this.issuedQuantity = issuedQuantity; }
}
