package com.mycompany.tugas_akhir.model;

import jakarta.persistence.*;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "stock_transfer_order")
public class StockTransferOrder {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(name = "order_code", nullable = false, unique = true, length = 24)
    private String orderCode;

    @ManyToOne(fetch = FetchType.EAGER, optional = false)
    @JoinColumn(name = "source_warehouse_id", nullable = false)
    private Warehouse sourceWarehouse;

    @ManyToOne(fetch = FetchType.EAGER, optional = false)
    @JoinColumn(name = "destination_warehouse_id", nullable = false)
    private Warehouse destinationWarehouse;

    @ManyToOne(fetch = FetchType.EAGER, optional = false)
    @JoinColumn(name = "created_by", nullable = false)
    private User createdBy;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "reviewed_by")
    private User reviewedBy;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "assigned_picker_id")
    private User assignedPicker;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 40)
    private StockTransferOrderStatus status;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String reason;

    @Column(name = "review_notes", columnDefinition = "TEXT")
    private String reviewNotes;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "reviewed_at")
    private LocalDateTime reviewedAt;

    @Column(name = "assigned_at")
    private LocalDateTime assignedAt;

    @Column(name = "dispatched_at")
    private LocalDateTime dispatchedAt;

    @Column(name = "received_at")
    private LocalDateTime receivedAt;

    @OneToMany(mappedBy = "stockTransferOrder", cascade = CascadeType.ALL, orphanRemoval = true,
            fetch = FetchType.EAGER)
    private List<StockTransferOrderLine> lines = new ArrayList<>();

    protected StockTransferOrder() {}

    public StockTransferOrder(String orderCode, Warehouse sourceWarehouse, Warehouse destinationWarehouse,
                              User createdBy, String reason, LocalDateTime createdAt) {
        this.orderCode = orderCode;
        this.sourceWarehouse = sourceWarehouse;
        this.destinationWarehouse = destinationWarehouse;
        this.createdBy = createdBy;
        this.reason = reason;
        this.createdAt = createdAt;
        this.status = StockTransferOrderStatus.PENDING_APPROVAL;
    }

    public void addLine(StockTransferOrderLine line) {
        lines.add(line);
        line.setStockTransferOrder(this);
    }

    public void review(User reviewer, boolean approved, String notes, LocalDateTime time) {
        reviewedBy = reviewer;
        reviewedAt = time;
        reviewNotes = notes;
        status = approved ? StockTransferOrderStatus.APPROVED : StockTransferOrderStatus.REJECTED;
    }

    public void markDispatched(LocalDateTime time) {
        dispatchedAt = time;
        status = StockTransferOrderStatus.DISPATCHED;
    }

    public void assignPicker(User picker, LocalDateTime time) {
        assignedPicker = picker;
        assignedAt = time;
    }

    public void markPartiallyDispatched() {
        status = StockTransferOrderStatus.PARTIALLY_DISPATCHED;
    }

    public void markPartiallyReceived() {
        status = StockTransferOrderStatus.PARTIALLY_RECEIVED;
    }

    public void updateScannedProgress(int dispatchedQuantity, int receivedQuantity,
                                      int requestedQuantity, LocalDateTime time) {
        boolean dispatchComplete = dispatchedQuantity == requestedQuantity;
        boolean receiveStarted = receivedQuantity > 0;
        boolean receiptComplete = receivedQuantity == dispatchedQuantity;
        if (dispatchComplete && dispatchedAt == null) dispatchedAt = time;
        if (receiptComplete && dispatchComplete) {
            markReceived(time);
        } else if (dispatchComplete) {
            status = receiveStarted
                    ? StockTransferOrderStatus.PARTIALLY_RECEIVED
                    : StockTransferOrderStatus.DISPATCHED;
        } else if (receiveStarted) {
            status = StockTransferOrderStatus.PARTIALLY_DISPATCHED_AND_RECEIVED;
        } else {
            status = StockTransferOrderStatus.PARTIALLY_DISPATCHED;
        }
    }

    public void markReceived(LocalDateTime time) {
        receivedAt = time;
        status = StockTransferOrderStatus.RECEIVED;
    }

    public Integer getId() { return id; }
    public String getOrderCode() { return orderCode; }
    public Warehouse getSourceWarehouse() { return sourceWarehouse; }
    public Warehouse getDestinationWarehouse() { return destinationWarehouse; }
    public User getCreatedBy() { return createdBy; }
    public User getReviewedBy() { return reviewedBy; }
    public User getAssignedPicker() { return assignedPicker; }
    public StockTransferOrderStatus getStatus() { return status; }
    public String getReason() { return reason; }
    public String getReviewNotes() { return reviewNotes; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public LocalDateTime getReviewedAt() { return reviewedAt; }
    public LocalDateTime getAssignedAt() { return assignedAt; }
    public LocalDateTime getDispatchedAt() { return dispatchedAt; }
    public LocalDateTime getReceivedAt() { return receivedAt; }
    public List<StockTransferOrderLine> getLines() { return lines; }
}
