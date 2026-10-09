package com.mycompany.tugas_akhir.model;

import jakarta.persistence.*;

@Entity
@Table(name = "approval_routing_rule",
        uniqueConstraints = @UniqueConstraint(name = "uk_approval_requester_department",
                columnNames = "requester_department"))
public class ApprovalRoutingRule {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(name = "requester_department", nullable = false, length = 100)
    private String requesterDepartment;

    @Column(name = "approver_department", nullable = false, length = 100)
    private String approverDepartment;

    protected ApprovalRoutingRule() {}

    public ApprovalRoutingRule(String requesterDepartment, String approverDepartment) {
        this.requesterDepartment = requesterDepartment;
        this.approverDepartment = approverDepartment;
    }

    public Integer getId() { return id; }
    public String getRequesterDepartment() { return requesterDepartment; }
    public String getApproverDepartment() { return approverDepartment; }
    public void setApproverDepartment(String approverDepartment) {
        this.approverDepartment = approverDepartment;
    }
}
