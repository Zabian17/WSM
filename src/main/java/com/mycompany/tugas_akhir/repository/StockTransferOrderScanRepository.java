package com.mycompany.tugas_akhir.repository;

import com.mycompany.tugas_akhir.model.StockTransferOrderScan;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface StockTransferOrderScanRepository extends JpaRepository<StockTransferOrderScan, Integer> {

    @Query("SELECT s FROM StockTransferOrderScan s JOIN FETCH s.picker "
            + "JOIN FETCH s.stockTransferOrderLine line JOIN FETCH line.barang "
            + "LEFT JOIN FETCH s.sourceScan source "
            + "WHERE s.stockTransferOrder.id = :orderId ORDER BY s.scannedAt, s.id")
    List<StockTransferOrderScan> findByOrderId(@Param("orderId") Integer orderId);
}
