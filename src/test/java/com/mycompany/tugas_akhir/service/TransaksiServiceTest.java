package com.mycompany.tugas_akhir.service;

import com.mycompany.tugas_akhir.model.Barang;
import com.mycompany.tugas_akhir.model.InventoryBalance;
import com.mycompany.tugas_akhir.model.InventoryLotBalance;
import com.mycompany.tugas_akhir.model.Rak;
import com.mycompany.tugas_akhir.model.User;
import com.mycompany.tugas_akhir.repository.BarangRepository;
import com.mycompany.tugas_akhir.repository.InventoryBalanceRepository;
import com.mycompany.tugas_akhir.repository.InventoryLotBalanceRepository;
import com.mycompany.tugas_akhir.repository.RakRepository;
import com.mycompany.tugas_akhir.repository.SupplierRepository;
import com.mycompany.tugas_akhir.repository.TransaksiRepository;
import com.mycompany.tugas_akhir.repository.TransactionLotAllocationRepository;
import com.mycompany.tugas_akhir.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TransaksiServiceTest {

    @Mock private TransaksiRepository transaksiRepository;
    @Mock private BarangRepository barangRepository;
    @Mock private SupplierRepository supplierRepository;
    @Mock private UserRepository userRepository;
    @Mock private InventoryBalanceRepository balanceRepository;
    @Mock private RakRepository rakRepository;
    @Mock private InventoryLotBalanceRepository lotBalanceRepository;
    @Mock private TransactionLotAllocationRepository lotAllocationRepository;

    private TransaksiService service;

    @BeforeEach
    void setUp() {
        service = new TransaksiService(transaksiRepository, barangRepository, supplierRepository,
            userRepository, balanceRepository, rakRepository, lotBalanceRepository, lotAllocationRepository,
            new InventoryPickingPolicy(lotBalanceRepository, "FEFO"));
    }

    @Test
    void rejectsInvalidTypeAndNonPositiveQuantityBeforeDatabaseAccess() {
        assertFalse(service.addTransaksi("adjustment", 1, null, null, null, null, 3, null, 1));
        assertFalse(service.addTransaksi("masuk", 1, null, null, null, null, 0, null, 1));
        assertFalse(service.addTransaksi("keluar", 1, null, null, null, null, -2, null, 1));

        verifyNoInteractions(transaksiRepository, barangRepository, supplierRepository, userRepository,
            balanceRepository, rakRepository, lotBalanceRepository, lotAllocationRepository);
    }

    @Test
    void recordsInboundMovementAgainstTheAuthenticatedUser() {
        Barang barang = Barang.builder().id(5).namaBarang("Box").stok(10).build();
        User user = User.builder().id(9).fullName("Operator").build();
        Rak rak = new Rak(2, null, "A-01", 100, 0);
        when(barangRepository.findByIdForUpdate(5)).thenReturn(Optional.of(barang));
        when(rakRepository.findByIdForUpdate(2)).thenReturn(Optional.of(rak));
        when(balanceRepository.findByBarangIdAndLocationKeyForUpdate(5, "RAK-2")).thenReturn(Optional.empty());
        when(lotBalanceRepository.findByBarangIdAndLocationKeyAndBatchCodeForUpdate(5, "RAK-2", "RESTOCK"))
            .thenReturn(Optional.empty());
        when(userRepository.findById(9)).thenReturn(Optional.of(user));

        assertTrue(service.addTransaksi("masuk", 5, null, 2, "restock", null, 3, "restock", 9));

        assertEquals(13, barang.getStok());
        verify(transaksiRepository).save(any());
        verify(barangRepository).save(barang);
        verify(balanceRepository).save(any(InventoryBalance.class));
        verify(lotBalanceRepository).save(any(InventoryLotBalance.class));
        verify(lotAllocationRepository).save(any());
        verify(userRepository).findById(9);
        verify(userRepository, never()).findAll();
    }

    @Test
    void outboundCannotExceedTheSelectedLocationBalance() {
        Barang barang = Barang.builder().id(5).namaBarang("Box").stok(10).build();
        Rak rak = new Rak(2, null, "A-01", 100, 5);
        InventoryBalance balance = new InventoryBalance(barang, rak, "RAK-2", 2);
        when(barangRepository.findByIdForUpdate(5)).thenReturn(Optional.of(barang));
        when(rakRepository.findByIdForUpdate(2)).thenReturn(Optional.of(rak));
        when(balanceRepository.findByBarangIdAndLocationKeyForUpdate(5, "RAK-2"))
                .thenReturn(Optional.of(balance));
        assertFalse(service.addTransaksi("keluar", 5, null, 2, null, null, 3, null, 9));

        assertEquals(10, barang.getStok());
        assertEquals(2, balance.getQuantity());
        verify(transaksiRepository, never()).save(any());
        verify(userRepository, never()).findById(any());
    }

    @Test
    void inboundRejectsAFullRackWithoutChangingAnyBalance() {
        Barang barang = Barang.builder().id(5).namaBarang("Box").stok(10).build();
        Rak rak = new Rak(2, null, "A-01", 10, 9);
        when(barangRepository.findByIdForUpdate(5)).thenReturn(Optional.of(barang));
        when(rakRepository.findByIdForUpdate(2)).thenReturn(Optional.of(rak));

        assertFalse(service.addTransaksi("masuk", 5, null, 2, "new-batch", null, 2, null, 9));

        assertEquals(10, barang.getStok());
        assertEquals(9, rak.getTerisi());
        verifyNoInteractions(balanceRepository, transaksiRepository, userRepository);
    }

    @Test
    void outboundDecrementsSelectedLocationAndGlobalStockTogether() {
        Barang barang = Barang.builder().id(5).namaBarang("Box").stok(10).build();
        User user = User.builder().id(9).fullName("Operator").build();
        Rak rak = new Rak(2, null, "A-01", 100, 4);
        InventoryBalance balance = new InventoryBalance(barang, rak, "RAK-2", 4);
        when(barangRepository.findByIdForUpdate(5)).thenReturn(Optional.of(barang));
        when(rakRepository.findByIdForUpdate(2)).thenReturn(Optional.of(rak));
        InventoryLotBalance earliest = new InventoryLotBalance(barang, rak, "RAK-2", "LOT-A",
            java.time.LocalDate.now().plusDays(10), java.time.LocalDateTime.now().minusDays(5), 2);
        InventoryLotBalance later = new InventoryLotBalance(barang, rak, "RAK-2", "LOT-B",
            java.time.LocalDate.now().plusDays(20), java.time.LocalDateTime.now().minusDays(2), 4);
        when(balanceRepository.findByBarangIdAndLocationKeyForUpdate(5, "RAK-2"))
                .thenReturn(Optional.of(balance));
        when(lotBalanceRepository.findAvailableFefoForUpdate(eq(5), eq("RAK-2"), any()))
            .thenReturn(java.util.List.of(earliest, later));
        when(userRepository.findById(9)).thenReturn(Optional.of(user));

        assertTrue(service.addTransaksi("keluar", 5, null, 2, null, null, 3, "order", 9));

        assertEquals(7, barang.getStok());
        assertEquals(1, balance.getQuantity());
        assertEquals(0, earliest.getQuantity());
        assertEquals(3, later.getQuantity());
        verify(balanceRepository).save(balance);
        verify(barangRepository).save(barang);
        verify(lotAllocationRepository, times(2)).save(any());
    }

        @Test
        void lefoConsumesTheLatestExpiryLotFirst() {
            service = new TransaksiService(transaksiRepository, barangRepository, supplierRepository,
                userRepository, balanceRepository, rakRepository, lotBalanceRepository, lotAllocationRepository,
                new InventoryPickingPolicy(lotBalanceRepository, "LEFO"));
        Barang barang = Barang.builder().id(5).namaBarang("Box").stok(6).build();
        User user = User.builder().id(9).fullName("Operator").build();
        Rak rak = new Rak(2, null, "A-01", 100, 6);
        InventoryBalance balance = new InventoryBalance(barang, rak, "RAK-2", 6);
        InventoryLotBalance earlierExpiry = new InventoryLotBalance(barang, rak, "RAK-2", "LOT-A",
            java.time.LocalDate.now().plusDays(10), java.time.LocalDateTime.now().minusDays(4), 3);
        InventoryLotBalance laterExpiry = new InventoryLotBalance(barang, rak, "RAK-2", "LOT-B",
            java.time.LocalDate.now().plusDays(30), java.time.LocalDateTime.now().minusDays(2), 3);
        when(barangRepository.findByIdForUpdate(5)).thenReturn(Optional.of(barang));
        when(rakRepository.findByIdForUpdate(2)).thenReturn(Optional.of(rak));
        when(balanceRepository.findByBarangIdAndLocationKeyForUpdate(5, "RAK-2"))
            .thenReturn(Optional.of(balance));
        when(lotBalanceRepository.findAvailableLefoForUpdate(eq(5), eq("RAK-2"), any()))
            .thenReturn(java.util.List.of(laterExpiry, earlierExpiry));
        when(userRepository.findById(9)).thenReturn(Optional.of(user));

        assertTrue(service.addTransaksi("keluar", 5, null, 2, null, null, 2, "lefo", 9));

        assertEquals(3, earlierExpiry.getQuantity());
        assertEquals(1, laterExpiry.getQuantity());
        verify(lotAllocationRepository).save(argThat(allocation ->
            ((com.mycompany.tugas_akhir.model.TransactionLotAllocation) allocation)
                .getLotBalance().getBatchCode().equals("LOT-B")));
        }

    @Test
    void refusesMovementWhenUserDoesNotExist() {
        Barang barang = Barang.builder().id(5).namaBarang("Box").stok(10).build();
        when(barangRepository.findByIdForUpdate(5)).thenReturn(Optional.of(barang));
        when(userRepository.findById(eq(99))).thenReturn(Optional.empty());

        when(balanceRepository.findByBarangIdAndLocationKeyForUpdate(5, InventoryBalance.UNASSIGNED_LOCATION))
            .thenReturn(Optional.of(new InventoryBalance(barang, null, InventoryBalance.UNASSIGNED_LOCATION, 10)));
        InventoryLotBalance opening = new InventoryLotBalance(barang, null, InventoryBalance.UNASSIGNED_LOCATION,
            InventoryLotBalance.OPENING_BATCH_CODE, null, java.time.LocalDateTime.now(), 10);
        when(lotBalanceRepository.findAvailableFefoForUpdate(eq(5), eq(InventoryBalance.UNASSIGNED_LOCATION), any()))
            .thenReturn(java.util.List.of(opening));
        assertFalse(service.addTransaksi("keluar", 5, null, null, null, null, 3, null, 99));

        assertEquals(10, barang.getStok());
        verify(transaksiRepository, never()).save(any());
        verify(barangRepository, never()).save(any());
    }
}