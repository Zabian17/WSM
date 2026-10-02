package com.mycompany.tugas_akhir.repository;

import com.mycompany.tugas_akhir.model.Rak;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface RakRepository extends JpaRepository<Rak, Integer> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT r FROM Rak r WHERE r.id = :id")
    Optional<Rak> findByIdForUpdate(@Param("id") Integer id);

    List<Rak> findBySectionId(Integer sectionId);

    Optional<Rak> findByKodeRak(String kodeRak);
}
