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
class ReturnRequestServiceTest {

    @Mock private ReturnRequestRepository returnRepository;
    @Mock private PurchaseOrderRepository purchaseOrderRepository;
    @Mock private DeliveryOrderLineRepository deliveryOrderLineRepository;
    @Mock private InventoryLotBalanceRepository lotBalanceRepository;
    @Mock private RakRepository rakRepository;
    @Mock private UserRepository userRepository;
    @Mock private InboundStockPoster inboundStockPoster;
    @Mock private OutboundStockPoster outboundStockPoster;
    @Mock private ApprovalRouter approvalRoutingService;
    @Mock private WarehouseZoneRuleService warehouseZoneRuleService;
    private ReturnRequestService service;

    @BeforeEach
    void setUp() {
        service = new ReturnRequestService(returnRepository, purchaseOrderRepository,
                deliveryOrderLineRepository, lotBalanceRepository,
                rakRepository, userRepository, inboundStockPoster, outboundStockPoster, approvalRoutingService,
                warehouseZoneRuleService);
        lenient().when(approvalRoutingService.canReview(any(), any())).thenReturn(true);
    }

    @Test
    void purchaseReturnMustReferenceReceivedPoAndCannotExceedReceivedQuantity() {
        PurchaseOrderLine source = purchaseLine();
        when(purchaseOrderRepository.findLineByIdForUpdate(8)).thenReturn(Optional.of(source));
        when(rakRepository.findById(2)).thenReturn(Optional.of(rack()));
        when(userRepository.findById(3)).thenReturn(Optional.of(user(3)));
        when(returnRepository.sumPurchaseReturnQuantity(eq(8), any())).thenReturn(3L);

        assertNull(service.createPurchaseReturn(8, 2, "LOT-1", 3, "Damaged on arrival", 3));
        verify(returnRepository, never()).save(any());
    }

    @Test
    void createsMultiLinePurchaseReturnFromOnePurchaseOrder() {
        PurchaseOrderLine first = purchaseLine();
        PurchaseOrderLine second = purchaseLine();
        ReflectionTestUtils.setField(second, "id", 10);
        when(purchaseOrderRepository.findLineByIdForUpdate(8)).thenReturn(Optional.of(first));
        when(purchaseOrderRepository.findLineByIdForUpdate(10)).thenReturn(Optional.of(second));
        when(rakRepository.findById(2)).thenReturn(Optional.of(rack()));
        when(userRepository.findById(3)).thenReturn(Optional.of(user(3)));
        when(returnRepository.sumPurchaseReturnQuantity(anyInt(), any())).thenReturn(0L);
        when(returnRepository.save(any(ReturnRequest.class))).thenAnswer(invocation -> invocation.getArgument(0));

        ReturnRequest request = service.createPurchaseReturn(List.of(
                returnLineInput(8, 2, "LOT-1", null, 2),
                returnLineInput(10, 2, "LOT-2", null, 3)), "Damaged items", 3);

        assertNotNull(request);
        assertEquals(2, request.getLines().size());
        assertEquals(List.of("LOT-1", "LOT-2"), request.getLines().stream()
                .map(ReturnRequestLine::getBatchCode).toList());
    }

    @Test
    void multiLinePurchaseReturnCountsRepeatedSourceLineBeforeSaving() {
        PurchaseOrderLine source = purchaseLine();
        when(purchaseOrderRepository.findLineByIdForUpdate(8)).thenReturn(Optional.of(source));
        when(rakRepository.findById(2)).thenReturn(Optional.of(rack()));
        when(userRepository.findById(3)).thenReturn(Optional.of(user(3)));
        when(returnRepository.sumPurchaseReturnQuantity(eq(8), any())).thenReturn(4L);

        ReturnRequest request = service.createPurchaseReturn(List.of(
                returnLineInput(8, 2, "LOT-1", null, 1),
                returnLineInput(8, 2, "LOT-2", null, 1)), "Damaged items", 3);

        assertNull(request);
        verify(returnRepository, never()).save(any());
    }

