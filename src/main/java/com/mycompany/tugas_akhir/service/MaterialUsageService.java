package com.mycompany.tugas_akhir.service;

import com.mycompany.tugas_akhir.model.*;
import com.mycompany.tugas_akhir.repository.BarangRepository;
import com.mycompany.tugas_akhir.repository.InventoryLotBalanceRepository;
import com.mycompany.tugas_akhir.repository.MaterialUsageRepository;
import com.mycompany.tugas_akhir.repository.MaterialUsageScanRepository;
import com.mycompany.tugas_akhir.repository.MaterialUsageReservationRepository;
import com.mycompany.tugas_akhir.repository.PickTaskRepository;
import com.mycompany.tugas_akhir.repository.RakRepository;
import com.mycompany.tugas_akhir.repository.UserRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
public class MaterialUsageService {

    public record MobilePickInstruction(Integer lineId, String itemCode, String itemName, String unit,
                                         Integer requestedQuantity, Integer issuedQuantity,
                                         Integer remainingQuantity) {}

    private final MaterialUsageRepository materialUsageRepository;
    private final BarangRepository barangRepository;
    private final RakRepository rakRepository;
    private final UserRepository userRepository;
    private final InventoryLotBalanceRepository lotBalanceRepository;
    private final OutboundStockPoster stockPoster;
    private final InventoryPickingPolicy pickingPolicy;
    private final MaterialUsageScanRepository scanRepository;
    private final PickTaskRepository pickTaskRepository;
    private final MaterialUsageReservationRepository reservationRepository;
    private final ApprovalRouter approvalRoutingService;

    public MaterialUsageService(MaterialUsageRepository materialUsageRepository,
                                BarangRepository barangRepository,
                                RakRepository rakRepository,
                                UserRepository userRepository,
                                InventoryLotBalanceRepository lotBalanceRepository,
                                OutboundStockPoster stockPoster,
                                InventoryPickingPolicy pickingPolicy,
                                MaterialUsageScanRepository scanRepository,
                                PickTaskRepository pickTaskRepository,
                                MaterialUsageReservationRepository reservationRepository,
                                ApprovalRouter approvalRoutingService) {
        this.materialUsageRepository = materialUsageRepository;
        this.barangRepository = barangRepository;
        this.rakRepository = rakRepository;
        this.userRepository = userRepository;
        this.lotBalanceRepository = lotBalanceRepository;
        this.stockPoster = stockPoster;
        this.pickingPolicy = pickingPolicy;
        this.scanRepository = scanRepository;
        this.pickTaskRepository = pickTaskRepository;
        this.reservationRepository = reservationRepository;
        this.approvalRoutingService = approvalRoutingService;
    }

    @Transactional(readOnly = true)
    public List<MaterialUsage> getRecentRequests() {
        return materialUsageRepository.findTop100ByOrderByCreatedAtDesc();
    }

    @Transactional(readOnly = true)
    public Page<PickTask> getMobilePickTasks(Integer pickerId, Pageable pageable) {
        return pickTaskRepository.findMobileMaterialUsageTasks(pickerId, pageable);
    }

    @Transactional(readOnly = true)
    public java.util.Optional<PickTask> getMobilePickTask(Integer taskId, Integer pickerId) {
        if (taskId == null || pickerId == null) return java.util.Optional.empty();
        return pickTaskRepository.findById(taskId).filter(task -> task.getMaterialUsage() != null
                && task.getAssignedPicker() != null && pickerId.equals(task.getAssignedPicker().getId())
                && (task.getStatus() == PickTaskStatus.ASSIGNED || task.getStatus() == PickTaskStatus.IN_PROGRESS)
                && (task.getMaterialUsage().getStatus() == MaterialUsageStatus.APPROVED
                || task.getMaterialUsage().getStatus() == MaterialUsageStatus.PARTIALLY_ISSUED));
    }

