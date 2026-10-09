package com.mycompany.tugas_akhir.model;

import jakarta.persistence.*;

@Entity
@Table(name = "sales_order_line",
        uniqueConstraints = @UniqueConstraint(name = "uk_sales_order_item",
                columnNames = {"sales_order_id", "barang_id"}))
public class SalesOrderLine {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "sales_order_id", nullable = false)
    private SalesOrder salesOrder;

    @ManyToOne(fetch = FetchType.EAGER, optional = false)
    @JoinColumn(name = "barang_id", nullable = false)
    private Barang barang;

    @Column(name = "ordered_quantity", nullable = false)
    private Integer orderedQuantity;

    @Column(name = "converted_quantity", nullable = false)
    private Integer convertedQuantity = 0;

    public SalesOrderLine() {}

    public SalesOrderLine(Barang barang, Integer orderedQuantity) {
        this.barang = barang;
        this.orderedQuantity = orderedQuantity;
    }

    public Integer getId() { return id; }
    public SalesOrder getSalesOrder() { return salesOrder; }
    public Barang getBarang() { return barang; }
    public Integer getOrderedQuantity() { return orderedQuantity; }
    public Integer getConvertedQuantity() { return convertedQuantity == null ? 0 : convertedQuantity; }
    public Integer getRemainingQuantity() { return getOrderedQuantity() - getConvertedQuantity(); }
    public void setSalesOrder(SalesOrder salesOrder) { this.salesOrder = salesOrder; }
    public void setConvertedQuantity(Integer convertedQuantity) { this.convertedQuantity = convertedQuantity; }
}