    @Test
    void createsMultiLineSalesReturnFromOneDeliveryOrder() {
        DeliveryOrderLine first = deliveryLine();
        DeliveryOrderLine second = deliveryLine();
        ReflectionTestUtils.setField(second, "id", 10);
        when(deliveryOrderLineRepository.findByIdForUpdate(9)).thenReturn(Optional.of(first));
        when(deliveryOrderLineRepository.findByIdForUpdate(10)).thenReturn(Optional.of(second));
        when(rakRepository.findById(2)).thenReturn(Optional.of(rack()));
        when(userRepository.findById(3)).thenReturn(Optional.of(user(3)));
        when(returnRepository.sumSalesReturnQuantity(anyInt(), any())).thenReturn(0L);
        when(returnRepository.save(any(ReturnRequest.class))).thenAnswer(invocation -> invocation.getArgument(0));

        ReturnRequest request = service.createSalesReturn(List.of(
                returnLineInput(9, 2, "RET-LOT-1", LocalDate.now().plusDays(30), 1),
                returnLineInput(10, 2, "RET-LOT-2", LocalDate.now().plusDays(60), 2)),
                "Customer return", 3);

        assertNotNull(request);
        assertEquals(2, request.getLines().size());
        assertEquals(3, request.getLines().stream().mapToInt(ReturnRequestLine::getQuantity).sum());
    }

    @Test
    void purchaseReturnRequiresSourceZoneFromConfiguredPicklist() {
        PurchaseOrderLine source = purchaseLine();
        Rak sourceRack = rack();
        Section section = new Section(4, "SEC-4", "Section 4", 10);
        section.setZoneCode("BULK");
        sourceRack.setSection(section);
        when(purchaseOrderRepository.findLineByIdForUpdate(8)).thenReturn(Optional.of(source));
        when(rakRepository.findById(2)).thenReturn(Optional.of(sourceRack));
        when(userRepository.findById(3)).thenReturn(Optional.of(user(3)));
        when(warehouseZoneRuleService.getAllowedZones(WarehouseZoneRule.Operation.PURCHASE_RETURN_PICKLIST_ZONE))
                .thenReturn(java.util.Set.of("PICK"));

        assertNull(service.createPurchaseReturn(8, 2, "LOT-1", 1, "Damaged", 3));
        verify(returnRepository, never()).save(any());
    }

    @Test
    void salesReturnRequiresDispatchedSourceAndAvailableShippedQuantity() {
        DeliveryOrderLine source = deliveryLine();
        when(deliveryOrderLineRepository.findByIdForUpdate(9)).thenReturn(Optional.of(source));
        when(rakRepository.findById(2)).thenReturn(Optional.of(rack()));
        when(userRepository.findById(3)).thenReturn(Optional.of(user(3)));
        when(returnRepository.sumSalesReturnQuantity(eq(9), any())).thenReturn(2L);
        when(returnRepository.save(any(ReturnRequest.class))).thenAnswer(invocation -> invocation.getArgument(0));

        ReturnRequest request = service.createSalesReturn(9, 2, "RETURN-LOT", LocalDate.now().plusDays(30),
                1, "Customer returned item", 3);

        assertNotNull(request);
        assertEquals(ReturnType.SALES, request.getReturnType());
        assertEquals(ReturnStatus.PENDING_REVIEW, request.getStatus());
    }

    @Test
    void scannedPurchaseReturnRequiresMatchingSkuBatchExpiryAndSourceSloc() {
        PurchaseOrderLine source = purchaseLine();
        LocalDate expiry = LocalDate.now().plusDays(30);
        InventoryLotBalance lot = new InventoryLotBalance(item(), rack(), "RAK-2", "LOT-1",
                expiry, LocalDateTime.now().minusDays(2), 5);
        when(purchaseOrderRepository.findLineByIdForUpdate(8)).thenReturn(Optional.of(source));
        when(rakRepository.findByKodeRak("A-01")).thenReturn(Optional.of(rack()));
        when(rakRepository.findById(2)).thenReturn(Optional.of(rack()));
        when(lotBalanceRepository.findByBarangIdAndLocationKeyAndBatchCodeForUpdate(1, "RAK-2", "LOT-1"))
                .thenReturn(Optional.of(lot));
        when(userRepository.findById(3)).thenReturn(Optional.of(user(3)));
        when(returnRepository.sumPurchaseReturnQuantity(eq(8), any())).thenReturn(0L);
        when(returnRepository.save(any(ReturnRequest.class))).thenAnswer(invocation -> invocation.getArgument(0));

        ReturnRequest request = service.createPurchaseReturnWithBarcodes(8, itemQr("SKU-1", "LOT-1", expiry),
                locationQr("A-01"), 2, "Damaged item", 3);

        assertNotNull(request);
        assertEquals("LOT-1", request.getLines().get(0).getBatchCode());
        assertEquals("A-01", request.getLines().get(0).getRak().getKodeRak());
    }

