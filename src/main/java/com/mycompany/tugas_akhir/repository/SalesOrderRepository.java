package com.mycompany.tugas_akhir.repository;

import com.mycompany.tugas_akhir.model.SalesOrder;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface SalesOrderRepository extends JpaRepository<SalesOrder, Integer> {

    List<SalesOrder> findTop100ByOrderByCreatedAtDesc();

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT DISTINCT s FROM SalesOrder s LEFT JOIN FETCH s.lines WHERE s.id = :id")
    Optional<SalesOrder> findByIdWithLinesForUpdate(@Param("id") Integer id);
}
