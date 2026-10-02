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

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
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

    private DeliveryOrderService service;

    @BeforeEach
    void setUp() {
        service = new DeliveryOrderService(orderRepository, customerRepository, barangRepository,
            userRepository, stockPoster, lotBalanceRepository, reservationRepository);
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
    void releaseRequiresEnoughStockInMappedLocations() {
        DeliveryOrder order = order(DeliveryOrderStatus.DRAFT, 4);
        Rak rack = new Rak(2, null, "A-01", 100, 3);
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
    }

    @Test
    void scanRejectsMismatchedSkuOrLocation() {
        DeliveryOrder order = order(DeliveryOrderStatus.PICKING, 3);
        Rak rack = new Rak(2, null, "A-01", 100, 3);
        InventoryLotBalance lot = lot(order.getLines().get(0).getBarang(), rack, "LOT-A", 3);
        DeliveryOrderLotReservation reservation = new DeliveryOrderLotReservation(order.getLines().get(0), lot, 3);
        User picker = User.builder().id(9).fullName("Picker").build();
        when(orderRepository.findByIdWithLinesForUpdate(7)).thenReturn(Optional.of(order));
        when(userRepository.findById(9)).thenReturn(Optional.of(picker));
        when(reservationRepository.findByLineIdForUpdate(order.getLines().get(0).getId()))
                .thenReturn(List.of(reservation));

        assertFalse(service.recordPick(7, order.getLines().get(0).getId(), "WRONG-SKU", "A-01", "LOT-A", 1, 9));
        assertFalse(service.recordPick(7, order.getLines().get(0).getId(), "SKU-1", "B-99", "LOT-A", 1, 9));
        assertEquals(0, reservation.getPickedQuantity());
        verify(reservationRepository, never()).save(any());
    }

    @Test
    void validScanUpdatesPickedQuantityAndPreventsOverPick() {
        DeliveryOrder order = order(DeliveryOrderStatus.PICKING, 3);
        DeliveryOrderLine line = order.getLines().get(0);
        Rak rack = new Rak(2, null, "A-01", 100, 3);
        InventoryLotBalance lot = lot(line.getBarang(), rack, "LOT-A", 3);
        lot.setReservedQuantity(3);
        DeliveryOrderLotReservation reservation = new DeliveryOrderLotReservation(line, lot, 3);
        User picker = User.builder().id(9).fullName("Picker").build();
        when(orderRepository.findByIdWithLinesForUpdate(7)).thenReturn(Optional.of(order));
        when(userRepository.findById(9)).thenReturn(Optional.of(picker));
        when(reservationRepository.findByLineIdForUpdate(line.getId())).thenReturn(List.of(reservation));

        assertTrue(service.recordPick(7, line.getId(), "SKU-1", "A-01", "LOT-A", 2, 9));
        assertFalse(service.recordPick(7, line.getId(), "SKU-1", "A-01", "LOT-A", 2, 9));

        assertEquals(2, reservation.getPickedQuantity());
        verify(reservationRepository, times(1)).save(reservation);
    }

    @Test
    void pickingCannotCompleteUntilEveryLineIsFullyScanned() {
        DeliveryOrder order = order(DeliveryOrderStatus.PICKING, 3);
        DeliveryOrderLine line = order.getLines().get(0);
        Rak rack = new Rak(2, null, "A-01", 100, 3);
        InventoryLotBalance lot = lot(line.getBarang(), rack, "LOT-A", 3);
        DeliveryOrderLotReservation reservation = reservation(order, lot, 3);
        reservation.setPickedQuantity(2);
        when(orderRepository.findByIdWithLinesForUpdate(7)).thenReturn(Optional.of(order));
        when(reservationRepository.findByOrderIdForUpdate(7)).thenReturn(List.of(reservation));

        assertFalse(service.completePicking(7));
        reservation.setPickedQuantity(3);
        assertTrue(service.completePicking(7));
        assertEquals(DeliveryOrderStatus.PICKED, order.getStatus());
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
        when(stockPoster.postOutbound(1, 2, "LOT-A", 4, "Goods Issue " + order.getOrderCode(), 9)).thenReturn(false);

        assertThrows(IllegalStateException.class, () -> service.dispatch(7, 9));

        assertEquals(DeliveryOrderStatus.LOADED, order.getStatus());
        verify(orderRepository, never()).save(order);
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
        lot.setReservedQuantity(0);
        return lot;
    }

    private DeliveryOrderLotReservation reservation(DeliveryOrder order, InventoryLotBalance lot, int quantity) {
        lot.setReservedQuantity(lot.getReservedQuantity() + quantity);
        DeliveryOrderLotReservation reservation = new DeliveryOrderLotReservation(
                order.getLines().get(0), lot, quantity);
        reservation.setPickedQuantity(quantity);
        return reservation;
    }
}