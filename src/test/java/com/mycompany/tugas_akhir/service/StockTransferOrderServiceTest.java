package com.mycompany.tugas_akhir.service;

import com.mycompany.tugas_akhir.model.*;
import com.mycompany.tugas_akhir.repository.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class StockTransferOrderServiceTest {

    @Mock private StockTransferOrderRepository orderRepository;
    @Mock private WarehouseRepository warehouseRepository;
    @Mock private RakRepository rakRepository;
    @Mock private BarangRepository barangRepository;
    @Mock private UserRepository userRepository;
    @Mock private InventoryLotBalanceRepository lotBalanceRepository;
    @Mock private OutboundStockPoster outboundStockPoster;
    @Mock private InboundStockPoster inboundStockPoster;
    @Mock private StockTransferOrderScanRepository scanRepository;
    @Mock private ApprovalRouter approvalRoutingService;
    private StockTransferOrderService service;

    @BeforeEach
    void setUp() {
        service = new StockTransferOrderService(orderRepository, warehouseRepository, rakRepository,
                barangRepository, userRepository, lotBalanceRepository,
                outboundStockPoster, inboundStockPoster,
                new InventoryPickingPolicy(lotBalanceRepository, "FEFO"), scanRepository, approvalRoutingService);
        lenient().when(approvalRoutingService.canReview(any(), any())).thenReturn(true);
    }

    @Test
    void planRequiresRacksBelongingToDifferentWarehouses() {
        Warehouse sourceWarehouse = warehouse(1, "WH-A");
        Warehouse destinationWarehouse = warehouse(2, "WH-B");
        Rak sourceRack = rack(11, "A-01", sourceWarehouse, 2);
        Rak destinationRack = rack(21, "B-01", sourceWarehouse, 0);
        when(warehouseRepository.findById(1)).thenReturn(Optional.of(sourceWarehouse));
        when(warehouseRepository.findById(2)).thenReturn(Optional.of(destinationWarehouse));
        when(rakRepository.findById(11)).thenReturn(Optional.of(sourceRack));
        when(rakRepository.findById(21)).thenReturn(Optional.of(destinationRack));
        when(barangRepository.findById(5)).thenReturn(Optional.of(item()));
        when(userRepository.findById(3)).thenReturn(Optional.of(user(3)));

        assertNull(service.createPlan(1, 2, 11, 21, 5, 4, "Replenish", 3));
        verify(orderRepository, never()).save(any());
    }

    @Test
    void dispatchAndReceivePreserveBatchAndExpiryAcrossWarehouses() {
        Warehouse sourceWarehouse = warehouse(1, "WH-A");
        Warehouse destinationWarehouse = warehouse(2, "WH-B");
        StockTransferOrder order = order(sourceWarehouse, destinationWarehouse);
        order.review(user(4), true, null, LocalDateTime.now());
        ReflectionTestUtils.setField(order, "id", 8);
        Rak sourceRack = order.getLines().get(0).getSourceRak();
        InventoryLotBalance lot = new InventoryLotBalance(item(), sourceRack, "RAK-11",
                "BATCH-1", LocalDate.of(2027, 4, 1), LocalDateTime.now(), 5);
        lot.setReservedQuantity(1);
        when(orderRepository.findByIdWithLinesForUpdate(8)).thenReturn(Optional.of(order));
        when(userRepository.findById(4)).thenReturn(Optional.of(user(4)));
        when(lotBalanceRepository.findAvailableFefoForUpdate(5, "RAK-11", LocalDate.now()))
                .thenReturn(List.of(lot));
        when(outboundStockPoster.postOutbound(5, 11, "BATCH-1", 4,
                "STO Dispatch " + order.getOrderCode(), 4)).thenReturn(true);

        assertTrue(service.dispatch(8, 4));
        assertEquals(StockTransferOrderStatus.DISPATCHED, order.getStatus());
        assertEquals(1, order.getLines().get(0).getLots().size());

        when(inboundStockPoster.postInbound(5, null, 21, "BATCH-1", LocalDate.of(2027, 4, 1),
                4, "STO In " + order.getOrderCode(), 4)).thenReturn(true);
        assertTrue(service.receive(8, 4));
        assertEquals(StockTransferOrderStatus.RECEIVED, order.getStatus());
        verify(inboundStockPoster).postInbound(5, null, 21, "BATCH-1", LocalDate.of(2027, 4, 1),
                4, "STO In " + order.getOrderCode(), 4);
    }

    @Test
    void pickerCanOnlyBeAssignedToAnApprovedUnscannedOrder() {
        StockTransferOrder order = order(warehouse(1, "WH-A"), warehouse(2, "WH-B"));
        ReflectionTestUtils.setField(order, "id", 8);
        when(orderRepository.findByIdWithLinesForUpdate(8)).thenReturn(Optional.of(order));
        when(userRepository.findById(4)).thenReturn(Optional.of(user(4)));

        assertFalse(service.assignPicker(8, 4));

        order.review(user(3), true, null, LocalDateTime.now());
        when(scanRepository.findByOrderId(8)).thenReturn(List.of());
        assertTrue(service.assignPicker(8, 4));
        assertEquals(4, order.getAssignedPicker().getId());
        verify(orderRepository).save(order);
    }

    @Test
    void scannedDispatchAllowsPartialPickOnlyForAssignedPickerAndMatchingSourceLot() {
        Warehouse sourceWarehouse = warehouse(1, "WH-A");
        Warehouse destinationWarehouse = warehouse(2, "WH-B");
        StockTransferOrder order = order(sourceWarehouse, destinationWarehouse);
        order.review(user(4), true, null, LocalDateTime.now());
        ReflectionTestUtils.setField(order, "id", 8);
        ReflectionTestUtils.setField(order.getLines().get(0), "id", 18);
        Rak sourceRack = order.getLines().get(0).getSourceRak();
        order.assignPicker(user(4), LocalDateTime.now());
        InventoryLotBalance lot = new InventoryLotBalance(item(), sourceRack, "RAK-11",
                "BATCH-1", LocalDate.of(2027, 4, 1), LocalDateTime.now(), 5);
        when(orderRepository.findByIdWithLinesForUpdate(8)).thenReturn(Optional.of(order));
        when(userRepository.findById(4)).thenReturn(Optional.of(user(4)));
        when(userRepository.findById(5)).thenReturn(Optional.of(user(5)));
        when(scanRepository.findByOrderId(8)).thenReturn(List.of());
        when(lotBalanceRepository.findAvailableFefoForUpdate(5, "RAK-11", LocalDate.now()))
                .thenReturn(List.of(lot));
        when(lotBalanceRepository.findByBarangIdAndLocationKeyAndBatchCodeForUpdate(5, "RAK-11", "BATCH-1"))
                .thenReturn(Optional.of(lot));
        when(outboundStockPoster.postOutbound(5, 11, "BATCH-1", 2,
                "STO Dispatch " + order.getOrderCode(), 4)).thenReturn(true);

        assertFalse(service.recordDispatchScan(8, 5, itemQr("SKU-5", "BATCH-1", "2027-04-01"),
                locationBarcode("A-01"), 2));
        assertTrue(service.recordDispatchScan(8, 4, itemQr("SKU-5", "BATCH-1", "2027-04-01"),
                locationBarcode("A-01"), 2));
        assertEquals(StockTransferOrderStatus.PARTIALLY_DISPATCHED, order.getStatus());
        verify(outboundStockPoster).postOutbound(5, 11, "BATCH-1", 2,
                "STO Dispatch " + order.getOrderCode(), 4);
        verify(scanRepository).save(argThat(scan -> scan.getScanType() == StockTransferOrderScanType.PICK
                && scan.getQuantity() == 2 && scan.getPicker().getId().equals(4)));

        StockTransferOrderScan firstPick = new StockTransferOrderScan(order, order.getLines().get(0), user(4),
                lot, null, StockTransferOrderScanType.PICK, "SKU-5", "A-01", "BATCH-1",
                LocalDate.of(2027, 4, 1), 2, LocalDateTime.now());
        ReflectionTestUtils.setField(firstPick, "id", 31);
        when(orderRepository.findByIdWithLinesForUpdate(8)).thenReturn(Optional.of(order));
        when(scanRepository.findByOrderId(8)).thenReturn(List.of(firstPick));
        assertTrue(service.recordDispatchScan(8, 4, itemQr("SKU-5", "BATCH-1", "2027-04-01"),
                locationBarcode("A-01"), 2));
        assertEquals(StockTransferOrderStatus.DISPATCHED, order.getStatus());
        verify(outboundStockPoster, times(2)).postOutbound(5, 11, "BATCH-1", 2,
                "STO Dispatch " + order.getOrderCode(), 4);
    }

    @Test
    void scannedReceiptSupportsPartialReceiptWithoutExceedingDispatchedQuantity() {
        Warehouse sourceWarehouse = warehouse(1, "WH-A");
        Warehouse destinationWarehouse = warehouse(2, "WH-B");
        StockTransferOrder order = order(sourceWarehouse, destinationWarehouse);
        order.review(user(4), true, null, LocalDateTime.now());
        order.markDispatched(LocalDateTime.now());
        ReflectionTestUtils.setField(order, "id", 8);
        StockTransferOrderLine line = order.getLines().get(0);
        ReflectionTestUtils.setField(line, "id", 18);
        StockTransferOrderScan pick = new StockTransferOrderScan(order, line, user(4), null, null,
                StockTransferOrderScanType.PICK, "SKU-5", "A-01", "BATCH-1",
                LocalDate.of(2027, 4, 1), 4, LocalDateTime.now());
        ReflectionTestUtils.setField(pick, "id", 25);
        when(orderRepository.findByIdWithLinesForUpdate(8)).thenReturn(Optional.of(order));
        when(userRepository.findById(6)).thenReturn(Optional.of(user(6)));
        when(scanRepository.findByOrderId(8)).thenReturn(List.of(pick));
        when(inboundStockPoster.postInbound(5, null, 21, "BATCH-1", LocalDate.of(2027, 4, 1),
                2, "STO In " + order.getOrderCode(), 6)).thenReturn(true);

        assertFalse(service.recordReceiptScan(8, 6, itemQr("SKU-5", "BATCH-1", "2027-04-01"),
                locationBarcode("B-01"), 5));
        verifyNoInteractions(inboundStockPoster);
        assertTrue(service.recordReceiptScan(8, 6, itemQr("SKU-5", "BATCH-1", "2027-04-01"),
                locationBarcode("B-01"), 2));
        assertEquals(StockTransferOrderStatus.PARTIALLY_RECEIVED, order.getStatus());
        verify(inboundStockPoster).postInbound(5, null, 21, "BATCH-1", LocalDate.of(2027, 4, 1),
                2, "STO In " + order.getOrderCode(), 6);
        verify(scanRepository).save(argThat(scan -> scan.getScanType() == StockTransferOrderScanType.RECEIVE
                && scan.getSourceScan() == pick && scan.getQuantity() == 2));

        StockTransferOrderScan firstReceipt = new StockTransferOrderScan(order, line, user(6), null, pick,
                StockTransferOrderScanType.RECEIVE, "SKU-5", "B-01", "BATCH-1",
                LocalDate.of(2027, 4, 1), 2, LocalDateTime.now());
        ReflectionTestUtils.setField(firstReceipt, "id", 26);
        when(scanRepository.findByOrderId(8)).thenReturn(List.of(pick, firstReceipt));
        assertTrue(service.recordReceiptScan(8, 6, itemQr("SKU-5", "BATCH-1", "2027-04-01"),
                locationBarcode("B-01"), 2));
        assertEquals(StockTransferOrderStatus.RECEIVED, order.getStatus());
        verify(inboundStockPoster, times(2)).postInbound(5, null, 21, "BATCH-1", LocalDate.of(2027, 4, 1),
                2, "STO In " + order.getOrderCode(), 6);
    }

    @Test
    void partialDispatchAndReceiptCanProgressIndependently() {
        Warehouse sourceWarehouse = warehouse(1, "WH-A");
        Warehouse destinationWarehouse = warehouse(2, "WH-B");
        StockTransferOrder order = order(sourceWarehouse, destinationWarehouse);
        order.review(user(4), true, null, LocalDateTime.now());
        order.assignPicker(user(4), LocalDateTime.now());
        ReflectionTestUtils.setField(order, "id", 8);
        StockTransferOrderLine line = order.getLines().get(0);
        ReflectionTestUtils.setField(line, "id", 18);
        Rak sourceRack = line.getSourceRak();
        InventoryLotBalance lot = new InventoryLotBalance(item(), sourceRack, "RAK-11",
                "BATCH-1", LocalDate.of(2027, 4, 1), LocalDateTime.now(), 4);
        when(orderRepository.findByIdWithLinesForUpdate(8)).thenReturn(Optional.of(order));
        when(userRepository.findById(4)).thenReturn(Optional.of(user(4)));
        when(userRepository.findById(6)).thenReturn(Optional.of(user(6)));
        when(scanRepository.findByOrderId(8)).thenReturn(List.of());
        when(lotBalanceRepository.findAvailableFefoForUpdate(5, "RAK-11", LocalDate.now()))
                .thenReturn(List.of(lot));
        when(lotBalanceRepository.findByBarangIdAndLocationKeyAndBatchCodeForUpdate(5, "RAK-11", "BATCH-1"))
                .thenReturn(Optional.of(lot));
        when(outboundStockPoster.postOutbound(5, 11, "BATCH-1", 2,
                "STO Dispatch " + order.getOrderCode(), 4)).thenReturn(true);
        when(inboundStockPoster.postInbound(5, null, 21, "BATCH-1", LocalDate.of(2027, 4, 1),
                1, "STO In " + order.getOrderCode(), 6)).thenReturn(true);
        when(inboundStockPoster.postInbound(5, null, 21, "BATCH-1", LocalDate.of(2027, 4, 1),
                2, "STO In " + order.getOrderCode(), 6)).thenReturn(true);

        assertTrue(service.recordDispatchScan(8, 4, itemQr("SKU-5", "BATCH-1", "2027-04-01"),
                locationBarcode("A-01"), 2));
        assertEquals(StockTransferOrderStatus.PARTIALLY_DISPATCHED, order.getStatus());

        StockTransferOrderScan firstPick = new StockTransferOrderScan(order, line, user(4), lot, null,
                StockTransferOrderScanType.PICK, "SKU-5", "A-01", "BATCH-1",
                LocalDate.of(2027, 4, 1), 2, LocalDateTime.now());
        ReflectionTestUtils.setField(firstPick, "id", 31);
        when(scanRepository.findByOrderId(8)).thenReturn(List.of(firstPick));
        assertTrue(service.recordReceiptScan(8, 6, itemQr("SKU-5", "BATCH-1", "2027-04-01"),
                locationBarcode("B-01"), 1));
        assertEquals(StockTransferOrderStatus.PARTIALLY_DISPATCHED_AND_RECEIVED, order.getStatus());

        StockTransferOrderScan firstReceipt = new StockTransferOrderScan(order, line, user(6), null, firstPick,
                StockTransferOrderScanType.RECEIVE, "SKU-5", "B-01", "BATCH-1",
                LocalDate.of(2027, 4, 1), 1, LocalDateTime.now());
        ReflectionTestUtils.setField(firstReceipt, "id", 32);
        when(scanRepository.findByOrderId(8)).thenReturn(List.of(firstPick, firstReceipt));
        assertTrue(service.recordDispatchScan(8, 4, itemQr("SKU-5", "BATCH-1", "2027-04-01"),
                locationBarcode("A-01"), 2));
        assertEquals(StockTransferOrderStatus.PARTIALLY_RECEIVED, order.getStatus());

        StockTransferOrderScan secondPick = new StockTransferOrderScan(order, line, user(4), lot, null,
                StockTransferOrderScanType.PICK, "SKU-5", "A-01", "BATCH-1",
                LocalDate.of(2027, 4, 1), 2, LocalDateTime.now());
        ReflectionTestUtils.setField(secondPick, "id", 33);
        when(scanRepository.findByOrderId(8)).thenReturn(List.of(firstPick, firstReceipt, secondPick));
        assertTrue(service.recordReceiptScan(8, 6, itemQr("SKU-5", "BATCH-1", "2027-04-01"),
                locationBarcode("B-01"), 3));
        assertEquals(StockTransferOrderStatus.RECEIVED, order.getStatus());
    }

    private StockTransferOrder order(Warehouse sourceWarehouse, Warehouse destinationWarehouse) {
        Rak source = rack(11, "A-01", sourceWarehouse, 5);
        Rak destination = rack(21, "B-01", destinationWarehouse, 0);
        StockTransferOrder order = new StockTransferOrder("STO-TEST", sourceWarehouse,
                destinationWarehouse, user(3), "Replenish", LocalDateTime.now());
        order.addLine(new StockTransferOrderLine(item(), source, destination, 4));
        return order;
    }

    private Warehouse warehouse(int id, String code) {
        Warehouse warehouse = new Warehouse(code, "Warehouse " + code, null);
        ReflectionTestUtils.setField(warehouse, "id", id);
        return warehouse;
    }

    private Rak rack(int id, String code, Warehouse warehouse, int occupied) {
        Section section = new Section(id, "SEC-" + id, code, 10);
        section.setWarehouse(warehouse);
        return new Rak(id, section, code, 20, occupied);
    }

    private Barang item() { return Barang.builder().id(5).kodeBarang("SKU-5").namaBarang("Item").build(); }
    private User user(int id) { return User.builder().id(id).fullName("User " + id).build(); }

    private String itemQr(String sku, String batch, String expiry) {
        return "WMS1|" + encode(sku) + "|" + encode(batch) + "|" + expiry;
    }

    private String locationBarcode(String rackCode) {
        return "SLOC1|" + encode(rackCode);
    }

    private String encode(String value) {
        return Base64.getUrlEncoder().withoutPadding()
                .encodeToString(value.getBytes(StandardCharsets.UTF_8));
    }
}
