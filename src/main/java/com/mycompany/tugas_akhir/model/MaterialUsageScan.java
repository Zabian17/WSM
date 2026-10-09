package com.mycompany.tugas_akhir.model;

import jakarta.persistence.*;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "material_usage_scan")
public class MaterialUsageScan {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "material_usage_id", nullable = false)
    private MaterialUsage materialUsage;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "material_usage_line_id", nullable = false)
    private MaterialUsageLine materialUsageLine;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "issuer_id", nullable = false)
    private User issuer;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "lot_balance_id", nullable = false)
    private InventoryLotBalance lotBalance;

    @Enumerated(EnumType.STRING)
    @Column(name = "scan_type", nullable = false, length = 10)
    private MaterialUsageScanType scanType;

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

    protected MaterialUsageScan() {}

    public MaterialUsageScan(MaterialUsage materialUsage, MaterialUsageLine materialUsageLine,
                             User issuer, InventoryLotBalance lotBalance, String scannedSku,
                             String scannedRack, String scannedBatch, LocalDate scannedExpiry,
                             Integer quantity, LocalDateTime scannedAt) {
        this.materialUsage = materialUsage;
        this.materialUsageLine = materialUsageLine;
        this.issuer = issuer;
        this.lotBalance = lotBalance;
        this.scanType = MaterialUsageScanType.ISSUE;
        this.scannedSku = scannedSku;
        this.scannedRack = scannedRack;
        this.scannedBatch = scannedBatch;
        this.scannedExpiry = scannedExpiry;
        this.quantity = quantity;
        this.scannedAt = scannedAt;
    }

    public Integer getId() { return id; }
    public MaterialUsage getMaterialUsage() { return materialUsage; }
    public MaterialUsageLine getMaterialUsageLine() { return materialUsageLine; }
    public User getIssuer() { return issuer; }
    public InventoryLotBalance getLotBalance() { return lotBalance; }
    public MaterialUsageScanType getScanType() { return scanType; }
    public String getScannedSku() { return scannedSku; }
    public String getScannedRack() { return scannedRack; }
    public String getScannedBatch() { return scannedBatch; }
    public LocalDate getScannedExpiry() { return scannedExpiry; }
    public Integer getQuantity() { return quantity; }
    public LocalDateTime getScannedAt() { return scannedAt; }
}
