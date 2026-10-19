package com.mycompany.tugas_akhir.service;

import com.mycompany.tugas_akhir.model.Section;
import com.mycompany.tugas_akhir.model.WarehouseZoneRule;
import com.mycompany.tugas_akhir.repository.SectionRepository;
import com.mycompany.tugas_akhir.repository.WarehouseZoneRuleRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class WarehouseZoneRuleServiceTest {
    @Mock private WarehouseZoneRuleRepository ruleRepository;
    @Mock private SectionRepository sectionRepository;

    private WarehouseZoneRuleService service;

    @BeforeEach
    void setUp() {
        service = new WarehouseZoneRuleService(ruleRepository, sectionRepository);
    }

    @Test
    void addsOnlyZonesDefinedByWarehouseSections() {
        Section section = new Section(1, "A", "Section A", 12);
        section.setZoneCode("PICK");
        when(sectionRepository.findAllByOrderByIdAsc()).thenReturn(List.of(section));
        when(ruleRepository.existsByOperationAndZoneCode(
                WarehouseZoneRule.Operation.DELIVERY_ORDER_EXCLUSION, "PICK")).thenReturn(false);

        assertTrue(service.addExclusion(WarehouseZoneRule.Operation.DELIVERY_ORDER_EXCLUSION, "pick"));

        verify(ruleRepository).save(argThat(rule -> rule.getOperation()
                == WarehouseZoneRule.Operation.DELIVERY_ORDER_EXCLUSION && rule.getZoneCode().equals("PICK")));
        assertThrows(IllegalArgumentException.class, () -> service.addExclusion(
                WarehouseZoneRule.Operation.DELIVERY_ORDER_EXCLUSION, "UNKNOWN"));
        verify(ruleRepository, times(1)).save(any(WarehouseZoneRule.class));
    }

    @Test
    void duplicateRuleDoesNotCreateAnotherRow() {
        Section section = new Section(1, "A", "Section A", 12);
        section.setZoneCode("PICK");
        when(sectionRepository.findAllByOrderByIdAsc()).thenReturn(List.of(section));
        when(ruleRepository.existsByOperationAndZoneCode(
                WarehouseZoneRule.Operation.DELIVERY_ORDER_EXCLUSION, "PICK")).thenReturn(true);

        assertFalse(service.addExclusion(WarehouseZoneRule.Operation.DELIVERY_ORDER_EXCLUSION, "PICK"));
        verify(ruleRepository, never()).save(any());
    }

    @Test
    void storesSLocAndBarcodeTransferExclusionsIndependently() {
        Section section = new Section(1, "A", "Section A", 12);
        section.setZoneCode("PICK");
        when(sectionRepository.findAllByOrderByIdAsc()).thenReturn(List.of(section));
        when(ruleRepository.existsByOperationAndZoneCode(
                WarehouseZoneRule.Operation.SLOC_TRANSFER_EXCLUSION, "PICK")).thenReturn(false);
        when(ruleRepository.existsByOperationAndZoneCode(
                WarehouseZoneRule.Operation.BARCODE_TRANSFER_EXCLUSION, "PICK")).thenReturn(false);

        assertTrue(service.addExclusion(WarehouseZoneRule.Operation.SLOC_TRANSFER_EXCLUSION, "PICK"));
        assertTrue(service.addExclusion(WarehouseZoneRule.Operation.BARCODE_TRANSFER_EXCLUSION, "PICK"));

        verify(ruleRepository).save(argThat(rule -> rule.getOperation()
                == WarehouseZoneRule.Operation.SLOC_TRANSFER_EXCLUSION));
        verify(ruleRepository).save(argThat(rule -> rule.getOperation()
                == WarehouseZoneRule.Operation.BARCODE_TRANSFER_EXCLUSION));
    }

    @Test
    void excludedZonesAreNormalizedAndComparedWithoutCaseSensitivity() {
        WarehouseZoneRule rule = new WarehouseZoneRule(
                WarehouseZoneRule.Operation.DELIVERY_ORDER_EXCLUSION, "PICK");
        when(ruleRepository.findByOperation(WarehouseZoneRule.Operation.DELIVERY_ORDER_EXCLUSION))
                .thenReturn(Set.of(rule));

        Set<String> excluded = service.getExcludedZones(WarehouseZoneRule.Operation.DELIVERY_ORDER_EXCLUSION);

        assertFalse(WarehouseZoneRuleService.isZoneAllowed("pick", excluded));
        assertTrue(WarehouseZoneRuleService.isZoneAllowed("BULK", excluded));
        assertTrue(WarehouseZoneRuleService.isZoneAllowed(null, excluded));
    }

    @Test
    void picklistZonesAllowAllWhenUnconfiguredAndRestrictWhenConfigured() {
        WarehouseZoneRule rule = new WarehouseZoneRule(WarehouseZoneRule.Operation.STO_PICKLIST_ZONE, "PICK");
        when(ruleRepository.findByOperation(WarehouseZoneRule.Operation.STO_PICKLIST_ZONE)).thenReturn(Set.of(rule));

        Set<String> allowed = service.getAllowedZones(WarehouseZoneRule.Operation.STO_PICKLIST_ZONE);

        assertTrue(WarehouseZoneRuleService.isPicklistZoneAllowed("pick", allowed));
        assertFalse(WarehouseZoneRuleService.isPicklistZoneAllowed("BULK", allowed));
        assertFalse(WarehouseZoneRuleService.isPicklistZoneAllowed(null, allowed));
        assertTrue(WarehouseZoneRuleService.isPicklistZoneAllowed("BULK", Set.of()));
    }
}
