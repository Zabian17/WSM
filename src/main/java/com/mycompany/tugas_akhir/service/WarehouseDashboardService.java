package com.mycompany.tugas_akhir.service;

import com.mycompany.tugas_akhir.model.DeliveryOrder;
import com.mycompany.tugas_akhir.model.DeliveryOrderLine;
import com.mycompany.tugas_akhir.model.InboundReceipt;
import com.mycompany.tugas_akhir.model.PurchaseOrderLine;
import com.mycompany.tugas_akhir.model.WarehouseDashboardTarget;
import com.mycompany.tugas_akhir.repository.DeliveryOrderLotReservationRepository;
import com.mycompany.tugas_akhir.repository.DeliveryOrderPickScanRepository;
import com.mycompany.tugas_akhir.repository.DeliveryOrderRepository;
import com.mycompany.tugas_akhir.repository.InboundReceiptRepository;
import com.mycompany.tugas_akhir.repository.PickerProductivityProjection;
import com.mycompany.tugas_akhir.repository.WarehouseStageTotalsProjection;
import com.mycompany.tugas_akhir.repository.WarehouseDashboardTargetRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
public class WarehouseDashboardService {
    private final DeliveryOrderRepository deliveryOrderRepository;
    private final DeliveryOrderPickScanRepository pickScanRepository;
    private final InboundReceiptRepository inboundReceiptRepository;
    private final DeliveryOrderLotReservationRepository reservationRepository;
    private final WarehouseDashboardTargetRepository targetRepository;

    public WarehouseDashboardService(DeliveryOrderRepository deliveryOrderRepository,
                                     DeliveryOrderPickScanRepository pickScanRepository,
                                     InboundReceiptRepository inboundReceiptRepository,
                                     DeliveryOrderLotReservationRepository reservationRepository,
                                     WarehouseDashboardTargetRepository targetRepository) {
        this.deliveryOrderRepository = deliveryOrderRepository;
        this.pickScanRepository = pickScanRepository;
        this.inboundReceiptRepository = inboundReceiptRepository;
        this.reservationRepository = reservationRepository;
        this.targetRepository = targetRepository;
    }

    @Transactional(readOnly = true)
    public DashboardTargets getTargets() {
        return targetRepository.findById(1).map(target -> new DashboardTargets(
                target.getFulfillmentPercent(), target.getPackingPercent(), target.getLoadingPercent(),
                target.getSupplierQcMaxHours(), target.getSupplierLeadMaxHours(),
                target.getPickerMinimumQuantity())).orElseGet(DashboardTargets::empty);
    }

    @Transactional
    public boolean updateTargets(DashboardTargets targets) {
        if (targets == null || !validPercent(targets.fulfillmentPercent())
                || !validPercent(targets.packingPercent()) || !validPercent(targets.loadingPercent())
                || !validHours(targets.supplierQcMaxHours()) || !validHours(targets.supplierLeadMaxHours())
                || targets.pickerMinimumQuantity() != null && targets.pickerMinimumQuantity() < 0) return false;
        targetRepository.save(new WarehouseDashboardTarget(targets.fulfillmentPercent(), targets.packingPercent(),
                targets.loadingPercent(), targets.supplierQcMaxHours(), targets.supplierLeadMaxHours(),
                targets.pickerMinimumQuantity()));
        return true;
    }

