package com.mycompany.tugas_akhir.service;

import com.mycompany.tugas_akhir.model.*;
import com.mycompany.tugas_akhir.repository.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@Service
public class ReturnRequestService {

    private static final List<ReturnStatus> COUNTED_RETURN_STATUSES =
            List.of(ReturnStatus.PENDING_REVIEW, ReturnStatus.APPROVED, ReturnStatus.POSTED);

    private final ReturnRequestRepository returnRepository;
    private final PurchaseOrderRepository purchaseOrderRepository;
    private final DeliveryOrderLineRepository deliveryOrderLineRepository;
    private final InventoryLotBalanceRepository lotBalanceRepository;
    private final RakRepository rakRepository;
    private final UserRepository userRepository;
    private final InboundStockPoster inboundStockPoster;
    private final OutboundStockPoster outboundStockPoster;
    private final ApprovalRouter approvalRoutingService;
    private final WarehouseZoneRuleService warehouseZoneRuleService;

    public ReturnRequestService(ReturnRequestRepository returnRepository,
                                PurchaseOrderRepository purchaseOrderRepository,
                                DeliveryOrderLineRepository deliveryOrderLineRepository,
                                InventoryLotBalanceRepository lotBalanceRepository,
                                RakRepository rakRepository,
                                UserRepository userRepository,
                                InboundStockPoster inboundStockPoster,
                                OutboundStockPoster outboundStockPoster,
                                ApprovalRouter approvalRoutingService,
                                WarehouseZoneRuleService warehouseZoneRuleService) {
        this.returnRepository = returnRepository;
        this.purchaseOrderRepository = purchaseOrderRepository;
        this.deliveryOrderLineRepository = deliveryOrderLineRepository;
        this.lotBalanceRepository = lotBalanceRepository;
        this.rakRepository = rakRepository;
        this.userRepository = userRepository;
        this.inboundStockPoster = inboundStockPoster;
        this.outboundStockPoster = outboundStockPoster;
        this.approvalRoutingService = approvalRoutingService;
        this.warehouseZoneRuleService = warehouseZoneRuleService;
    }

    @Transactional(readOnly = true)
    public List<ReturnRequest> getRecentReturns() {
        return returnRepository.findTop200ByOrderByCreatedAtDesc();
    }

    @Transactional(readOnly = true)
    public Page<ReturnRequest> getMobileApprovedReturns(Pageable pageable) {
        return returnRepository.findByStatusOrderByCreatedAtAsc(ReturnStatus.APPROVED, pageable);
    }

    @Transactional(readOnly = true)
    public java.util.Optional<ReturnRequest> getMobileApprovedReturn(Integer requestId) {
        if (requestId == null) return java.util.Optional.empty();
        return returnRepository.findById(requestId).filter(request -> request.getStatus() == ReturnStatus.APPROVED);
    }

    @Transactional
    public boolean postApprovedReturnWithBarcodes(Integer requestId, Integer userId,
                                                   List<MobileReturnScan> scans) {
        if (requestId == null || userId == null || scans == null || scans.isEmpty() || scans.size() > 100) {
            return false;
        }
        ReturnRequest request = returnRepository.findByIdWithLinesForUpdate(requestId).orElse(null);
        if (request == null || request.getStatus() != ReturnStatus.APPROVED || request.getLines().isEmpty()
                || scans.size() != request.getLines().size()) return false;
        java.util.Map<Integer, MobileReturnScan> scansByLine = new java.util.HashMap<>();
        Map<ReturnLotKey, Long> requestedByLot = new HashMap<>();
        Map<ReturnLotKey, InventoryLotBalance> lots = new HashMap<>();
        for (MobileReturnScan scan : scans) {
            if (scan == null || scan.lineId() == null || scan.itemBarcode() == null
                    || scan.itemBarcode().isBlank() || scan.itemBarcode().length() > 512
                    || scan.locationBarcode() == null || scan.locationBarcode().isBlank()
                    || scan.locationBarcode().length() > 512
                    || scansByLine.putIfAbsent(scan.lineId(), scan) != null) return false;
        }
        for (ReturnRequestLine line : request.getLines()) {
            MobileReturnScan scan = scansByLine.get(line.getId());
            BarcodePayload.Item item = scan == null ? null : BarcodePayload.parseItem(scan.itemBarcode());
            String rackCode = scan == null ? null : BarcodePayload.parseLocation(scan.locationBarcode());
            if (item == null || rackCode == null
                    || !line.getBarang().getKodeBarang().equalsIgnoreCase(item.sku())
                    || !line.getRak().getKodeRak().equalsIgnoreCase(rackCode)
                    || !line.getBatchCode().equals(item.batchCode())) return false;
            if (request.getReturnType() == ReturnType.PURCHASE) {
                if (!isPurchaseReturnPicklistZoneAllowed(line.getRak())) return false;
                InventoryLotBalance lot = lotBalanceRepository.findByBarangIdAndLocationKeyAndBatchCodeForUpdate(
                        line.getBarang().getId(), "RAK-" + line.getRak().getId(), line.getBatchCode()).orElse(null);
                ReturnLotKey lotKey = new ReturnLotKey(line.getBarang().getId(), line.getRak().getId(),
                        line.getBatchCode().toUpperCase(java.util.Locale.ROOT));
                if (lot == null || !java.util.Objects.equals(lot.getExpiryDate(), item.expiryDate())) return false;
                lots.putIfAbsent(lotKey, lot);
                long totalRequested = requestedByLot.merge(lotKey, (long) line.getQuantity(), Long::sum);
                if (lots.get(lotKey).getAvailableQuantity() < totalRequested) return false;
            } else if (!java.util.Objects.equals(line.getExpiryDate(), item.expiryDate())) {
                return false;
            }
        }
        return postApprovedReturn(requestId, userId);
    }

