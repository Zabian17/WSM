package com.mycompany.tugas_akhir.service;

import com.mycompany.tugas_akhir.model.*;
import com.mycompany.tugas_akhir.repository.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
public class StockTransferOrderService {

    private final StockTransferOrderRepository orderRepository;
    private final WarehouseRepository warehouseRepository;
    private final RakRepository rakRepository;
    private final BarangRepository barangRepository;
    private final UserRepository userRepository;
    private final InventoryLotBalanceRepository lotBalanceRepository;
    private final OutboundStockPoster outboundStockPoster;
    private final InboundStockPoster inboundStockPoster;
    private final InventoryPickingPolicy pickingPolicy;
    private final StockTransferOrderScanRepository scanRepository;
    private final ApprovalRouter approvalRoutingService;

    public StockTransferOrderService(StockTransferOrderRepository orderRepository,
                                    WarehouseRepository warehouseRepository,
                                    RakRepository rakRepository,
                                    BarangRepository barangRepository,
                                    UserRepository userRepository,
                                    InventoryLotBalanceRepository lotBalanceRepository,
                                    OutboundStockPoster outboundStockPoster,
                                    InboundStockPoster inboundStockPoster,
                                    InventoryPickingPolicy pickingPolicy,
                                    StockTransferOrderScanRepository scanRepository,
                                    ApprovalRouter approvalRoutingService) {
        this.orderRepository = orderRepository;
        this.warehouseRepository = warehouseRepository;
        this.rakRepository = rakRepository;
        this.barangRepository = barangRepository;
        this.userRepository = userRepository;
        this.lotBalanceRepository = lotBalanceRepository;
        this.outboundStockPoster = outboundStockPoster;
        this.inboundStockPoster = inboundStockPoster;
        this.pickingPolicy = pickingPolicy;
        this.scanRepository = scanRepository;
        this.approvalRoutingService = approvalRoutingService;
    }

    @Transactional(readOnly = true)
    public List<StockTransferOrder> getRecentOrders() {
        return orderRepository.findTop200ByOrderByCreatedAtDesc();
    }

    @Transactional
    public StockTransferOrder createPlan(Integer sourceWarehouseId, Integer destinationWarehouseId,
                                         Integer sourceRackId, Integer destinationRackId,
                                         Integer barangId, int quantity, String reason, Integer userId) {
        if (sourceWarehouseId == null || destinationWarehouseId == null || sourceRackId == null
                || destinationRackId == null || barangId == null || userId == null || quantity <= 0
                || reason == null || reason.isBlank() || reason.trim().length() > 2000
                || sourceWarehouseId.equals(destinationWarehouseId)) return null;
        Warehouse sourceWarehouse = warehouseRepository.findById(sourceWarehouseId).orElse(null);
        Warehouse destinationWarehouse = warehouseRepository.findById(destinationWarehouseId).orElse(null);
        Rak sourceRack = rakRepository.findById(sourceRackId).orElse(null);
        Rak destinationRack = rakRepository.findById(destinationRackId).orElse(null);
        Barang item = barangRepository.findById(barangId).orElse(null);
        User creator = userRepository.findById(userId).orElse(null);
        if (sourceWarehouse == null || destinationWarehouse == null || sourceRack == null
                || destinationRack == null || item == null || creator == null
                || !belongsTo(sourceRack, sourceWarehouseId)
                || !belongsTo(destinationRack, destinationWarehouseId)
                || destinationRack.getTerisi() + (long) quantity > destinationRack.getKapasitas()) return null;
        StockTransferOrder order = new StockTransferOrder("STO-" + UUID.randomUUID().toString()
                .replace("-", "").substring(0, 16).toUpperCase(java.util.Locale.ROOT),
                sourceWarehouse, destinationWarehouse, creator, reason.trim(), LocalDateTime.now());
        order.addLine(new StockTransferOrderLine(item, sourceRack, destinationRack, quantity));
        return orderRepository.save(order);
    }

    @Transactional
    public boolean reviewPlan(Integer orderId, Integer reviewerId, boolean approved, String notes) {
        if (orderId == null || reviewerId == null || !approved
                && (notes == null || notes.isBlank() || notes.trim().length() > 2000)) return false;
        StockTransferOrder order = orderRepository.findByIdWithLinesForUpdate(orderId).orElse(null);
        User reviewer = userRepository.findById(reviewerId).orElse(null);
        if (order == null || reviewer == null
                || order.getStatus() != StockTransferOrderStatus.PENDING_APPROVAL
                || order.getCreatedBy().getId().equals(reviewerId)
                || !approvalRoutingService.canReview(order.getCreatedBy(), reviewer)) return false;
        order.review(reviewer, approved, approved ? null : notes.trim(), LocalDateTime.now());
        orderRepository.save(order);
        return true;
    }

