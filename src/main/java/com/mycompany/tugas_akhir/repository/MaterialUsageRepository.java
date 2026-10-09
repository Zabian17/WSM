package com.mycompany.tugas_akhir.repository;

import com.mycompany.tugas_akhir.model.MaterialUsage;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface MaterialUsageRepository extends JpaRepository<MaterialUsage, Integer> {

    List<MaterialUsage> findTop100ByOrderByCreatedAtDesc();

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT DISTINCT m FROM MaterialUsage m LEFT JOIN FETCH m.lines WHERE m.id = :id")
    Optional<MaterialUsage> findByIdWithLinesForUpdate(@Param("id") Integer id);
}