    @Transactional(readOnly = true)
    public DashboardMetrics getMetrics(LocalDate from, LocalDate to) {
        if (from == null || to == null || from.isAfter(to)) {
            throw new IllegalArgumentException("Rentang tanggal dashboard tidak valid");
        }
        LocalDateTime start = from.atStartOfDay();
        LocalDateTime end = to.atTime(LocalTime.MAX);

        long ordered = 0;
        long shipped = 0;
        for (DeliveryOrder order : deliveryOrderRepository.findInRangeWithLines(start, end)) {
            for (DeliveryOrderLine line : order.getLines()) {
                ordered += value(line.getOrderedQuantity());
                shipped += value(line.getShippedQuantity());
            }
        }

        List<PickerMetric> pickers = pickScanRepository.findPickerProductivity(start, end).stream()
                .map(row -> new PickerMetric(row.pickerName(), value(row.scanCount()), value(row.pickedQuantity())))
                .toList();
        WarehouseStageTotalsProjection totals = reservationRepository.getWarehouseStageTotals(start, end);
        long picked = totals == null ? 0 : value(totals.pickedQuantity());
        long packed = totals == null ? 0 : value(totals.packedQuantity());
        long loaded = totals == null ? 0 : value(totals.loadedQuantity());

        Map<String, SupplierAccumulator> supplierTotals = new LinkedHashMap<>();
        for (InboundReceipt receipt : inboundReceiptRepository.findInRangeWithPurchaseOrder(start, end)) {
            if (receipt.getSupplier() == null) continue;
            String name = receipt.getSupplier().getNamaSupplier();
            SupplierAccumulator accumulator = supplierTotals.computeIfAbsent(name, ignored -> new SupplierAccumulator());
            accumulator.receipts++;
            LocalDateTime receivedAt = receipt.getCreatedAt();
            if (receivedAt != null && receipt.getQcAt() != null && !receipt.getQcAt().isBefore(receivedAt)) {
                accumulator.qcMinutes += Duration.between(receivedAt, receipt.getQcAt()).toMinutes();
                accumulator.qcCompleted++;
            }
            PurchaseOrderLine line = receipt.getPurchaseOrderLine();
            LocalDateTime orderedAt = line == null || line.getPurchaseOrder() == null
                    ? null : line.getPurchaseOrder().getCreatedAt();
            if (receivedAt != null && orderedAt != null && !receivedAt.isBefore(orderedAt)) {
                accumulator.poToReceiptMinutes += Duration.between(orderedAt, receivedAt).toMinutes();
                accumulator.poToReceiptCount++;
            }
        }
        List<SupplierMetric> suppliers = new ArrayList<>();
        supplierTotals.forEach((name, value) -> suppliers.add(new SupplierMetric(name, value.receipts,
                value.qcCompleted, value.poToReceiptCount, averageHours(value.qcMinutes, value.qcCompleted),
                averageHours(value.poToReceiptMinutes, value.poToReceiptCount))));

        return new DashboardMetrics(from, to, ordered, shipped, percentage(shipped, ordered), pickers,
                picked, packed, percentage(packed, picked), packed, loaded, percentage(loaded, packed), suppliers);
    }

    public record PickerMetric(String pickerName, long scanCount, long pickedQuantity) {}
    public record SupplierMetric(String supplierName, int receiptCount, int qcCompleted, int poToReceiptSamples,
                                Double averageQcHours, Double averagePoToReceiptHours) {}
    public record DashboardMetrics(LocalDate from, LocalDate to, long orderedQuantity, long shippedQuantity,
                                  double fulfillmentRatePercent, List<PickerMetric> pickers,
                                  long pickedQuantity, long packedQuantity, double packingEfficiencyPercent,
                                  long packedForLoading, long loadedQuantity, double loadingEfficiencyPercent,
                                  List<SupplierMetric> suppliers) {}
    public record DashboardTargets(BigDecimal fulfillmentPercent, BigDecimal packingPercent,
                                   BigDecimal loadingPercent, BigDecimal supplierQcMaxHours,
                                   BigDecimal supplierLeadMaxHours, Integer pickerMinimumQuantity) {
        public static DashboardTargets empty() { return new DashboardTargets(null, null, null, null, null, null); }
    }

    private static int value(Integer value) { return value == null ? 0 : value; }
    private static long value(Long value) { return value == null ? 0 : value; }
    private static double percentage(long numerator, long denominator) {
        return denominator == 0 ? 0 : Math.round(numerator * 1000.0 / denominator) / 10.0;
    }
    private static Double averageHours(long totalMinutes, int count) {
        return count == 0 ? null : Math.round(totalMinutes / (double) count) / 60.0;
    }
    private static boolean validPercent(BigDecimal value) {
        return value == null || value.compareTo(BigDecimal.ZERO) >= 0
                && value.compareTo(BigDecimal.valueOf(100)) <= 0;
    }
    private static boolean validHours(BigDecimal value) {
        return value == null || value.signum() >= 0 && value.compareTo(BigDecimal.valueOf(87600)) <= 0;
    }

    private static final class SupplierAccumulator {
        private int receipts;
        private int qcCompleted;
        private long qcMinutes;
        private int poToReceiptCount;
        private long poToReceiptMinutes;
    }
}
