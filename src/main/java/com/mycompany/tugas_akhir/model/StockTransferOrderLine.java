package com.mycompany.tugas_akhir.model;

import jakarta.persistence.*;

import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "stock_transfer_order_line")
public class StockTransferOrderLine {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "stock_transfer_order_id", nullable = false)
    private StockTransferOrder stockTransferOrder;

    @ManyToOne(fetch = FetchType.EAGER, optional = false)
    @JoinColumn(name = "barang_id", nullable = false)
    private Barang barang;

    @ManyToOne(fetch = FetchType.EAGER, optional = false)
    @JoinColumn(name = "source_rak_id", nullable = false)
    private Rak sourceRak;

    @ManyToOne(fetch = FetchType.EAGER, optional = false)
    @JoinColumn(name = "destination_rak_id", nullable = false)
    private Rak destinationRak;

    @Column(nullable = false)
    private Integer quantity;

    @OneToMany(mappedBy = "stockTransferOrderLine", cascade = CascadeType.ALL,
            orphanRemoval = true, fetch = FetchType.EAGER)
    private List<StockTransferOrderLot> lots = new ArrayList<>();

    protected StockTransferOrderLine() {}

    public StockTransferOrderLine(Barang barang, Rak sourceRak, Rak destinationRak, Integer quantity) {
        this.barang = barang;
        this.sourceRak = sourceRak;
        this.destinationRak = destinationRak;
        this.quantity = quantity;
    }

    public void addLot(StockTransferOrderLot lot) {
        lots.add(lot);
        lot.setStockTransferOrderLine(this);
    }

    public Integer getId() { return id; }
    public StockTransferOrder getStockTransferOrder() { return stockTransferOrder; }
    public Barang getBarang() { return barang; }
    public Rak getSourceRak() { return sourceRak; }
    public Rak getDestinationRak() { return destinationRak; }
    public Integer getQuantity() { return quantity; }
    public List<StockTransferOrderLot> getLots() { return lots; }
    public void setStockTransferOrder(StockTransferOrder stockTransferOrder) {
        this.stockTransferOrder = stockTransferOrder;
    }
}
