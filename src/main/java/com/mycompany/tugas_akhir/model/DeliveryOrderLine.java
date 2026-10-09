package com.mycompany.tugas_akhir.model;

import jakarta.persistence.*;

@Entity
@Table(name = "delivery_order_line")
public class DeliveryOrderLine {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "delivery_order_id", nullable = false)
    private DeliveryOrder deliveryOrder;

    @ManyToOne(fetch = FetchType.EAGER, optional = false)
    @JoinColumn(name = "barang_id", nullable = false)
    private Barang barang;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "sales_order_line_id")
    private SalesOrderLine salesOrderLine;

    @Column(name = "ordered_quantity", nullable = false)
    private Integer orderedQuantity;

    @Column(name = "shipped_quantity", nullable = false)
    private Integer shippedQuantity = 0;

    public DeliveryOrderLine() {}

    public DeliveryOrderLine(Barang barang, Integer orderedQuantity) {
        this.barang = barang;
        this.orderedQuantity = orderedQuantity;
    }

    public DeliveryOrderLine(Barang barang, SalesOrderLine salesOrderLine, Integer orderedQuantity) {
        this(barang, orderedQuantity);
        this.salesOrderLine = salesOrderLine;
    }

    public Integer getId() { return id; }
    public DeliveryOrder getDeliveryOrder() { return deliveryOrder; }
    public Barang getBarang() { return barang; }
    public SalesOrderLine getSalesOrderLine() { return salesOrderLine; }
    public Integer getOrderedQuantity() { return orderedQuantity; }
    public Integer getShippedQuantity() { return shippedQuantity == null ? 0 : shippedQuantity; }
    public Integer getRemainingQuantity() { return orderedQuantity - getShippedQuantity(); }
    public void setDeliveryOrder(DeliveryOrder deliveryOrder) { this.deliveryOrder = deliveryOrder; }
    public void setShippedQuantity(Integer shippedQuantity) { this.shippedQuantity = shippedQuantity; }

    @Override
    public String toString() {
        String orderCode = deliveryOrder == null ? "-" : deliveryOrder.getOrderCode();
        return orderCode + " | " + barang.getKodeBarang() + " - "
                + barang.getNamaBarang() + " | terkirim " + getShippedQuantity();
    }
}