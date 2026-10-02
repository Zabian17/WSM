package com.mycompany.tugas_akhir.repository;

import com.mycompany.tugas_akhir.model.Supplier;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface SupplierRepository extends JpaRepository<Supplier, Integer> {

    List<Supplier> findAllByOrderByNamaSupplierAsc();
}
