package com.mycompany.tugas_akhir.model;

import jakarta.persistence.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "delivery_order_fulfillment_audit")
public class DeliveryOrderFulfillmentAudit {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "delivery_order_id", nullable = false)
    private DeliveryOrder deliveryOrder;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "delivery_order_line_id", nullable = false)
    private DeliveryOrderLine deliveryOrderLine;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "actor_id", nullable = false)
    private User actor;

    @Column(name = "stage", nullable = false, length = 16)
    private String stage;

    @Column(name = "verified_quantity", nullable = false)
    private Integer verifiedQuantity;

    @Column(name = "recorded_at", nullable = false)
    private LocalDateTime recordedAt;

    protected DeliveryOrderFulfillmentAudit() {}

    public DeliveryOrderFulfillmentAudit(DeliveryOrder deliveryOrder, DeliveryOrderLine deliveryOrderLine,
                                         User actor, String stage, Integer verifiedQuantity,
                                         LocalDateTime recordedAt) {
        this.deliveryOrder = deliveryOrder;
        this.deliveryOrderLine = deliveryOrderLine;
        this.actor = actor;
        this.stage = stage;
        this.verifiedQuantity = verifiedQuantity;
        this.recordedAt = recordedAt;
    }

    public Integer getId() { return id; }
    public DeliveryOrder getDeliveryOrder() { return deliveryOrder; }
    public DeliveryOrderLine getDeliveryOrderLine() { return deliveryOrderLine; }
    public User getActor() { return actor; }
    public String getStage() { return stage; }
    public Integer getVerifiedQuantity() { return verifiedQuantity; }
    public LocalDateTime getRecordedAt() { return recordedAt; }
}
