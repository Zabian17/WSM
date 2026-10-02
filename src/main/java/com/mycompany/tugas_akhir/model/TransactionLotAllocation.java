package com.mycompany.tugas_akhir.model;

import jakarta.persistence.*;

@Entity
@Table(name = "transaction_lot_allocation")
public class TransactionLotAllocation {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "transaksi_id", nullable = false)
    private Transaksi transaksi;

    @ManyToOne(fetch = FetchType.EAGER, optional = false)
    @JoinColumn(name = "lot_balance_id", nullable = false)
    private InventoryLotBalance lotBalance;

    @Column(nullable = false)
    private Integer quantity;

    public TransactionLotAllocation() {}

    public TransactionLotAllocation(Transaksi transaksi, InventoryLotBalance lotBalance, Integer quantity) {
        this.transaksi = transaksi;
        this.lotBalance = lotBalance;
        this.quantity = quantity;
    }

    public InventoryLotBalance getLotBalance() { return lotBalance; }
    public Integer getQuantity() { return quantity; }
}