    @Transactional(readOnly = true)
    public List<MobilePickInstruction> getMobilePickInstructions(Integer taskId, Integer pickerId) {
        PickTask task = getMobilePickTask(taskId, pickerId).orElse(null);
        if (task == null) return List.of();
        Map<Integer, Integer> issuedByLine = new java.util.HashMap<>();
        for (MaterialUsageScan scan : scanRepository.findByRequestId(task.getMaterialUsage().getId())) {
            Integer lineId = scan.getMaterialUsageLine().getId();
            issuedByLine.merge(lineId, scan.getQuantity(), Integer::sum);
        }
        return task.getMaterialUsage().getLines().stream().map(line -> {
            int issued = issuedByLine.getOrDefault(line.getId(), 0);
            return new MobilePickInstruction(line.getId(), line.getBarang().getKodeBarang(),
                    line.getBarang().getNamaBarang(), line.getBarang().getSatuan(),
                    line.getRequestedQuantity(), issued, Math.max(0, line.getRequestedQuantity() - issued));
        }).toList();
    }

    @Transactional
    public boolean recordMobilePick(Integer taskId, String itemBarcode, String locationBarcode,
                                    Integer quantity, Integer pickerId) {
        PickTask task = getMobilePickTask(taskId, pickerId).orElse(null);
        return quantity != null && quantity >= 1 && task != null
                && issueApprovedRequestWithBarcodes(task.getMaterialUsage().getId(), pickerId,
                itemBarcode, locationBarcode, quantity);
    }

    @Transactional(readOnly = true)
    public List<MaterialUsageScan> getScans(Integer requestId) {
        if (requestId == null || !materialUsageRepository.existsById(requestId)) return List.of();
        return scanRepository.findByRequestId(requestId);
    }

    @Transactional
    public MaterialUsage createRequest(Integer userId, String reason, Map<Integer, Integer> quantities) {
        if (userId == null || reason == null || reason.isBlank() || reason.trim().length() > 2000
                || quantities == null || quantities.isEmpty() || quantities.size() > 100) return null;
        User creator = userRepository.findById(userId).orElse(null);
        if (creator == null) return null;
        MaterialUsage request = new MaterialUsage("MTU-" + UUID.randomUUID().toString()
                .replace("-", "").substring(0, 16).toUpperCase(java.util.Locale.ROOT),
                creator, reason.trim(), LocalDateTime.now());
        for (Map.Entry<Integer, Integer> entry : quantities.entrySet()) {
            if (entry.getKey() == null || entry.getValue() == null || entry.getValue() <= 0) return null;
            Barang item = barangRepository.findById(entry.getKey()).orElse(null);
            if (item == null) return null;
            request.addLine(new MaterialUsageLine(item, entry.getValue()));
        }
        return materialUsageRepository.save(request);
    }

    @Transactional
    public boolean reviewRequest(Integer requestId, Integer reviewerId, boolean approved, String notes) {
        if (requestId == null || reviewerId == null || (!approved && (notes == null || notes.isBlank()
                || notes.trim().length() > 2000))) return false;
        MaterialUsage request = materialUsageRepository.findByIdWithLinesForUpdate(requestId).orElse(null);
        User reviewer = userRepository.findById(reviewerId).orElse(null);
        if (request == null || reviewer == null || request.getStatus() != MaterialUsageStatus.PENDING_APPROVAL
                || request.getCreatedBy().getId().equals(reviewerId)
                || !approvalRoutingService.canReview(request.getCreatedBy(), reviewer)) return false;
        List<MaterialUsageReservation> allocations = approved ? reserveRequest(request) : List.of();
        if (allocations == null) return false;
        request.review(reviewer, approved, approved ? null : notes.trim(), LocalDateTime.now());
        materialUsageRepository.save(request);
        if (approved) {
            for (MaterialUsageReservation allocation : allocations) {
                InventoryLotBalance lot = allocation.getLotBalance();
                lot.setReservedQuantity(lot.getReservedQuantity() + allocation.getReservedQuantity());
                lotBalanceRepository.save(lot);
                reservationRepository.save(allocation);
            }
            String taskCode = "MTP-" + UUID.randomUUID().toString()
                    .replace("-", "").substring(0, 16).toUpperCase(java.util.Locale.ROOT);
            pickTaskRepository.save(new PickTask(taskCode, request, LocalDateTime.now()));
        }
        return true;
    }

