package com.mycompany.tugas_akhir.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "inventory_transfer")
public class InventoryTransfer {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(name = "transfer_code", nullable = false, unique = true, length = 24)
    private String transferCode;

    @ManyToOne(fetch = FetchType.EAGER, optional = false)
    @JoinColumn(name = "barang_id", nullable = false)
    private Barang barang;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "source_rak_id")
    private Rak sourceRak;

    @ManyToOne(fetch = FetchType.EAGER, optional = false)
    @JoinColumn(name = "destination_rak_id", nullable = false)
    private Rak destinationRak;

    @Column(nullable = false)
    private Integer quantity;

    @Column(columnDefinition = "TEXT")
    private String notes;

    @ManyToOne(fetch = FetchType.EAGER, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    public InventoryTransfer() {}

    public InventoryTransfer(String transferCode, Barang barang, Rak sourceRak, Rak destinationRak,
                             Integer quantity, String notes, User user, LocalDateTime createdAt) {
        this.transferCode = transferCode;
        this.barang = barang;
        this.sourceRak = sourceRak;
        this.destinationRak = destinationRak;
        this.quantity = quantity;
        this.notes = notes;
        this.user = user;
        this.createdAt = createdAt;
    }

    public Integer getId() { return id; }
    public String getTransferCode() { return transferCode; }
    public Barang getBarang() { return barang; }
    public Rak getSourceRak() { return sourceRak; }
    public Rak getDestinationRak() { return destinationRak; }
    public Integer getQuantity() { return quantity; }
    public String getNotes() { return notes; }
    public User getUser() { return user; }
    public LocalDateTime getCreatedAt() { return createdAt; }
}