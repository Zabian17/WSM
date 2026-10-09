package com.mycompany.tugas_akhir.service;

import com.mycompany.tugas_akhir.model.*;
import com.mycompany.tugas_akhir.repository.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class StockCountServiceTest {

    @Mock private StockCountRepository stockCountRepository;
    @Mock private BarangRepository barangRepository;
    @Mock private RakRepository rakRepository;
    @Mock private UserRepository userRepository;
    @Mock private InventoryBalanceRepository balanceRepository;
    @Mock private InventoryLotBalanceRepository lotBalanceRepository;

    private StockCountService service;

    @BeforeEach
    void setUp() {
        service = new StockCountService(stockCountRepository, barangRepository, rakRepository,
                userRepository, balanceRepository, lotBalanceRepository);
    }

    @Test
    void createsSnapshotContainingEverySkuIncludingZeroBalances() {
        Rak rack = rack(4, 100, 30);
        User user = user(7);
        Barang stocked = item(1, 8);
        Barang empty = item(2, 0);
        when(userRepository.findById(7)).thenReturn(Optional.of(user));
        when(rakRepository.findByIdForUpdate(4)).thenReturn(Optional.of(rack));
        when(stockCountRepository.existsByRakIdAndStatusIn(eq(4), anyList())).thenReturn(false);
        when(barangRepository.findAll()).thenReturn(List.of(stocked, empty));
        when(balanceRepository.findByBarangIdAndLocationKey(1, "RAK-4"))
                .thenReturn(Optional.of(new InventoryBalance(stocked, rack, "RAK-4", 8)));
        when(balanceRepository.findByBarangIdAndLocationKey(2, "RAK-4")).thenReturn(Optional.empty());
        when(stockCountRepository.save(any(StockCount.class))).thenAnswer(invocation -> {
            StockCount count = invocation.getArgument(0);
            ReflectionTestUtils.setField(count, "id", 11);
            for (int i = 0; i < count.getLines().size(); i++) {
                ReflectionTestUtils.setField(count.getLines().get(i), "id", i + 21);
            }
            return count;
        });

        StockCount result = service.createCycleCount(4, 7);

        assertEquals(StockCountStatus.COUNTING, result.getStatus());
        assertEquals(2, result.getLines().size());
        assertEquals(8, result.getLines().get(0).getExpectedQuantity());
        assertEquals(0, result.getLines().get(1).getExpectedQuantity());
    }

    @Test
    void rejectsSecondActiveCountForSameRack() {
        when(userRepository.findById(7)).thenReturn(Optional.of(user(7)));
        when(rakRepository.findByIdForUpdate(4)).thenReturn(Optional.of(rack(4, 100, 0)));
        when(stockCountRepository.existsByRakIdAndStatusIn(eq(4), anyList())).thenReturn(true);

        assertThrows(IllegalStateException.class, () -> service.createCycleCount(4, 7));
        verify(barangRepository, never()).findAll();
    }

    @Test
    void requiresCountResultForEverySkuAndRecordsCounter() {
        StockCount count = countedSession(11, rack(4, 100, 8), item(1, 8), 8, 8);
        count.setStatus(StockCountStatus.COUNTING);
        count.getLines().get(0).setCountedQuantity(null);
        when(stockCountRepository.findByIdForUpdate(11)).thenReturn(Optional.of(count));
        when(userRepository.findById(7)).thenReturn(Optional.of(user(7)));

        assertThrows(IllegalArgumentException.class, () -> service.submitCounts(11, Map.of(), 7));
        assertEquals(StockCountStatus.COUNTING, count.getStatus());

        service.submitCounts(11, Map.of(count.getLines().get(0).getId(), 9), 7);

        assertEquals(9, count.getLines().get(0).getCountedQuantity());
        assertEquals(7, count.getCountedBy().getId());
        assertEquals(StockCountStatus.COUNTED, count.getStatus());
    }

    @Test
    void postsPositiveVarianceToBalanceLotAndRackAsOneOperation() {
        Barang item = item(1, 10);
        Rak rack = rack(4, 100, 10);
        StockCount count = countedSession(11, rack, item, 10, 12);
        User user = user(7);
        InventoryBalance balance = new InventoryBalance(item, rack, "RAK-4", 10);
        when(stockCountRepository.findByIdForUpdate(11)).thenReturn(Optional.of(count));
        when(userRepository.findById(7)).thenReturn(Optional.of(user));
        when(barangRepository.findByIdForUpdate(1)).thenReturn(Optional.of(item));
        when(rakRepository.findByIdForUpdate(4)).thenReturn(Optional.of(rack));
        when(balanceRepository.findByBarangIdAndLocationKeyForUpdate(1, "RAK-4"))
                .thenReturn(Optional.of(balance));
        when(lotBalanceRepository.findByBarangIdAndLocationKeyAndBatchCodeForUpdate(
                eq(1), eq("RAK-4"), startsWith("COUNT-"))).thenReturn(Optional.empty());

        service.postCount(11, 7);

        assertEquals(12, balance.getQuantity());
        assertEquals(12, item.getStok());
        assertEquals(12, rack.getTerisi());
        assertEquals(StockCountStatus.POSTED, count.getStatus());
        assertEquals(user, count.getPostedBy());
        verify(lotBalanceRepository).save(argThat(lot -> lot.getQuantity() == 2 && lot.getBatchCode().startsWith("COUNT-")));
    }

    @Test
    void postsNegativeVarianceOnlyAgainstUnreservedLotQuantity() {
        Barang item = item(1, 8);
        Rak rack = rack(4, 100, 8);
        StockCount count = countedSession(11, rack, item, 8, 4);
        InventoryBalance balance = new InventoryBalance(item, rack, "RAK-4", 8);
        InventoryLotBalance lot = new InventoryLotBalance(item, rack, "RAK-4", "LOT-1",
                null, LocalDateTime.now().minusDays(1), 8);
        lot.setReservedQuantity(3);
        when(stockCountRepository.findByIdForUpdate(11)).thenReturn(Optional.of(count));
        when(userRepository.findById(7)).thenReturn(Optional.of(user(7)));
        when(barangRepository.findByIdForUpdate(1)).thenReturn(Optional.of(item));
        when(rakRepository.findByIdForUpdate(4)).thenReturn(Optional.of(rack));
        when(balanceRepository.findByBarangIdAndLocationKeyForUpdate(1, "RAK-4"))
                .thenReturn(Optional.of(balance));
        when(lotBalanceRepository.findAllPositiveForUpdate(1, "RAK-4")).thenReturn(List.of(lot));

        service.postCount(11, 7);

        assertEquals(4, balance.getQuantity());
        assertEquals(4, lot.getQuantity());
        assertEquals(3, lot.getReservedQuantity());
        assertEquals(4, item.getStok());
        assertEquals(4, rack.getTerisi());
    }

    @Test
    void refusesToPostIfBookStockChangedAfterSnapshot() {
        Barang item = item(1, 10);
        Rak rack = rack(4, 100, 11);
        StockCount count = countedSession(11, rack, item, 10, 12);
        when(stockCountRepository.findByIdForUpdate(11)).thenReturn(Optional.of(count));
        when(userRepository.findById(7)).thenReturn(Optional.of(user(7)));
        when(barangRepository.findByIdForUpdate(1)).thenReturn(Optional.of(item));
        when(rakRepository.findByIdForUpdate(4)).thenReturn(Optional.of(rack));
        when(balanceRepository.findByBarangIdAndLocationKeyForUpdate(1, "RAK-4"))
                .thenReturn(Optional.of(new InventoryBalance(item, rack, "RAK-4", 11)));

        assertThrows(IllegalStateException.class, () -> service.postCount(11, 7));

        verify(balanceRepository, never()).save(any());
        verify(lotBalanceRepository, never()).save(any());
        assertEquals(StockCountStatus.COUNTED, count.getStatus());
    }

    @Test
    void refusesCountVarianceThatWouldConsumeReservedUnits() {
        Barang item = item(1, 8);
        Rak rack = rack(4, 100, 8);
        StockCount count = countedSession(11, rack, item, 8, 2);
        InventoryLotBalance lot = new InventoryLotBalance(item, rack, "RAK-4", "LOT-1",
                null, LocalDateTime.now(), 8);
        lot.setReservedQuantity(7);
        when(stockCountRepository.findByIdForUpdate(11)).thenReturn(Optional.of(count));
        when(userRepository.findById(7)).thenReturn(Optional.of(user(7)));
        when(barangRepository.findByIdForUpdate(1)).thenReturn(Optional.of(item));
        when(rakRepository.findByIdForUpdate(4)).thenReturn(Optional.of(rack));
        when(balanceRepository.findByBarangIdAndLocationKeyForUpdate(1, "RAK-4"))
                .thenReturn(Optional.of(new InventoryBalance(item, rack, "RAK-4", 8)));
        when(lotBalanceRepository.findAllPositiveForUpdate(1, "RAK-4")).thenReturn(List.of(lot));

        assertThrows(IllegalStateException.class, () -> service.postCount(11, 7));
        verify(balanceRepository, never()).save(any());
    }

    @Test
    void refusesAdjustmentThatWouldExceedRackCapacity() {
        Barang item = item(1, 10);
        Rak rack = rack(4, 100, 99);
        StockCount count = countedSession(11, rack, item, 10, 12);
        when(stockCountRepository.findByIdForUpdate(11)).thenReturn(Optional.of(count));
        when(userRepository.findById(7)).thenReturn(Optional.of(user(7)));
        when(barangRepository.findByIdForUpdate(1)).thenReturn(Optional.of(item));
        when(rakRepository.findByIdForUpdate(4)).thenReturn(Optional.of(rack));
        when(balanceRepository.findByBarangIdAndLocationKeyForUpdate(1, "RAK-4"))
                .thenReturn(Optional.of(new InventoryBalance(item, rack, "RAK-4", 10)));

        assertThrows(IllegalStateException.class, () -> service.postCount(11, 7));
        verify(balanceRepository, never()).save(any());
        verify(lotBalanceRepository, never()).save(any());
        assertEquals(StockCountStatus.COUNTED, count.getStatus());
    }

    private StockCount countedSession(int id, Rak rack, Barang item, int expected, int counted) {
        StockCount count = new StockCount("SC-TEST-" + id, rack, user(7), LocalDateTime.now());
        StockCountLine line = new StockCountLine(item, expected);
        line.setCountedQuantity(counted);
        count.addLine(line);
        ReflectionTestUtils.setField(count, "id", id);
        ReflectionTestUtils.setField(line, "id", id + 100);
        count.setStatus(StockCountStatus.COUNTED);
        return count;
    }

    private Barang item(int id, int stock) {
        Barang item = Barang.builder().id(id).kodeBarang("ITEM-" + id).namaBarang("Item " + id)
                .stok(stock).stokMin(0).build();
        return item;
    }

    private Rak rack(int id, int capacity, int occupied) {
        Rak rack = new Rak(null, new Section(1, "A", "Section A", 1), "A-" + id, capacity, occupied);
        ReflectionTestUtils.setField(rack, "id", id);
        return rack;
    }

    private User user(int id) {
        User user = User.builder().id(id).fullName("Operator").build();
        return user;
    }
}
