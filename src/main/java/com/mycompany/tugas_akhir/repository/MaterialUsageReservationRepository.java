package com.mycompany.tugas_akhir.repository;

import com.mycompany.tugas_akhir.model.MaterialUsageReservation;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface MaterialUsageReservationRepository extends JpaRepository<MaterialUsageReservation, Integer> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT r FROM MaterialUsageReservation r JOIN FETCH r.materialUsageLine line "
            + "JOIN FETCH r.lotBalance WHERE line.materialUsage.id = :requestId ORDER BY r.id")
    List<MaterialUsageReservation> findByRequestIdForUpdate(@Param("requestId") Integer requestId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT r FROM MaterialUsageReservation r JOIN FETCH r.lotBalance "
            + "WHERE r.materialUsageLine.id = :lineId ORDER BY r.id")
    List<MaterialUsageReservation> findByLineIdForUpdate(@Param("lineId") Integer lineId);
}
