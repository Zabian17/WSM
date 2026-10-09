package com.mycompany.tugas_akhir.model;

import jakarta.persistence.*;

@Entity
@Table(name = "stock_count_line",
        uniqueConstraints = @UniqueConstraint(name = "uk_stock_count_barang", columnNames = {"stock_count_id", "barang_id"}))
public class StockCountLine {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "stock_count_id", nullable = false)
    private StockCount stockCount;

    @ManyToOne(fetch = FetchType.EAGER, optional = false)
    @JoinColumn(name = "barang_id", nullable = false)
    private Barang barang;

    @Column(name = "expected_quantity", nullable = false)
    private Integer expectedQuantity;

    @Column(name = "counted_quantity")
    private Integer countedQuantity;

    public StockCountLine() {}

    public StockCountLine(Barang barang, Integer expectedQuantity) {
        this.barang = barang;
        this.expectedQuantity = expectedQuantity;
    }

    public Integer getId() { return id; }
    public StockCount getStockCount() { return stockCount; }
    public Barang getBarang() { return barang; }
    public Integer getExpectedQuantity() { return expectedQuantity; }
    public Integer getCountedQuantity() { return countedQuantity; }
    public void setStockCount(StockCount stockCount) { this.stockCount = stockCount; }
    public void setCountedQuantity(Integer countedQuantity) { this.countedQuantity = countedQuantity; }
}
