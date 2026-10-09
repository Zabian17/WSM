package com.mycompany.tugas_akhir.model;

import jakarta.persistence.*;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "sales_order")
public class SalesOrder {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(name = "order_code", nullable = false, unique = true, length = 24)
    private String orderCode;

    @ManyToOne(fetch = FetchType.EAGER, optional = false)
    @JoinColumn(name = "customer_id", nullable = false)
    private Customer customer;

    @ManyToOne(fetch = FetchType.EAGER, optional = false)
    @JoinColumn(name = "created_by", nullable = false)
    private User createdBy;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 24)
    private SalesOrderStatus status;

    @Column(columnDefinition = "TEXT")
    private String notes;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @OneToMany(mappedBy = "salesOrder", cascade = CascadeType.ALL,
            orphanRemoval = true, fetch = FetchType.EAGER)
    private List<SalesOrderLine> lines = new ArrayList<>();

    protected SalesOrder() {}

    public SalesOrder(String orderCode, Customer customer, User createdBy,
                      String notes, LocalDateTime createdAt) {
        this.orderCode = orderCode;
        this.customer = customer;
        this.createdBy = createdBy;
        this.notes = notes;
        this.createdAt = createdAt;
        this.status = SalesOrderStatus.OPEN;
    }

    public void addLine(SalesOrderLine line) {
        lines.add(line);
        line.setSalesOrder(this);
    }

    public Integer getId() { return id; }
    public String getOrderCode() { return orderCode; }
    public Customer getCustomer() { return customer; }
    public User getCreatedBy() { return createdBy; }
    public SalesOrderStatus getStatus() { return status; }
    public String getNotes() { return notes; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public List<SalesOrderLine> getLines() { return lines; }
    public void setStatus(SalesOrderStatus status) { this.status = status; }
}
