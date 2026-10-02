package com.mycompany.tugas_akhir.model;

import jakarta.persistence.*;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "inbound_receipt")
public class InboundReceipt {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(name = "receipt_code", nullable = false, unique = true, length = 24)
    private String receiptCode;

    @ManyToOne(fetch = FetchType.EAGER, optional = false)
    @JoinColumn(name = "barang_id", nullable = false)
    private Barang barang;

    @ManyToOne(fetch = FetchType.EAGER, optional = false)
    @JoinColumn(name = "supplier_id", nullable = false)
    private Supplier supplier;

    @Column(nullable = false)
    private Integer quantity;

    @Column(name = "batch_code", nullable = false, length = 64)
    private String batchCode;

    @Column(name = "expiry_date")
    private LocalDate expiryDate;

    @Column(columnDefinition = "TEXT")
    private String notes;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 24)
    private InboundReceiptStatus status;

    @Column(name = "qc_notes", columnDefinition = "TEXT")
    private String qcNotes;

    @ManyToOne(fetch = FetchType.EAGER, optional = false)
    @JoinColumn(name = "created_by", nullable = false)
    private User createdBy;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "qc_by")
    private User qcBy;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "putaway_rak_id")
    private Rak putawayRak;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "qc_at")
    private LocalDateTime qcAt;

    public InboundReceipt() {}

    public InboundReceipt(String receiptCode, Barang barang, Supplier supplier, Integer quantity,
                          String batchCode, LocalDate expiryDate, String notes, User createdBy,
                          LocalDateTime createdAt) {
        this.receiptCode = receiptCode;
        this.barang = barang;
        this.supplier = supplier;
        this.quantity = quantity;
        this.batchCode = batchCode;
        this.expiryDate = expiryDate;
        this.notes = notes;
        this.createdBy = createdBy;
        this.createdAt = createdAt;
        this.status = InboundReceiptStatus.QC_PENDING;
    }

    public Integer getId() { return id; }
    public String getReceiptCode() { return receiptCode; }
    public Barang getBarang() { return barang; }
    public Supplier getSupplier() { return supplier; }
    public Integer getQuantity() { return quantity; }
    public String getBatchCode() { return batchCode; }
    public LocalDate getExpiryDate() { return expiryDate; }
    public String getNotes() { return notes; }
    public InboundReceiptStatus getStatus() { return status; }
    public String getQcNotes() { return qcNotes; }
    public User getCreatedBy() { return createdBy; }
    public User getQcBy() { return qcBy; }
    public Rak getPutawayRak() { return putawayRak; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public LocalDateTime getQcAt() { return qcAt; }

    public void setStatus(InboundReceiptStatus status) { this.status = status; }
    public void setQcNotes(String qcNotes) { this.qcNotes = qcNotes; }
    public void setQcBy(User qcBy) { this.qcBy = qcBy; }
    public void setQcAt(LocalDateTime qcAt) { this.qcAt = qcAt; }
    public void setPutawayRak(Rak putawayRak) { this.putawayRak = putawayRak; }
}