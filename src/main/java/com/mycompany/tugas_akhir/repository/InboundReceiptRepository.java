package com.mycompany.tugas_akhir.repository;

import com.mycompany.tugas_akhir.model.InboundReceipt;
import com.mycompany.tugas_akhir.model.InboundReceiptStatus;
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

public interface InboundReceiptRepository extends JpaRepository<InboundReceipt, Integer> {

    Page<InboundReceipt> findAllByStatusOrderByCreatedAtAsc(InboundReceiptStatus status, Pageable pageable);

    @Query("SELECT r FROM InboundReceipt r "
            + "LEFT JOIN FETCH r.purchaseOrderLine line "
            + "LEFT JOIN FETCH line.purchaseOrder "
            + "ORDER BY r.createdAt DESC")
    List<InboundReceipt> findRecentWithPurchaseOrder(Pageable pageable);

    @Query("SELECT r FROM InboundReceipt r "
            + "LEFT JOIN FETCH r.purchaseOrderLine line LEFT JOIN FETCH line.purchaseOrder "
            + "WHERE (:from IS NULL OR r.createdAt >= :from) AND (:to IS NULL OR r.createdAt <= :to) "
            + "ORDER BY r.createdAt DESC")
    List<InboundReceipt> findInRangeWithPurchaseOrder(@Param("from") LocalDateTime from,
                                                       @Param("to") LocalDateTime to);

    @Query("SELECT r FROM InboundReceipt r "
            + "LEFT JOIN FETCH r.purchaseOrderLine line "
            + "LEFT JOIN FETCH line.purchaseOrder "
            + "ORDER BY CASE WHEN r.status = :pending THEN 0 ELSE 1 END, "
            + "CASE WHEN r.status = :pending THEN r.createdAt END ASC, r.createdAt DESC")
    List<InboundReceipt> findWithPurchaseOrderQcFifo(@Param("pending") InboundReceiptStatus pending,
                                                      Pageable pageable);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT r FROM InboundReceipt r WHERE r.id = :id")
    Optional<InboundReceipt> findByIdForUpdate(@Param("id") Integer id);
}
