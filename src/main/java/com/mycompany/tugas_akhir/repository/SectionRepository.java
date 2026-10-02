package com.mycompany.tugas_akhir.repository;

import com.mycompany.tugas_akhir.model.Section;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface SectionRepository extends JpaRepository<Section, Integer> {

    List<Section> findAllByOrderByIdAsc();

    Optional<Section> findByKodeSection(String kodeSection);
}