    @Transactional
    public boolean assignPicker(Integer orderId, Integer pickerId) {
        if (orderId == null || pickerId == null) return false;
        StockTransferOrder order = orderRepository.findByIdWithLinesForUpdate(orderId).orElse(null);
        User picker = userRepository.findById(pickerId).orElse(null);
        if (order == null || picker == null || order.getStatus() != StockTransferOrderStatus.APPROVED
                || !scanRepository.findByOrderId(orderId).isEmpty()) return false;
        order.assignPicker(picker, LocalDateTime.now());
        orderRepository.save(order);
        return true;
    }

    @Transactional
    public boolean recordDispatchScan(Integer orderId, Integer pickerId, String itemQrPayload,
                                      String sourceLocationBarcode, int quantity) {
        BarcodePayload.Item scannedItem = BarcodePayload.parseItem(itemQrPayload);
        String sourceRackCode = BarcodePayload.parseLocation(sourceLocationBarcode);
        if (orderId == null || pickerId == null || quantity <= 0
                || scannedItem == null || sourceRackCode == null) return false;
        StockTransferOrder order = orderRepository.findByIdWithLinesForUpdate(orderId).orElse(null);
        User picker = userRepository.findById(pickerId).orElse(null);
        if (order == null || picker == null
                || order.getAssignedPicker() == null
                || !order.getAssignedPicker().getId().equals(pickerId)
                || order.getStatus() != StockTransferOrderStatus.APPROVED
                && order.getStatus() != StockTransferOrderStatus.PARTIALLY_DISPATCHED
                && order.getStatus() != StockTransferOrderStatus.PARTIALLY_DISPATCHED_AND_RECEIVED) return false;
        List<StockTransferOrderScan> scans = scanRepository.findByOrderId(orderId);
        StockTransferOrderLine line = order.getLines().stream()
                .filter(candidate -> candidate.getBarang().getKodeBarang().equalsIgnoreCase(scannedItem.sku()))
                .filter(candidate -> candidate.getSourceRak().getKodeRak().equalsIgnoreCase(sourceRackCode))
                .filter(candidate -> dispatchedQuantity(candidate, scans) < candidate.getQuantity())
                .findFirst().orElse(null);
        if (line == null) return false;
        int remaining = line.getQuantity() - dispatchedQuantity(line, scans);
        if (quantity > remaining) return false;
        Rak sourceRack = line.getSourceRak();
        List<InventoryLotBalance> recommendedLots = pickingPolicy.availableAt(
                line.getBarang().getId(), "RAK-" + sourceRack.getId(), LocalDate.now());
        if (recommendedLots.isEmpty()
                || !recommendedLots.get(0).getBatchCode().equals(scannedItem.batchCode())) return false;
        InventoryLotBalance lot = lotBalanceRepository.findByBarangIdAndLocationKeyAndBatchCodeForUpdate(
                line.getBarang().getId(), "RAK-" + sourceRack.getId(), scannedItem.batchCode()).orElse(null);
        if (lot == null || lot.getRak() == null || !lot.getRak().getId().equals(sourceRack.getId())
                || !java.util.Objects.equals(lot.getExpiryDate(), scannedItem.expiryDate())
                || lot.getAvailableQuantity() < quantity
                || lot.getExpiryDate() != null && lot.getExpiryDate().isBefore(LocalDate.now())) return false;
        if (!outboundStockPoster.postOutbound(line.getBarang().getId(), sourceRack.getId(), lot.getBatchCode(),
                quantity, "STO Dispatch " + order.getOrderCode(), pickerId)) return false;

        LocalDateTime scannedAt = LocalDateTime.now();
        line.addLot(new StockTransferOrderLot(lot.getBatchCode(), lot.getExpiryDate(), quantity));
        scanRepository.save(new StockTransferOrderScan(order, line, picker, lot, null,
                StockTransferOrderScanType.PICK, scannedItem.sku(), sourceRackCode,
                scannedItem.batchCode(), scannedItem.expiryDate(), quantity, scannedAt));
        int totalDispatched = scans.stream()
                .filter(scan -> scan.getScanType() == StockTransferOrderScanType.PICK)
                .mapToInt(StockTransferOrderScan::getQuantity).sum() + quantity;
        int totalReceived = scans.stream()
                .filter(scan -> scan.getScanType() == StockTransferOrderScanType.RECEIVE)
                .mapToInt(StockTransferOrderScan::getQuantity).sum();
        int totalRequested = order.getLines().stream().mapToInt(StockTransferOrderLine::getQuantity).sum();
        order.updateScannedProgress(totalDispatched, totalReceived, totalRequested, scannedAt);
        orderRepository.save(order);
        return true;
    }

