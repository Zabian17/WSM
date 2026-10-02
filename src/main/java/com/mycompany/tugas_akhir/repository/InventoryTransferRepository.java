package com.mycompany.tugas_akhir.repository;

import com.mycompany.tugas_akhir.model.InventoryTransfer;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface InventoryTransferRepository extends JpaRepository<InventoryTransfer, Integer> {

    List<InventoryTransfer> findTop200ByOrderByCreatedAtDesc();
}