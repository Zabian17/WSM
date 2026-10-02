package com.mycompany.tugas_akhir.repository;

import com.mycompany.tugas_akhir.model.Barang;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import jakarta.persistence.LockModeType;

import java.util.List;

@Repository
public interface BarangRepository extends JpaRepository<Barang, Integer> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT b FROM Barang b WHERE b.id = :id")
    java.util.Optional<Barang> findByIdForUpdate(@Param("id") Integer id);

    @Query("SELECT b FROM Barang b WHERE LOWER(b.namaBarang) LIKE LOWER(CONCAT('%', :kw, '%')) OR LOWER(b.kodeBarang) LIKE LOWER(CONCAT('%', :kw, '%')) OR LOWER(b.kategori) LIKE LOWER(CONCAT('%', :kw, '%'))")
    List<Barang> searchByKeyword(@Param("kw") String keyword);

    @Query("SELECT COALESCE(MAX(CAST(SUBSTRING(b.kodeBarang, 5) AS int)), 0) FROM Barang b WHERE b.kodeBarang LIKE 'BRG-%'")
    int findMaxKodeSequence();
}