    @Transactional
    public boolean recordReceiptScan(Integer orderId, Integer receiverId, String itemQrPayload,
                                     String destinationLocationBarcode, int quantity) {
        BarcodePayload.Item scannedItem = BarcodePayload.parseItem(itemQrPayload);
        String destinationRackCode = BarcodePayload.parseLocation(destinationLocationBarcode);
        if (orderId == null || receiverId == null || quantity <= 0
                || scannedItem == null || destinationRackCode == null) return false;
        StockTransferOrder order = orderRepository.findByIdWithLinesForUpdate(orderId).orElse(null);
        User receiver = userRepository.findById(receiverId).orElse(null);
        if (order == null || receiver == null
                || order.getStatus() != StockTransferOrderStatus.PARTIALLY_DISPATCHED
                && order.getStatus() != StockTransferOrderStatus.PARTIALLY_DISPATCHED_AND_RECEIVED
                && order.getStatus() != StockTransferOrderStatus.DISPATCHED
                && order.getStatus() != StockTransferOrderStatus.PARTIALLY_RECEIVED) return false;
        List<StockTransferOrderScan> scans = scanRepository.findByOrderId(orderId);
        StockTransferOrderLine line = order.getLines().stream()
                .filter(candidate -> candidate.getBarang().getKodeBarang().equalsIgnoreCase(scannedItem.sku()))
                .filter(candidate -> candidate.getDestinationRak().getKodeRak()
                        .equalsIgnoreCase(destinationRackCode))
                .findFirst().orElse(null);
        if (line == null) return false;
        List<StockTransferOrderScan> matchingPicks = scans.stream()
                .filter(scan -> scan.getScanType() == StockTransferOrderScanType.PICK)
                .filter(scan -> scan.getStockTransferOrderLine().getId().equals(line.getId()))
                .filter(scan -> scan.getScannedBatch().equals(scannedItem.batchCode())
                        && java.util.Objects.equals(scan.getScannedExpiry(), scannedItem.expiryDate()))
                .toList();
        int availableForReceipt = matchingPicks.stream()
                .mapToInt(pick -> pick.getQuantity() - receivedQuantity(pick, scans)).sum();
        if (availableForReceipt < quantity) return false;

        int remainingToReceive = quantity;
        LocalDateTime scannedAt = LocalDateTime.now();
        for (StockTransferOrderScan pick : matchingPicks) {
            int pickRemaining = pick.getQuantity() - receivedQuantity(pick, scans);
            int receivedNow = Math.min(remainingToReceive, pickRemaining);
            if (receivedNow <= 0) continue;
            Rak destinationRack = line.getDestinationRak();
            if (!inboundStockPoster.postInbound(line.getBarang().getId(), null, destinationRack.getId(),
                    pick.getScannedBatch(), pick.getScannedExpiry(), receivedNow,
                    "STO In " + order.getOrderCode(), receiverId)) {
                throw new IllegalStateException("STO In gagal; penerimaan dibatalkan.");
            }
            scanRepository.save(new StockTransferOrderScan(order, line, receiver, null, pick,
                    StockTransferOrderScanType.RECEIVE, scannedItem.sku(), destinationRackCode,
                    scannedItem.batchCode(), scannedItem.expiryDate(), receivedNow, scannedAt));
            remainingToReceive -= receivedNow;
            if (remainingToReceive == 0) break;
        }
        int totalReceived = scans.stream()
                .filter(scan -> scan.getScanType() == StockTransferOrderScanType.RECEIVE)
                .mapToInt(StockTransferOrderScan::getQuantity).sum() + quantity;
        int totalDispatched = scans.stream()
                .filter(scan -> scan.getScanType() == StockTransferOrderScanType.PICK)
                .mapToInt(StockTransferOrderScan::getQuantity).sum();
        int totalRequested = order.getLines().stream().mapToInt(StockTransferOrderLine::getQuantity).sum();
        order.updateScannedProgress(totalDispatched, totalReceived, totalRequested, scannedAt);
        orderRepository.save(order);
        return true;
    }

    @Transactional(readOnly = true)
    public List<StockTransferOrderScan> getScans(Integer orderId) {
        if (orderId == null || !orderRepository.existsById(orderId)) return List.of();
        return scanRepository.findByOrderId(orderId);
    }

