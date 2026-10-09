package com.mycompany.tugas_akhir.repository;

import com.mycompany.tugas_akhir.model.PurchaseOrder;
import com.mycompany.tugas_akhir.model.PurchaseOrderLine;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface PurchaseOrderRepository extends JpaRepository<PurchaseOrder, Integer> {

    List<PurchaseOrder> findTop100ByOrderByCreatedAtDesc();

    @Query("SELECT p FROM PurchaseOrder p WHERE p.status = com.mycompany.tugas_akhir.model.PurchaseOrderStatus.PENDING_APPROVAL "
            + "ORDER BY p.createdAt")
    List<PurchaseOrder> findPendingApproval();

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT p FROM PurchaseOrder p WHERE p.id = :id")
    Optional<PurchaseOrder> findByIdForUpdate(@Param("id") Integer id);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT DISTINCT p FROM PurchaseOrder p LEFT JOIN FETCH p.lines WHERE p.id = :id")
    Optional<PurchaseOrder> findByIdWithLinesForUpdate(@Param("id") Integer id);

    @Query("SELECT line FROM PurchaseOrderLine line "
            + "JOIN FETCH line.purchaseOrder purchaseOrder "
            + "WHERE line.id = :id")
    Optional<PurchaseOrderLine> findLineById(@Param("id") Integer id);

    @Query("SELECT DISTINCT line FROM PurchaseOrderLine line "
            + "JOIN FETCH line.barang "
            + "JOIN FETCH line.purchaseOrder purchaseOrder "
            + "JOIN FETCH purchaseOrder.supplier "
            + "WHERE purchaseOrder.status IN (com.mycompany.tugas_akhir.model.PurchaseOrderStatus.APPROVED, "
            + "com.mycompany.tugas_akhir.model.PurchaseOrderStatus.PARTIALLY_RECEIVED, "
            + "com.mycompany.tugas_akhir.model.PurchaseOrderStatus.OPEN) "
            + "AND line.orderedQuantity > line.receivedQuantity + line.pendingQuantity "
            + "ORDER BY purchaseOrder.createdAt DESC, line.id")
    List<PurchaseOrderLine> findOpenLines();

    @Query("SELECT DISTINCT line FROM PurchaseOrderLine line "
            + "JOIN FETCH line.barang JOIN FETCH line.purchaseOrder po "
            + "WHERE po.status IN (com.mycompany.tugas_akhir.model.PurchaseOrderStatus.RECEIVED, "
            + "com.mycompany.tugas_akhir.model.PurchaseOrderStatus.PARTIALLY_RECEIVED) "
            + "AND line.receivedQuantity > 0 ORDER BY po.createdAt DESC, line.id")
    List<PurchaseOrderLine> findReceivedLines();

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT line FROM PurchaseOrderLine line "
            + "JOIN FETCH line.barang "
            + "JOIN FETCH line.purchaseOrder purchaseOrder "
            + "JOIN FETCH purchaseOrder.supplier "
            + "WHERE line.id = :id")
    Optional<PurchaseOrderLine> findLineByIdForUpdate(@Param("id") Integer id);
}