    private List<MaterialUsageReservation> reserveRequest(MaterialUsage request) {
        List<MaterialUsageReservation> allocations = new java.util.ArrayList<>();
        for (MaterialUsageLine line : request.getLines()) {
            int remaining = line.getRequestedQuantity();
            for (InventoryLotBalance lot : pickingPolicy.availableAcrossLocations(
                    line.getBarang().getId(), LocalDate.now())) {
                if (lot.getRak() == null) continue;
                int quantity = Math.min(remaining, lot.getAvailableQuantity());
                if (quantity <= 0) continue;
                allocations.add(new MaterialUsageReservation(line, lot, quantity));
                remaining -= quantity;
                if (remaining == 0) break;
            }
            if (remaining > 0) return null;
        }
        return allocations;
    }

    @Transactional
    public boolean assignPicker(Integer requestId, Integer pickerId) {
        if (requestId == null || pickerId == null) return false;
        MaterialUsage request = materialUsageRepository.findByIdWithLinesForUpdate(requestId).orElse(null);
        User picker = userRepository.findById(pickerId).orElse(null);
        PickTask task = activePickTask(requestId);
        if (request == null || picker == null || task == null
                || request.getStatus() != MaterialUsageStatus.APPROVED
                && request.getStatus() != MaterialUsageStatus.PARTIALLY_ISSUED) return false;
        if (task.getStatus() == PickTaskStatus.ASSIGNED
                && !scanRepository.findByRequestId(requestId).isEmpty()
                || task.getStatus() != PickTaskStatus.OPEN
                && task.getStatus() != PickTaskStatus.ASSIGNED) return false;
        task.assign(picker, null, LocalDateTime.now());
        pickTaskRepository.save(task);
        return true;
    }

    @Transactional
    public boolean issueApprovedRequest(Integer requestId, Integer userId) {
        if (requestId == null || userId == null) return false;
        MaterialUsage request = materialUsageRepository.findByIdWithLinesForUpdate(requestId).orElse(null);
        User issuer = userRepository.findById(userId).orElse(null);
        if (request == null || issuer == null || request.getStatus() != MaterialUsageStatus.APPROVED
                || request.getLines().isEmpty() || !isAssignedPicker(requestId, userId)) return false;
        List<MaterialUsageReservation> reservations = reservationRepository.findByRequestIdForUpdate(requestId);
        for (MaterialUsageLine line : request.getLines()) {
            int reserved = reservations.stream()
                    .filter(value -> java.util.Objects.equals(
                            value.getMaterialUsageLine().getId(), line.getId()))
                    .mapToInt(MaterialUsageReservation::getRemainingQuantity).sum();
            if (reserved != line.getRequestedQuantity()) return false;
        }
        for (MaterialUsageReservation reservation : reservations) {
            int quantity = reservation.getRemainingQuantity();
            InventoryLotBalance lot = lotBalanceRepository.findByIdForUpdate(
                    reservation.getLotBalance().getId()).orElse(null);
            if (quantity <= 0 || lot == null || lot.getRak() == null
                    || lot.getReservedQuantity() < quantity) return false;
            lot.setReservedQuantity(lot.getReservedQuantity() - quantity);
            lotBalanceRepository.save(lot);
            if (!stockPoster.postOutbound(lot.getBarang().getId(), lot.getRak().getId(),
                    lot.getBatchCode(), quantity, "Material Usage " + request.getRequestCode()
                            + ": " + request.getReason(), userId)) {
                throw new IllegalStateException("Goods Issue MRO gagal; seluruh pengeluaran dibatalkan.");
            }
            reservation.setIssuedQuantity(reservation.getReservedQuantity());
            reservationRepository.save(reservation);
        }
        request.markIssued(LocalDateTime.now());
        materialUsageRepository.save(request);
        completePickTask(activePickTask(requestId));
        return true;
    }

