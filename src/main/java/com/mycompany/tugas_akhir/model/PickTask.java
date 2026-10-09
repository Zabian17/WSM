package com.mycompany.tugas_akhir.model;

import jakarta.persistence.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "pick_task")
public class PickTask {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(name = "task_code", nullable = false, unique = true, length = 24)
    private String taskCode;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "delivery_order_id")
    private DeliveryOrder deliveryOrder;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "material_usage_id")
    private MaterialUsage materialUsage;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "assigned_picker_id")
    private User assignedPicker;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "assigned_section_id")
    private Section assignedSection;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 16)
    private PickTaskStatus status;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "assigned_at")
    private LocalDateTime assignedAt;

    @Column(name = "completed_at")
    private LocalDateTime completedAt;

    protected PickTask() {}

    public PickTask(String taskCode, DeliveryOrder deliveryOrder, LocalDateTime createdAt) {
        this.taskCode = taskCode;
        this.deliveryOrder = deliveryOrder;
        this.createdAt = createdAt;
        this.status = PickTaskStatus.OPEN;
    }

    public PickTask(String taskCode, MaterialUsage materialUsage, LocalDateTime createdAt) {
        this.taskCode = taskCode;
        this.materialUsage = materialUsage;
        this.createdAt = createdAt;
        this.status = PickTaskStatus.OPEN;
    }

    public Integer getId() { return id; }
    public String getTaskCode() { return taskCode; }
    public DeliveryOrder getDeliveryOrder() { return deliveryOrder; }
    public MaterialUsage getMaterialUsage() { return materialUsage; }
    public User getAssignedPicker() { return assignedPicker; }
    public Section getAssignedSection() { return assignedSection; }
    public PickTaskStatus getStatus() { return status; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public LocalDateTime getAssignedAt() { return assignedAt; }
    public LocalDateTime getCompletedAt() { return completedAt; }
    public void assign(User picker, Section section, LocalDateTime time) {
        assignedPicker = picker;
        assignedSection = section;
        assignedAt = time;
        status = PickTaskStatus.ASSIGNED;
    }
    public void setStatus(PickTaskStatus status) { this.status = status; }
    public void setCompletedAt(LocalDateTime completedAt) { this.completedAt = completedAt; }
}
