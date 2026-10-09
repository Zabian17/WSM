package com.mycompany.tugas_akhir.repository;

import com.mycompany.tugas_akhir.model.DeliveryOrderLine;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface DeliveryOrderLineRepository extends JpaRepository<DeliveryOrderLine, Integer> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT line FROM DeliveryOrderLine line WHERE line.id = :id")
    Optional<DeliveryOrderLine> findByIdForUpdate(@Param("id") Integer id);
}