    @Transactional
    public boolean issueApprovedRequestWithBarcodes(Integer requestId, Integer userId,
                                                    String itemQrPayload, String locationBarcode,
                                                    int quantity) {
        BarcodePayload.Item scannedItem = BarcodePayload.parseItem(itemQrPayload);
        String rackCode = BarcodePayload.parseLocation(locationBarcode);
        if (requestId == null || userId == null || quantity <= 0
                || scannedItem == null || rackCode == null) return false;
        MaterialUsage request = materialUsageRepository.findByIdWithLinesForUpdate(requestId).orElse(null);
        User issuer = userRepository.findById(userId).orElse(null);
        if (request == null || issuer == null
                || request.getStatus() != MaterialUsageStatus.APPROVED
                && request.getStatus() != MaterialUsageStatus.PARTIALLY_ISSUED
                || request.getLines().isEmpty() || !isAssignedPicker(requestId, userId)) return false;
        MaterialUsageLine line = request.getLines().stream()
                .filter(candidate -> candidate.getBarang().getKodeBarang().equalsIgnoreCase(scannedItem.sku()))
                .findFirst().orElse(null);
        if (line == null) return false;
        List<MaterialUsageScan> existingScans = scanRepository.findByRequestId(requestId);
        int alreadyIssued = existingScans.stream()
                .filter(scan -> scan.getMaterialUsageLine().getId().equals(line.getId()))
                .mapToInt(MaterialUsageScan::getQuantity).sum();
        if (alreadyIssued + quantity > line.getRequestedQuantity()) return false;
        Rak rack = rakRepository.findByKodeRak(rackCode).orElse(null);
        if (rack == null) return false;
        InventoryLotBalance lot = lotBalanceRepository.findByBarangIdAndLocationKeyAndBatchCodeForUpdate(
                line.getBarang().getId(), "RAK-" + rack.getId(), scannedItem.batchCode()).orElse(null);
        if (lot == null || lot.getRak() == null || !java.util.Objects.equals(lot.getRak().getId(), rack.getId())
                || !java.util.Objects.equals(lot.getExpiryDate(), scannedItem.expiryDate())
                || lot.getExpiryDate() != null && lot.getExpiryDate().isBefore(LocalDate.now())) {
            return false;
        }
        MaterialUsageReservation reservation = reservationRepository.findByLineIdForUpdate(line.getId()).stream()
                .filter(value -> java.util.Objects.equals(value.getLotBalance().getId(), lot.getId()))
                .findFirst().orElse(null);
        if (reservation == null || reservation.getRemainingQuantity() < quantity
                || lot.getReservedQuantity() < quantity) return false;
        lot.setReservedQuantity(lot.getReservedQuantity() - quantity);
        lotBalanceRepository.save(lot);
        if (!stockPoster.postOutbound(line.getBarang().getId(), rack.getId(), lot.getBatchCode(),
                quantity, "Material Usage " + request.getRequestCode()
                        + ": " + request.getReason(), userId)) {
            throw new IllegalStateException("Goods Issue MRO gagal; seluruh scan dibatalkan.");
        }
        reservation.setIssuedQuantity(reservation.getIssuedQuantity() + quantity);
        reservationRepository.save(reservation);
        LocalDateTime scannedAt = LocalDateTime.now();
        scanRepository.save(new MaterialUsageScan(request, line, issuer, lot, scannedItem.sku(),
                rackCode, scannedItem.batchCode(), scannedItem.expiryDate(), quantity, scannedAt));
        int totalIssued = existingScans.stream().mapToInt(MaterialUsageScan::getQuantity).sum() + quantity;
        int requested = request.getLines().stream().mapToInt(MaterialUsageLine::getRequestedQuantity).sum();
        PickTask task = activePickTask(requestId);
        if (totalIssued == requested) {
            request.markIssued(scannedAt);
            completePickTask(task);
        } else {
            request.markPartiallyIssued();
            task.setStatus(PickTaskStatus.IN_PROGRESS);
            pickTaskRepository.save(task);
        }
        materialUsageRepository.save(request);
        return true;
    }

    private PickTask activePickTask(Integer requestId) {
        List<PickTask> tasks = pickTaskRepository.findActiveByMaterialUsageId(requestId);
        return tasks.isEmpty() ? null : tasks.get(0);
    }

    private boolean isAssignedPicker(Integer requestId, Integer pickerId) {
        PickTask task = activePickTask(requestId);
        return task != null && task.getAssignedPicker() != null
                && pickerId.equals(task.getAssignedPicker().getId())
                && (task.getStatus() == PickTaskStatus.ASSIGNED
                || task.getStatus() == PickTaskStatus.IN_PROGRESS);
    }

    private void completePickTask(PickTask task) {
        task.setStatus(PickTaskStatus.COMPLETED);
        task.setCompletedAt(LocalDateTime.now());
        pickTaskRepository.save(task);
    }
}
