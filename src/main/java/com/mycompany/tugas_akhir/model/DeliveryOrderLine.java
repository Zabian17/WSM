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

    @Column(name = "ordered_quantity", nullable = false)
    private Integer orderedQuantity;

    public DeliveryOrderLine() {}

    public DeliveryOrderLine(Barang barang, Integer orderedQuantity) {
        this.barang = barang;
        this.orderedQuantity = orderedQuantity;
    }

    public Integer getId() { return id; }
    public DeliveryOrder getDeliveryOrder() { return deliveryOrder; }
    public Barang getBarang() { return barang; }
    public Integer getOrderedQuantity() { return orderedQuantity; }
    public void setDeliveryOrder(DeliveryOrder deliveryOrder) { this.deliveryOrder = deliveryOrder; }

    @Override
    public String toString() {
        return barang.getKodeBarang() + " - " + barang.getNamaBarang() + " (" + orderedQuantity + ")";
    }
}