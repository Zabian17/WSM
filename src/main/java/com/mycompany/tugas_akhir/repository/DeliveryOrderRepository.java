package com.mycompany.tugas_akhir.repository;

import com.mycompany.tugas_akhir.model.DeliveryOrder;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface DeliveryOrderRepository extends JpaRepository<DeliveryOrder, Integer> {

    List<DeliveryOrder> findTop100ByOrderByCreatedAtDesc();

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT DISTINCT o FROM DeliveryOrder o LEFT JOIN FETCH o.lines WHERE o.id = :id")
    Optional<DeliveryOrder> findByIdWithLinesForUpdate(@Param("id") Integer id);
}