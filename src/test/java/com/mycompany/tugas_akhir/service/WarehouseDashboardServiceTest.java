package com.mycompany.tugas_akhir.service;

import com.mycompany.tugas_akhir.model.*;
import com.mycompany.tugas_akhir.repository.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class WarehouseDashboardServiceTest {
    @Mock private DeliveryOrderRepository deliveryOrderRepository;
    @Mock private DeliveryOrderPickScanRepository pickScanRepository;
    @Mock private InboundReceiptRepository inboundReceiptRepository;
    @Mock private DeliveryOrderLotReservationRepository reservationRepository;
    @Mock private WarehouseDashboardTargetRepository targetRepository;

    private WarehouseDashboardService service;

    @BeforeEach
    void setUp() {
        service = new WarehouseDashboardService(deliveryOrderRepository, pickScanRepository,
                inboundReceiptRepository, reservationRepository, targetRepository);
    }

    @Test
    void calculatesFulfillmentAndPackingLoadingRatiosForSameDateRange() {
        LocalDate from = LocalDate.of(2026, 10, 1);
        LocalDate to = LocalDate.of(2026, 10, 30);
        DeliveryOrder order = mock(DeliveryOrder.class);
        DeliveryOrderLine line = mock(DeliveryOrderLine.class);
        when(order.getLines()).thenReturn(List.of(line));
        when(line.getOrderedQuantity()).thenReturn(10);
        when(line.getShippedQuantity()).thenReturn(7);
        when(deliveryOrderRepository.findInRangeWithLines(from.atStartOfDay(), to.atTime(23, 59, 59, 999_999_999)))
                .thenReturn(List.of(order));
        when(pickScanRepository.findPickerProductivity(from.atStartOfDay(),
                to.atTime(23, 59, 59, 999_999_999))).thenReturn(List.of());
        when(reservationRepository.getWarehouseStageTotals(from.atStartOfDay(),
                to.atTime(23, 59, 59, 999_999_999)))
                .thenReturn(new WarehouseStageTotalsProjection(100L, 80L, 60L));
        when(inboundReceiptRepository.findInRangeWithPurchaseOrder(from.atStartOfDay(),
                to.atTime(23, 59, 59, 999_999_999))).thenReturn(List.of());

        WarehouseDashboardService.DashboardMetrics metrics = service.getMetrics(from, to);

        assertEquals(70.0, metrics.fulfillmentRatePercent());
        assertEquals(7, metrics.shippedQuantity());
        assertEquals(10, metrics.orderedQuantity());
        assertEquals(80.0, metrics.packingEfficiencyPercent());
        assertEquals(75.0, metrics.loadingEfficiencyPercent());
    }

    @Test
    void aggregatesPickerOutputAndSupplierTurnaround() {
        LocalDate from = LocalDate.of(2026, 10, 1);
        LocalDate to = LocalDate.of(2026, 10, 30);
        LocalDateTime start = from.atStartOfDay();
        LocalDateTime end = to.atTime(23, 59, 59, 999_999_999);
        when(deliveryOrderRepository.findInRangeWithLines(start, end)).thenReturn(List.of());
        when(pickScanRepository.findPickerProductivity(start, end)).thenReturn(List.of(
                new PickerProductivityProjection("Ayu", 12L, 84L)));
        when(reservationRepository.getWarehouseStageTotals(start, end))
                .thenReturn(new WarehouseStageTotalsProjection(84L, 70L, 55L));

        InboundReceipt receipt = mock(InboundReceipt.class);
        Supplier supplier = mock(Supplier.class);
        PurchaseOrderLine purchaseLine = mock(PurchaseOrderLine.class);
        PurchaseOrder purchaseOrder = mock(PurchaseOrder.class);
        when(receipt.getSupplier()).thenReturn(supplier);
        when(supplier.getNamaSupplier()).thenReturn("Supplier A");
        when(receipt.getCreatedAt()).thenReturn(LocalDateTime.of(2026, 10, 5, 8, 0));
        when(receipt.getQcAt()).thenReturn(LocalDateTime.of(2026, 10, 5, 14, 0));
        when(receipt.getPurchaseOrderLine()).thenReturn(purchaseLine);
        when(purchaseLine.getPurchaseOrder()).thenReturn(purchaseOrder);
        when(purchaseOrder.getCreatedAt()).thenReturn(LocalDateTime.of(2026, 10, 3, 14, 0));
        when(inboundReceiptRepository.findInRangeWithPurchaseOrder(start, end)).thenReturn(List.of(receipt));

        WarehouseDashboardService.DashboardMetrics metrics = service.getMetrics(from, to);

        assertEquals(1, metrics.pickers().size());
        assertEquals("Ayu", metrics.pickers().get(0).pickerName());
        assertEquals(12, metrics.pickers().get(0).scanCount());
        assertEquals(84, metrics.pickers().get(0).pickedQuantity());
        assertEquals(1, metrics.suppliers().get(0).receiptCount());
        assertEquals(1, metrics.suppliers().get(0).qcCompleted());
        assertEquals(1, metrics.suppliers().get(0).poToReceiptSamples());
        assertEquals(6.0, metrics.suppliers().get(0).averageQcHours());
        assertEquals(42.0, metrics.suppliers().get(0).averagePoToReceiptHours());
    }

    @Test
    void savesSharedTargetsAndAllowsBlankTargetsToDisableThresholds() {
        var targets = new WarehouseDashboardService.DashboardTargets(new BigDecimal("95"), null,
                new BigDecimal("98.5"), new BigDecimal("4"), null, 120);

        assertTrue(service.updateTargets(targets));
        verify(targetRepository).save(org.mockito.ArgumentMatchers.argThat(saved ->
                saved.getFulfillmentPercent().compareTo(new BigDecimal("95")) == 0
                        && saved.getPackingPercent() == null
                        && saved.getLoadingPercent().compareTo(new BigDecimal("98.5")) == 0
                        && saved.getSupplierQcMaxHours().compareTo(new BigDecimal("4")) == 0
                        && saved.getSupplierLeadMaxHours() == null
                        && saved.getPickerMinimumQuantity() == 120));
    }

    @Test
    void rejectsOutOfRangeDashboardTargets() {
        assertFalse(service.updateTargets(new WarehouseDashboardService.DashboardTargets(
                new BigDecimal("100.01"), null, null, null, null, null)));
        assertFalse(service.updateTargets(new WarehouseDashboardService.DashboardTargets(
                null, null, null, new BigDecimal("-1"), null, null)));
        assertFalse(service.updateTargets(new WarehouseDashboardService.DashboardTargets(
                null, null, null, null, null, -1)));
        verifyNoInteractions(targetRepository);
    }

    @Test
    void returnsDisabledTargetsUntilAnAdminConfiguresThem() {
        when(targetRepository.findById(1)).thenReturn(java.util.Optional.empty());

        assertEquals(WarehouseDashboardService.DashboardTargets.empty(), service.getTargets());
        verify(targetRepository).findById(1);
        verifyNoMoreInteractions(targetRepository);
    }
}
