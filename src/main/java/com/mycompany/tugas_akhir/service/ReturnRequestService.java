package com.mycompany.tugas_akhir.service;

import com.mycompany.tugas_akhir.model.*;
import com.mycompany.tugas_akhir.repository.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Service
public class ReturnRequestService {

    private static final List<ReturnStatus> COUNTED_RETURN_STATUSES =
            List.of(ReturnStatus.PENDING_REVIEW, ReturnStatus.APPROVED, ReturnStatus.POSTED);

    private final ReturnRequestRepository returnRepository;
    private final PurchaseOrderRepository purchaseOrderRepository;
    private final DeliveryOrderRepository deliveryOrderRepository;
    private final DeliveryOrderLineRepository deliveryOrderLineRepository;
    private final InventoryLotBalanceRepository lotBalanceRepository;
    private final RakRepository rakRepository;
    private final UserRepository userRepository;
    private final InboundStockPoster inboundStockPoster;
    private final OutboundStockPoster outboundStockPoster;
    private final ApprovalRouter approvalRoutingService;

    public ReturnRequestService(ReturnRequestRepository returnRepository,
                                PurchaseOrderRepository purchaseOrderRepository,
                                DeliveryOrderRepository deliveryOrderRepository,
                                DeliveryOrderLineRepository deliveryOrderLineRepository,
                                InventoryLotBalanceRepository lotBalanceRepository,
                                RakRepository rakRepository,
                                UserRepository userRepository,
                                InboundStockPoster inboundStockPoster,
                                OutboundStockPoster outboundStockPoster,
                                ApprovalRouter approvalRoutingService) {
        this.returnRepository = returnRepository;
        this.purchaseOrderRepository = purchaseOrderRepository;
        this.deliveryOrderRepository = deliveryOrderRepository;
        this.deliveryOrderLineRepository = deliveryOrderLineRepository;
        this.lotBalanceRepository = lotBalanceRepository;
        this.rakRepository = rakRepository;
        this.userRepository = userRepository;
        this.inboundStockPoster = inboundStockPoster;
        this.outboundStockPoster = outboundStockPoster;
        this.approvalRoutingService = approvalRoutingService;
    }

    @Transactional(readOnly = true)
    public List<ReturnRequest> getRecentReturns() {
        return returnRepository.findTop200ByOrderByCreatedAtDesc();
    }

    @Transactional
    public ReturnRequest createPurchaseReturn(Integer sourceLineId, Integer rackId, String batchCode,
                                              int quantity, String reason, Integer creatorId) {
        if (sourceLineId == null || rackId == null || quantity <= 0 || invalidReason(reason)
                || batchCode == null || batchCode.isBlank() || batchCode.trim().length() > 64) return null;
        PurchaseOrderLine source = purchaseOrderRepository.findLineByIdForUpdate(sourceLineId).orElse(null);
        Rak rack = rakRepository.findById(rackId).orElse(null);
        User creator = userRepository.findById(creatorId).orElse(null);
        if (source == null || rack == null || creator == null
                || source.getPurchaseOrder().getStatus() != PurchaseOrderStatus.RECEIVED
                && source.getPurchaseOrder().getStatus() != PurchaseOrderStatus.PARTIALLY_RECEIVED) return null;
        long alreadyReturned = returnRepository.sumPurchaseReturnQuantity(sourceLineId, COUNTED_RETURN_STATUSES);
        if (alreadyReturned + quantity > source.getReceivedQuantity()) return null;
        ReturnRequest request = new ReturnRequest(returnCode(), ReturnType.PURCHASE, creator,
                reason.trim(), LocalDateTime.now());
        request.addLine(new ReturnRequestLine(source, rack, batchCode.trim(), quantity));
        return returnRepository.save(request);
    }

