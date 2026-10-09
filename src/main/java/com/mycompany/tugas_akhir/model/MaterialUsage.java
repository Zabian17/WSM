package com.mycompany.tugas_akhir.model;

import jakarta.persistence.*;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "material_usage")
public class MaterialUsage {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(name = "request_code", nullable = false, unique = true, length = 24)
    private String requestCode;

    @ManyToOne(fetch = FetchType.EAGER, optional = false)
    @JoinColumn(name = "created_by", nullable = false)
    private User createdBy;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "reviewed_by")
    private User reviewedBy;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 24)
    private MaterialUsageStatus status;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String reason;

    @Column(columnDefinition = "TEXT")
    private String reviewNotes;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "reviewed_at")
    private LocalDateTime reviewedAt;

    @Column(name = "issued_at")
    private LocalDateTime issuedAt;

    @OneToMany(mappedBy = "materialUsage", cascade = CascadeType.ALL, orphanRemoval = true,
            fetch = FetchType.EAGER)
    private List<MaterialUsageLine> lines = new ArrayList<>();

    protected MaterialUsage() {}

    public MaterialUsage(String requestCode, User createdBy, String reason, LocalDateTime createdAt) {
        this.requestCode = requestCode;
        this.createdBy = createdBy;
        this.reason = reason;
        this.createdAt = createdAt;
        this.status = MaterialUsageStatus.PENDING_APPROVAL;
    }

    public void addLine(MaterialUsageLine line) {
        lines.add(line);
        line.setMaterialUsage(this);
    }

    public Integer getId() { return id; }
    public String getRequestCode() { return requestCode; }
    public User getCreatedBy() { return createdBy; }
    public User getReviewedBy() { return reviewedBy; }
    public MaterialUsageStatus getStatus() { return status; }
    public String getReason() { return reason; }
    public String getReviewNotes() { return reviewNotes; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public LocalDateTime getReviewedAt() { return reviewedAt; }
    public LocalDateTime getIssuedAt() { return issuedAt; }
    public List<MaterialUsageLine> getLines() { return lines; }

    public void review(User reviewer, boolean approved, String notes, LocalDateTime reviewedAt) {
        this.reviewedBy = reviewer;
        this.reviewedAt = reviewedAt;
        this.reviewNotes = notes;
        this.status = approved ? MaterialUsageStatus.APPROVED : MaterialUsageStatus.REJECTED;
    }

    public void markIssued(LocalDateTime issuedAt) {
        this.issuedAt = issuedAt;
        this.status = MaterialUsageStatus.ISSUED;
    }

    public void markPartiallyIssued() {
        this.status = MaterialUsageStatus.PARTIALLY_ISSUED;
    }
}