    @Test
    void postsApprovedSalesReturnAsInboundLedgerTransaction() {
        ReturnRequest request = salesReturn();
        request.review(user(4), true, null, LocalDateTime.now());
        DeliveryOrder sourceOrder = dispatchedOrder();
        when(returnRepository.findByIdWithLinesForUpdate(15)).thenReturn(Optional.of(request));
        when(userRepository.findById(4)).thenReturn(Optional.of(user(4)));
        when(deliveryOrderLineRepository.findByIdForUpdate(9)).thenReturn(Optional.of(request
                .getLines().get(0).getDeliveryOrderLine()));
        when(returnRepository.sumSalesReturnQuantity(eq(9), any())).thenReturn(2L);
        when(inboundStockPoster.postInbound(1, null, 2, "RETURN-LOT", LocalDate.now().plusDays(30),
                1, "Sales Return " + request.getReturnCode() + " dari DO " + sourceOrder.getOrderCode(), 4))
                .thenReturn(true);

        assertTrue(service.postApprovedReturn(15, 4));
        assertEquals(ReturnStatus.POSTED, request.getStatus());
        verify(inboundStockPoster).postInbound(eq(1), isNull(), eq(2), eq("RETURN-LOT"),
                eq(LocalDate.now().plusDays(30)), eq(1), any(), eq(4));
    }

    @Test
    void scannedApprovedSalesReturnRequiresMatchingItemAndRackBeforePosting() {
        ReturnRequest request = salesReturn();
        ReflectionTestUtils.setField(request.getLines().get(0), "id", 1);
        request.review(user(4), true, null, LocalDateTime.now());
        when(returnRepository.findByIdWithLinesForUpdate(15)).thenReturn(Optional.of(request));

        assertFalse(service.postApprovedReturnWithBarcodes(15, 4,
                java.util.List.of(new ReturnRequestService.MobileReturnScan(99,
                        itemQr("SKU-1", "RETURN-LOT", LocalDate.now().plusDays(30)), locationQr("A-01")))));
        verify(inboundStockPoster, never()).postInbound(any(), any(), any(), any(), any(), anyInt(), any(), any());
    }

    @Test
    void scannedApprovedSalesReturnPostsOnlyAfterMatchingItemBatchExpiryAndRack() {
        ReturnRequest request = salesReturn();
        ReflectionTestUtils.setField(request.getLines().get(0), "id", 1);
        request.review(user(4), true, null, LocalDateTime.now());
        DeliveryOrder sourceOrder = dispatchedOrder();
        when(returnRepository.findByIdWithLinesForUpdate(15)).thenReturn(Optional.of(request));
        when(userRepository.findById(4)).thenReturn(Optional.of(user(4)));
        when(deliveryOrderLineRepository.findByIdForUpdate(9)).thenReturn(Optional.of(request
                .getLines().get(0).getDeliveryOrderLine()));
        when(returnRepository.sumSalesReturnQuantity(eq(9), any())).thenReturn(2L);
        when(inboundStockPoster.postInbound(1, null, 2, "RETURN-LOT", LocalDate.now().plusDays(30),
                1, "Sales Return " + request.getReturnCode() + " dari DO " + sourceOrder.getOrderCode(), 4))
                .thenReturn(true);

        assertTrue(service.postApprovedReturnWithBarcodes(15, 4,
                java.util.List.of(new ReturnRequestService.MobileReturnScan(1,
                        itemQr("SKU-1", "RETURN-LOT", LocalDate.now().plusDays(30)), locationQr("A-01")))));
        assertEquals(ReturnStatus.POSTED, request.getStatus());
        verify(inboundStockPoster).postInbound(eq(1), isNull(), eq(2), eq("RETURN-LOT"),
                eq(LocalDate.now().plusDays(30)), eq(1), any(), eq(4));
    }

