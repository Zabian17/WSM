package com.mycompany.tugas_akhir.repository;

import com.mycompany.tugas_akhir.model.InventoryBalance;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.List;

@Repository
public interface InventoryBalanceRepository extends JpaRepository<InventoryBalance, Integer> {

        @Query("SELECT CASE WHEN COUNT(b) > 0 THEN true ELSE false END FROM InventoryBalance b WHERE b.barang.id = :barangId")
        boolean existsByBarangId(@Param("barangId") Integer barangId);

        @Query("SELECT b FROM InventoryBalance b JOIN FETCH b.barang LEFT JOIN FETCH b.rak "
                        + "ORDER BY b.locationKey, b.barang.namaBarang")
        List<InventoryBalance> findAllForLocationReport();

            @Lock(LockModeType.PESSIMISTIC_WRITE)
            @Query("SELECT b FROM InventoryBalance b LEFT JOIN FETCH b.rak "
                    + "WHERE b.barang.id = :barangId AND b.quantity > 0 ORDER BY b.locationKey")
            List<InventoryBalance> findPositiveByBarangIdForUpdate(@Param("barangId") Integer barangId);

    @Query("SELECT b FROM InventoryBalance b WHERE b.barang.id = :barangId AND b.locationKey = :locationKey")
    Optional<InventoryBalance> findByBarangIdAndLocationKey(
            @Param("barangId") Integer barangId, @Param("locationKey") String locationKey);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT b FROM InventoryBalance b WHERE b.barang.id = :barangId AND b.locationKey = :locationKey")
    Optional<InventoryBalance> findByBarangIdAndLocationKeyForUpdate(
            @Param("barangId") Integer barangId, @Param("locationKey") String locationKey);
}