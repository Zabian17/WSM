package com.mycompany.tugas_akhir.model;

import jakarta.persistence.*;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "return_request")
public class ReturnRequest {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(name = "return_code", nullable = false, unique = true, length = 24)
    private String returnCode;

    @Enumerated(EnumType.STRING)
    @Column(name = "return_type", nullable = false, length = 16)
    private ReturnType returnType;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private ReturnStatus status;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String reason;

    @ManyToOne(fetch = FetchType.EAGER, optional = false)
    @JoinColumn(name = "created_by", nullable = false)
    private User createdBy;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "reviewed_by")
    private User reviewedBy;

    @Column(name = "review_notes", columnDefinition = "TEXT")
    private String reviewNotes;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "reviewed_at")
    private LocalDateTime reviewedAt;

    @Column(name = "posted_at")
    private LocalDateTime postedAt;

    @OneToMany(mappedBy = "returnRequest", cascade = CascadeType.ALL, orphanRemoval = true,
            fetch = FetchType.EAGER)
    private List<ReturnRequestLine> lines = new ArrayList<>();

    protected ReturnRequest() {}

    public ReturnRequest(String returnCode, ReturnType returnType, User createdBy,
                         String reason, LocalDateTime createdAt) {
        this.returnCode = returnCode;
        this.returnType = returnType;
        this.createdBy = createdBy;
        this.reason = reason;
        this.createdAt = createdAt;
        this.status = ReturnStatus.PENDING_REVIEW;
    }

    public void addLine(ReturnRequestLine line) {
        lines.add(line);
        line.setReturnRequest(this);
    }

    public void review(User reviewer, boolean approved, String notes, LocalDateTime time) {
        reviewedBy = reviewer;
        reviewedAt = time;
        reviewNotes = notes;
        status = approved ? ReturnStatus.APPROVED : ReturnStatus.REJECTED;
    }

    public void markPosted(LocalDateTime time) {
        postedAt = time;
        status = ReturnStatus.POSTED;
    }

    public Integer getId() { return id; }
    public String getReturnCode() { return returnCode; }
    public ReturnType getReturnType() { return returnType; }
    public ReturnStatus getStatus() { return status; }
    public String getReason() { return reason; }
    public User getCreatedBy() { return createdBy; }
    public User getReviewedBy() { return reviewedBy; }
    public String getReviewNotes() { return reviewNotes; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public LocalDateTime getReviewedAt() { return reviewedAt; }
    public LocalDateTime getPostedAt() { return postedAt; }
    public List<ReturnRequestLine> getLines() { return lines; }
}
