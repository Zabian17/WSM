package com.mycompany.tugas_akhir.service;

import com.mycompany.tugas_akhir.repository.InventoryLotBalanceRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class InventoryPickingPolicyTest {

    @Mock
    private InventoryLotBalanceRepository lotBalanceRepository;

    @Test
    void appliesConfiguredFifoAcrossLocations() {
        InventoryPickingPolicy policy = new InventoryPickingPolicy(lotBalanceRepository, "fifo");
        LocalDate today = LocalDate.now();
        when(lotBalanceRepository.findAvailableFifoAcrossLocationsForUpdate(4, today)).thenReturn(List.of());

        policy.availableAcrossLocations(4, today);

        verify(lotBalanceRepository).findAvailableFifoAcrossLocationsForUpdate(4, today);
        verifyNoMoreInteractions(lotBalanceRepository);
    }

    @Test
    void rejectsUnsupportedPickingStrategies() {
        assertThrows(IllegalArgumentException.class,
                () -> new InventoryPickingPolicy(lotBalanceRepository, "LIFO"));
        verifyNoInteractions(lotBalanceRepository);
    }
}
