package com.mycompany.tugas_akhir.repository;

import com.mycompany.tugas_akhir.model.StockCountLine;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface StockCountLineRepository extends JpaRepository<StockCountLine, Integer> {
    @Query(value = "SELECT new com.mycompany.tugas_akhir.repository.StockCountLineProjection("
            + "l.id, l.barang.kodeBarang, l.barang.namaBarang, l.barang.satuan, "
            + "CASE WHEN l.assignedUser IS NULL THEN null ELSE l.assignedUser.fullName END) "
            + "FROM StockCountLine l WHERE l.stockCount.id = :countId AND l.countedQuantity IS NULL "
            + "AND (l.assignedUser IS NULL OR l.assignedUser.id = :userId) "
            + "ORDER BY l.barang.kodeBarang",
            countQuery = "SELECT COUNT(l) FROM StockCountLine l WHERE l.stockCount.id = :countId "
                    + "AND l.countedQuantity IS NULL AND (l.assignedUser IS NULL OR l.assignedUser.id = :userId)")
    Page<StockCountLineProjection> findOpenLinesForUser(@Param("countId") Integer countId,
                                                       @Param("userId") Integer userId, Pageable pageable);

    @Query("SELECT l.barang.kodeBarang FROM StockCountLine l WHERE l.id = :lineId "
            + "AND l.stockCount.id = :countId AND l.countedQuantity IS NULL "
            + "AND (l.assignedUser IS NULL OR l.assignedUser.id = :userId)")
    Optional<String> findOpenSkuForUser(@Param("countId") Integer countId,
                                        @Param("lineId") Integer lineId,
                                        @Param("userId") Integer userId);
}
