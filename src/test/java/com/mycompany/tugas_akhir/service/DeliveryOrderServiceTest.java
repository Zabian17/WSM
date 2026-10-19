package com.mycompany.tugas_akhir.service;

import com.mycompany.tugas_akhir.model.*;
import com.mycompany.tugas_akhir.repository.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.time.LocalDate;
import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class DeliveryOrderServiceTest {

    @Mock private DeliveryOrderRepository orderRepository;
    @Mock private CustomerRepository customerRepository;
    @Mock private BarangRepository barangRepository;
    @Mock private UserRepository userRepository;
    @Mock private OutboundStockPoster stockPoster;
    @Mock private InventoryLotBalanceRepository lotBalanceRepository;
    @Mock private DeliveryOrderLotReservationRepository reservationRepository;
    @Mock private DeliveryOrderPickScanRepository pickScanRepository;
    @Mock private DeliveryOrderFulfillmentAuditRepository fulfillmentAuditRepository;
    @Mock private PickTaskRepository pickTaskRepository;
    @Mock private SectionRepository sectionRepository;

    private DeliveryOrderService service;

    @BeforeEach
    void setUp() {
        service = new DeliveryOrderService(orderRepository, customerRepository, barangRepository,
            userRepository, stockPoster, lotBalanceRepository, reservationRepository, pickScanRepository,
            fulfillmentAuditRepository, pickTaskRepository, sectionRepository,
            new InventoryPickingPolicy(lotBalanceRepository, "FEFO"));
    }

    @Test
    void createsDraftWithoutChangingInventory() {
        Customer customer = new Customer(2, "Customer", null, null, null, null, "Aktif");
        User user = User.builder().id(3).fullName("Operator").build();
        Barang item = Barang.builder().id(1).kodeBarang("SKU-1").namaBarang("Item").build();
        when(customerRepository.findById(2)).thenReturn(Optional.of(customer));
        when(userRepository.findById(3)).thenReturn(Optional.of(user));
        when(barangRepository.findById(1)).thenReturn(Optional.of(item));
        when(orderRepository.save(any(DeliveryOrder.class))).thenAnswer(invocation -> invocation.getArgument(0));

        DeliveryOrder order = service.createDraft(2, 3, Map.of(1, 4), "urgent");

        assertNotNull(order);
        assertEquals(DeliveryOrderStatus.DRAFT, order.getStatus());
        assertEquals(4, order.getLines().get(0).getOrderedQuantity());
        verifyNoInteractions(stockPoster);
    }

    @Test
    void loadsOrderReservationsWithoutLockingOrMutatingThem() {
        when(reservationRepository.findByOrderId(7)).thenReturn(List.of());

        assertTrue(service.getReservations(7).isEmpty());
        assertTrue(service.getReservations(null).isEmpty());
        verify(reservationRepository).findByOrderId(7);
        verifyNoInteractions(stockPoster);
    }

    @Test
    void releaseRequiresEnoughStockInMappedLocations() {
        DeliveryOrder order = order(DeliveryOrderStatus.DRAFT, 4);
        Rak rack = new Rak(2, pickingSection(), "A-01", 100, 3);
        InventoryLotBalance lot = lot(order.getLines().get(0).getBarang(), rack, "LOT-A", 3);
        when(orderRepository.findByIdWithLinesForUpdate(7)).thenReturn(Optional.of(order));
        when(barangRepository.findByIdForUpdate(1)).thenReturn(Optional.of(order.getLines().get(0).getBarang()));
        when(lotBalanceRepository.findAvailableFefoAcrossLocationsForUpdate(1, java.time.LocalDate.now()))
            .thenReturn(List.of(lot));

        assertFalse(service.releaseToPicking(7));

        assertEquals(DeliveryOrderStatus.DRAFT, order.getStatus());
        assertEquals(0, lot.getReservedQuantity());
        verify(orderRepository, never()).save(order);
        verifyNoInteractions(reservationRepository);
    }

    @Test
    void releaseReservesLotsOnlyAfterEveryLineHasEnoughAvailableStock() {
        DeliveryOrder order = order(DeliveryOrderStatus.DRAFT, 4);
        Rak rack = new Rak(2, null, "A-01", 100, 5);
        InventoryLotBalance first = lot(order.getLines().get(0).getBarang(), rack, "LOT-A", 3);
        InventoryLotBalance second = lot(order.getLines().get(0).getBarang(), rack, "LOT-B", 2);
        when(orderRepository.findByIdWithLinesForUpdate(7)).thenReturn(Optional.of(order));
        when(barangRepository.findByIdForUpdate(1)).thenReturn(Optional.of(order.getLines().get(0).getBarang()));
        when(lotBalanceRepository.findAvailableFefoAcrossLocationsForUpdate(1, java.time.LocalDate.now()))
                .thenReturn(List.of(first, second));

        assertTrue(service.releaseToPicking(7));

        assertEquals(DeliveryOrderStatus.READY_TO_PICK, order.getStatus());
        assertEquals(3, first.getReservedQuantity());
        assertEquals(1, second.getReservedQuantity());
        verify(reservationRepository, times(2)).save(any(DeliveryOrderLotReservation.class));
        verify(pickTaskRepository).save(any(PickTask.class));
    }

    @Test
    void pickingMustBeAssignedToPickerAndSectionBeforeItStarts() {
        DeliveryOrder order = order(DeliveryOrderStatus.READY_TO_PICK, 3);
        User picker = User.builder().id(9).fullName("Picker").build();
        Section section = new Section(5, "SEC-5", "Zone 5", 12);
        PickTask task = new PickTask("PICK-TEST", order, LocalDateTime.now());
        when(orderRepository.findByIdWithLinesForUpdate(7)).thenReturn(Optional.of(order));
        when(pickTaskRepository.findActiveByOrderId(7)).thenReturn(List.of(task));
        when(userRepository.findById(9)).thenReturn(Optional.of(picker));
        when(sectionRepository.findById(5)).thenReturn(Optional.of(section));

        assertFalse(service.startPicking(7));
        assertTrue(service.assignPicker(7, 9, 5));
        assertTrue(service.startPicking(7));
        assertEquals(PickTaskStatus.IN_PROGRESS, task.getStatus());
        assertEquals(picker, task.getAssignedPicker());
        assertEquals(section, task.getAssignedSection());
        assertEquals(DeliveryOrderStatus.PICKING, order.getStatus());
    }

    @Test
    void assignsDifferentPickersAndShiftsToDistinctReservedSections() {
        DeliveryOrder order = order(DeliveryOrderStatus.READY_TO_PICK, 4);
        Section firstSection = new Section(5, "SEC-5", "Zone 5", 12);
        Section secondSection = new Section(6, "SEC-6", "Zone 6", 12);
        User firstPicker = User.builder().id(9).fullName("Picker 1").build();
        User secondPicker = User.builder().id(10).fullName("Picker 2").build();
        PickTask openTask = new PickTask("PICK-OPEN", order, LocalDateTime.now());
        Rak firstRack = new Rak(2, firstSection, "A-01", 100, 0);
        Rak secondRack = new Rak(3, secondSection, "B-01", 0, 0);
        InventoryLotBalance firstLot = lot(order.getLines().getFirst().getBarang(), firstRack, "LOT-A", 2);
        InventoryLotBalance secondLot = lot(order.getLines().getFirst().getBarang(), secondRack, "LOT-B", 2);
        when(orderRepository.findByIdWithLinesForUpdate(7)).thenReturn(Optional.of(order));
        when(pickTaskRepository.findActiveByOrderId(7)).thenReturn(List.of(openTask));
        when(userRepository.findById(9)).thenReturn(Optional.of(firstPicker));
        when(userRepository.findById(10)).thenReturn(Optional.of(secondPicker));
        when(sectionRepository.findById(5)).thenReturn(Optional.of(firstSection));
        when(sectionRepository.findById(6)).thenReturn(Optional.of(secondSection));
        when(reservationRepository.findByOrderIdForUpdate(7)).thenReturn(List.of(
                new DeliveryOrderLotReservation(order.getLines().getFirst(), firstLot, 2),
                new DeliveryOrderLotReservation(order.getLines().getFirst(), secondLot, 2)));

        assertTrue(service.assignPickers(7, List.of(
                new DeliveryOrderService.PickerAssignment(9, 5, "Pagi"),
                new DeliveryOrderService.PickerAssignment(10, 6, "Malam"))));

        assertEquals(PickTaskStatus.CANCELLED, openTask.getStatus());
        verify(pickTaskRepository, times(3)).save(any(PickTask.class));
        verify(pickTaskRepository).save(argThat(value -> value.getAssignedPicker() == firstPicker
                && value.getAssignedSection() == firstSection && "Pagi".equals(value.getAssignedShift())));
        verify(pickTaskRepository).save(argThat(value -> value.getAssignedPicker() == secondPicker
                && value.getAssignedSection() == secondSection && "Malam".equals(value.getAssignedShift())));
    }

    @Test
    void pickerCannotScanReservedRackOutsideTheirAssignedSection() {
        DeliveryOrder order = order(DeliveryOrderStatus.PICKING, 3);
        DeliveryOrderLine line = order.getLines().getFirst();
        User firstPicker = User.builder().id(9).fullName("Picker 1").build();
        User secondPicker = User.builder().id(10).fullName("Picker 2").build();
        Section otherSection = new Section(6, "SEC-6", "Zone 6", 12);
        Rak otherRack = new Rak(3, otherSection, "B-01", 100, 0);
        InventoryLotBalance lot = lot(line.getBarang(), otherRack, "LOT-B", 3);
        DeliveryOrderLotReservation reservation = new DeliveryOrderLotReservation(line, lot, 3);
        PickTask firstTask = activeTask(order, firstPicker);
        PickTask secondTask = new PickTask("PICK-OTHER", order, LocalDateTime.now());
        secondTask.assign(secondPicker, otherSection, "Malam", LocalDateTime.now());
        secondTask.setStatus(PickTaskStatus.IN_PROGRESS);
        when(orderRepository.findByIdWithLinesForUpdate(7)).thenReturn(Optional.of(order));
        when(userRepository.findById(9)).thenReturn(Optional.of(firstPicker));
        when(pickTaskRepository.findActiveByOrderId(7)).thenReturn(List.of(firstTask, secondTask));
        when(reservationRepository.findByLineIdForUpdate(line.getId())).thenReturn(List.of(reservation));

        assertFalse(service.recordPickWithBarcodes(7, line.getId(),
                itemQr("SKU-1", "LOT-B", "-"), locationBarcode("B-01"), 1, 9));
        verify(reservationRepository, never()).save(any());
    }

    @Test
    void scanRejectsMismatchedSkuOrLocation() {
        DeliveryOrder order = order(DeliveryOrderStatus.PICKING, 3);
        Rak rack = new Rak(2, pickingSection(), "A-01", 100, 3);
        InventoryLotBalance lot = lot(order.getLines().get(0).getBarang(), rack, "LOT-A", 3);
        DeliveryOrderLotReservation reservation = new DeliveryOrderLotReservation(order.getLines().get(0), lot, 3);
        User picker = User.builder().id(9).fullName("Picker").build();
        when(orderRepository.findByIdWithLinesForUpdate(7)).thenReturn(Optional.of(order));
        when(userRepository.findById(9)).thenReturn(Optional.of(picker));
        when(pickTaskRepository.findActiveByOrderId(7)).thenReturn(List.of(activeTask(order, picker)));
        when(reservationRepository.findByLineIdForUpdate(order.getLines().get(0).getId()))
                .thenReturn(List.of(reservation));

        assertFalse(service.recordPickWithBarcodes(7, order.getLines().get(0).getId(),
                itemQr("WRONG-SKU", "LOT-A", "-"), locationBarcode("A-01"), 1, 9));
        assertFalse(service.recordPickWithBarcodes(7, order.getLines().get(0).getId(),
                itemQr("SKU-1", "LOT-A", "-"), locationBarcode("B-99"), 1, 9));
        assertEquals(0, reservation.getPickedQuantity());
        verify(reservationRepository, never()).save(any());
    }

    @Test
    void validScanUpdatesPickedQuantityAndPreventsOverPick() {
        DeliveryOrder order = order(DeliveryOrderStatus.PICKING, 3);
        DeliveryOrderLine line = order.getLines().get(0);
        Rak rack = new Rak(2, pickingSection(), "A-01", 100, 3);
        InventoryLotBalance lot = lot(line.getBarang(), rack, "LOT-A", 3);
        lot.setReservedQuantity(3);
        DeliveryOrderLotReservation reservation = new DeliveryOrderLotReservation(line, lot, 3);
        User picker = User.builder().id(9).fullName("Picker").build();
        when(orderRepository.findByIdWithLinesForUpdate(7)).thenReturn(Optional.of(order));
        when(userRepository.findById(9)).thenReturn(Optional.of(picker));
        when(pickTaskRepository.findActiveByOrderId(7)).thenReturn(List.of(activeTask(order, picker)));
        when(reservationRepository.findByLineIdForUpdate(line.getId())).thenReturn(List.of(reservation));

        assertTrue(service.recordPickWithBarcodes(7, line.getId(),
                itemQr("SKU-1", "LOT-A", "-"), locationBarcode("A-01"), 2, 9));
        assertFalse(service.recordPickWithBarcodes(7, line.getId(),
                itemQr("SKU-1", "LOT-A", "-"), locationBarcode("A-01"), 2, 9));

        assertEquals(2, reservation.getPickedQuantity());
        verify(reservationRepository, times(1)).save(reservation);
        verify(pickScanRepository).save(any(DeliveryOrderPickScan.class));
    }

    @Test
    void pickingCanCompletePartiallyAndReleasesUnpickedReservation() {
        DeliveryOrder order = order(DeliveryOrderStatus.PICKING, 3);
        DeliveryOrderLine line = order.getLines().get(0);
        Rak rack = new Rak(2, null, "A-01", 100, 3);
        InventoryLotBalance lot = lot(line.getBarang(), rack, "LOT-A", 3);
        DeliveryOrderLotReservation reservation = reservation(order, lot, 3);
        reservation.setPickedQuantity(2);
        when(orderRepository.findByIdWithLinesForUpdate(7)).thenReturn(Optional.of(order));
        when(pickTaskRepository.findActiveByOrderId(7))
                .thenReturn(List.of(activeTask(order, User.builder().id(9).fullName("Picker").build())));
        when(reservationRepository.findByOrderIdForUpdate(7)).thenReturn(List.of(reservation));
        when(lotBalanceRepository.findByIdForUpdate(lot.getId())).thenReturn(Optional.of(lot));

        assertTrue(service.completePicking(7));
        assertEquals(DeliveryOrderStatus.PICKED, order.getStatus());
        assertEquals(2, reservation.getReservedQuantity());
        assertEquals(2, lot.getReservedQuantity());
    }

    @Test
    void qrPickRequiresMatchingItemBatchExpiryAndLocation() {
        DeliveryOrder order = order(DeliveryOrderStatus.PICKING, 3);
        DeliveryOrderLine line = order.getLines().get(0);
        User picker = User.builder().id(9).fullName("Picker").build();
        LocalDate expiry = LocalDate.of(2027, 5, 31);
        InventoryLotBalance lot = new InventoryLotBalance(line.getBarang(),
                new Rak(2, pickingSection(), "A-01", 100, 3), "RAK-2", "LOT-A",
                expiry, LocalDateTime.now(), 3);
        ReflectionTestUtils.setField(lot, "id", 101);
        lot.setReservedQuantity(3);
        DeliveryOrderLotReservation reservation = new DeliveryOrderLotReservation(line, lot, 3);
        when(orderRepository.findByIdWithLinesForUpdate(7)).thenReturn(Optional.of(order));
        when(userRepository.findById(9)).thenReturn(Optional.of(picker));
        when(pickTaskRepository.findActiveByOrderId(7)).thenReturn(List.of(activeTask(order, picker)));
        when(reservationRepository.findByLineIdForUpdate(line.getId())).thenReturn(List.of(reservation));

        String qr = itemQr("SKU-1", "LOT-A", expiry.toString());
        String sloc = locationBarcode("A-01");
        assertFalse(service.recordPickWithBarcodes(7, line.getId(), itemQr("SKU-1", "LOT-A", "2027-06-30"),
                sloc, 1, 9));
        assertFalse(service.recordPickWithBarcodes(7, line.getId(), qr, locationBarcode("A-02"), 1, 9));
        assertFalse(service.recordPickWithBarcodes(7, line.getId(), "WMS1|bad|payload|-", sloc, 1, 9));
        assertTrue(service.recordPickWithBarcodes(7, line.getId(), qr, sloc, 2, 9));

        assertEquals(2, reservation.getPickedQuantity());
        verify(pickScanRepository).save(argThat(scan -> expiry.equals(scan.getScannedExpiry())));
    }

    @Test
    void qrPickCannotLeaveAssignedSection() {
        DeliveryOrder order = order(DeliveryOrderStatus.PICKING, 1);
        DeliveryOrderLine line = order.getLines().get(0);
        User picker = User.builder().id(9).fullName("Picker").build();
        Rak otherSectionRack = new Rak(3, new Section(6, "SEC-6", "Other zone", 12), "A-06", 100, 1);
        InventoryLotBalance lot = lot(line.getBarang(), otherSectionRack, "LOT-A", 1);
        DeliveryOrderLotReservation reservation = new DeliveryOrderLotReservation(line, lot, 1);
        when(orderRepository.findByIdWithLinesForUpdate(7)).thenReturn(Optional.of(order));
        when(userRepository.findById(9)).thenReturn(Optional.of(picker));
        when(pickTaskRepository.findActiveByOrderId(7)).thenReturn(List.of(activeTask(order, picker)));
        when(reservationRepository.findByLineIdForUpdate(line.getId())).thenReturn(List.of(reservation));

        assertFalse(service.recordPickWithBarcodes(7, line.getId(),
                itemQr("SKU-1", "LOT-A", "-"), locationBarcode("A-06"), 1, 9));
        verify(pickScanRepository, never()).save(any());
    }

    @Test
    void mobilePickInstructionsAreLimitedToAssignedSectionAndRemainingLots() {
        DeliveryOrder order = order(DeliveryOrderStatus.PICKING, 3);
        User picker = User.builder().id(9).fullName("Picker").build();
        PickTask task = activeTask(order, picker);
        ReflectionTestUtils.setField(task, "id", 22);
        InventoryLotBalance assignedLot = lot(order.getLines().get(0).getBarang(),
                new Rak(2, pickingSection(), "A-01", 100, 3), "LOT-A", 3);
        InventoryLotBalance otherLot = lot(order.getLines().get(0).getBarang(),
                new Rak(3, new Section(6, "SEC-6", "Zone 6", 12), "B-01", 100, 3), "LOT-B", 3);
        DeliveryOrderLotReservation assignedReservation = new DeliveryOrderLotReservation(
                order.getLines().get(0), assignedLot, 3);
        assignedReservation.setPickedQuantity(1);
        DeliveryOrderLotReservation otherReservation = new DeliveryOrderLotReservation(
                order.getLines().get(0), otherLot, 3);
        when(pickTaskRepository.findById(22)).thenReturn(Optional.of(task));
        when(reservationRepository.findByOrderId(7)).thenReturn(List.of(assignedReservation, otherReservation));

        List<DeliveryOrderService.MobilePickInstruction> instructions = service.getMobilePickInstructions(22, 9);

        assertEquals(1, instructions.size());
        assertEquals("A-01", instructions.get(0).rackCode());
        assertEquals(2, instructions.get(0).remainingQuantity());
    }

    @Test
    void mobilePickingCanCompletePartiallyAndReleasesUnpickedReservations() {
        DeliveryOrder order = order(DeliveryOrderStatus.PICKING, 3);
        User picker = User.builder().id(9).fullName("Picker").build();
        PickTask task = activeTask(order, picker);
        ReflectionTestUtils.setField(task, "id", 22);
        DeliveryOrderLotReservation reservation = reservation(order,
                lot(order.getLines().get(0).getBarang(),
                        new Rak(2, pickingSection(), "A-01", 100, 3), "LOT-A", 3), 3);
        reservation.setPickedQuantity(2);
        when(pickTaskRepository.findById(22)).thenReturn(Optional.of(task));
        when(orderRepository.findByIdWithLinesForUpdate(7)).thenReturn(Optional.of(order));
        when(pickTaskRepository.findActiveByOrderId(7)).thenReturn(List.of(task));
        when(reservationRepository.findByOrderIdForUpdate(7)).thenReturn(List.of(reservation));
        when(lotBalanceRepository.findByIdForUpdate(101))
                .thenReturn(Optional.of(reservation.getLotBalance()));

        assertTrue(service.completeMobilePicking(22, 9));

        assertEquals(DeliveryOrderStatus.PICKED, order.getStatus());
        assertEquals(2, reservation.getReservedQuantity());
    }

    @Test
    void mobilePickingCompletesOrderAfterAllReservedLotsArePicked() {
        DeliveryOrder order = order(DeliveryOrderStatus.PICKING, 3);
        User picker = User.builder().id(9).fullName("Picker").build();
        PickTask task = activeTask(order, picker);
        ReflectionTestUtils.setField(task, "id", 22);
        InventoryLotBalance lot = lot(order.getLines().get(0).getBarang(),
                new Rak(2, pickingSection(), "A-01", 100, 3), "LOT-A", 3);
        DeliveryOrderLotReservation reservation = reservation(order, lot, 3);
        when(pickTaskRepository.findById(22)).thenReturn(Optional.of(task));
        when(orderRepository.findByIdWithLinesForUpdate(7)).thenReturn(Optional.of(order));
        when(pickTaskRepository.findActiveByOrderId(7)).thenReturn(List.of(task));
        when(reservationRepository.findByOrderIdForUpdate(7)).thenReturn(List.of(reservation));
        when(lotBalanceRepository.findByIdForUpdate(101)).thenReturn(Optional.of(lot));

        assertTrue(service.completeMobilePicking(22, 9));

        assertEquals(DeliveryOrderStatus.PICKED, order.getStatus());
        assertEquals(PickTaskStatus.COMPLETED, task.getStatus());
    }

    @Test
    void mobilePickerCannotReadAnotherPickersTask() {
        DeliveryOrder order = order(DeliveryOrderStatus.PICKING, 3);
        PickTask task = activeTask(order, User.builder().id(9).fullName("Picker").build());
        ReflectionTestUtils.setField(task, "id", 22);
        when(pickTaskRepository.findById(22)).thenReturn(Optional.of(task));

        assertTrue(service.getMobilePickTask(22, 10).isEmpty());
    }

    @Test
    void dispatchPostsGoodsIssueAcrossLocationsAndAdvancesState() {
        DeliveryOrder order = order(DeliveryOrderStatus.LOADED, 5);
        Rak firstRack = new Rak(2, null, "A-01", 100, 3);
        Rak secondRack = new Rak(3, null, "A-02", 100, 4);
        when(orderRepository.findByIdWithLinesForUpdate(7)).thenReturn(Optional.of(order));
        when(userRepository.findById(9)).thenReturn(Optional.of(User.builder().id(9).fullName("Dispatcher").build()));
        InventoryLotBalance firstLot = lot(order.getLines().get(0).getBarang(), firstRack, "LOT-A", 3);
        InventoryLotBalance secondLot = lot(order.getLines().get(0).getBarang(), secondRack, "LOT-B", 2);
        DeliveryOrderLotReservation firstReservation = reservation(order, firstLot, 3);
        DeliveryOrderLotReservation secondReservation = reservation(order, secondLot, 2);
        when(reservationRepository.findByOrderIdForUpdate(7))
            .thenReturn(List.of(firstReservation, secondReservation));
        when(lotBalanceRepository.findByIdForUpdate(firstLot.getId())).thenReturn(Optional.of(firstLot));
        when(lotBalanceRepository.findByIdForUpdate(secondLot.getId())).thenReturn(Optional.of(secondLot));
        when(stockPoster.postOutbound(1, 2, "LOT-A", 3, "Goods Issue " + order.getOrderCode(), 9)).thenReturn(true);
        when(stockPoster.postOutbound(1, 3, "LOT-B", 2, "Goods Issue " + order.getOrderCode(), 9)).thenReturn(true);

        assertTrue(service.dispatch(7, 9));

        assertEquals(DeliveryOrderStatus.DISPATCHED, order.getStatus());
        verify(stockPoster).postOutbound(1, 2, "LOT-A", 3, "Goods Issue " + order.getOrderCode(), 9);
        verify(stockPoster).postOutbound(1, 3, "LOT-B", 2, "Goods Issue " + order.getOrderCode(), 9);
    }

    @Test
    void failedGoodsIssueDoesNotAdvanceOrderStatus() {
        DeliveryOrder order = order(DeliveryOrderStatus.LOADED, 4);
        Rak rack = new Rak(2, null, "A-01", 100, 4);
        when(orderRepository.findByIdWithLinesForUpdate(7)).thenReturn(Optional.of(order));
        when(userRepository.findById(9)).thenReturn(Optional.of(User.builder().id(9).fullName("Dispatcher").build()));
        InventoryLotBalance lot = lot(order.getLines().get(0).getBarang(), rack, "LOT-A", 4);
        DeliveryOrderLotReservation reservation = reservation(order, lot, 4);
        when(reservationRepository.findByOrderIdForUpdate(7)).thenReturn(List.of(reservation));
        when(lotBalanceRepository.findByIdForUpdate(lot.getId())).thenReturn(Optional.of(lot));
        when(stockPoster.postOutbound(1, 2, "LOT-A", 4, "Goods Issue " + order.getOrderCode(), 9)).thenReturn(false);

        assertThrows(IllegalStateException.class, () -> service.dispatch(7, 9));

        assertEquals(DeliveryOrderStatus.LOADED, order.getStatus());
        verify(orderRepository, never()).save(order);
    }

    @Test
    void packingAndLoadingRecordActualQuantitiesAndRejectOverages() {
        DeliveryOrder order = order(DeliveryOrderStatus.PICKED, 4);
        InventoryLotBalance lot = lot(order.getLines().get(0).getBarang(),
                new Rak(2, null, "A-01", 100, 4), "LOT-A", 4);
        DeliveryOrderLotReservation reservation = reservation(order, lot, 4);
        reservation.setPackedQuantity(0);
        reservation.setLoadedQuantity(0);
        when(orderRepository.findByIdWithLinesForUpdate(7)).thenReturn(Optional.of(order));
        when(reservationRepository.findByOrderIdForUpdate(7)).thenReturn(List.of(reservation));
        when(userRepository.findById(9)).thenReturn(Optional.of(User.builder().id(9).fullName("Operator").build()));

        assertFalse(service.completePacking(7, Map.of(8, 5), 9));
        assertTrue(service.completePacking(7, Map.of(8, 3), 9));
        assertEquals(DeliveryOrderStatus.PACKED, order.getStatus());
        assertEquals(3, reservation.getPackedQuantity());

        assertFalse(service.completeLoading(7, Map.of(8, 4), 9));
        assertTrue(service.completeLoading(7, Map.of(8, 2), 9));
        assertEquals(DeliveryOrderStatus.LOADED, order.getStatus());
        assertEquals(2, reservation.getLoadedQuantity());
        verify(fulfillmentAuditRepository, times(2)).save(any(DeliveryOrderFulfillmentAudit.class));
    }

    @Test
    void fulfillmentBarcodeScanMustMatchPackedReservationAndLot() {
        DeliveryOrder order = order(DeliveryOrderStatus.PACKED, 4);
        DeliveryOrderLine line = order.getLines().get(0);
        java.time.LocalDate expiry = java.time.LocalDate.of(2027, 6, 30);
        Rak rack = new Rak(2, null, "A-01", 100, 4);
        InventoryLotBalance lot = new InventoryLotBalance(line.getBarang(), rack, rack.getKodeRak(),
                "LOT-A", expiry, java.time.LocalDateTime.now(), 4);
        DeliveryOrderLotReservation reservation = reservation(order, lot, 4);
        reservation.setPickedQuantity(3);
        reservation.setPackedQuantity(2);
        reservation.setLoadedQuantity(0);
        when(orderRepository.findByIdWithLinesForUpdate(7)).thenReturn(Optional.of(order));
        when(userRepository.findById(9)).thenReturn(Optional.of(User.builder().id(9).fullName("Operator").build()));
        when(reservationRepository.findByLineIdForUpdate(line.getId())).thenReturn(List.of(reservation));
        when(reservationRepository.findByOrderIdForUpdate(7)).thenReturn(List.of(reservation));

        String itemPayload = itemQr("SKU-1", "LOT-A", expiry.toString());
        assertFalse(service.recordFulfillmentScan(7, line.getId(), "DO-WRONG", itemPayload, 1, 9));
        assertFalse(service.recordFulfillmentScan(7, line.getId(), "DO-TEST",
                itemQr("SKU-1", "OTHER", expiry.toString()), 1, 9));
        assertFalse(service.recordFulfillmentScan(7, line.getId(), "DO-TEST", itemPayload, 3, 9));
        assertTrue(service.recordFulfillmentScan(7, line.getId(), "DO-TEST", itemPayload, 2, 9));

        assertEquals(2, reservation.getLoadedQuantity());
        verify(fulfillmentAuditRepository).save(argThat(audit -> audit.getStage().equals("LOADING_SCAN")
                && audit.getVerifiedQuantity() == 2));
        assertFalse(service.completeLoading(7, Map.of(line.getId(), 1), 9));
        assertTrue(service.completeLoading(7, Map.of(line.getId(), 2), 9));
    }

    @Test
    void mobileFulfillmentRequiresDoBarcodeForPackingScans() {
        DeliveryOrder order = order(DeliveryOrderStatus.PICKED, 4);
        DeliveryOrderLine line = order.getLines().get(0);
        InventoryLotBalance lot = lot(line.getBarang(), new Rak(2, null, "A-01", 100, 4), "LOT-A", 4);
        DeliveryOrderLotReservation reservation = reservation(order, lot, 4);
        reservation.setPackedQuantity(0);
        reservation.setLoadedQuantity(0);
        when(orderRepository.findByIdWithLines(7)).thenReturn(Optional.of(order));
        when(orderRepository.findByIdWithLinesForUpdate(7)).thenReturn(Optional.of(order));
        when(userRepository.findById(9)).thenReturn(Optional.of(User.builder().id(9).fullName("Operator").build()));
        when(reservationRepository.findByLineIdForUpdate(8)).thenReturn(List.of(reservation));

        String itemPayload = itemQr("SKU-1", "LOT-A", "-");
        assertFalse(service.recordMobileFulfillmentScan(7, 8, "DO-WRONG", itemPayload, 1, 9));
        assertTrue(service.recordMobileFulfillmentScan(7, 8, "DO-TEST", itemPayload, 2, 9));

        assertEquals(2, reservation.getPackedQuantity());
        verify(fulfillmentAuditRepository).save(argThat(audit -> audit.getStage().equals("PACKING_SCAN")
                && audit.getVerifiedQuantity() == 2));
    }

    @Test
    void mobileFulfillmentDetailReportsOnlyCurrentStageAvailability() {
        DeliveryOrder order = order(DeliveryOrderStatus.PICKED, 5);
        InventoryLotBalance lot = lot(order.getLines().get(0).getBarang(),
                new Rak(2, null, "A-01", 100, 5), "LOT-A", 5);
        DeliveryOrderLotReservation reservation = reservation(order, lot, 5);
        reservation.setPackedQuantity(2);
        reservation.setLoadedQuantity(1);
        when(orderRepository.findByIdWithLines(7)).thenReturn(Optional.of(order));
        when(reservationRepository.findByOrderId(7)).thenReturn(List.of(reservation));

        var detail = service.getMobileFulfillmentOrder(7).orElseThrow();

        assertEquals("DO-TEST", detail.order().orderCode());
        assertEquals(3, detail.lines().getFirst().availableQuantity());
        assertEquals(2, detail.lines().getFirst().packedQuantity());
    }

    @Test
    void mobilePackingCompletionUsesScannedTotalsOnly() {
        DeliveryOrder order = order(DeliveryOrderStatus.PICKED, 4);
        InventoryLotBalance lot = lot(order.getLines().get(0).getBarang(),
                new Rak(2, null, "A-01", 100, 4), "LOT-A", 4);
        DeliveryOrderLotReservation reservation = reservation(order, lot, 4);
        reservation.setPackedQuantity(3);
        reservation.setLoadedQuantity(0);
        when(orderRepository.findByIdWithLinesForUpdate(7)).thenReturn(Optional.of(order));
        when(userRepository.findById(9)).thenReturn(Optional.of(User.builder().id(9).fullName("Operator").build()));
        when(reservationRepository.findByOrderIdForUpdate(7)).thenReturn(List.of(reservation));

        assertEquals(DeliveryOrderStatus.PACKED, service.completeMobileFulfillment(7, 9));
        assertEquals(3, reservation.getPackedQuantity());
        assertEquals(DeliveryOrderStatus.PACKED, order.getStatus());
    }

    @Test
    void mobilePackingCannotCompleteFromAnEarlierCycleWithoutCurrentScans() {
        DeliveryOrder order = order(DeliveryOrderStatus.PICKED, 4);
        InventoryLotBalance lot = lot(order.getLines().get(0).getBarang(),
                new Rak(2, null, "A-01", 100, 4), "LOT-A", 4);
        DeliveryOrderLotReservation reservation = reservation(order, lot, 4);
        reservation.setPackedQuantity(0);
        reservation.setLoadedQuantity(0);
        when(orderRepository.findByIdWithLinesForUpdate(7)).thenReturn(Optional.of(order));
        when(reservationRepository.findByOrderIdForUpdate(7)).thenReturn(List.of(reservation));

        assertNull(service.completeMobileFulfillment(7, 9));
        assertEquals(DeliveryOrderStatus.PICKED, order.getStatus());
        verify(fulfillmentAuditRepository, never()).findByOrderId(7);
    }

    @Test
    void mobileDispatchReturnsTheActualPartialDispatchStatus() {
        DeliveryOrder order = order(DeliveryOrderStatus.LOADED, 5);
        Rak rack = new Rak(2, null, "A-01", 100, 5);
        InventoryLotBalance lot = lot(order.getLines().get(0).getBarang(), rack, "LOT-A", 5);
        DeliveryOrderLotReservation reservation = reservation(order, lot, 5);
        reservation.setPackedQuantity(3);
        reservation.setLoadedQuantity(3);
        when(orderRepository.findByIdWithLinesForUpdate(7)).thenReturn(Optional.of(order));
        when(userRepository.findById(9)).thenReturn(Optional.of(User.builder().id(9).fullName("Dispatcher").build()));
        when(reservationRepository.findByOrderIdForUpdate(7)).thenReturn(List.of(reservation));
        when(lotBalanceRepository.findByIdForUpdate(lot.getId())).thenReturn(Optional.of(lot));
        when(stockPoster.postOutbound(1, 2, "LOT-A", 3, "Goods Issue " + order.getOrderCode(), 9))
                .thenReturn(true);
        when(orderRepository.findStatusById(7)).thenReturn(Optional.of(DeliveryOrderStatus.PARTIALLY_DISPATCHED));

        assertEquals(DeliveryOrderStatus.PARTIALLY_DISPATCHED, service.dispatchMobileFulfillment(7, 9));
        assertEquals(DeliveryOrderStatus.PARTIALLY_DISPATCHED, order.getStatus());
    }

    @Test
    void partialDispatchKeepsOutstandingQuantityOnSameOrderForNextCycle() {
        DeliveryOrder order = order(DeliveryOrderStatus.LOADED, 5);
        Rak rack = new Rak(2, null, "A-01", 100, 5);
        InventoryLotBalance lot = lot(order.getLines().get(0).getBarang(), rack, "LOT-A", 5);
        DeliveryOrderLotReservation reservation = reservation(order, lot, 5);
        reservation.setPackedQuantity(3);
        reservation.setLoadedQuantity(3);
        when(orderRepository.findByIdWithLinesForUpdate(7)).thenReturn(Optional.of(order));
        when(userRepository.findById(9)).thenReturn(Optional.of(User.builder().id(9).fullName("Dispatcher").build()));
        when(reservationRepository.findByOrderIdForUpdate(7)).thenReturn(List.of(reservation));
        when(lotBalanceRepository.findByIdForUpdate(lot.getId())).thenReturn(Optional.of(lot));
        when(stockPoster.postOutbound(1, 2, "LOT-A", 3, "Goods Issue " + order.getOrderCode(), 9))
                .thenReturn(true);

        assertTrue(service.dispatch(7, 9));

        assertEquals(DeliveryOrderStatus.PARTIALLY_DISPATCHED, order.getStatus());
        assertEquals(3, order.getLines().get(0).getShippedQuantity());
        when(orderRepository.findByIdWithLinesForUpdate(7)).thenReturn(Optional.of(order));
        when(barangRepository.findByIdForUpdate(1)).thenReturn(Optional.of(order.getLines().get(0).getBarang()));
        when(lotBalanceRepository.findAvailableFefoAcrossLocationsForUpdate(1, java.time.LocalDate.now()))
                .thenReturn(List.of(lot));

        assertTrue(service.releaseToPicking(7));

        assertEquals(DeliveryOrderStatus.READY_TO_PICK, order.getStatus());
        verify(reservationRepository).save(argThat(nextReservation ->
                nextReservation.getReservedQuantity() == 2));
    }

    private DeliveryOrder order(DeliveryOrderStatus status, int quantity) {
        Customer customer = new Customer(2, "Customer", null, null, null, null, "Aktif");
        User creator = User.builder().id(3).fullName("Creator").build();
        Barang item = Barang.builder().id(1).kodeBarang("SKU-1").namaBarang("Item").build();
        DeliveryOrder order = new DeliveryOrder("DO-TEST", customer, creator, null,
                java.time.LocalDateTime.now());
        ReflectionTestUtils.setField(order, "id", 7);
        order.addLine(new DeliveryOrderLine(item, quantity));
        ReflectionTestUtils.setField(order.getLines().get(0), "id", 8);
        order.setStatus(status);
        return order;
    }

    private InventoryLotBalance lot(Barang item, Rak rack, String batch, int quantity) {
        InventoryLotBalance lot = new InventoryLotBalance(item, rack, "RAK-" + rack.getId(), batch,
                null, java.time.LocalDateTime.now(), quantity);
        ReflectionTestUtils.setField(lot, "id", batch.endsWith("A") ? 101 : 102);
        lot.setReservedQuantity(0);
        return lot;
    }

    private DeliveryOrderLotReservation reservation(DeliveryOrder order, InventoryLotBalance lot, int quantity) {
        lot.setReservedQuantity(lot.getReservedQuantity() + quantity);
        DeliveryOrderLotReservation reservation = new DeliveryOrderLotReservation(
                order.getLines().get(0), lot, quantity);
        reservation.setPickedQuantity(quantity);
        reservation.setPackedQuantity(quantity);
        reservation.setLoadedQuantity(quantity);
        return reservation;
    }

    private PickTask activeTask(DeliveryOrder order, User picker) {
        PickTask task = new PickTask("PICK-TEST", order, LocalDateTime.now());
        task.assign(picker, pickingSection(), LocalDateTime.now());
        task.setStatus(PickTaskStatus.IN_PROGRESS);
        return task;
    }

    private Section pickingSection() {
        return new Section(5, "SEC-5", "Zone 5", 12);
    }

    private String itemQr(String sku, String batch, String expiry) {
        return "WMS1|" + encode(sku) + "|" + encode(batch) + "|" + expiry;
    }

    private String locationBarcode(String code) {
        return "SLOC1|" + encode(code);
    }

    private String encode(String value) {
        return Base64.getUrlEncoder().withoutPadding()
                .encodeToString(value.getBytes(StandardCharsets.UTF_8));
    }
}
