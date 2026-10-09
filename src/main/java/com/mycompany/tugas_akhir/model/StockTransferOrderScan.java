package com.mycompany.tugas_akhir.model;

import jakarta.persistence.*;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "stock_transfer_order_scan")
public class StockTransferOrderScan {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "stock_transfer_order_id", nullable = false)
    private StockTransferOrder stockTransferOrder;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "stock_transfer_order_line_id", nullable = false)
    private StockTransferOrderLine stockTransferOrderLine;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "picker_id", nullable = false)
    private User picker;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "lot_balance_id")
    private InventoryLotBalance lotBalance;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "source_scan_id")
    private StockTransferOrderScan sourceScan;

    @Enumerated(EnumType.STRING)
    @Column(name = "scan_type", nullable = false, length = 10)
    private StockTransferOrderScanType scanType;

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

    protected StockTransferOrderScan() {}

    public StockTransferOrderScan(StockTransferOrder order, StockTransferOrderLine line, User picker,
                                  InventoryLotBalance lotBalance, StockTransferOrderScan sourceScan,
                                  StockTransferOrderScanType scanType, String scannedSku,
                                  String scannedRack, String scannedBatch, LocalDate scannedExpiry,
                                  Integer quantity, LocalDateTime scannedAt) {
        this.stockTransferOrder = order;
        this.stockTransferOrderLine = line;
        this.picker = picker;
        this.lotBalance = lotBalance;
        this.sourceScan = sourceScan;
        this.scanType = scanType;
        this.scannedSku = scannedSku;
        this.scannedRack = scannedRack;
        this.scannedBatch = scannedBatch;
        this.scannedExpiry = scannedExpiry;
        this.quantity = quantity;
        this.scannedAt = scannedAt;
    }

    public Integer getId() { return id; }
    public StockTransferOrder getStockTransferOrder() { return stockTransferOrder; }
    public StockTransferOrderLine getStockTransferOrderLine() { return stockTransferOrderLine; }
    public User getPicker() { return picker; }
    public InventoryLotBalance getLotBalance() { return lotBalance; }
    public StockTransferOrderScan getSourceScan() { return sourceScan; }
    public StockTransferOrderScanType getScanType() { return scanType; }
    public String getScannedSku() { return scannedSku; }
    public String getScannedRack() { return scannedRack; }
    public String getScannedBatch() { return scannedBatch; }
    public LocalDate getScannedExpiry() { return scannedExpiry; }
    public Integer getQuantity() { return quantity; }
    public LocalDateTime getScannedAt() { return scannedAt; }
}
