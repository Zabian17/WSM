package com.mycompany.tugas_akhir.model;

import jakarta.persistence.*;

@Entity
@Table(name = "delivery_order_lot_reservation", uniqueConstraints =
        @UniqueConstraint(name = "uk_do_line_lot", columnNames = {"delivery_order_line_id", "lot_balance_id"}))
public class DeliveryOrderLotReservation {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @ManyToOne(fetch = FetchType.EAGER, optional = false)
    @JoinColumn(name = "delivery_order_line_id", nullable = false)
    private DeliveryOrderLine deliveryOrderLine;

    @ManyToOne(fetch = FetchType.EAGER, optional = false)
    @JoinColumn(name = "lot_balance_id", nullable = false)
    private InventoryLotBalance lotBalance;

    @Column(name = "reserved_quantity", nullable = false)
    private Integer reservedQuantity;

    @Column(name = "picked_quantity", nullable = false)
    private Integer pickedQuantity = 0;

    @Column(name = "packed_quantity", nullable = false)
    private Integer packedQuantity = 0;

    @Column(name = "loaded_quantity", nullable = false)
    private Integer loadedQuantity = 0;

    public DeliveryOrderLotReservation() {}

    public DeliveryOrderLotReservation(DeliveryOrderLine deliveryOrderLine,
                                      InventoryLotBalance lotBalance, Integer reservedQuantity) {
        this.deliveryOrderLine = deliveryOrderLine;
        this.lotBalance = lotBalance;
        this.reservedQuantity = reservedQuantity;
    }

    public Integer getId() { return id; }
    public DeliveryOrderLine getDeliveryOrderLine() { return deliveryOrderLine; }
    public InventoryLotBalance getLotBalance() { return lotBalance; }
    public Integer getReservedQuantity() { return reservedQuantity; }
    public Integer getPickedQuantity() { return pickedQuantity == null ? 0 : pickedQuantity; }
    public Integer getPackedQuantity() { return packedQuantity == null ? 0 : packedQuantity; }
    public Integer getLoadedQuantity() { return loadedQuantity == null ? 0 : loadedQuantity; }
    public void setPickedQuantity(Integer pickedQuantity) { this.pickedQuantity = pickedQuantity; }
    public void setReservedQuantity(Integer reservedQuantity) { this.reservedQuantity = reservedQuantity; }
    public void setPackedQuantity(Integer packedQuantity) { this.packedQuantity = packedQuantity; }
    public void setLoadedQuantity(Integer loadedQuantity) { this.loadedQuantity = loadedQuantity; }
}