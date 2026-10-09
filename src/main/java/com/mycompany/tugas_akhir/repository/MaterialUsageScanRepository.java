package com.mycompany.tugas_akhir.repository;

import com.mycompany.tugas_akhir.model.MaterialUsageScan;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface MaterialUsageScanRepository extends JpaRepository<MaterialUsageScan, Integer> {

    @Query("SELECT s FROM MaterialUsageScan s JOIN FETCH s.materialUsageLine line "
            + "JOIN FETCH line.barang JOIN FETCH s.issuer JOIN FETCH s.lotBalance lot "
            + "JOIN FETCH lot.rak WHERE s.materialUsage.id = :requestId ORDER BY s.scannedAt, s.id")
    List<MaterialUsageScan> findByRequestId(@Param("requestId") Integer requestId);
}
