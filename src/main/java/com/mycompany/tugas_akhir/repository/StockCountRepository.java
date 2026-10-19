package com.mycompany.tugas_akhir.repository;

import com.mycompany.tugas_akhir.model.StockCount;
import com.mycompany.tugas_akhir.model.StockCountStatus;
import jakarta.persistence.LockModeType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
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

    @Query(value = "SELECT new com.mycompany.tugas_akhir.repository.StockCountSummaryProjection("
            + "c.id, c.countCode, c.cycleCode, c.cycleType, c.rak.kodeRak, c.status, c.dueDate, c.createdAt) "
            + "FROM StockCount c WHERE c.status IN :statuses AND (c.status = :scheduled "
            + "OR c.status = :counted OR EXISTS "
            + "(SELECT l.id FROM StockCountLine l WHERE l.stockCount = c AND l.countedQuantity IS NULL "
            + "AND (l.assignedUser IS NULL OR l.assignedUser.id = :userId))) "
            + "ORDER BY CASE WHEN c.dueDate IS NULL THEN 1 ELSE 0 END, c.dueDate, c.createdAt DESC",
            countQuery = "SELECT COUNT(c) FROM StockCount c WHERE c.status IN :statuses AND (c.status = :scheduled "
                    + "OR c.status = :counted "
                    + "OR EXISTS (SELECT l.id FROM StockCountLine l WHERE l.stockCount = c "
                    + "AND l.countedQuantity IS NULL AND (l.assignedUser IS NULL OR l.assignedUser.id = :userId)))")
    Page<StockCountSummaryProjection> findMobileActiveCounts(@Param("userId") Integer userId,
                                                             @Param("statuses") List<StockCountStatus> statuses,
                                                             @Param("scheduled") StockCountStatus scheduled,
                                                             @Param("counted") StockCountStatus counted,
                                                             Pageable pageable);

    @Query("SELECT c.status FROM StockCount c WHERE c.id = :countId")
    Optional<StockCountStatus> findStatusById(@Param("countId") Integer countId);
}