    @Transactional
    public ReturnRequest createPurchaseReturn(Integer sourceLineId, Integer rackId, String batchCode,
                                              int quantity, String reason, Integer creatorId) {
        return createPurchaseReturn(List.of(new ReturnLineInput(sourceLineId, rackId, batchCode, null, quantity)),
                reason, creatorId);
    }

    @Transactional
    public ReturnRequest createPurchaseReturn(List<ReturnLineInput> lines, String reason, Integer creatorId) {
        if (!validReturnLines(lines) || invalidReason(reason) || creatorId == null) return null;
        User creator = userRepository.findById(creatorId).orElse(null);
        if (creator == null) return null;
        Map<Integer, PurchaseOrderLine> sources = new HashMap<>();
        Map<Integer, Rak> racks = new HashMap<>();
        Map<Integer, Long> requestedBySource = new HashMap<>();
        for (ReturnLineInput line : lines) {
            if (line == null || line.sourceLineId() == null || line.rackId() == null || line.quantity() <= 0
                    || line.batchCode() == null || line.batchCode().isBlank()
                    || line.batchCode().trim().length() > 64) return null;
            PurchaseOrderLine source = sources.computeIfAbsent(line.sourceLineId(), id ->
                    purchaseOrderRepository.findLineByIdForUpdate(id).orElse(null));
            Rak rack = racks.computeIfAbsent(line.rackId(), id -> rakRepository.findById(id).orElse(null));
            if (source == null || rack == null || !isPurchaseReturnPicklistZoneAllowed(rack)
                    || source.getPurchaseOrder().getStatus() != PurchaseOrderStatus.RECEIVED
                    && source.getPurchaseOrder().getStatus() != PurchaseOrderStatus.PARTIALLY_RECEIVED) return null;
            requestedBySource.merge(line.sourceLineId(), (long) line.quantity(), Long::sum);
        }
        for (Map.Entry<Integer, Long> entry : requestedBySource.entrySet()) {
            PurchaseOrderLine source = sources.get(entry.getKey());
            long alreadyReturned = returnRepository.sumPurchaseReturnQuantity(entry.getKey(), COUNTED_RETURN_STATUSES);
            if (alreadyReturned + entry.getValue() > source.getReceivedQuantity()) return null;
        }
        ReturnRequest request = new ReturnRequest(returnCode(), ReturnType.PURCHASE, creator,
                reason.trim(), LocalDateTime.now());
        for (ReturnLineInput line : lines) {
            request.addLine(new ReturnRequestLine(sources.get(line.sourceLineId()), racks.get(line.rackId()),
                    line.batchCode().trim(), line.quantity()));
        }
        return returnRepository.save(request);
    }

    @Transactional
    public ReturnRequest createPurchaseReturnWithBarcodes(Integer sourceLineId, String itemQrPayload,
                                                          String sourceLocationBarcode, int quantity,
                                                          String reason, Integer creatorId) {
        return createPurchaseReturnWithBarcodes(List.of(new BarcodeReturnLineInput(sourceLineId,
                itemQrPayload, sourceLocationBarcode, quantity)), reason, creatorId);
    }

