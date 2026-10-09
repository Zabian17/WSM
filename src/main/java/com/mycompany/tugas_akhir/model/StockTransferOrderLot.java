package com.mycompany.tugas_akhir.model;

import jakarta.persistence.*;

import java.time.LocalDate;

@Entity
@Table(name = "stock_transfer_order_lot")
public class StockTransferOrderLot {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "stock_transfer_order_line_id", nullable = false)
    private StockTransferOrderLine stockTransferOrderLine;

    @Column(name = "batch_code", nullable = false, length = 64)
    private String batchCode;

    @Column(name = "expiry_date")
    private LocalDate expiryDate;

    @Column(nullable = false)
    private Integer quantity;

    protected StockTransferOrderLot() {}

    public StockTransferOrderLot(String batchCode, LocalDate expiryDate, Integer quantity) {
        this.batchCode = batchCode;
        this.expiryDate = expiryDate;
        this.quantity = quantity;
    }

    public Integer getId() { return id; }
    public StockTransferOrderLine getStockTransferOrderLine() { return stockTransferOrderLine; }
    public String getBatchCode() { return batchCode; }
    public LocalDate getExpiryDate() { return expiryDate; }
    public Integer getQuantity() { return quantity; }
    public void setStockTransferOrderLine(StockTransferOrderLine line) { this.stockTransferOrderLine = line; }
}
