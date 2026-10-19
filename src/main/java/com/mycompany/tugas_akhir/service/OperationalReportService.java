package com.mycompany.tugas_akhir.service;

import com.mycompany.tugas_akhir.model.*;
import com.mycompany.tugas_akhir.repository.*;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
public class OperationalReportService {

    private final InboundReceiptRepository inboundReceiptRepository;
    private final DeliveryOrderRepository deliveryOrderRepository;
    private final StockTransferOrderRepository stockTransferOrderRepository;
    private final DeliveryOrderPickScanRepository pickScanRepository;
    private final DeliveryOrderFulfillmentAuditRepository fulfillmentAuditRepository;

    public OperationalReportService(InboundReceiptRepository inboundReceiptRepository,
                                    DeliveryOrderRepository deliveryOrderRepository,
                                    StockTransferOrderRepository stockTransferOrderRepository,
                                    DeliveryOrderPickScanRepository pickScanRepository,
                                    DeliveryOrderFulfillmentAuditRepository fulfillmentAuditRepository) {
        this.inboundReceiptRepository = inboundReceiptRepository;
        this.deliveryOrderRepository = deliveryOrderRepository;
        this.stockTransferOrderRepository = stockTransferOrderRepository;
        this.pickScanRepository = pickScanRepository;
        this.fulfillmentAuditRepository = fulfillmentAuditRepository;
    }

    @Transactional(readOnly = true)
    public List<OperationalReportRow> getRecentOperationalReport() {
        return getRecentOperationalReport(null, null);
    }