    @Transactional
    public ReturnRequest createPurchaseReturnWithBarcodes(List<BarcodeReturnLineInput> scannedLines,
                                                          String reason, Integer creatorId) {
        if (!validBarcodeReturnLines(scannedLines) || invalidReason(reason) || creatorId == null) return null;
        Map<Integer, PurchaseOrderLine> sources = new HashMap<>();
        Map<String, Long> requestedByLot = new HashMap<>();
        List<ReturnLineInput> lines = new java.util.ArrayList<>();
        for (BarcodeReturnLineInput scan : scannedLines) {
            if (!validScan(scan)) return null;
            BarcodePayload.Item item = BarcodePayload.parseItem(scan.itemQrPayload());
            String rackCode = BarcodePayload.parseLocation(scan.locationBarcode());
            if (item == null || rackCode == null) return null;
            PurchaseOrderLine source = sources.computeIfAbsent(scan.sourceLineId(), id ->
                    purchaseOrderRepository.findLineByIdForUpdate(id).orElse(null));
            Rak rack = rakRepository.findByKodeRak(rackCode).orElse(null);
            if (source == null || rack == null || !source.getBarang().getKodeBarang().equalsIgnoreCase(item.sku())
                    || !isPurchaseReturnPicklistZoneAllowed(rack)) return null;
            InventoryLotBalance lot = lotBalanceRepository.findByBarangIdAndLocationKeyAndBatchCodeForUpdate(
                    source.getBarang().getId(), "RAK-" + rack.getId(), item.batchCode()).orElse(null);
            if (lot == null || !java.util.Objects.equals(lot.getExpiryDate(), item.expiryDate())
                    || lot.getExpiryDate() != null && lot.getExpiryDate().isBefore(LocalDate.now())) return null;
            String lotKey = source.getBarang().getId() + ":" + rack.getId() + ":" + item.batchCode().toUpperCase(java.util.Locale.ROOT);
            long totalRequested = requestedByLot.merge(lotKey, (long) scan.quantity(), Long::sum);
            if (lot.getAvailableQuantity() < totalRequested) return null;
            lines.add(new ReturnLineInput(scan.sourceLineId(), rack.getId(), item.batchCode(), item.expiryDate(),
                    scan.quantity()));
        }
        return createPurchaseReturn(lines, reason, creatorId);
    }

    @Transactional
    public ReturnRequest createSalesReturn(Integer sourceLineId, Integer rackId, String batchCode,
                                           LocalDate expiryDate, int quantity, String reason, Integer creatorId) {
        return createSalesReturn(List.of(new ReturnLineInput(sourceLineId, rackId, batchCode, expiryDate, quantity)),
                reason, creatorId);
    }

    @Transactional
    public ReturnRequest createSalesReturn(List<ReturnLineInput> lines, String reason, Integer creatorId) {
        if (!validReturnLines(lines) || invalidReason(reason) || creatorId == null) return null;
        User creator = userRepository.findById(creatorId).orElse(null);
        if (creator == null) return null;
        Map<Integer, DeliveryOrderLine> sources = new HashMap<>();
        Map<Integer, Rak> racks = new HashMap<>();
        Map<Integer, Long> requestedBySource = new HashMap<>();
        for (ReturnLineInput line : lines) {
            if (line == null || line.sourceLineId() == null || line.rackId() == null || line.quantity() <= 0
                    || line.batchCode() == null || line.batchCode().isBlank()
                    || line.batchCode().trim().length() > 64
                    || line.expiryDate() != null && line.expiryDate().isBefore(LocalDate.now())) return null;
            DeliveryOrderLine source = sources.computeIfAbsent(line.sourceLineId(), id ->
                    deliveryOrderLineRepository.findByIdForUpdate(id).orElse(null));
            Rak rack = racks.computeIfAbsent(line.rackId(), id -> rakRepository.findById(id).orElse(null));
            DeliveryOrder order = source == null ? null : source.getDeliveryOrder();
            if (source == null || rack == null || order == null
                    || order.getStatus() != DeliveryOrderStatus.DISPATCHED
                    && order.getStatus() != DeliveryOrderStatus.PARTIALLY_DISPATCHED) return null;
            requestedBySource.merge(line.sourceLineId(), (long) line.quantity(), Long::sum);
        }
        for (Map.Entry<Integer, Long> entry : requestedBySource.entrySet()) {
            DeliveryOrderLine source = sources.get(entry.getKey());
            long alreadyReturned = returnRepository.sumSalesReturnQuantity(entry.getKey(), COUNTED_RETURN_STATUSES);
            if (alreadyReturned + entry.getValue() > source.getShippedQuantity()) return null;
        }
        ReturnRequest request = new ReturnRequest(returnCode(), ReturnType.SALES, creator,
                reason.trim(), LocalDateTime.now());
        for (ReturnLineInput line : lines) {
            request.addLine(new ReturnRequestLine(sources.get(line.sourceLineId()), racks.get(line.rackId()),
                    line.batchCode().trim(), line.expiryDate(), line.quantity()));
        }
        return returnRepository.save(request);
    }

