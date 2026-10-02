package com.mycompany.tugas_akhir.model;

import jakarta.persistence.*;

@Entity
@Table(name = "inventory_balance", uniqueConstraints =
        @UniqueConstraint(name = "uk_inventory_barang_location", columnNames = {"barang_id", "location_key"}))
public class InventoryBalance {

    public static final String UNASSIGNED_LOCATION = "UNASSIGNED";

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

    @Column(nullable = false)
    private Integer quantity;

    public InventoryBalance() {}

    public InventoryBalance(Barang barang, Rak rak, String locationKey, Integer quantity) {
        this.barang = barang;
        this.rak = rak;
        this.locationKey = locationKey;
        this.quantity = quantity;
    }

    public Integer getId() { return id; }
    public Barang getBarang() { return barang; }
    public Rak getRak() { return rak; }
    public String getLocationKey() { return locationKey; }
    public Integer getQuantity() { return quantity; }
    public void setQuantity(Integer quantity) { this.quantity = quantity; }
}