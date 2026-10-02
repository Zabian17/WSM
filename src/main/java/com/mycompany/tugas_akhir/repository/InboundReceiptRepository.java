package com.mycompany.tugas_akhir.repository;

import com.mycompany.tugas_akhir.model.InboundReceipt;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface InboundReceiptRepository extends JpaRepository<InboundReceipt, Integer> {

    List<InboundReceipt> findTop100ByOrderByCreatedAtDesc();

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT r FROM InboundReceipt r WHERE r.id = :id")
    Optional<InboundReceipt> findByIdForUpdate(@Param("id") Integer id);
}