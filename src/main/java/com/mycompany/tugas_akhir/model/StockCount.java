package com.mycompany.tugas_akhir.model;

import jakarta.persistence.*;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "stock_count")
public class StockCount {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(name = "count_code", nullable = false, unique = true, length = 24)
    private String countCode;

    @ManyToOne(fetch = FetchType.EAGER, optional = false)
    @JoinColumn(name = "rak_id", nullable = false)
    private Rak rak;

    @ManyToOne(fetch = FetchType.EAGER, optional = false)
    @JoinColumn(name = "created_by", nullable = false)
    private User createdBy;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "posted_by")
    private User postedBy;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "counted_by")
    private User countedBy;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 16)
    private StockCountStatus status;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "posted_at")
    private LocalDateTime postedAt;

    @OneToMany(mappedBy = "stockCount", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.EAGER)
    private List<StockCountLine> lines = new ArrayList<>();

    public StockCount() {}

    public StockCount(String countCode, Rak rak, User createdBy, LocalDateTime createdAt) {
        this.countCode = countCode;
        this.rak = rak;
        this.createdBy = createdBy;
        this.createdAt = createdAt;
        this.status = StockCountStatus.COUNTING;
    }

    public void addLine(StockCountLine line) {
        lines.add(line);
        line.setStockCount(this);
    }

    public Integer getId() { return id; }
    public String getCountCode() { return countCode; }
    public Rak getRak() { return rak; }
    public User getCreatedBy() { return createdBy; }
    public User getPostedBy() { return postedBy; }
    public User getCountedBy() { return countedBy; }
    public StockCountStatus getStatus() { return status; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public LocalDateTime getPostedAt() { return postedAt; }
    public List<StockCountLine> getLines() { return lines; }
    public void setStatus(StockCountStatus status) { this.status = status; }
    public void setPostedBy(User postedBy) { this.postedBy = postedBy; }
    public void setCountedBy(User countedBy) { this.countedBy = countedBy; }
    public void setPostedAt(LocalDateTime postedAt) { this.postedAt = postedAt; }
}