    @Transactional
    public ReturnRequest createSalesReturnWithBarcodes(Integer sourceLineId, String itemQrPayload,
                                                       String destinationLocationBarcode, int quantity,
                                                       String reason, Integer creatorId) {
        return createSalesReturnWithBarcodes(List.of(new BarcodeReturnLineInput(sourceLineId,
                itemQrPayload, destinationLocationBarcode, quantity)), reason, creatorId);
    }

    @Transactional
    public ReturnRequest createSalesReturnWithBarcodes(List<BarcodeReturnLineInput> scannedLines,
                                                       String reason, Integer creatorId) {
        if (!validBarcodeReturnLines(scannedLines) || invalidReason(reason) || creatorId == null) return null;
        Map<Integer, DeliveryOrderLine> sources = new HashMap<>();
        List<ReturnLineInput> lines = new java.util.ArrayList<>();
        for (BarcodeReturnLineInput scan : scannedLines) {
            if (!validScan(scan)) return null;
            BarcodePayload.Item item = BarcodePayload.parseItem(scan.itemQrPayload());
            String rackCode = BarcodePayload.parseLocation(scan.locationBarcode());
            if (item == null || rackCode == null) return null;
            DeliveryOrderLine source = sources.computeIfAbsent(scan.sourceLineId(), id ->
                    deliveryOrderLineRepository.findByIdForUpdate(id).orElse(null));
            Rak rack = rakRepository.findByKodeRak(rackCode).orElse(null);
            if (source == null || rack == null
                    || !source.getBarang().getKodeBarang().equalsIgnoreCase(item.sku())) return null;
            lines.add(new ReturnLineInput(scan.sourceLineId(), rack.getId(), item.batchCode(), item.expiryDate(),
                    scan.quantity()));
        }
        return createSalesReturn(lines, reason, creatorId);
    }

    @Transactional
    public boolean reviewReturn(Integer requestId, Integer reviewerId, boolean approved, String notes) {
        if (requestId == null || reviewerId == null || !approved && invalidReason(notes)) return false;
        ReturnRequest request = returnRepository.findByIdWithLinesForUpdate(requestId).orElse(null);
        User reviewer = userRepository.findById(reviewerId).orElse(null);
        if (request == null || reviewer == null || request.getStatus() != ReturnStatus.PENDING_REVIEW
                || request.getCreatedBy().getId().equals(reviewerId)
                || !approvalRoutingService.canReview(request.getCreatedBy(), reviewer)) return false;
        request.review(reviewer, approved, approved ? null : notes.trim(), LocalDateTime.now());
        returnRepository.save(request);
        return true;
    }

