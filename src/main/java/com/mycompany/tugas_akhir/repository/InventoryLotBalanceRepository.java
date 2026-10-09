package com.mycompany.tugas_akhir.repository;

import com.mycompany.tugas_akhir.model.InventoryLotBalance;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface InventoryLotBalanceRepository extends JpaRepository<InventoryLotBalance, Integer> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT l FROM InventoryLotBalance l WHERE l.id = :id")
    Optional<InventoryLotBalance> findByIdForUpdate(@Param("id") Integer id);

    @Query("SELECT CASE WHEN COUNT(l) > 0 THEN true ELSE false END FROM InventoryLotBalance l "
            + "WHERE l.barang.id = :barangId AND l.locationKey = :locationKey")
    boolean existsByBarangIdAndLocationKey(
            @Param("barangId") Integer barangId, @Param("locationKey") String locationKey);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT l FROM InventoryLotBalance l WHERE l.barang.id = :barangId "
            + "AND l.locationKey = :locationKey AND l.batchCode = :batchCode")
    Optional<InventoryLotBalance> findByBarangIdAndLocationKeyAndBatchCodeForUpdate(
            @Param("barangId") Integer barangId,
            @Param("locationKey") String locationKey,
            @Param("batchCode") String batchCode);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT l FROM InventoryLotBalance l WHERE l.barang.id = :barangId "
            + "AND l.locationKey = :locationKey AND l.quantity > l.reservedQuantity "
            + "AND (l.expiryDate IS NULL OR l.expiryDate >= :today) "
            + "ORDER BY CASE WHEN l.expiryDate IS NULL THEN 1 ELSE 0 END, l.expiryDate, l.receivedAt, l.id")
    List<InventoryLotBalance> findAvailableFefoForUpdate(
            @Param("barangId") Integer barangId,
            @Param("locationKey") String locationKey,
            @Param("today") LocalDate today);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT l FROM InventoryLotBalance l WHERE l.barang.id = :barangId "
            + "AND l.locationKey = :locationKey AND l.quantity > l.reservedQuantity "
            + "AND (l.expiryDate IS NULL OR l.expiryDate >= :today) ORDER BY l.receivedAt, l.id")
    List<InventoryLotBalance> findAvailableFifoForUpdate(
            @Param("barangId") Integer barangId,
            @Param("locationKey") String locationKey,
            @Param("today") LocalDate today);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT l FROM InventoryLotBalance l WHERE l.barang.id = :barangId "
            + "AND l.locationKey = :locationKey AND l.quantity > l.reservedQuantity "
            + "AND (l.expiryDate IS NULL OR l.expiryDate >= :today) "
            + "ORDER BY CASE WHEN l.expiryDate IS NULL THEN 1 ELSE 0 END, l.expiryDate DESC, l.receivedAt, l.id")
    List<InventoryLotBalance> findAvailableLefoForUpdate(
            @Param("barangId") Integer barangId,
            @Param("locationKey") String locationKey,
            @Param("today") LocalDate today);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT l FROM InventoryLotBalance l WHERE l.barang.id = :barangId "
            + "AND l.locationKey = :locationKey AND l.quantity > l.reservedQuantity ORDER BY l.receivedAt, l.id")
    List<InventoryLotBalance> findAllPositiveForUpdate(
            @Param("barangId") Integer barangId, @Param("locationKey") String locationKey);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT l FROM InventoryLotBalance l WHERE l.barang.id = :barangId "
            + "AND l.quantity > l.reservedQuantity AND (l.expiryDate IS NULL OR l.expiryDate >= :today) "
            + "ORDER BY CASE WHEN l.expiryDate IS NULL THEN 1 ELSE 0 END, l.expiryDate, l.receivedAt, l.id")
    List<InventoryLotBalance> findAvailableFefoAcrossLocationsForUpdate(
            @Param("barangId") Integer barangId, @Param("today") LocalDate today);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT l FROM InventoryLotBalance l WHERE l.barang.id = :barangId "
            + "AND l.quantity > l.reservedQuantity AND (l.expiryDate IS NULL OR l.expiryDate >= :today) "
            + "ORDER BY l.receivedAt, l.id")
    List<InventoryLotBalance> findAvailableFifoAcrossLocationsForUpdate(
            @Param("barangId") Integer barangId, @Param("today") LocalDate today);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT l FROM InventoryLotBalance l WHERE l.barang.id = :barangId "
            + "AND l.quantity > l.reservedQuantity AND (l.expiryDate IS NULL OR l.expiryDate >= :today) "
            + "ORDER BY CASE WHEN l.expiryDate IS NULL THEN 1 ELSE 0 END, l.expiryDate DESC, l.receivedAt, l.id")
    List<InventoryLotBalance> findAvailableLefoAcrossLocationsForUpdate(
            @Param("barangId") Integer barangId, @Param("today") LocalDate today);
}