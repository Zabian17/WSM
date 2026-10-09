package com.mycompany.tugas_akhir.repository;

import com.mycompany.tugas_akhir.model.DeliveryOrderFulfillmentAudit;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface DeliveryOrderFulfillmentAuditRepository
        extends JpaRepository<DeliveryOrderFulfillmentAudit, Integer> {

    @Query("SELECT a FROM DeliveryOrderFulfillmentAudit a "
            + "JOIN FETCH a.deliveryOrderLine line JOIN FETCH line.barang "
            + "JOIN FETCH a.actor WHERE a.deliveryOrder.id = :orderId "
            + "ORDER BY a.recordedAt DESC")
    List<DeliveryOrderFulfillmentAudit> findByOrderId(@Param("orderId") Integer orderId);

    @Query("SELECT a FROM DeliveryOrderFulfillmentAudit a JOIN FETCH a.deliveryOrder "
            + "JOIN FETCH a.deliveryOrderLine line JOIN FETCH line.barang JOIN FETCH a.actor "
            + "ORDER BY a.recordedAt DESC")
    List<DeliveryOrderFulfillmentAudit> findRecent(Pageable pageable);
}