    @Transactional
    public boolean postApprovedReturn(Integer requestId, Integer userId) {
        if (requestId == null || userId == null) return false;
        ReturnRequest request = returnRepository.findByIdWithLinesForUpdate(requestId).orElse(null);
        User poster = userRepository.findById(userId).orElse(null);
        if (request == null || poster == null || request.getStatus() != ReturnStatus.APPROVED
                || request.getLines().isEmpty()) return false;
        List<PurchaseOrderLine> purchaseSources = new java.util.ArrayList<>();
        List<DeliveryOrderLine> salesSources = new java.util.ArrayList<>();
        for (ReturnRequestLine line : request.getLines()) {
            if (request.getReturnType() == ReturnType.PURCHASE) {
                if (!isPurchaseReturnPicklistZoneAllowed(line.getRak())) return false;
                PurchaseOrderLine source = purchaseOrderRepository
                        .findLineByIdForUpdate(line.getPurchaseOrderLine().getId()).orElse(null);
                if (source == null || source.getPurchaseOrder().getStatus() != PurchaseOrderStatus.RECEIVED
                        && source.getPurchaseOrder().getStatus() != PurchaseOrderStatus.PARTIALLY_RECEIVED
                        || returnRepository.sumPurchaseReturnQuantity(source.getId(),
                        COUNTED_RETURN_STATUSES) > source.getReceivedQuantity()) return false;
                purchaseSources.add(source);
            } else {
                DeliveryOrderLine source = line.getDeliveryOrderLine();
                DeliveryOrderLine lockedSource = deliveryOrderLineRepository
                        .findByIdForUpdate(source.getId()).orElse(null);
                DeliveryOrder sourceOrder = lockedSource == null ? null : lockedSource.getDeliveryOrder();
                if (lockedSource == null || sourceOrder == null
                        || sourceOrder.getStatus() != DeliveryOrderStatus.DISPATCHED
                        && sourceOrder.getStatus() != DeliveryOrderStatus.PARTIALLY_DISPATCHED
                        || returnRepository.sumSalesReturnQuantity(source.getId(), COUNTED_RETURN_STATUSES)
                        > lockedSource.getShippedQuantity()) return false;
                salesSources.add(lockedSource);
            }
        }
        int sourceIndex = 0;
        for (ReturnRequestLine line : request.getLines()) {
            if (request.getReturnType() == ReturnType.PURCHASE) {
                PurchaseOrderLine source = purchaseSources.get(sourceIndex++);
                if (!outboundStockPoster.postOutbound(line.getBarang().getId(), line.getRak().getId(),
                        line.getBatchCode(), line.getQuantity(), "Purchase Return " + request.getReturnCode()
                                + " dari PO " + source.getPurchaseOrder().getOrderCode(), userId)) {
                    throw new IllegalStateException("Goods Issue purchase return gagal; transaksi dibatalkan.");
                }
            } else {
                DeliveryOrderLine source = salesSources.get(sourceIndex++);
                DeliveryOrder sourceOrder = source.getDeliveryOrder();
                if (!inboundStockPoster.postInbound(line.getBarang().getId(), null, line.getRak().getId(),
                        line.getBatchCode(), line.getExpiryDate(), line.getQuantity(),
                        "Sales Return " + request.getReturnCode() + " dari DO "
                                + sourceOrder.getOrderCode(), userId)) {
                    throw new IllegalStateException("Penerimaan sales return gagal; transaksi dibatalkan.");
                }
            }
        }
        request.markPosted(LocalDateTime.now());
        returnRepository.save(request);
        return true;
    }

    private boolean invalidReason(String reason) {
        return reason == null || reason.isBlank() || reason.trim().length() > 2000;
    }

    private boolean validReturnLines(List<ReturnLineInput> lines) {
        return lines != null && !lines.isEmpty() && lines.size() <= 100;
    }

    private boolean validBarcodeReturnLines(List<BarcodeReturnLineInput> lines) {
        return lines != null && !lines.isEmpty() && lines.size() <= 100;
    }

    private boolean validScan(BarcodeReturnLineInput scan) {
        return scan != null && scan.sourceLineId() != null && scan.sourceLineId() > 0
                && scan.quantity() > 0 && scan.itemQrPayload() != null
                && !scan.itemQrPayload().isBlank() && scan.itemQrPayload().length() <= 512
                && scan.locationBarcode() != null && !scan.locationBarcode().isBlank()
                && scan.locationBarcode().length() <= 512;
    }

    private boolean isPurchaseReturnPicklistZoneAllowed(Rak rack) {
        String zoneCode = rack == null || rack.getSection() == null ? null : rack.getSection().getZoneCode();
        return WarehouseZoneRuleService.isPicklistZoneAllowed(zoneCode,
                warehouseZoneRuleService.getAllowedZones(WarehouseZoneRule.Operation.PURCHASE_RETURN_PICKLIST_ZONE));
    }

    public record MobileReturnScan(Integer lineId, String itemBarcode, String locationBarcode) {}
    public record BarcodeReturnLineInput(Integer sourceLineId, String itemQrPayload,
                                         String locationBarcode, int quantity) {}
    public record ReturnLineInput(Integer sourceLineId, Integer rackId, String batchCode,
                                  LocalDate expiryDate, int quantity) {}
    private record ReturnLotKey(Integer itemId, Integer rackId, String batchCode) {}

    private String returnCode() {
        return "RET-" + UUID.randomUUID().toString().replace("-", "")
                .substring(0, 16).toUpperCase(java.util.Locale.ROOT);
    }
}
