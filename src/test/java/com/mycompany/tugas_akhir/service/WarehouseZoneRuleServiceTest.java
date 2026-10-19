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

        assertTrue(service.addDeliveryOrderExclusion("pick"));

        verify(ruleRepository).save(argThat(rule -> rule.getOperation()
                == WarehouseZoneRule.Operation.DELIVERY_ORDER_EXCLUSION && rule.getZoneCode().equals("PICK")));
        assertThrows(IllegalArgumentException.class, () -> service.addDeliveryOrderExclusion("UNKNOWN"));
        verify(ruleRepository, times(1)).save(any(WarehouseZoneRule.class));
    }

    @Test
    void duplicateRuleDoesNotCreateAnotherRow() {
        Section section = new Section(1, "A", "Section A", 12);
        section.setZoneCode("PICK");
        when(sectionRepository.findAllByOrderByIdAsc()).thenReturn(List.of(section));
        when(ruleRepository.existsByOperationAndZoneCode(
                WarehouseZoneRule.Operation.DELIVERY_ORDER_EXCLUSION, "PICK")).thenReturn(true);

        assertFalse(service.addDeliveryOrderExclusion("PICK"));
        verify(ruleRepository, never()).save(any());
    }

    @Test
    void excludedZonesAreNormalizedAndComparedWithoutCaseSensitivity() {
        WarehouseZoneRule rule = new WarehouseZoneRule(
                WarehouseZoneRule.Operation.DELIVERY_ORDER_EXCLUSION, "PICK");
        when(ruleRepository.findByOperation(WarehouseZoneRule.Operation.DELIVERY_ORDER_EXCLUSION))
                .thenReturn(Set.of(rule));

        Set<String> excluded = service.getDeliveryOrderExcludedZones();

        assertFalse(WarehouseZoneRuleService.isDeliveryOrderZoneAllowed("pick", excluded));
        assertTrue(WarehouseZoneRuleService.isDeliveryOrderZoneAllowed("BULK", excluded));
        assertTrue(WarehouseZoneRuleService.isDeliveryOrderZoneAllowed(null, excluded));
    }
}
