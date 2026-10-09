package com.mycompany.tugas_akhir.repository;

import com.mycompany.tugas_akhir.model.Warehouse;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface WarehouseRepository extends JpaRepository<Warehouse, Integer> {
    List<Warehouse> findByActiveTrueOrderByWarehouseCodeAsc();
    Optional<Warehouse> findFirstByOrderByIdAsc();
    Optional<Warehouse> findByWarehouseCodeIgnoreCase(String warehouseCode);
}