    @Transactional
    public boolean dispatch(Integer orderId, Integer userId) {
        if (orderId == null || userId == null) return false;
        StockTransferOrder order = orderRepository.findByIdWithLinesForUpdate(orderId).orElse(null);
        if (order == null || userRepository.findById(userId).isEmpty()
                || order.getStatus() != StockTransferOrderStatus.APPROVED || order.getLines().isEmpty()) return false;
        Map<StockTransferOrderLine, List<LotAllocation>> planned = new LinkedHashMap<>();
        for (StockTransferOrderLine line : order.getLines()) {
            String sourceKey = "RAK-" + line.getSourceRak().getId();
            List<InventoryLotBalance> lots =
                    pickingPolicy.availableAt(line.getBarang().getId(), sourceKey, LocalDate.now());
            if (lots.stream().mapToLong(InventoryLotBalance::getAvailableQuantity).sum() < line.getQuantity()) {
                return false;
            }
            int remaining = line.getQuantity();
            List<LotAllocation> allocations = new java.util.ArrayList<>();
            for (InventoryLotBalance lot : lots) {
                int quantity = Math.min(remaining, lot.getAvailableQuantity());
                if (quantity <= 0) continue;
                allocations.add(new LotAllocation(lot.getBatchCode(), lot.getExpiryDate(), quantity));
                remaining -= quantity;
                if (remaining == 0) break;
            }
            if (remaining > 0) return false;
            planned.put(line, allocations);
        }
        for (Map.Entry<StockTransferOrderLine, List<LotAllocation>> entry : planned.entrySet()) {
            StockTransferOrderLine line = entry.getKey();
            for (LotAllocation allocation : entry.getValue()) {
                if (!outboundStockPoster.postOutbound(line.getBarang().getId(), line.getSourceRak().getId(),
                        allocation.batchCode(), allocation.quantity(),
                        "STO Dispatch " + order.getOrderCode(), userId)) {
                    throw new IllegalStateException("STO dispatch gagal; transaksi dibatalkan.");
                }
                line.addLot(new StockTransferOrderLot(allocation.batchCode(),
                        allocation.expiryDate(), allocation.quantity()));
            }
        }
        order.markDispatched(LocalDateTime.now());
        orderRepository.save(order);
        return true;
    }

    @Transactional
    public boolean receive(Integer orderId, Integer userId) {
        if (orderId == null || userId == null) return false;
        StockTransferOrder order = orderRepository.findByIdWithLinesForUpdate(orderId).orElse(null);
        if (order == null || userRepository.findById(userId).isEmpty()
                || order.getStatus() != StockTransferOrderStatus.DISPATCHED || order.getLines().isEmpty()) return false;
        if (!scanRepository.findByOrderId(orderId).isEmpty()) return false;
        for (StockTransferOrderLine line : order.getLines()) {
            for (StockTransferOrderLot lot : line.getLots()) {
                if (!inboundStockPoster.postInbound(line.getBarang().getId(), null,
                        line.getDestinationRak().getId(), lot.getBatchCode(), lot.getExpiryDate(),
                        lot.getQuantity(), "STO In " + order.getOrderCode(), userId)) {
                    throw new IllegalStateException("STO In gagal; penerimaan dibatalkan.");
                }
            }
        }
        order.markReceived(LocalDateTime.now());
        orderRepository.save(order);
        return true;
    }

    private int dispatchedQuantity(StockTransferOrderLine line, List<StockTransferOrderScan> scans) {
        return scans.stream()
                .filter(scan -> scan.getScanType() == StockTransferOrderScanType.PICK)
                .filter(scan -> scan.getStockTransferOrderLine().getId().equals(line.getId()))
                .mapToInt(StockTransferOrderScan::getQuantity).sum();
    }

    private int receivedQuantity(StockTransferOrderScan pick, List<StockTransferOrderScan> scans) {
        return scans.stream()
                .filter(scan -> scan.getScanType() == StockTransferOrderScanType.RECEIVE)
                .filter(scan -> scan.getSourceScan() != null && scan.getSourceScan().getId().equals(pick.getId()))
                .mapToInt(StockTransferOrderScan::getQuantity).sum();
    }

    private boolean belongsTo(Rak rack, Integer warehouseId) {
        return rack.getSection() != null && rack.getSection().getWarehouse() != null
                && warehouseId.equals(rack.getSection().getWarehouse().getId());
    }

    private record LotAllocation(String batchCode, java.time.LocalDate expiryDate, int quantity) {}
}
