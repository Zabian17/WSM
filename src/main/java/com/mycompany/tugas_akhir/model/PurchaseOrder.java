package com.mycompany.tugas_akhir.model;

import jakarta.persistence.*;

import java.time.LocalDateTime;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "purchase_order")
public class PurchaseOrder {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(name = "order_code", nullable = false, unique = true, length = 24)
    private String orderCode;

    @ManyToOne(fetch = FetchType.EAGER, optional = false)
    @JoinColumn(name = "supplier_id", nullable = false)
    private Supplier supplier;

    @ManyToOne(fetch = FetchType.EAGER, optional = false)
    @JoinColumn(name = "created_by", nullable = false)
    private User createdBy;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 24)
    private PurchaseOrderStatus status;

    @Column(columnDefinition = "TEXT")
    private String notes;

    @Column(name = "currency_code", nullable = false, length = 3)
    private String currencyCode = "IDR";

    @Column(name = "tender_reference", length = 80)
    private String tenderReference;

    @Column(name = "payment_terms", length = 120)
    private String paymentTerms;

    @Column(name = "expected_delivery_date")
    private LocalDate expectedDeliveryDate;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "open_line_count", nullable = false)
    private Integer openLineCount = 0;

    @Column(name = "received_quantity", nullable = false)
    private Integer receivedQuantity = 0;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "reviewed_by")
    private User reviewedBy;

    @Column(name = "reviewed_at")
    private LocalDateTime reviewedAt;

    @Column(name = "review_notes", columnDefinition = "TEXT")
    private String reviewNotes;

    @OneToMany(mappedBy = "purchaseOrder", cascade = CascadeType.ALL,
            orphanRemoval = true, fetch = FetchType.EAGER)
    @OrderBy("id ASC")
    private List<PurchaseOrderLine> lines = new ArrayList<>();

    protected PurchaseOrder() {}

    public PurchaseOrder(String orderCode, Supplier supplier, User createdBy,
                         String notes, LocalDateTime createdAt) {
        this.orderCode = orderCode;
        this.supplier = supplier;
        this.createdBy = createdBy;
        this.notes = notes;
        this.createdAt = createdAt;
        this.status = PurchaseOrderStatus.PENDING_APPROVAL;
    }

    public void addLine(PurchaseOrderLine line) {
        lines.add(line);
        line.setPurchaseOrder(this);
        openLineCount++;
    }

    public Integer getId() { return id; }
    public String getOrderCode() { return orderCode; }
    public Supplier getSupplier() { return supplier; }
    public User getCreatedBy() { return createdBy; }
    public PurchaseOrderStatus getStatus() { return status; }
    public String getNotes() { return notes; }
    public String getCurrencyCode() { return currencyCode; }
    public String getTenderReference() { return tenderReference; }
    public String getPaymentTerms() { return paymentTerms; }
    public LocalDate getExpectedDeliveryDate() { return expectedDeliveryDate; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public List<PurchaseOrderLine> getLines() { return lines; }
    public Integer getOpenLineCount() { return openLineCount == null ? 0 : openLineCount; }
    public Integer getReceivedQuantity() { return receivedQuantity == null ? 0 : receivedQuantity; }
    public User getReviewedBy() { return reviewedBy; }
    public LocalDateTime getReviewedAt() { return reviewedAt; }
    public String getReviewNotes() { return reviewNotes; }
    public void setStatus(PurchaseOrderStatus status) { this.status = status; }
    public void setOpenLineCount(Integer openLineCount) { this.openLineCount = openLineCount; }
    public void setReceivedQuantity(Integer receivedQuantity) { this.receivedQuantity = receivedQuantity; }
    public void setReviewedBy(User reviewedBy) { this.reviewedBy = reviewedBy; }
    public void setReviewedAt(LocalDateTime reviewedAt) { this.reviewedAt = reviewedAt; }
    public void setReviewNotes(String reviewNotes) { this.reviewNotes = reviewNotes; }
    public void setCommercialDetails(String currencyCode, String tenderReference,
                                     String paymentTerms, LocalDate expectedDeliveryDate) {
        this.currencyCode = currencyCode;
        this.tenderReference = tenderReference;
        this.paymentTerms = paymentTerms;
        this.expectedDeliveryDate = expectedDeliveryDate;
    }
    public java.math.BigDecimal getTotalAmount() {
        return lines.stream().map(PurchaseOrderLine::getLineAmount)
                .reduce(java.math.BigDecimal.ZERO, java.math.BigDecimal::add);
    }
}
