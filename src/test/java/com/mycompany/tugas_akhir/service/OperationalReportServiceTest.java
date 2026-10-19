package com.mycompany.tugas_akhir.service;

import com.mycompany.tugas_akhir.model.*;
import com.mycompany.tugas_akhir.repository.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Pageable;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class OperationalReportServiceTest {

    @Mock private InboundReceiptRepository inboundReceiptRepository;
    @Mock private DeliveryOrderRepository deliveryOrderRepository;
    @Mock private StockTransferOrderRepository stockTransferOrderRepository;
    @Mock private DeliveryOrderPickScanRepository pickScanRepository;
    @Mock private DeliveryOrderFulfillmentAuditRepository fulfillmentAuditRepository;

    @Test
    void reportsOutboundFulfillmentFromDispatchedQuantities() {
        Customer customer = new Customer(2, "Customer", null, null, null, null, "Aktif");
        User creator = User.builder().id(3).fullName("Operator").build();
        Barang item = Barang.builder().id(1).kodeBarang("SKU-1").namaBarang("Item").build();
        DeliveryOrder order = new DeliveryOrder("DO-REPORT", customer, creator, null, LocalDateTime.now());
        DeliveryOrderLine line = new DeliveryOrderLine(item, 10);
        line.setShippedQuantity(7);
        order.addLine(line);
        order.setStatus(DeliveryOrderStatus.PARTIALLY_DISPATCHED);
        when(inboundReceiptRepository.findRecentWithPurchaseOrder(any(Pageable.class))).thenReturn(List.of());
        when(deliveryOrderRepository.findTop100ByOrderByCreatedAtDesc()).thenReturn(List.of(order));
        when(stockTransferOrderRepository.findTop200ByOrderByCreatedAtDesc()).thenReturn(List.of());
        when(pickScanRepository.findRecent(any(Pageable.class))).thenReturn(List.of());
        when(fulfillmentAuditRepository.findRecent(any(Pageable.class))).thenReturn(List.of());

        OperationalReportService service = new OperationalReportService(inboundReceiptRepository,
                deliveryOrderRepository, stockTransferOrderRepository,
                pickScanRepository, fulfillmentAuditRepository);

        OperationalReportRow row = service.getRecentOperationalReport().get(0);

        assertEquals("Outbound", row.category());
        assertEquals("DO-REPORT", row.documentCode());
        assertEquals(7, row.quantity());
        assertTrue(row.metric().contains("70.0% fulfillment"));
        assertTrue(service.getRecentOperationalReport(LocalDate.now().plusDays(1), null).isEmpty());
    }

    @Test
    void rejectsAnInvertedOperationalReportDateRange() {
        OperationalReportService service = new OperationalReportService(inboundReceiptRepository,
                deliveryOrderRepository, stockTransferOrderRepository,
                pickScanRepository, fulfillmentAuditRepository);

        assertThrows(IllegalArgumentException.class,
                () -> service.getRecentOperationalReport(LocalDate.now(), LocalDate.now().minusDays(1)));
    }

    @Test
    void appliesHistoricalDateRangeInRepositoryQueriesBeforeRecentRowLimits() {
        LocalDate from = LocalDate.of(2024, 2, 1);
        LocalDate to = LocalDate.of(2024, 2, 29);
        Customer customer = new Customer(2, "Customer", null, null, null, null, "Aktif");
        User creator = User.builder().id(3).fullName("Operator").build();
        Barang item = Barang.builder().id(1).kodeBarang("SKU-1").namaBarang("Item").build();
        DeliveryOrder order = new DeliveryOrder("DO-HISTORY", customer, creator, null,
                LocalDateTime.of(2024, 2, 15, 10, 0));
        order.addLine(new DeliveryOrderLine(item, 4));
        when(deliveryOrderRepository.findInRangeWithLines(any(LocalDateTime.class), any(LocalDateTime.class)))
                .thenReturn(List.of(order));
        OperationalReportService service = new OperationalReportService(inboundReceiptRepository,
                deliveryOrderRepository, stockTransferOrderRepository,
                pickScanRepository, fulfillmentAuditRepository);

        List<OperationalReportRow> rows = service.getRecentOperationalReport(from, to);

        LocalDateTime start = from.atStartOfDay();
        LocalDateTime end = to.atTime(23, 59, 59, 999_999_999);
        verify(inboundReceiptRepository).findInRangeWithPurchaseOrder(eq(start), eq(end));
        verify(deliveryOrderRepository).findInRangeWithLines(eq(start), eq(end));
        verify(stockTransferOrderRepository).findInRangeWithLines(eq(start), eq(end));
        verify(pickScanRepository).findInRange(eq(start), eq(end));
        verify(fulfillmentAuditRepository).findInRange(eq(start), eq(end));
        assertTrue(rows.stream()
                .anyMatch(row -> row.documentCode().equals("DO-HISTORY")));
    }
}
