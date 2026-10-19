package com.mycompany.tugas_akhir.service;

import com.mycompany.tugas_akhir.model.Section;
import com.mycompany.tugas_akhir.model.WarehouseZoneRule;
import com.mycompany.tugas_akhir.repository.SectionRepository;
import com.mycompany.tugas_akhir.repository.WarehouseZoneRuleRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Locale;
import java.util.Set;
import java.util.TreeSet;

@Service
public class WarehouseZoneRuleService {
    private final WarehouseZoneRuleRepository ruleRepository;
    private final SectionRepository sectionRepository;

    public WarehouseZoneRuleService(WarehouseZoneRuleRepository ruleRepository,
                                    SectionRepository sectionRepository) {
        this.ruleRepository = ruleRepository;
        this.sectionRepository = sectionRepository;
    }

    @Transactional(readOnly = true)
    public java.util.List<WarehouseZoneRule> getRules() {
        return ruleRepository.findAllByOrderByOperationAscZoneCodeAsc();
    }

    @Transactional(readOnly = true)
    public java.util.List<String> getZoneCodes() {
        return sectionRepository.findAllByOrderByIdAsc().stream()
                .map(Section::getZoneCode)
                .filter(zone -> zone != null && !zone.isBlank())
                .map(WarehouseZoneRuleService::normalize)
                .distinct()
                .sorted(String.CASE_INSENSITIVE_ORDER)
                .toList();
    }

    @Transactional
    public boolean addExclusion(WarehouseZoneRule.Operation operation, String zoneCode) {
        if (operation == null) throw new IllegalArgumentException("Pilih operasi yang valid.");
        String normalized = validateZone(zoneCode);
        if (!getZoneCodes().contains(normalized)) {
            throw new IllegalArgumentException("Zona tidak ditemukan pada konfigurasi section.");
        }
        if (ruleRepository.existsByOperationAndZoneCode(operation, normalized)) return false;
        ruleRepository.save(new WarehouseZoneRule(operation, normalized));
        return true;
    }

    @Transactional
    public boolean removeRule(Integer ruleId) {
        if (ruleId == null || !ruleRepository.existsById(ruleId)) return false;
        ruleRepository.deleteById(ruleId);
        return true;
    }

    @Transactional(readOnly = true)
    public Set<String> getExcludedZones(WarehouseZoneRule.Operation operation) {
        Set<String> zones = new TreeSet<>(String.CASE_INSENSITIVE_ORDER);
        ruleRepository.findByOperation(operation)
                .forEach(rule -> zones.add(normalize(rule.getZoneCode())));
        return Set.copyOf(zones);
    }

    public static boolean isZoneAllowed(String zoneCode, Set<String> excludedZones) {
        return zoneCode == null || zoneCode.isBlank()
                || !excludedZones.contains(normalize(zoneCode));
    }

    private String validateZone(String zoneCode) {
        if (zoneCode == null || zoneCode.isBlank() || zoneCode.trim().length() > 40) {
            throw new IllegalArgumentException("Pilih zona valid dengan panjang maksimal 40 karakter.");
        }
        return normalize(zoneCode);
    }

    private static String normalize(String zoneCode) {
        return zoneCode.trim().toUpperCase(Locale.ROOT);
    }
}
