package com.mycompany.tugas_akhir.repository;

import com.mycompany.tugas_akhir.model.Transaksi;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface TransaksiRepository extends JpaRepository<Transaksi, Integer> {

    List<Transaksi> findTop200ByOrderByTanggalDesc();

    @Query("SELECT t.tipe, COALESCE(SUM(t.jumlah), 0) FROM Transaksi t WHERE t.tanggal >= :since GROUP BY t.tipe")
    List<Object[]> getStatistikSince(@Param("since") LocalDateTime since);

    @Query("SELECT t.rak.id AS rackId, COUNT(t.id) AS mutationCount FROM Transaksi t "
            + "WHERE t.rak IS NOT NULL AND t.tanggal >= :since GROUP BY t.rak.id")
    List<RackMutationCount> countMutationsByRackSince(@Param("since") LocalDateTime since);

}