    @Transactional(readOnly = true)
    public List<OperationalReportRow> getRecentOperationalReport(LocalDate from, LocalDate to) {
        if (from != null && to != null && from.isAfter(to)) {
            throw new IllegalArgumentException("Tanggal awal laporan tidak boleh melewati tanggal akhir.");
        }
        boolean filtered = from != null || to != null;
        LocalDateTime rangeStart = from == null ? null : from.atStartOfDay();
        LocalDateTime rangeEnd = to == null ? null : to.atTime(LocalTime.MAX);
        List<OperationalReportRow> rows = new ArrayList<>();
        Map<String, SupplierSummary> supplierSummaries = new LinkedHashMap<>();
        Map<String, SupplierSummary> supplierDeliverySummaries = new LinkedHashMap<>();
        List<InboundReceipt> receipts = filtered
                ? inboundReceiptRepository.findInRangeWithPurchaseOrder(rangeStart, rangeEnd)
                : inboundReceiptRepository.findRecentWithPurchaseOrder(PageRequest.of(0, 200));
        for (InboundReceipt receipt : receipts) {
            if (!inRange(receipt.getCreatedAt(), from, to)) continue;
            String supplier = receipt.getSupplier().getNamaSupplier();
            String qcMetric = "QC pending";
            if (receipt.getQcAt() != null) {
                long qcHours = Math.max(0, Duration.between(receipt.getCreatedAt(), receipt.getQcAt()).toHours());
                qcMetric = qcHours + " jam ke QC";
                supplierSummaries.computeIfAbsent(supplier, ignored -> new SupplierSummary())
                        .add(receipt.getQuantity(), qcHours);
            } else {
                supplierSummaries.computeIfAbsent(supplier, ignored -> new SupplierSummary())
                        .add(receipt.getQuantity(), null);
            }
            if (receipt.getPurchaseOrderLine() != null) {
                LocalDateTime purchaseOrderCreatedAt =
                receipt.getPurchaseOrderLine().getPurchaseOrder().getCreatedAt();
                if (purchaseOrderCreatedAt != null && receipt.getCreatedAt() != null
                && !receipt.getCreatedAt().isBefore(purchaseOrderCreatedAt)) {
                    long leadHours = Duration.between(purchaseOrderCreatedAt, receipt.getCreatedAt()).toHours();
                    supplierDeliverySummaries.computeIfAbsent(supplier, ignored -> new SupplierSummary())
                    .addDeliveryLead(leadHours);
                }
            }
            rows.add(new OperationalReportRow("Inbound", receipt.getReceiptCode(),
                    receipt.getStatus().name(), supplier, receipt.getBarang().getKodeBarang(),
                    receipt.getQuantity(), qcMetric,
                    receipt.getQcBy() == null ? "-" : receipt.getQcBy().getFullName(),
                    receipt.getCreatedAt()));
        }
        for (Map.Entry<String, SupplierSummary> entry : supplierSummaries.entrySet()) {
            SupplierSummary summary = entry.getValue();
            rows.add(new OperationalReportRow("Supplier SLA", entry.getKey(),
                    summary.completed + "/" + summary.receipts + " receipt QC",
                    "-", "-", summary.totalQuantity,
                    summary.completed == 0 ? "Belum ada QC selesai"
                            : String.format(java.util.Locale.ROOT, "Rata-rata %.1f jam ke QC",
                                    (double) summary.totalQcHours / summary.completed),
                    "-", null));
        }
        for (Map.Entry<String, SupplierSummary> entry : supplierDeliverySummaries.entrySet()) {
            SupplierSummary summary = entry.getValue();
            rows.add(new OperationalReportRow("Supplier Delivery SLA", entry.getKey(),
                    summary.poReceipts + " receipt PO", "-", "-", 0,
                    String.format(java.util.Locale.ROOT, "Rata-rata %.1f jam dari PO ke receipt",
                            (double) summary.totalDeliveryLeadHours / summary.poReceipts),
                    "-", null));
        }

        List<DeliveryOrder> deliveryOrders = filtered
                ? deliveryOrderRepository.findInRangeWithLines(rangeStart, rangeEnd)
                : deliveryOrderRepository.findTop100ByOrderByCreatedAtDesc();
        for (DeliveryOrder order : deliveryOrders) {
            if (!inRange(order.getCreatedAt(), from, to)) continue;
            for (DeliveryOrderLine line : order.getLines()) {
                int shipped = line.getShippedQuantity();
                int ordered = line.getOrderedQuantity();
                String rate = ordered <= 0 ? "0%" : String.format(java.util.Locale.ROOT,
                        "%.1f%% fulfillment", 100.0 * shipped / ordered);
                rows.add(new OperationalReportRow("Outbound", order.getOrderCode(),
                        order.getStatus().name(), order.getCustomer().getNamaCustomer(),
                        line.getBarang().getKodeBarang(), shipped,
                        "Terkirim " + shipped + "/" + ordered + " | " + rate,
                        order.getCreatedBy().getFullName(), order.getCreatedAt()));
            }
        }

        List<StockTransferOrder> transferOrders = filtered
                ? stockTransferOrderRepository.findInRangeWithLines(rangeStart, rangeEnd)
                : stockTransferOrderRepository.findTop200ByOrderByCreatedAtDesc();
        for (StockTransferOrder order : transferOrders) {
            if (!inRange(order.getCreatedAt(), from, to)) continue;
            for (StockTransferOrderLine line : order.getLines()) {
                rows.add(new OperationalReportRow("STO", order.getOrderCode(),
                        order.getStatus().name(),
                        order.getSourceWarehouse().getWarehouseCode() + " -> "
                                + order.getDestinationWarehouse().getWarehouseCode(),
                        line.getBarang().getKodeBarang(), line.getQuantity(),
                        "SLOC " + line.getSourceRak().getKodeRak() + " -> "
                                + line.getDestinationRak().getKodeRak(),
                        order.getCreatedBy().getFullName(), order.getCreatedAt()));
            }
        }

        Map<String, PickerSummary> pickerSummaries = new LinkedHashMap<>();
        List<DeliveryOrderPickScan> scans = filtered
                ? pickScanRepository.findInRange(rangeStart, rangeEnd)
                : pickScanRepository.findRecent(PageRequest.of(0, 500));
        for (DeliveryOrderPickScan scan : scans) {
            if (!inRange(scan.getScannedAt(), from, to)) continue;
            String picker = scan.getPicker().getFullName();
            pickerSummaries.computeIfAbsent(picker, ignored -> new PickerSummary())
                    .add(scan.getQuantity());
            rows.add(new OperationalReportRow("Pick Scan", scan.getDeliveryOrder().getOrderCode(),
                    "RECORDED", picker, scan.getScannedSku(), scan.getQuantity(),
                    scan.getScannedRack() + " / " + scan.getScannedBatch(),
                    picker, scan.getScannedAt()));
        }
        for (Map.Entry<String, PickerSummary> entry : pickerSummaries.entrySet()) {
            rows.add(new OperationalReportRow("Picker Productivity", entry.getKey(),
                    entry.getValue().scans + " scan", "-", "-",
                    entry.getValue().quantity, "Unit ter-pick tercatat",
                    entry.getKey(), null));
        }
        List<DeliveryOrderFulfillmentAudit> audits = filtered
                ? fulfillmentAuditRepository.findInRange(rangeStart, rangeEnd)
                : fulfillmentAuditRepository.findRecent(PageRequest.of(0, 500));
        for (DeliveryOrderFulfillmentAudit audit : audits) {
            if (!inRange(audit.getRecordedAt(), from, to)) continue;
            rows.add(new OperationalReportRow(audit.getStage(), audit.getDeliveryOrder().getOrderCode(),
                    "AUDITED", audit.getActor().getFullName(),
                    audit.getDeliveryOrderLine().getBarang().getKodeBarang(),
                    audit.getVerifiedQuantity(), "Jumlah diverifikasi",
                    audit.getActor().getFullName(), audit.getRecordedAt()));
        }
        return List.copyOf(rows);
    }

    private boolean inRange(LocalDateTime timestamp, LocalDate from, LocalDate to) {
        if (timestamp == null) return from == null && to == null;
        LocalDate date = timestamp.toLocalDate();
        return (from == null || !date.isBefore(from)) && (to == null || !date.isAfter(to));
    }

    private static final class SupplierSummary {
        private int receipts;
        private int completed;
        private int totalQuantity;
        private long totalQcHours;
        private int poReceipts;
        private long totalDeliveryLeadHours;

        private void add(int quantity, Long qcHours) {
            receipts++;
            totalQuantity += quantity;
            if (qcHours != null) {
                completed++;
                totalQcHours += qcHours;
            }
        }

        private void addDeliveryLead(long hours) {
            poReceipts++;
            totalDeliveryLeadHours += hours;
        }
    }

    private static final class PickerSummary {
        private int scans;
        private int quantity;

        private void add(Integer quantity) {
            scans++;
            this.quantity += quantity == null ? 0 : quantity;
        }
    }
}
