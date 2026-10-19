package com.mycompany.tugas_akhir.repository;

import com.mycompany.tugas_akhir.model.DeliveryOrderPickScan;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.domain.Pageable;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;

public interface DeliveryOrderPickScanRepository extends JpaRepository<DeliveryOrderPickScan, Integer> {

    @Query("SELECT s FROM DeliveryOrderPickScan s JOIN FETCH s.picker "
            + "JOIN FETCH s.deliveryOrderLine l JOIN FETCH l.barang "
            + "JOIN FETCH s.lotBalance lot JOIN FETCH lot.rak "
            + "WHERE s.deliveryOrder.id = :orderId ORDER BY s.scannedAt DESC")
    List<DeliveryOrderPickScan> findByOrderId(@Param("orderId") Integer orderId, Pageable pageable);

    @Query("SELECT s FROM DeliveryOrderPickScan s JOIN FETCH s.picker JOIN FETCH s.deliveryOrder "
            + "JOIN FETCH s.deliveryOrderLine line JOIN FETCH line.barang "
            + "JOIN FETCH s.lotBalance lot JOIN FETCH lot.rak ORDER BY s.scannedAt DESC")
    List<DeliveryOrderPickScan> findRecent(Pageable pageable);

    @Query("SELECT s FROM DeliveryOrderPickScan s JOIN FETCH s.picker JOIN FETCH s.deliveryOrder "
            + "JOIN FETCH s.deliveryOrderLine line JOIN FETCH line.barang "
            + "JOIN FETCH s.lotBalance lot JOIN FETCH lot.rak "
            + "WHERE (:from IS NULL OR s.scannedAt >= :from) AND (:to IS NULL OR s.scannedAt <= :to) "
            + "ORDER BY s.scannedAt DESC")
    List<DeliveryOrderPickScan> findInRange(@Param("from") LocalDateTime from,
                                             @Param("to") LocalDateTime to);
}
