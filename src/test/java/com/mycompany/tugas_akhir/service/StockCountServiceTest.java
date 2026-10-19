package com.mycompany.tugas_akhir.service;

import com.mycompany.tugas_akhir.model.*;
import com.mycompany.tugas_akhir.repository.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class StockCountServiceTest {

    @Mock private StockCountRepository stockCountRepository;
    @Mock private StockCountLineRepository stockCountLineRepository;
    @Mock private BarangRepository barangRepository;
    @Mock private RakRepository rakRepository;
    @Mock private UserRepository userRepository;
    @Mock private InventoryBalanceRepository balanceRepository;
    @Mock private InventoryLotBalanceRepository lotBalanceRepository;
    @Mock private TransaksiRepository transaksiRepository;
    @Mock private TransactionLotAllocationRepository transactionLotAllocationRepository;
    @Mock private InventoryTransferRepository inventoryTransferRepository;

    private StockCountService service;

    @BeforeEach
    void setUp() {
        service = new StockCountService(stockCountRepository, stockCountLineRepository, barangRepository, rakRepository,
                userRepository, balanceRepository, lotBalanceRepository, transaksiRepository,
                transactionLotAllocationRepository, inventoryTransferRepository);
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
    void periodicCycleAssignsSkuRowsAcrossUsersAndSharesCycleCode() {
        User first = user(7);
        User second = user(8);
        Rak rackA = rack(4, 100, 0);
        Rak rackB = rack(5, 100, 0);
        when(userRepository.findById(7)).thenReturn(Optional.of(first));
        when(userRepository.findAll()).thenReturn(List.of(first, second));
        when(rakRepository.findAll()).thenReturn(List.of(rackA, rackB));
        when(rakRepository.findByIdForUpdate(4)).thenReturn(Optional.of(rackA));
        when(rakRepository.findByIdForUpdate(5)).thenReturn(Optional.of(rackB));
        when(stockCountRepository.existsByRakIdAndStatusIn(anyInt(), anyList())).thenReturn(false);
        when(barangRepository.findAll()).thenReturn(List.of(item(1, 4), item(2, 6)));
        when(balanceRepository.findAllForLocationReport()).thenReturn(List.of());
        when(stockCountRepository.save(any(StockCount.class))).thenAnswer(invocation -> invocation.getArgument(0));

        List<StockCount> counts = service.createPeriodicCycleCount(7);

        assertEquals(2, counts.size());
        assertEquals(counts.get(0).getCycleCode(), counts.get(1).getCycleCode());
        assertEquals(StockCountCycleType.PERIODIC, counts.get(0).getCycleType());
        List<Integer> assignments = counts.stream().flatMap(count -> count.getLines().stream())
                .map(line -> line.getAssignedUser().getId()).toList();
        assertEquals(List.of(7, 8, 7, 8), assignments);
    }

    @Test
    void periodicSubmissionsKeepSessionOpenUntilEveryAssignedSkuIsCounted() {
        User first = user(7);
        User second = user(8);
        StockCount count = new StockCount("SC-PERIODIC", rack(4, 100, 0), first, LocalDateTime.now());
        count.setCycle("PC-TEST", StockCountCycleType.PERIODIC);
        StockCountLine firstLine = new StockCountLine(item(1, 0), 0);
        firstLine.setAssignedUser(first);
        StockCountLine secondLine = new StockCountLine(item(2, 0), 0);
        secondLine.setAssignedUser(second);
        count.addLine(firstLine);
        count.addLine(secondLine);
        ReflectionTestUtils.setField(firstLine, "id", 101);
        ReflectionTestUtils.setField(secondLine, "id", 102);
        ReflectionTestUtils.setField(count, "id", 11);
        when(stockCountRepository.findByIdForUpdate(11)).thenReturn(Optional.of(count));
        when(userRepository.findById(7)).thenReturn(Optional.of(first));
        when(userRepository.findById(8)).thenReturn(Optional.of(second));

        service.submitCounts(11, Map.of(101, 3), 7);
        assertEquals(StockCountStatus.COUNTING, count.getStatus());
        assertNull(count.getCountedBy());

        service.submitCounts(11, Map.of(102, 4), 8);
        assertEquals(StockCountStatus.COUNTED, count.getStatus());
        assertEquals(8, count.getCountedBy().getId());
    }

    @Test
    void countOperatorCanSubmitAssignedSkusInMultipleBatches() {
        User operator = user(7);
        StockCount count = new StockCount("SC-BATCHED", rack(4, 100, 0), operator, LocalDateTime.now());
        StockCountLine firstLine = new StockCountLine(item(1, 0), 0);
        firstLine.setAssignedUser(operator);
        StockCountLine secondLine = new StockCountLine(item(2, 0), 0);
        secondLine.setAssignedUser(operator);
        count.addLine(firstLine);
        count.addLine(secondLine);
        ReflectionTestUtils.setField(count, "id", 13);
        ReflectionTestUtils.setField(firstLine, "id", 201);
        ReflectionTestUtils.setField(secondLine, "id", 202);
        when(stockCountRepository.findByIdForUpdate(13)).thenReturn(Optional.of(count));
        when(userRepository.findById(7)).thenReturn(Optional.of(operator));

        service.submitCounts(13, Map.of(201, 3), 7);

        assertEquals(StockCountStatus.COUNTING, count.getStatus());
        assertEquals(3, firstLine.getCountedQuantity());
        assertNull(secondLine.getCountedQuantity());

        service.submitCounts(13, Map.of(202, 2), 7);

        assertEquals(StockCountStatus.COUNTED, count.getStatus());
        assertEquals(2, secondLine.getCountedQuantity());
    }

    @Test
    void monthlyScheduleSpreadsRackTasksAcrossWeekdaysWithoutTakingEarlySnapshots() {
        User creator = user(7);
        Rak rackA = rack(4, 100, 0);
        Rak rackB = rack(5, 100, 0);
        when(userRepository.findById(7)).thenReturn(Optional.of(creator));
        when(stockCountRepository.existsByCycleCode("MC-202611")).thenReturn(false);
        when(rakRepository.findAll()).thenReturn(List.of(rackA, rackB));
        when(rakRepository.findByIdForUpdate(4)).thenReturn(Optional.of(rackA));
        when(rakRepository.findByIdForUpdate(5)).thenReturn(Optional.of(rackB));
        when(stockCountRepository.existsByRakIdAndStatusIn(anyInt(), anyList())).thenReturn(false);
        when(stockCountRepository.save(any(StockCount.class))).thenAnswer(invocation -> invocation.getArgument(0));

        List<StockCount> schedule = service.createMonthlySchedule(YearMonth.of(2026, 11), 7);

        assertEquals(2, schedule.size());
        assertTrue(schedule.stream().allMatch(count -> count.getStatus() == StockCountStatus.SCHEDULED));
        assertTrue(schedule.stream().allMatch(count -> count.getLines().isEmpty()));
        assertTrue(schedule.stream().allMatch(count -> count.getDueDate().getDayOfWeek().getValue() <= 5));
        assertEquals("MC-202611", schedule.getFirst().getCycleCode());
    }

    @Test
    void dailyCycleCreatesSnapshotsOnlyForRacksWithTodaysMutations() {
        User creator = user(7);
        Rak rackA = rack(4, 100, 8);
        Rak rackB = rack(5, 100, 3);
        Rak quietRack = rack(6, 100, 0);
        Barang item = item(1, 11);
        when(userRepository.findById(7)).thenReturn(Optional.of(creator));
        when(stockCountRepository.existsByCycleCode("DC-" + LocalDate.now().toString().replace("-", "")))
                .thenReturn(false);
        when(transaksiRepository.countMutationsByRackSince(any(LocalDateTime.class)))
                .thenReturn(List.of(mutationCount(4, 1)));
        when(inventoryTransferRepository.countMutationsFromRackSince(any(LocalDateTime.class)))
                .thenReturn(List.of(mutationCount(5, 1)));
        when(inventoryTransferRepository.countMutationsIntoRackSince(any(LocalDateTime.class)))
                .thenReturn(List.of(mutationCount(4, 1)));
        when(rakRepository.findAll()).thenReturn(List.of(rackA, rackB, quietRack));
        when(rakRepository.findByIdForUpdate(4)).thenReturn(Optional.of(rackA));
        when(rakRepository.findByIdForUpdate(5)).thenReturn(Optional.of(rackB));
        when(stockCountRepository.existsByRakIdAndStatusIn(anyInt(), anyList())).thenReturn(false);
        when(barangRepository.findAll()).thenReturn(List.of(item));
        when(balanceRepository.findAllForLocationReport()).thenReturn(List.of(
                new InventoryBalance(item, rackA, "RAK-4", 6)));
        when(stockCountRepository.save(any(StockCount.class))).thenAnswer(invocation -> invocation.getArgument(0));

        List<StockCount> counts = service.createDailyCycleCount(7);

        assertEquals(List.of(4, 5), counts.stream().map(count -> count.getRak().getId()).toList());
        assertTrue(counts.stream().allMatch(count -> count.getCycleType() == StockCountCycleType.DAILY));
        assertTrue(counts.stream().allMatch(count -> count.getStatus() == StockCountStatus.COUNTING));
        assertTrue(counts.stream().allMatch(count -> count.getCycleCode().startsWith("DC-")));
        assertEquals(6, counts.getFirst().getLines().getFirst().getExpectedQuantity());
        assertEquals(0, counts.getLast().getLines().getFirst().getExpectedQuantity());
    }

    @Test
    void dailyCycleDoesNotCreateSessionsWhenThereWereNoMutations() {
        when(userRepository.findById(7)).thenReturn(Optional.of(user(7)));
        when(stockCountRepository.existsByCycleCode(anyString())).thenReturn(false);
        when(transaksiRepository.countMutationsByRackSince(any(LocalDateTime.class))).thenReturn(List.of());
        when(inventoryTransferRepository.countMutationsFromRackSince(any(LocalDateTime.class)))
                .thenReturn(List.of());
        when(inventoryTransferRepository.countMutationsIntoRackSince(any(LocalDateTime.class)))
                .thenReturn(List.of());

        assertTrue(service.createDailyCycleCount(7).isEmpty());
        verify(rakRepository, never()).findAll();
        verify(stockCountRepository, never()).save(any(StockCount.class));
    }

    @Test
    void dailyCycleCanBeCreatedOnlyOncePerDay() {
        when(userRepository.findById(7)).thenReturn(Optional.of(user(7)));
        when(stockCountRepository.existsByCycleCode(anyString())).thenReturn(true);

        assertThrows(IllegalStateException.class, () -> service.createDailyCycleCount(7));
        verify(transaksiRepository, never()).countMutationsByRackSince(any(LocalDateTime.class));
    }

    @Test
    void mobileCountBarcodeMustMatchAnOpenAssignedLine() {
        when(stockCountLineRepository.findOpenSkuForUser(11, 21, 7)).thenReturn(Optional.of("SKU-1"));

        service.validateMobileCountBarcode(11, 21, 7, "WMS1|U0tVLTE|QkFUQ0gtMQ|-");

        assertThrows(IllegalStateException.class, () -> service.validateMobileCountBarcode(
                11, 21, 7, "WMS1|U0tVLTI|QkFUQ0gtMQ|-"));
        assertThrows(IllegalArgumentException.class, () -> service.validateMobileCountBarcode(
                11, 21, 7, "SLOC1|U0xPQy0x"));
    }

    @Test
    void mobileVarianceReviewIsUnavailableUntilCountIsComplete() {
        when(stockCountRepository.findStatusById(9)).thenReturn(Optional.of(StockCountStatus.COUNTING));

        assertThrows(IllegalStateException.class,
                () -> service.getMobileCountReviewLines(9, PageRequest.of(0, 100)));
        verify(stockCountLineRepository, never()).findReviewLines(any(), any());
    }

    @Test
    void startingMonthlyTaskTakesFreshSnapshotAndMovesItToCounting() {
        User creator = user(7);
        Rak rack = rack(4, 100, 0);
        StockCount scheduled = new StockCount("SC-MONTHLY", rack, creator, LocalDateTime.now());
        scheduled.setCycle("MC-202610", StockCountCycleType.MONTHLY);
        scheduled.setDueDate(LocalDate.now().minusDays(1));
        scheduled.setStatus(StockCountStatus.SCHEDULED);
        ReflectionTestUtils.setField(scheduled, "id", 12);
        Barang item = item(1, 9);
        when(stockCountRepository.findByIdForUpdate(12)).thenReturn(Optional.of(scheduled));
        when(rakRepository.findByIdForUpdate(4)).thenReturn(Optional.of(rack));
        when(barangRepository.findAll()).thenReturn(List.of(item));
        when(balanceRepository.findAllForLocationReport())
                .thenReturn(List.of(new InventoryBalance(item, rack, "RAK-4", 9)));
        when(stockCountRepository.save(scheduled)).thenReturn(scheduled);

        StockCount started = service.startScheduledCount(12);

        assertEquals(StockCountStatus.COUNTING, started.getStatus());
        assertEquals(1, started.getLines().size());
        assertEquals(9, started.getLines().getFirst().getExpectedQuantity());
    }

    @Test
    void prioritizesRacksWithTodayStockTransactionsAndSlocTransfers() {
        Rak quietRack = rack(4, 100, 20);
        Rak activeRack = rack(5, 100, 20);
        when(stockCountRepository.findAllByStatusOrderByPostedAtDesc(StockCountStatus.POSTED))
                .thenReturn(List.of());
        when(balanceRepository.findAllForLocationReport()).thenReturn(List.of());
        when(rakRepository.findAll()).thenReturn(List.of(quietRack, activeRack));
        when(transaksiRepository.countMutationsByRackSince(any(LocalDateTime.class)))
                .thenReturn(List.of(mutationCount(5, 1)));
        when(inventoryTransferRepository.countMutationsFromRackSince(any(LocalDateTime.class)))
                .thenReturn(List.of(mutationCount(5, 1)));
        when(inventoryTransferRepository.countMutationsIntoRackSince(any(LocalDateTime.class)))
                .thenReturn(List.of(mutationCount(5, 1)));

        List<StockCountService.CycleCountRecommendation> recommendations =
                service.getCycleCountRecommendations();

        assertEquals(activeRack, recommendations.getFirst().rack());
        assertEquals(3, recommendations.getFirst().dailyMutationCount());
        assertTrue(recommendations.getFirst().reason().contains("3 mutasi stok hari ini"));
        assertTrue(recommendations.getFirst().priorityScore() > recommendations.getLast().priorityScore());
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
        when(transaksiRepository.save(any(Transaksi.class))).thenAnswer(invocation -> invocation.getArgument(0));

        service.postCount(11, 7);

        assertEquals(12, balance.getQuantity());
        assertEquals(12, item.getStok());
        assertEquals(12, rack.getTerisi());
        assertEquals(StockCountStatus.POSTED, count.getStatus());
        assertEquals(user, count.getPostedBy());
        verify(lotBalanceRepository).save(argThat(lot -> lot.getQuantity() == 2 && lot.getBatchCode().startsWith("COUNT-")));
        verify(transaksiRepository).save(argThat(transaction -> "masuk".equals(transaction.getTipe())
                && transaction.getJumlah() == 2 && transaction.getRak() == rack
                && transaction.getUser() == user && transaction.getKeterangan().contains(count.getCountCode())));
        verify(transactionLotAllocationRepository).save(argThat(allocation -> allocation.getQuantity() == 2));
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
        when(transaksiRepository.save(any(Transaksi.class))).thenAnswer(invocation -> invocation.getArgument(0));

        service.postCount(11, 7);

        assertEquals(4, balance.getQuantity());
        assertEquals(4, lot.getQuantity());
        assertEquals(3, lot.getReservedQuantity());
        assertEquals(4, item.getStok());
        assertEquals(4, rack.getTerisi());
        verify(transaksiRepository).save(argThat(transaction -> "keluar".equals(transaction.getTipe())
                && transaction.getJumlah() == 4 && transaction.getRak() == rack));
        verify(transactionLotAllocationRepository).save(argThat(allocation -> allocation.getQuantity() == 4));
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

    private RackMutationCount mutationCount(Integer rackId, long count) {
        return new RackMutationCount() {
            @Override public Integer getRackId() { return rackId; }
            @Override public Long getMutationCount() { return count; }
        };
    }
}
