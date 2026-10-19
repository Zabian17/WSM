package com.mycompany.tugas_akhir.repository;

import com.mycompany.tugas_akhir.model.InventoryTransfer;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;

public interface InventoryTransferRepository extends JpaRepository<InventoryTransfer, Integer> {

    List<InventoryTransfer> findTop200ByOrderByCreatedAtDesc();

    @Query("SELECT t.sourceRak.id AS rackId, COUNT(t.id) AS mutationCount FROM InventoryTransfer t "
            + "WHERE t.sourceRak IS NOT NULL AND t.createdAt >= :since GROUP BY t.sourceRak.id")
    List<RackMutationCount> countMutationsFromRackSince(@Param("since") LocalDateTime since);

    @Query("SELECT t.destinationRak.id AS rackId, COUNT(t.id) AS mutationCount FROM InventoryTransfer t "
            + "WHERE t.createdAt >= :since GROUP BY t.destinationRak.id")
    List<RackMutationCount> countMutationsIntoRackSince(@Param("since") LocalDateTime since);
}
