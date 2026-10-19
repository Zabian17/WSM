package com.mycompany.tugas_akhir.repository;

import com.mycompany.tugas_akhir.model.StockTransferOrder;
import com.mycompany.tugas_akhir.model.StockTransferOrderStatus;
import jakarta.persistence.LockModeType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface StockTransferOrderRepository extends JpaRepository<StockTransferOrder, Integer> {

    List<StockTransferOrder> findTop200ByOrderByCreatedAtDesc();

    Page<StockTransferOrder> findByAssignedPicker_IdAndStatusInOrderByCreatedAtAsc(
            Integer pickerId, List<StockTransferOrderStatus> statuses, Pageable pageable);

    @Query("SELECT DISTINCT o FROM StockTransferOrder o LEFT JOIN FETCH o.lines line "
            + "JOIN FETCH line.barang JOIN FETCH line.sourceRak WHERE o.id = :id")
    Optional<StockTransferOrder> findByIdWithLines(@Param("id") Integer id);

    @Query("SELECT DISTINCT o FROM StockTransferOrder o LEFT JOIN FETCH o.lines line "
            + "LEFT JOIN FETCH line.barang WHERE (:from IS NULL OR o.createdAt >= :from) "
            + "AND (:to IS NULL OR o.createdAt <= :to) ORDER BY o.createdAt DESC")
    List<StockTransferOrder> findInRangeWithLines(@Param("from") LocalDateTime from,
                                                   @Param("to") LocalDateTime to);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT DISTINCT o FROM StockTransferOrder o LEFT JOIN FETCH o.lines WHERE o.id = :id")
    Optional<StockTransferOrder> findByIdWithLinesForUpdate(@Param("id") Integer id);
}
