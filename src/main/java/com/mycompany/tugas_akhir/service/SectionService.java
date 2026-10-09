package com.mycompany.tugas_akhir.service;

import com.mycompany.tugas_akhir.model.Rak;
import com.mycompany.tugas_akhir.model.Section;
import com.mycompany.tugas_akhir.model.Warehouse;
import com.mycompany.tugas_akhir.repository.RakRepository;
import com.mycompany.tugas_akhir.repository.SectionRepository;
import com.mycompany.tugas_akhir.repository.WarehouseRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Service
public class SectionService {

    private static final Logger log = LoggerFactory.getLogger(SectionService.class);
    private final SectionRepository sectionRepository;
    private final RakRepository rakRepository;
    private final WarehouseRepository warehouseRepository;

    public SectionService(SectionRepository sectionRepository, RakRepository rakRepository,
                          WarehouseRepository warehouseRepository) {
        this.sectionRepository = sectionRepository;
        this.rakRepository = rakRepository;
        this.warehouseRepository = warehouseRepository;
    }

    @Transactional
    public List<Section> getAllSections() {
        List<Section> sections = sectionRepository.findAllByOrderByIdAsc();
        if (sections.isEmpty()) {
            sections = createDefaultSections();
        }
        return sections;
    }

    @Transactional(readOnly = true)
    public List<Rak> getAllRaks() {
        return rakRepository.findAll();
    }

    @Transactional
    public List<Section> createDefaultSections() {
        List<Section> defaults = new ArrayList<>();
        Warehouse warehouse = getOrCreateDefaultWarehouse();

        Section secA = Section.builder().kodeSection("A").namaSection("Section A - Umum").kapasitas(12).alertThreshold(80).alertType("Kapasitas Penuh (>= %)").build();
        Section secB = Section.builder().kodeSection("B").namaSection("Section B - Fast Moving").kapasitas(12).alertThreshold(90).alertType("Kapasitas Penuh (>= %)").build();
        Section secC = Section.builder().kodeSection("C").namaSection("Section C - Overflow").kapasitas(12).alertThreshold(75).alertType("Kapasitas Penuh (>= %)").build();
        secA.setWarehouse(warehouse);
        secB.setWarehouse(warehouse);
        secC.setWarehouse(warehouse);
        secA.setWarehouse(warehouse);
        secB.setWarehouse(warehouse);
        secC.setWarehouse(warehouse);

        secA = sectionRepository.save(secA);
        secB = sectionRepository.save(secB);
        secC = sectionRepository.save(secC);

        createRaksForSection(secA, 12, new int[]{95, 40, 20, 10, 0, 0, 0, 0, 0, 0, 0, 0});
        createRaksForSection(secB, 12, new int[]{100, 80, 60, 50, 0, 0, 0, 0, 0, 0, 0, 0});
        createRaksForSection(secC, 12, new int[]{45, 100, 100, 100, 100, 100, 100, 100, 100, 100, 100, 100});

        defaults.add(secA);
        defaults.add(secB);
        defaults.add(secC);
        return defaults;
    }

    private void createRaksForSection(Section section, int count, int[] initialTerisi) {
        for (int i = 1; i <= count; i++) {
            String kode = String.format("%s-%02d", section.getKodeSection(), i);
            int terisi = (initialTerisi != null && i <= initialTerisi.length) ? initialTerisi[i - 1] : 0;
            Rak rak = new Rak(null, section, kode, 100, terisi);
            rakRepository.save(rak);
            section.getRaks().add(rak);
        }
    }

    @Transactional
    public Section addSection(String kodeSection, String namaSection, int kapasitas, String targetKategori, int threshold, String alertType) {
        String kode = kodeSection.trim().toUpperCase();
        if (sectionRepository.findByKodeSection(kode).isPresent()) {
            kode = kode + (sectionRepository.count() + 1);
        }

        Section section = Section.builder()
                .kodeSection(kode)
                .namaSection(namaSection.trim())
                .kapasitas(kapasitas)
                .targetKategori(targetKategori)
                .alertThreshold(threshold)
                .alertType(alertType)
                .alertActive(true)
                .build();
        section.setWarehouse(getOrCreateDefaultWarehouse());

        section = sectionRepository.save(section);
        createRaksForSection(section, kapasitas, null);
        return section;
    }

    @Transactional
    public boolean assignSectionToWarehouse(Integer sectionId, Integer warehouseId) {
        if (sectionId == null || warehouseId == null) return false;
        Section section = sectionRepository.findById(sectionId).orElse(null);
        Warehouse warehouse = warehouseRepository.findById(warehouseId).orElse(null);
        if (section == null || warehouse == null || !Boolean.TRUE.equals(warehouse.getActive())) return false;
        section.setWarehouse(warehouse);
        sectionRepository.save(section);
        return true;
    }

    @Transactional(readOnly = true)
    public List<Warehouse> getActiveWarehouses() {
        return warehouseRepository.findByActiveTrueOrderByWarehouseCodeAsc();
    }

    @Transactional
    public Warehouse createWarehouse(String code, String name, String address) {
        if (code == null || name == null || code.isBlank() || name.isBlank()
                || code.trim().length() > 20 || name.trim().length() > 100
                || address != null && address.trim().length() > 255) return null;
        String normalizedCode = code.trim().toUpperCase(java.util.Locale.ROOT);
        if (warehouseRepository.findByWarehouseCodeIgnoreCase(normalizedCode).isPresent()) return null;
        return warehouseRepository.save(new Warehouse(normalizedCode, name.trim(),
                address == null || address.isBlank() ? null : address.trim()));
    }

    private Warehouse getOrCreateDefaultWarehouse() {
        return warehouseRepository.findFirstByOrderByIdAsc().orElseGet(() ->
                warehouseRepository.save(new Warehouse("WH-MAIN", "Gudang Utama", null)));
    }

    @Transactional
    public boolean deleteSection(Integer sectionId) {
        Optional<Section> secOpt = sectionRepository.findById(sectionId);
        if (secOpt.isPresent()) {
            sectionRepository.delete(secOpt.get());
            log.info("Section id {} berhasil dihapus.", sectionId);
            return true;
        }
        return false;
    }

    @Transactional
    public Section updateSectionConfig(Integer sectionId, String namaSection, int kapasitas, String targetKategori, int threshold, String alertType, boolean alertActive) {
        Optional<Section> secOpt = sectionRepository.findById(sectionId);
        if (secOpt.isPresent()) {
            Section s = secOpt.get();
            s.setNamaSection(namaSection);
            s.setTargetKategori(targetKategori);
            s.setAlertThreshold(threshold);
            s.setAlertType(alertType);
            s.setAlertActive(alertActive);

            int currentSlots = s.getRaks().size();
            if (kapasitas > currentSlots) {
                for (int i = currentSlots + 1; i <= kapasitas; i++) {
                    String kode = String.format("%s-%02d", s.getKodeSection(), i);
                    Rak rak = new Rak(null, s, kode, 100, 0);
                    rakRepository.save(rak);
                    s.getRaks().add(rak);
                }
            }
            s.setKapasitas(kapasitas);
            return sectionRepository.save(s);
        }
        return null;
    }

}
