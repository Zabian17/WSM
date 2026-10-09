package com.mycompany.tugas_akhir.model;

import jakarta.persistence.*;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "delivery_order_pick_scan")
public class DeliveryOrderPickScan {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "delivery_order_id", nullable = false)
    private DeliveryOrder deliveryOrder;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "delivery_order_line_id", nullable = false)
    private DeliveryOrderLine deliveryOrderLine;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "picker_id", nullable = false)
    private User picker;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "lot_balance_id", nullable = false)
    private InventoryLotBalance lotBalance;

    @Column(name = "scanned_sku", nullable = false, length = 64)
    private String scannedSku;

    @Column(name = "scanned_rack", nullable = false, length = 10)
    private String scannedRack;

    @Column(name = "scanned_batch", nullable = false, length = 64)
    private String scannedBatch;

    @Column(name = "scanned_expiry")
    private LocalDate scannedExpiry;

    @Column(nullable = false)
    private Integer quantity;

    @Column(name = "scanned_at", nullable = false)
    private LocalDateTime scannedAt;

    protected DeliveryOrderPickScan() {}

    public DeliveryOrderPickScan(DeliveryOrder deliveryOrder, DeliveryOrderLine deliveryOrderLine,
                                 User picker, InventoryLotBalance lotBalance, String scannedSku,
                                 String scannedRack, String scannedBatch, LocalDate scannedExpiry, Integer quantity,
                                 LocalDateTime scannedAt) {
        this.deliveryOrder = deliveryOrder;
        this.deliveryOrderLine = deliveryOrderLine;
        this.picker = picker;
        this.lotBalance = lotBalance;
        this.scannedSku = scannedSku;
        this.scannedRack = scannedRack;
        this.scannedBatch = scannedBatch;
        this.scannedExpiry = scannedExpiry;
        this.quantity = quantity;
        this.scannedAt = scannedAt;
    }

    public Integer getId() { return id; }
    public DeliveryOrder getDeliveryOrder() { return deliveryOrder; }
    public DeliveryOrderLine getDeliveryOrderLine() { return deliveryOrderLine; }
    public User getPicker() { return picker; }
    public InventoryLotBalance getLotBalance() { return lotBalance; }
    public String getScannedSku() { return scannedSku; }
    public String getScannedRack() { return scannedRack; }
    public String getScannedBatch() { return scannedBatch; }
    public LocalDate getScannedExpiry() { return scannedExpiry; }
    public Integer getQuantity() { return quantity; }
    public LocalDateTime getScannedAt() { return scannedAt; }
}
