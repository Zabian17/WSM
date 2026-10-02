package com.mycompany.tugas_akhir.service;

import com.mycompany.tugas_akhir.model.Barang;
import com.mycompany.tugas_akhir.model.InventoryBalance;
import com.mycompany.tugas_akhir.model.InventoryLotBalance;
import com.mycompany.tugas_akhir.model.InventoryTransfer;
import com.mycompany.tugas_akhir.model.InventoryTransferLotAllocation;
import com.mycompany.tugas_akhir.model.Rak;
import com.mycompany.tugas_akhir.model.User;
import com.mycompany.tugas_akhir.repository.BarangRepository;
import com.mycompany.tugas_akhir.repository.InventoryBalanceRepository;
import com.mycompany.tugas_akhir.repository.InventoryLotBalanceRepository;
import com.mycompany.tugas_akhir.repository.InventoryTransferRepository;
import com.mycompany.tugas_akhir.repository.InventoryTransferLotAllocationRepository;
import com.mycompany.tugas_akhir.repository.RakRepository;
import com.mycompany.tugas_akhir.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.ArgumentCaptor;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class InventoryTransferServiceTest {

    @Mock private BarangRepository barangRepository;
    @Mock private InventoryBalanceRepository balanceRepository;
    @Mock private InventoryTransferRepository transferRepository;
    @Mock private RakRepository rakRepository;
    @Mock private UserRepository userRepository;
    @Mock private InventoryLotBalanceRepository lotBalanceRepository;
    @Mock private InventoryTransferLotAllocationRepository lotAllocationRepository;

    private InventoryTransferService service;

    @BeforeEach
    void setUp() {
        service = new InventoryTransferService(barangRepository, balanceRepository,
            transferRepository, rakRepository, userRepository, lotBalanceRepository, lotAllocationRepository);
    }

    @Test
    void rejectsInvalidTransferWithoutDatabaseAccess() {
        assertFalse(service.transferStock(1, 2, 2, 1, null, 4));
        assertFalse(service.transferStock(1, 2, 3, 0, null, 4));
        verifyNoInteractions(barangRepository, balanceRepository, transferRepository, rakRepository, userRepository,
            lotBalanceRepository, lotAllocationRepository);
    }

    @Test
    void movesQuantityBetweenLocationsWithoutChangingTotalStock() {
        Barang barang = Barang.builder().id(1).namaBarang("Box").stok(20).build();
        Rak source = new Rak(2, null, "A-01", 100, 8);
        Rak destination = new Rak(3, null, "A-02", 100, 2);
        User user = User.builder().id(4).fullName("Operator").build();
        InventoryBalance sourceBalance = new InventoryBalance(barang, source, "RAK-2", 8);
        InventoryBalance destinationBalance = new InventoryBalance(barang, destination, "RAK-3", 2);
        InventoryLotBalance sourceLot = new InventoryLotBalance(barang, source, "RAK-2", "LOT-A",
            java.time.LocalDate.now().plusDays(40), java.time.LocalDateTime.now().minusDays(4), 8);

        when(barangRepository.findByIdForUpdate(1)).thenReturn(Optional.of(barang));
        when(rakRepository.findByIdForUpdate(2)).thenReturn(Optional.of(source));
        when(rakRepository.findByIdForUpdate(3)).thenReturn(Optional.of(destination));
        when(balanceRepository.findByBarangIdAndLocationKeyForUpdate(1, "RAK-2"))
                .thenReturn(Optional.of(sourceBalance));
        when(balanceRepository.findByBarangIdAndLocationKeyForUpdate(1, "RAK-3"))
                .thenReturn(Optional.of(destinationBalance));
        when(lotBalanceRepository.findAllPositiveForUpdate(1, "RAK-2")).thenReturn(java.util.List.of(sourceLot));
        when(lotBalanceRepository.findByBarangIdAndLocationKeyAndBatchCodeForUpdate(1, "RAK-3", "LOT-A"))
            .thenReturn(Optional.empty());
        when(userRepository.findById(4)).thenReturn(Optional.of(user));

        assertTrue(service.transferStock(1, 2, 3, 5, "re-slotting", 4));

        assertEquals(3, sourceBalance.getQuantity());
        assertEquals(7, destinationBalance.getQuantity());
        assertEquals(3, source.getTerisi());
        assertEquals(7, destination.getTerisi());
        assertEquals(3, sourceLot.getQuantity());
        assertEquals(20, barang.getStok());
        verify(balanceRepository).save(sourceBalance);
        verify(balanceRepository).save(destinationBalance);
        verify(transferRepository).save(any(InventoryTransfer.class));
        ArgumentCaptor<InventoryLotBalance> lotCaptor = ArgumentCaptor.forClass(InventoryLotBalance.class);
        verify(lotBalanceRepository, times(2)).save(lotCaptor.capture());
        InventoryLotBalance movedLot = lotCaptor.getAllValues().stream()
            .filter(lot -> "RAK-3".equals(lot.getLocationKey()))
            .findFirst().orElseThrow();
        assertEquals("LOT-A", movedLot.getBatchCode());
        assertEquals(sourceLot.getExpiryDate(), movedLot.getExpiryDate());
        assertEquals(5, movedLot.getQuantity());
        verify(lotAllocationRepository).save(any(InventoryTransferLotAllocation.class));
        verify(barangRepository, never()).save(any());
    }

    @Test
    void rejectsTransferWhenSourceLocationDoesNotHaveEnoughStock() {
        Barang barang = Barang.builder().id(1).namaBarang("Box").stok(20).build();
        Rak source = new Rak(2, null, "A-01", 100, 5);
        Rak destination = new Rak(3, null, "A-02", 100, 0);
        InventoryBalance sourceBalance = new InventoryBalance(barang, source, "RAK-2", 2);
        when(barangRepository.findByIdForUpdate(1)).thenReturn(Optional.of(barang));
        when(rakRepository.findByIdForUpdate(2)).thenReturn(Optional.of(source));
        when(rakRepository.findByIdForUpdate(3)).thenReturn(Optional.of(destination));
        when(balanceRepository.findByBarangIdAndLocationKeyForUpdate(1, "RAK-2"))
                .thenReturn(Optional.of(sourceBalance));

        assertFalse(service.transferStock(1, 2, 3, 3, null, 4));

        assertEquals(2, sourceBalance.getQuantity());
        verify(transferRepository, never()).save(any());
        verify(userRepository, never()).findById(any());
    }

    @Test
    void rejectsTransferWhenDestinationRackHasInsufficientCapacity() {
        Barang barang = Barang.builder().id(1).namaBarang("Box").stok(20).build();
        Rak source = new Rak(2, null, "A-01", 100, 5);
        Rak destination = new Rak(3, null, "A-02", 5, 4);
        when(barangRepository.findByIdForUpdate(1)).thenReturn(Optional.of(barang));
        when(rakRepository.findByIdForUpdate(2)).thenReturn(Optional.of(source));
        when(rakRepository.findByIdForUpdate(3)).thenReturn(Optional.of(destination));

        assertFalse(service.transferStock(1, 2, 3, 2, null, 4));

        assertEquals(5, source.getTerisi());
        assertEquals(4, destination.getTerisi());
        verifyNoInteractions(balanceRepository, transferRepository, userRepository,
            lotBalanceRepository, lotAllocationRepository);
    }
}