package com.mycompany.tugas_akhir.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "delivery_order")
public class DeliveryOrder {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(name = "order_code", nullable = false, unique = true, length = 24)
    private String orderCode;

    @ManyToOne(fetch = FetchType.EAGER, optional = false)
    @JoinColumn(name = "customer_id", nullable = false)
    private Customer customer;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "sales_order_id")
    private SalesOrder salesOrder;

    @ManyToOne(fetch = FetchType.EAGER, optional = false)
    @JoinColumn(name = "created_by", nullable = false)
    private User createdBy;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 24)
    private DeliveryOrderStatus status;

    @Column(columnDefinition = "TEXT")
    private String notes;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @OneToMany(mappedBy = "deliveryOrder", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.EAGER)
    private List<DeliveryOrderLine> lines = new ArrayList<>();

    public DeliveryOrder() {}

    public DeliveryOrder(String orderCode, Customer customer, User createdBy, String notes, LocalDateTime createdAt) {
        this.orderCode = orderCode;
        this.customer = customer;
        this.createdBy = createdBy;
        this.notes = notes;
        this.createdAt = createdAt;
        this.status = DeliveryOrderStatus.DRAFT;
    }

    public DeliveryOrder(String orderCode, Customer customer, SalesOrder salesOrder,
                         User createdBy, String notes, LocalDateTime createdAt) {
        this(orderCode, customer, createdBy, notes, createdAt);
        this.salesOrder = salesOrder;
    }

    public void addLine(DeliveryOrderLine line) {
        lines.add(line);
        line.setDeliveryOrder(this);
    }

    public Integer getId() { return id; }
    public String getOrderCode() { return orderCode; }
    public Customer getCustomer() { return customer; }
    public SalesOrder getSalesOrder() { return salesOrder; }
    public User getCreatedBy() { return createdBy; }
    public DeliveryOrderStatus getStatus() { return status; }
    public String getNotes() { return notes; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public List<DeliveryOrderLine> getLines() { return lines; }
    public void setStatus(DeliveryOrderStatus status) { this.status = status; }
}