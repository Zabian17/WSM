package com.mycompany.tugas_akhir.model;

import jakarta.persistence.*;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "inventory_lot_balance", uniqueConstraints =
        @UniqueConstraint(name = "uk_lot_barang_location_batch",
                columnNames = {"barang_id", "location_key", "batch_code"}))
public class InventoryLotBalance {

    public static final String OPENING_BATCH_CODE = "OPENING";

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "barang_id", nullable = false)
    private Barang barang;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "rak_id")
    private Rak rak;

    @Column(name = "location_key", nullable = false, length = 24)
    private String locationKey;

    @Column(name = "batch_code", nullable = false, length = 64)
    private String batchCode;

    @Column(name = "expiry_date")
    private LocalDate expiryDate;

    @Column(name = "received_at", nullable = false)
    private LocalDateTime receivedAt;

    @Column(nullable = false)
    private Integer quantity;

    @Column(name = "reserved_quantity", nullable = false)
    private Integer reservedQuantity = 0;

    public InventoryLotBalance() {}

    public InventoryLotBalance(Barang barang, Rak rak, String locationKey, String batchCode,
                               LocalDate expiryDate, LocalDateTime receivedAt, Integer quantity) {
        this.barang = barang;
        this.rak = rak;
        this.locationKey = locationKey;
        this.batchCode = batchCode;
        this.expiryDate = expiryDate;
        this.receivedAt = receivedAt;
        this.quantity = quantity;
    }

    public Integer getId() { return id; }
    public Barang getBarang() { return barang; }
    public Rak getRak() { return rak; }
    public String getLocationKey() { return locationKey; }
    public String getBatchCode() { return batchCode; }
    public LocalDate getExpiryDate() { return expiryDate; }
    public LocalDateTime getReceivedAt() { return receivedAt; }
    public Integer getQuantity() { return quantity; }
    public Integer getReservedQuantity() { return reservedQuantity == null ? 0 : reservedQuantity; }
    public Integer getAvailableQuantity() { return quantity - getReservedQuantity(); }
    public void setQuantity(Integer quantity) { this.quantity = quantity; }
    public void setReservedQuantity(Integer reservedQuantity) { this.reservedQuantity = reservedQuantity; }
}