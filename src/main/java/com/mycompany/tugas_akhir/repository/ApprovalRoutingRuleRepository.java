package com.mycompany.tugas_akhir.repository;

import com.mycompany.tugas_akhir.model.ApprovalRoutingRule;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface ApprovalRoutingRuleRepository extends JpaRepository<ApprovalRoutingRule, Integer> {
    Optional<ApprovalRoutingRule> findByRequesterDepartmentIgnoreCase(String requesterDepartment);
}