    @Test
    void scannedPurchaseReturnRejectsRepeatedLotQuantityAboveAvailableStock() {
        PurchaseOrderLine source = purchaseLine();
        LocalDate expiry = LocalDate.now().plusDays(30);
        ReturnRequest request = new ReturnRequest("RET-TEST", ReturnType.PURCHASE, user(3),
                "Damaged goods", LocalDateTime.now());
        ReflectionTestUtils.setField(request, "id", 15);
        request.addLine(new ReturnRequestLine(source, rack(), "LOT-1", 2));
        request.addLine(new ReturnRequestLine(source, rack(), "LOT-1", 2));
        request.review(user(4), true, null, LocalDateTime.now());
        ReflectionTestUtils.setField(request.getLines().get(0), "id", 21);
        ReflectionTestUtils.setField(request.getLines().get(1), "id", 22);
        InventoryLotBalance lot = new InventoryLotBalance(item(), rack(), "RAK-2", "LOT-1",
                expiry, LocalDateTime.now().minusDays(2), 3);
        when(returnRepository.findByIdWithLinesForUpdate(15)).thenReturn(Optional.of(request));
        when(lotBalanceRepository.findByBarangIdAndLocationKeyAndBatchCodeForUpdate(1, "RAK-2", "LOT-1"))
                .thenReturn(Optional.of(lot));

        assertFalse(service.postApprovedReturnWithBarcodes(15, 4, List.of(
                new ReturnRequestService.MobileReturnScan(21, itemQr("SKU-1", "LOT-1", expiry), locationQr("A-01")),
                new ReturnRequestService.MobileReturnScan(22, itemQr("SKU-1", "LOT-1", expiry), locationQr("A-01")))));
        verify(outboundStockPoster, never()).postOutbound(any(), any(), any(), anyInt(), any(), anyInt());
    }

    private PurchaseOrderLine purchaseLine() {
        PurchaseOrder order = new PurchaseOrder("PO-1", new Supplier(4, "Supplier", null, null, null),
                user(3), null, LocalDateTime.now());
        order.setStatus(PurchaseOrderStatus.RECEIVED);
        PurchaseOrderLine line = new PurchaseOrderLine(item(), 5);
        line.setPurchaseOrder(order);
        line.setReceivedQuantity(5);
        ReflectionTestUtils.setField(line, "id", 8);
        return line;
    }

    private DeliveryOrderLine deliveryLine() {
        DeliveryOrder order = dispatchedOrder();
        DeliveryOrderLine line = order.getLines().get(0);
        ReflectionTestUtils.setField(line, "id", 9);
        return line;
    }

    private DeliveryOrder dispatchedOrder() {
        DeliveryOrder order = new DeliveryOrder("DO-1",
                new Customer(5, "Customer", null, null, null, null, "Aktif"),
                user(3), null, LocalDateTime.now());
        order.addLine(new DeliveryOrderLine(item(), 5));
        order.getLines().get(0).setShippedQuantity(3);
        order.setStatus(DeliveryOrderStatus.DISPATCHED);
        return order;
    }

    private ReturnRequest salesReturn() {
        ReturnRequest request = new ReturnRequest("RET-TEST", ReturnType.SALES, user(3),
                "Customer issue", LocalDateTime.now());
        ReflectionTestUtils.setField(request, "id", 15);
        request.addLine(new ReturnRequestLine(deliveryLine(), rack(), "RETURN-LOT",
                LocalDate.now().plusDays(30), 1));
        return request;
    }

    private Barang item() { return Barang.builder().id(1).kodeBarang("SKU-1").namaBarang("Item").build(); }
    private Rak rack() { return new Rak(2, null, "A-01", 50, 2); }
    private User user(int id) { return User.builder().id(id).fullName("User " + id).build(); }

    private String itemQr(String sku, String batch, LocalDate expiry) {
        return "WMS1|" + encode(sku) + "|" + encode(batch) + "|" + expiry;
    }

    private String locationQr(String rackCode) {
        return "SLOC1|" + encode(rackCode);
    }

    private String encode(String value) {
        return Base64.getUrlEncoder().withoutPadding()
                .encodeToString(value.getBytes(StandardCharsets.UTF_8));
    }

    private ReturnRequestService.ReturnLineInput returnLineInput(Integer sourceLineId, Integer rackId,
                                                                  String batch, LocalDate expiry, int quantity) {
        return new ReturnRequestService.ReturnLineInput(sourceLineId, rackId, batch, expiry, quantity);
    }
}
