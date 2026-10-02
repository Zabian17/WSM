package com.mycompany.tugas_akhir.model;

import jakarta.persistence.*;

@Entity
@Table(name = "inventory_transfer_lot_allocation")
public class InventoryTransferLotAllocation {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "transfer_id", nullable = false)
    private InventoryTransfer transfer;

    @ManyToOne(fetch = FetchType.EAGER, optional = false)
    @JoinColumn(name = "lot_balance_id", nullable = false)
    private InventoryLotBalance lotBalance;

    @Column(nullable = false)
    private Integer quantity;

    public InventoryTransferLotAllocation() {}

    public InventoryTransferLotAllocation(InventoryTransfer transfer, InventoryLotBalance lotBalance, Integer quantity) {
        this.transfer = transfer;
        this.lotBalance = lotBalance;
        this.quantity = quantity;
    }
}