    @Transactional
    public ReturnRequest createPurchaseReturnWithBarcodes(Integer sourceLineId, String itemQrPayload,
                                                          String sourceLocationBarcode, int quantity,
                                                          String reason, Integer creatorId) {
        BarcodePayload.Item scannedItem = BarcodePayload.parseItem(itemQrPayload);
        String rackCode = BarcodePayload.parseLocation(sourceLocationBarcode);
        if (scannedItem == null || rackCode == null || sourceLineId == null || quantity <= 0) return null;
        PurchaseOrderLine source = purchaseOrderRepository.findLineByIdForUpdate(sourceLineId).orElse(null);
        Rak rack = rakRepository.findByKodeRak(rackCode).orElse(null);
        if (source == null || rack == null
                || !source.getBarang().getKodeBarang().equalsIgnoreCase(scannedItem.sku())) return null;
        InventoryLotBalance lot = lotBalanceRepository.findByBarangIdAndLocationKeyAndBatchCodeForUpdate(
                source.getBarang().getId(), "RAK-" + rack.getId(), scannedItem.batchCode()).orElse(null);
        if (lot == null || !java.util.Objects.equals(lot.getExpiryDate(), scannedItem.expiryDate())
                || lot.getAvailableQuantity() < quantity
                || lot.getExpiryDate() != null && lot.getExpiryDate().isBefore(LocalDate.now())) return null;
        return createPurchaseReturn(sourceLineId, rack.getId(), scannedItem.batchCode(),
                quantity, reason, creatorId);
    }

    @Transactional
    public ReturnRequest createSalesReturn(Integer sourceLineId, Integer rackId, String batchCode,
                                           LocalDate expiryDate, int quantity, String reason, Integer creatorId) {
        if (sourceLineId == null || rackId == null || quantity <= 0 || invalidReason(reason)
                || batchCode == null || batchCode.isBlank() || batchCode.trim().length() > 64
                || expiryDate != null && expiryDate.isBefore(LocalDate.now())) return null;
        DeliveryOrderLine source = deliveryOrderLineRepository.findByIdForUpdate(sourceLineId).orElse(null);
        DeliveryOrder order = source == null ? null : source.getDeliveryOrder();
        Rak rack = rakRepository.findById(rackId).orElse(null);
        User creator = userRepository.findById(creatorId).orElse(null);
        if (source == null || rack == null || creator == null
                || order.getStatus() != DeliveryOrderStatus.DISPATCHED
                && order.getStatus() != DeliveryOrderStatus.PARTIALLY_DISPATCHED) return null;
        long alreadyReturned = returnRepository.sumSalesReturnQuantity(sourceLineId, COUNTED_RETURN_STATUSES);
        if (alreadyReturned + quantity > source.getShippedQuantity()) return null;
        ReturnRequest request = new ReturnRequest(returnCode(), ReturnType.SALES, creator,
                reason.trim(), LocalDateTime.now());
        request.addLine(new ReturnRequestLine(source, rack, batchCode.trim(), expiryDate, quantity));
        return returnRepository.save(request);
    }

    @Transactional
    public ReturnRequest createSalesReturnWithBarcodes(Integer sourceLineId, String itemQrPayload,
                                                       String destinationLocationBarcode, int quantity,
                                                       String reason, Integer creatorId) {
        BarcodePayload.Item scannedItem = BarcodePayload.parseItem(itemQrPayload);
        String rackCode = BarcodePayload.parseLocation(destinationLocationBarcode);
        if (scannedItem == null || rackCode == null || sourceLineId == null || quantity <= 0) return null;
        DeliveryOrderLine source = deliveryOrderLineRepository.findByIdForUpdate(sourceLineId).orElse(null);
        Rak rack = rakRepository.findByKodeRak(rackCode).orElse(null);
        if (source == null || rack == null
                || !source.getBarang().getKodeBarang().equalsIgnoreCase(scannedItem.sku())) return null;
        return createSalesReturn(sourceLineId, rack.getId(), scannedItem.batchCode(),
                scannedItem.expiryDate(), quantity, reason, creatorId);
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

    private String returnCode() {
        return "RET-" + UUID.randomUUID().toString().replace("-", "")
                .substring(0, 16).toUpperCase(java.util.Locale.ROOT);
    }
}
