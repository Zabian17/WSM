package com.mycompany.tugas_akhir.repository;

import com.mycompany.tugas_akhir.model.DeliveryOrderLotReservation;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface DeliveryOrderLotReservationRepository extends JpaRepository<DeliveryOrderLotReservation, Integer> {

    @Query("SELECT new com.mycompany.tugas_akhir.repository.WarehouseStageTotalsProjection("
            + "SUM(r.pickedQuantity), SUM(r.packedQuantity), SUM(r.loadedQuantity)) "
            + "FROM DeliveryOrderLotReservation r "
            + "WHERE r.deliveryOrderLine.deliveryOrder.createdAt >= :from "
            + "AND r.deliveryOrderLine.deliveryOrder.createdAt <= :to")
    WarehouseStageTotalsProjection getWarehouseStageTotals(@Param("from") java.time.LocalDateTime from,
                                                             @Param("to") java.time.LocalDateTime to);

    @Query("SELECT r FROM DeliveryOrderLotReservation r JOIN FETCH r.lotBalance "
            + "WHERE r.deliveryOrderLine.deliveryOrder.id = :orderId ORDER BY r.deliveryOrderLine.id, r.id")
    List<DeliveryOrderLotReservation> findByOrderId(@Param("orderId") Integer orderId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT r FROM DeliveryOrderLotReservation r JOIN FETCH r.lotBalance "
            + "WHERE r.deliveryOrderLine.deliveryOrder.id = :orderId ORDER BY r.id")
    List<DeliveryOrderLotReservation> findByOrderIdForUpdate(@Param("orderId") Integer orderId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT r FROM DeliveryOrderLotReservation r JOIN FETCH r.lotBalance "
            + "WHERE r.deliveryOrderLine.id = :lineId ORDER BY r.id")
    List<DeliveryOrderLotReservation> findByLineIdForUpdate(@Param("lineId") Integer lineId);
}
