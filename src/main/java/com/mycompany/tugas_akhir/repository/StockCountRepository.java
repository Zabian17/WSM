package com.mycompany.tugas_akhir.repository;

import com.mycompany.tugas_akhir.model.StockCount;
import com.mycompany.tugas_akhir.model.StockCountStatus;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface StockCountRepository extends JpaRepository<StockCount, Integer> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT c FROM StockCount c WHERE c.id = :id")
    Optional<StockCount> findByIdForUpdate(@Param("id") Integer id);

    List<StockCount> findAllByOrderByCreatedAtDesc();

    List<StockCount> findAllByStatusOrderByPostedAtDesc(StockCountStatus status);

    boolean existsByRakIdAndStatusIn(Integer rackId, List<StockCountStatus> statuses);

    boolean existsByCycleCode(String cycleCode);
}
