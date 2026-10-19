package com.mycompany.tugas_akhir.repository;

import com.mycompany.tugas_akhir.model.DeliveryOrder;
import com.mycompany.tugas_akhir.model.DeliveryOrderStatus;
import jakarta.persistence.LockModeType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface DeliveryOrderRepository extends JpaRepository<DeliveryOrder, Integer> {

    List<DeliveryOrder> findTop100ByOrderByCreatedAtDesc();

    @Query("SELECT DISTINCT o FROM DeliveryOrder o LEFT JOIN FETCH o.lines line "
            + "LEFT JOIN FETCH line.barang WHERE (:from IS NULL OR o.createdAt >= :from) "
            + "AND (:to IS NULL OR o.createdAt <= :to) ORDER BY o.createdAt DESC")
    List<DeliveryOrder> findInRangeWithLines(@Param("from") LocalDateTime from,
                                              @Param("to") LocalDateTime to);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT DISTINCT o FROM DeliveryOrder o LEFT JOIN FETCH o.lines WHERE o.id = :id")
    Optional<DeliveryOrder> findByIdWithLinesForUpdate(@Param("id") Integer id);

    @Query(value = "SELECT new com.mycompany.tugas_akhir.repository.MobileFulfillmentOrderProjection("
            + "o.id, o.orderCode, o.customer.namaCustomer, o.status, o.createdAt) "
            + "FROM DeliveryOrder o WHERE o.status IN :statuses ORDER BY o.createdAt ASC",
            countQuery = "SELECT COUNT(o) FROM DeliveryOrder o WHERE o.status IN :statuses")
    Page<MobileFulfillmentOrderProjection> findMobileFulfillmentOrders(
            @Param("statuses") List<DeliveryOrderStatus> statuses, Pageable pageable);

    @Query("SELECT DISTINCT o FROM DeliveryOrder o LEFT JOIN FETCH o.lines line "
            + "LEFT JOIN FETCH line.barang JOIN FETCH o.customer WHERE o.id = :id")
    Optional<DeliveryOrder> findByIdWithLines(@Param("id") Integer id);

    @Query("SELECT o.status FROM DeliveryOrder o WHERE o.id = :id")
    Optional<DeliveryOrderStatus> findStatusById(@Param("id") Integer id);
}
