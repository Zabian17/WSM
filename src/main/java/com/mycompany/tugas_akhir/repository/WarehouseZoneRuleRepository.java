package com.mycompany.tugas_akhir.repository;

import com.mycompany.tugas_akhir.model.WarehouseZoneRule;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Set;

public interface WarehouseZoneRuleRepository extends JpaRepository<WarehouseZoneRule, Integer> {
    List<WarehouseZoneRule> findAllByOrderByOperationAscZoneCodeAsc();
    Set<WarehouseZoneRule> findByOperation(WarehouseZoneRule.Operation operation);
    boolean existsByOperationAndZoneCode(WarehouseZoneRule.Operation operation, String zoneCode);
}
