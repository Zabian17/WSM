package com.mycompany.tugas_akhir.model;

import jakarta.persistence.*;
import java.math.BigDecimal;

@Entity
@Table(name = "purchase_order_line", uniqueConstraints =
        @UniqueConstraint(name = "uk_purchase_order_item", columnNames = {"purchase_order_id", "barang_id"}))
public class PurchaseOrderLine {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "purchase_order_id", nullable = false)
    private PurchaseOrder purchaseOrder;

    @ManyToOne(fetch = FetchType.EAGER, optional = false)
    @JoinColumn(name = "barang_id", nullable = false)
    private Barang barang;

    @Column(name = "ordered_quantity", nullable = false)
    private Integer orderedQuantity;

    @Column(name = "received_quantity", nullable = false)
    private Integer receivedQuantity = 0;

    @Column(name = "pending_quantity", nullable = false)
    private Integer pendingQuantity = 0;

    @Column(name = "unit_price", nullable = false, precision = 19, scale = 4)
    private BigDecimal unitPrice = BigDecimal.ZERO;

    protected PurchaseOrderLine() {}

    public PurchaseOrderLine(Barang barang, Integer orderedQuantity) {
        this(barang, orderedQuantity, BigDecimal.ZERO);
    }

    public PurchaseOrderLine(Barang barang, Integer orderedQuantity, BigDecimal unitPrice) {
        this.barang = barang;
        this.orderedQuantity = orderedQuantity;
        this.unitPrice = unitPrice;
    }

    public Integer getId() { return id; }
    public PurchaseOrder getPurchaseOrder() { return purchaseOrder; }
    public Barang getBarang() { return barang; }
    public Integer getOrderedQuantity() { return orderedQuantity; }
    public Integer getReceivedQuantity() { return receivedQuantity == null ? 0 : receivedQuantity; }
    public Integer getPendingQuantity() { return pendingQuantity == null ? 0 : pendingQuantity; }
    public BigDecimal getUnitPrice() { return unitPrice == null ? BigDecimal.ZERO : unitPrice; }
    public BigDecimal getLineAmount() { return getUnitPrice().multiply(BigDecimal.valueOf(orderedQuantity)); }
    public Integer getRemainingQuantity() {
        return orderedQuantity - getReceivedQuantity() - getPendingQuantity();
    }
    public void setPurchaseOrder(PurchaseOrder purchaseOrder) { this.purchaseOrder = purchaseOrder; }
    public void setReceivedQuantity(Integer receivedQuantity) { this.receivedQuantity = receivedQuantity; }
    public void setPendingQuantity(Integer pendingQuantity) { this.pendingQuantity = pendingQuantity; }

    @Override
    public String toString() {
        return purchaseOrder.getOrderCode() + " | " + barang.getKodeBarang() + " - "
                + barang.getNamaBarang() + " | sisa " + getRemainingQuantity();
    }
}
