package com.mycompany.tugas_akhir.service;

import com.mycompany.tugas_akhir.model.Barang;
import com.mycompany.tugas_akhir.model.Customer;
import com.mycompany.tugas_akhir.model.DeliveryOrder;
import com.mycompany.tugas_akhir.model.DeliveryOrderLine;
import com.mycompany.tugas_akhir.model.DeliveryOrderStatus;
import com.mycompany.tugas_akhir.model.InventoryLotBalance;
import com.mycompany.tugas_akhir.model.DeliveryOrderLotReservation;
import com.mycompany.tugas_akhir.model.DeliveryOrderPickScan;
import com.mycompany.tugas_akhir.model.DeliveryOrderFulfillmentAudit;
import com.mycompany.tugas_akhir.model.PickTask;
import com.mycompany.tugas_akhir.model.PickTaskStatus;
import com.mycompany.tugas_akhir.model.Rak;
import com.mycompany.tugas_akhir.model.Section;
import com.mycompany.tugas_akhir.model.User;
import com.mycompany.tugas_akhir.repository.BarangRepository;
import com.mycompany.tugas_akhir.repository.CustomerRepository;
import com.mycompany.tugas_akhir.repository.DeliveryOrderRepository;
import com.mycompany.tugas_akhir.repository.InventoryLotBalanceRepository;
import com.mycompany.tugas_akhir.repository.DeliveryOrderLotReservationRepository;
import com.mycompany.tugas_akhir.repository.DeliveryOrderPickScanRepository;
import com.mycompany.tugas_akhir.repository.DeliveryOrderFulfillmentAuditRepository;
import com.mycompany.tugas_akhir.repository.MobileFulfillmentOrderProjection;
import com.mycompany.tugas_akhir.repository.PickTaskRepository;
import com.mycompany.tugas_akhir.repository.SectionRepository;
import com.mycompany.tugas_akhir.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.time.LocalDateTime;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.LinkedHashMap;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;
import java.util.Objects;
import java.util.Optional;

@Service
public class DeliveryOrderService {

    private static final List<DeliveryOrderStatus> MOBILE_FULFILLMENT_STATUSES = List.of(
            DeliveryOrderStatus.PICKED, DeliveryOrderStatus.PACKED, DeliveryOrderStatus.LOADED);

    public record MobilePickInstruction(Integer lineId, String itemCode, String itemName, String unit,
                                        String rackCode, String batchCode, LocalDate expiryDate,
                                        Integer reservedQuantity, Integer pickedQuantity,
                                        Integer remainingQuantity) {}
    public record MobileFulfillmentSummary(Integer id, String orderCode, String customerName,
                                           DeliveryOrderStatus status, LocalDateTime createdAt) {}
    public record MobileFulfillmentLine(Integer lineId, String itemCode, String itemName,
                                        Integer pickedQuantity, Integer packedQuantity,
                                        Integer loadedQuantity, Integer availableQuantity) {}
    public record MobileFulfillmentDetail(MobileFulfillmentSummary order, List<MobileFulfillmentLine> lines) {}

    private static final Logger log = LoggerFactory.getLogger(DeliveryOrderService.class);
    private final DeliveryOrderRepository deliveryOrderRepository;
    private final CustomerRepository customerRepository;
    private final BarangRepository barangRepository;
    private final UserRepository userRepository;
    private final OutboundStockPoster stockPoster;
    private final InventoryLotBalanceRepository lotBalanceRepository;
    private final DeliveryOrderLotReservationRepository reservationRepository;
    private final DeliveryOrderPickScanRepository pickScanRepository;
    private final DeliveryOrderFulfillmentAuditRepository fulfillmentAuditRepository;
    private final PickTaskRepository pickTaskRepository;
    private final SectionRepository sectionRepository;
    private final InventoryPickingPolicy pickingPolicy;
    private final WarehouseZoneRuleService warehouseZoneRuleService;

    public DeliveryOrderService(DeliveryOrderRepository deliveryOrderRepository,
                                CustomerRepository customerRepository,
                                BarangRepository barangRepository,
                                UserRepository userRepository,
                                OutboundStockPoster stockPoster,
                                InventoryLotBalanceRepository lotBalanceRepository,
                                DeliveryOrderLotReservationRepository reservationRepository,
                                DeliveryOrderPickScanRepository pickScanRepository,
                                DeliveryOrderFulfillmentAuditRepository fulfillmentAuditRepository,
                                PickTaskRepository pickTaskRepository,
                                SectionRepository sectionRepository,
                                InventoryPickingPolicy pickingPolicy,
                                WarehouseZoneRuleService warehouseZoneRuleService) {
        this.deliveryOrderRepository = deliveryOrderRepository;
        this.customerRepository = customerRepository;
        this.barangRepository = barangRepository;
        this.userRepository = userRepository;
        this.stockPoster = stockPoster;
        this.lotBalanceRepository = lotBalanceRepository;
        this.reservationRepository = reservationRepository;
        this.pickScanRepository = pickScanRepository;
        this.fulfillmentAuditRepository = fulfillmentAuditRepository;
        this.pickTaskRepository = pickTaskRepository;
        this.sectionRepository = sectionRepository;
        this.pickingPolicy = pickingPolicy;
        this.warehouseZoneRuleService = warehouseZoneRuleService;
    }

    @Transactional(readOnly = true)
    public List<DeliveryOrder> getRecentOrders() {
        return deliveryOrderRepository.findTop100ByOrderByCreatedAtDesc();
    }

    @Transactional(readOnly = true)
    public List<DeliveryOrderPickScan> getPickScans(Integer orderId) {
        if (orderId == null) return List.of();
        return pickScanRepository.findByOrderId(orderId, PageRequest.of(0, 100));
    }

    @Transactional(readOnly = true)
    public List<PickTask> getPickTasks(Integer orderId) {
        return orderId == null ? List.of() : pickTaskRepository.findActiveByOrderId(orderId);
    }

    @Transactional(readOnly = true)
    public Page<PickTask> getMobilePickTasks(Integer pickerId, Pageable pageable) {
        return pickTaskRepository.findMobileTasks(pickerId, PickTaskStatus.IN_PROGRESS, pageable);
    }

    @Transactional(readOnly = true)
    public Optional<PickTask> getMobilePickTask(Integer taskId, Integer pickerId) {
        if (taskId == null || pickerId == null) return Optional.empty();
        return pickTaskRepository.findById(taskId).filter(task -> task.getStatus() == PickTaskStatus.IN_PROGRESS
                && task.getAssignedPicker() != null && Objects.equals(task.getAssignedPicker().getId(), pickerId));
    }

    @Transactional(readOnly = true)
    public List<MobilePickInstruction> getMobilePickInstructions(Integer taskId, Integer pickerId) {
        PickTask task = getMobilePickTask(taskId, pickerId).orElse(null);
        if (task == null || task.getAssignedSection() == null || task.getDeliveryOrder() == null) return List.of();
        Integer sectionId = task.getAssignedSection().getId();
        return reservationRepository.findByOrderId(task.getDeliveryOrder().getId()).stream()
                .filter(reservation -> reservation.getLotBalance() != null
                        && reservation.getLotBalance().getRak() != null
                        && reservation.getLotBalance().getRak().getSection() != null
                        && Objects.equals(reservation.getLotBalance().getRak().getSection().getId(), sectionId))
                .map(reservation -> {
                    InventoryLotBalance lot = reservation.getLotBalance();
                    DeliveryOrderLine line = reservation.getDeliveryOrderLine();
                    return new MobilePickInstruction(line.getId(), line.getBarang().getKodeBarang(),
                            line.getBarang().getNamaBarang(), line.getBarang().getSatuan(),
                            lot.getRak().getKodeRak(), lot.getBatchCode(), lot.getExpiryDate(),
                            reservation.getReservedQuantity(), reservation.getPickedQuantity(),
                            reservation.getReservedQuantity() - reservation.getPickedQuantity());
                })
                .toList();
    }

    @Transactional
    public boolean recordMobilePick(Integer taskId, Integer lineId, String itemQrPayload,
                                    String locationBarcode, int quantity, Integer pickerId) {
        PickTask task = getMobilePickTask(taskId, pickerId).orElse(null);
        return task != null && task.getDeliveryOrder() != null
                && recordPickWithBarcodes(task.getDeliveryOrder().getId(), lineId, itemQrPayload,
                locationBarcode, quantity, pickerId);
    }

    @Transactional
    public boolean completeMobilePicking(Integer taskId, Integer pickerId) {
        PickTask requestedTask = getMobilePickTask(taskId, pickerId).orElse(null);
        if (requestedTask == null || requestedTask.getDeliveryOrder() == null) return false;
        Integer orderId = requestedTask.getDeliveryOrder().getId();
        if (deliveryOrderRepository.findByIdWithLinesForUpdate(orderId).isEmpty()) return false;
        List<PickTask> tasks = pickTaskRepository.findActiveByOrderId(orderId);
        if (tasks.isEmpty() || tasks.stream().anyMatch(task -> task.getStatus() != PickTaskStatus.IN_PROGRESS))
            return false;
        return completePicking(orderId);
    }

    @Transactional(readOnly = true)
    public Page<MobileFulfillmentSummary> getMobileFulfillmentOrders(Pageable pageable) {
        return deliveryOrderRepository.findMobileFulfillmentOrders(MOBILE_FULFILLMENT_STATUSES, pageable)
                .map(order -> new MobileFulfillmentSummary(order.id(), order.orderCode(), order.customerName(),
                        order.status(), order.createdAt()));
    }

    @Transactional(readOnly = true)
    public Optional<MobileFulfillmentDetail> getMobileFulfillmentOrder(Integer orderId) {
        if (orderId == null) return Optional.empty();
        DeliveryOrder order = deliveryOrderRepository.findByIdWithLines(orderId).orElse(null);
        if (order == null || !MOBILE_FULFILLMENT_STATUSES.contains(order.getStatus())) return Optional.empty();
        List<DeliveryOrderLotReservation> reservations = reservationRepository.findByOrderId(orderId);
        List<MobileFulfillmentLine> lines = order.getLines().stream().map(line -> {
            List<DeliveryOrderLotReservation> lineReservations = reservations.stream()
                    .filter(reservation -> Objects.equals(reservation.getDeliveryOrderLine().getId(), line.getId()))
                    .toList();
            int picked = lineReservations.stream().mapToInt(DeliveryOrderLotReservation::getPickedQuantity).sum();
            int packed = lineReservations.stream().mapToInt(DeliveryOrderLotReservation::getPackedQuantity).sum();
            int loaded = lineReservations.stream().mapToInt(DeliveryOrderLotReservation::getLoadedQuantity).sum();
            int available = order.getStatus() == DeliveryOrderStatus.PICKED ? picked - packed
                    : order.getStatus() == DeliveryOrderStatus.PACKED ? packed - loaded : 0;
            return new MobileFulfillmentLine(line.getId(), line.getBarang().getKodeBarang(),
                    line.getBarang().getNamaBarang(), picked, packed, loaded, available);
        }).toList();
        return Optional.of(new MobileFulfillmentDetail(new MobileFulfillmentSummary(order.getId(),
                order.getOrderCode(), order.getCustomer().getNamaCustomer(), order.getStatus(),
                order.getCreatedAt()), lines));
    }

    @Transactional
    public boolean recordMobileFulfillmentScan(Integer orderId, Integer lineId, String deliveryOrderBarcode,
                                               String itemQrPayload, int quantity, Integer actorId) {
        if (orderId == null || deliveryOrderBarcode == null || deliveryOrderBarcode.isBlank()
                || deliveryOrderBarcode.length() > 128) return false;
        DeliveryOrder order = deliveryOrderRepository.findByIdWithLines(orderId).orElse(null);
        if (order == null || order.getStatus() != DeliveryOrderStatus.PICKED
                && order.getStatus() != DeliveryOrderStatus.PACKED
                || !order.getOrderCode().equalsIgnoreCase(deliveryOrderBarcode.trim())) return false;
        return recordFulfillmentScan(orderId, lineId, deliveryOrderBarcode, itemQrPayload, quantity, actorId);
    }

    @Transactional
    public DeliveryOrderStatus completeMobileFulfillment(Integer orderId, Integer actorId) {
        if (orderId == null || actorId == null) return null;
        DeliveryOrder order = deliveryOrderRepository.findByIdWithLinesForUpdate(orderId).orElse(null);
        if (order == null) return null;
        boolean packing = order.getStatus() == DeliveryOrderStatus.PICKED;
        if (!packing && order.getStatus() != DeliveryOrderStatus.PACKED) return null;
        Map<Integer, Integer> scannedTotals = new HashMap<>();
        for (DeliveryOrderLotReservation reservation : reservationRepository.findByOrderIdForUpdate(orderId)) {
            int scannedQuantity = packing ? reservation.getPackedQuantity() : reservation.getLoadedQuantity();
            if (scannedQuantity > 0) {
                scannedTotals.merge(reservation.getDeliveryOrderLine().getId(), scannedQuantity, Math::addExact);
            }
        }
        if (scannedTotals.isEmpty()) {
            return null;
        }
        Map<Integer, Integer> quantities = new LinkedHashMap<>();
        order.getLines().forEach(line -> quantities.put(line.getId(), scannedTotals.getOrDefault(line.getId(), 0)));
        boolean completed = packing ? completePacking(orderId, quantities, actorId)
                : completeLoading(orderId, quantities, actorId);
        return completed ? (packing ? DeliveryOrderStatus.PACKED : DeliveryOrderStatus.LOADED) : null;
    }

    @Transactional
    public DeliveryOrderStatus dispatchMobileFulfillment(Integer orderId, Integer actorId) {
        if (orderId == null || actorId == null || !dispatch(orderId, actorId)) return null;
        return deliveryOrderRepository.findStatusById(orderId).orElse(null);
    }

    @Transactional(readOnly = true)
    public List<DeliveryOrderFulfillmentAudit> getFulfillmentAudit(Integer orderId) {
        return orderId == null ? List.of() : fulfillmentAuditRepository.findByOrderId(orderId);
    }

    @Transactional(readOnly = true)
    public List<DeliveryOrderLotReservation> getReservations(Integer orderId) {
        return orderId == null ? List.of() : reservationRepository.findByOrderId(orderId);
    }

    @Transactional
    public DeliveryOrder createDraft(Integer customerId, Integer userId,
                                     Map<Integer, Integer> requestedQuantities, String notes) {
        if (customerId == null || userId == null || requestedQuantities == null
                || requestedQuantities.isEmpty() || requestedQuantities.size() > 100) {
            return null;
        }
        Customer customer = customerRepository.findById(customerId).orElse(null);
        User creator = userRepository.findById(userId).orElse(null);
        if (customer == null || creator == null || "Nonaktif".equalsIgnoreCase(customer.getStatus())) return null;

        DeliveryOrder order = new DeliveryOrder("DO-" + UUID.randomUUID().toString().replace("-", "")
                .substring(0, 16).toUpperCase(java.util.Locale.ROOT), customer, creator, notes, LocalDateTime.now());
        for (Map.Entry<Integer, Integer> request : requestedQuantities.entrySet()) {
            if (request.getKey() == null || request.getValue() == null || request.getValue() <= 0) return null;
            Barang barang = barangRepository.findById(request.getKey()).orElse(null);
            if (barang == null) return null;
            order.addLine(new DeliveryOrderLine(barang, request.getValue()));
        }
        return deliveryOrderRepository.save(order);
    }

    @Transactional
    public boolean releaseToPicking(Integer orderId) {
        DeliveryOrder order = deliveryOrderRepository.findByIdWithLinesForUpdate(orderId).orElse(null);
        if (order == null
                || (order.getStatus() != DeliveryOrderStatus.DRAFT
                && order.getStatus() != DeliveryOrderStatus.PARTIALLY_DISPATCHED)
                || order.getLines().isEmpty()) return false;

        LinkedHashMap<DeliveryOrderLine, LinkedHashMap<InventoryLotBalance, Integer>> planned = new LinkedHashMap<>();
        boolean hasOutstandingQuantity = false;
        Set<String> excludedZones = warehouseZoneRuleService.getExcludedZones(
                com.mycompany.tugas_akhir.model.WarehouseZoneRule.Operation.DELIVERY_ORDER_EXCLUSION);
        for (DeliveryOrderLine line : order.getLines()) {
            int remainingQuantity = line.getRemainingQuantity();
            if (remainingQuantity == 0) continue;
            hasOutstandingQuantity = true;
            Barang barang = barangRepository.findByIdForUpdate(line.getBarang().getId()).orElse(null);
            if (barang == null) return false;
            List<InventoryLotBalance> lots =
                    pickingPolicy.availableAcrossLocations(barang.getId(), java.time.LocalDate.now());
            int remaining = remainingQuantity;
            LinkedHashMap<InventoryLotBalance, Integer> lineReservations = new LinkedHashMap<>();
            for (InventoryLotBalance lot : lots) {
                if (lot.getRak() == null) continue;
                String zoneCode = lot.getRak().getSection() == null
                        ? null : lot.getRak().getSection().getZoneCode();
                if (!WarehouseZoneRuleService.isZoneAllowed(zoneCode, excludedZones)) continue;
                int quantity = Math.min(remaining, lot.getAvailableQuantity());
                if (quantity > 0) lineReservations.put(lot, quantity);
                remaining -= quantity;
                if (remaining == 0) break;
            }
            if (remaining > 0) {
                log.warn("DO {} tidak dirilis: stok SLOC kurang untuk {}.", order.getOrderCode(), barang.getKodeBarang());
                return false;
            }
            planned.put(line, lineReservations);
        }

        if (!hasOutstandingQuantity) return false;
        for (Map.Entry<DeliveryOrderLine, LinkedHashMap<InventoryLotBalance, Integer>> linePlan : planned.entrySet()) {
            for (Map.Entry<InventoryLotBalance, Integer> reservation : linePlan.getValue().entrySet()) {
                InventoryLotBalance lot = reservation.getKey();
                lot.setReservedQuantity(Math.addExact(lot.getReservedQuantity(), reservation.getValue()));
                lotBalanceRepository.save(lot);
                reservationRepository.save(new DeliveryOrderLotReservation(
                        linePlan.getKey(), lot, reservation.getValue()));
            }
        }

        order.setStatus(DeliveryOrderStatus.READY_TO_PICK);
        pickTaskRepository.save(new PickTask("PICK-" + UUID.randomUUID().toString()
                .replace("-", "").substring(0, 16).toUpperCase(java.util.Locale.ROOT),
                order, LocalDateTime.now()));
        deliveryOrderRepository.save(order);
        return true;
    }

    @Transactional
    public boolean startPicking(Integer orderId) {
        DeliveryOrder order = deliveryOrderRepository.findByIdWithLinesForUpdate(orderId).orElse(null);
        if (order == null || order.getStatus() != DeliveryOrderStatus.READY_TO_PICK) return false;
        List<PickTask> tasks = pickTaskRepository.findActiveByOrderId(orderId);
        if (tasks.isEmpty() || tasks.stream().anyMatch(task -> task.getStatus() != PickTaskStatus.ASSIGNED))
            return false;
        for (PickTask task : tasks) {
            task.setStatus(PickTaskStatus.IN_PROGRESS);
            pickTaskRepository.save(task);
        }
        order.setStatus(DeliveryOrderStatus.PICKING);
        deliveryOrderRepository.save(order);
        return true;
    }

    @Transactional
    public boolean assignPicker(Integer orderId, Integer pickerId, Integer sectionId) {
        if (orderId == null || pickerId == null || sectionId == null) return false;
        DeliveryOrder order = deliveryOrderRepository.findByIdWithLinesForUpdate(orderId).orElse(null);
        User picker = userRepository.findById(pickerId).orElse(null);
        Section section = sectionRepository.findById(sectionId).orElse(null);
        if (order == null || picker == null || section == null
                || order.getStatus() != DeliveryOrderStatus.READY_TO_PICK) return false;
        List<PickTask> tasks = pickTaskRepository.findActiveByOrderId(orderId);
        if (tasks.isEmpty() || (tasks.get(0).getStatus() != PickTaskStatus.OPEN
                && tasks.get(0).getStatus() != PickTaskStatus.ASSIGNED)) return false;
        tasks.get(0).assign(picker, section, LocalDateTime.now());
        pickTaskRepository.save(tasks.get(0));
        return true;
    }

    @Transactional
    public boolean assignPickers(Integer orderId, List<PickerAssignment> assignments) {
        if (orderId == null || assignments == null || assignments.isEmpty() || assignments.size() > 24)
            return false;
        DeliveryOrder order = deliveryOrderRepository.findByIdWithLinesForUpdate(orderId).orElse(null);
        if (order == null || order.getStatus() != DeliveryOrderStatus.READY_TO_PICK) return false;
        List<PickTask> activeTasks = pickTaskRepository.findActiveByOrderId(orderId);
        if (activeTasks.isEmpty() || activeTasks.stream().anyMatch(task -> task.getStatus() != PickTaskStatus.OPEN
                && task.getStatus() != PickTaskStatus.ASSIGNED)) return false;
        Set<Integer> sectionIds = new HashSet<>();
        List<ResolvedPickerAssignment> resolved = new ArrayList<>();
        for (PickerAssignment assignment : assignments) {
            if (assignment == null || assignment.pickerId() == null || assignment.sectionId() == null
                    || assignment.shift() == null || assignment.shift().isBlank()
                    || assignment.shift().trim().length() > 40 || !sectionIds.add(assignment.sectionId())) return false;
            User picker = userRepository.findById(assignment.pickerId()).orElse(null);
            Section section = sectionRepository.findById(assignment.sectionId()).orElse(null);
            if (picker == null || section == null) return false;
            resolved.add(new ResolvedPickerAssignment(picker, section, assignment.shift().trim()));
        }
        Set<Integer> reservedSections = reservationRepository.findByOrderIdForUpdate(orderId).stream()
                .map(DeliveryOrderLotReservation::getLotBalance)
                .filter(lot -> lot.getRak() != null && lot.getRak().getSection() != null)
                .map(lot -> lot.getRak().getSection().getId()).collect(java.util.stream.Collectors.toSet());
        if (resolved.stream().anyMatch(assignment -> !reservedSections.contains(assignment.section().getId())))
            return false;
        for (PickTask task : activeTasks) {
            task.setStatus(PickTaskStatus.CANCELLED);
            pickTaskRepository.save(task);
        }
        for (ResolvedPickerAssignment assignment : resolved) {
            PickTask task = new PickTask("PICK-" + UUID.randomUUID().toString()
                    .replace("-", "").substring(0, 16).toUpperCase(java.util.Locale.ROOT), order,
                    LocalDateTime.now());
            task.assign(assignment.picker(), assignment.section(), assignment.shift(), LocalDateTime.now());
            pickTaskRepository.save(task);
        }
        return true;
    }

    public record PickerAssignment(Integer pickerId, Integer sectionId, String shift) {}
    private record ResolvedPickerAssignment(User picker, Section section, String shift) {}

    @Transactional
    public boolean completePicking(Integer orderId) {
        DeliveryOrder order = deliveryOrderRepository.findByIdWithLinesForUpdate(orderId).orElse(null);
        if (order == null || order.getStatus() != DeliveryOrderStatus.PICKING) return false;
        List<PickTask> tasks = pickTaskRepository.findActiveByOrderId(orderId);
        if (tasks.isEmpty() || tasks.stream().anyMatch(task -> task.getStatus() != PickTaskStatus.IN_PROGRESS))
            return false;
        List<DeliveryOrderLotReservation> reservations = reservationRepository.findByOrderIdForUpdate(orderId);
        int totalPicked = 0;
        for (DeliveryOrderLine line : order.getLines()) {
            int picked = reservations.stream()
                .filter(reservation -> Objects.equals(
                    reservation.getDeliveryOrderLine().getId(), line.getId()))
                .mapToInt(DeliveryOrderLotReservation::getPickedQuantity).sum();
            if (picked < 0 || picked > line.getRemainingQuantity()) return false;
            totalPicked = Math.addExact(totalPicked, picked);
        }
        if (totalPicked == 0) return false;

        LinkedHashMap<DeliveryOrderLotReservation, InventoryLotBalance> lockedLots = new LinkedHashMap<>();
        for (DeliveryOrderLotReservation reservation : new ArrayList<>(reservations)) {
            if (reservation.getPickedQuantity() < 0
                    || reservation.getPickedQuantity() > reservation.getReservedQuantity()) return false;
            InventoryLotBalance lot = lotBalanceRepository
                    .findByIdForUpdate(reservation.getLotBalance().getId()).orElse(null);
            if (lot == null || lot.getReservedQuantity() < reservation.getReservedQuantity()) return false;
            lockedLots.put(reservation, lot);
        }
        for (DeliveryOrderLotReservation reservation : new ArrayList<>(reservations)) {
            InventoryLotBalance lot = lockedLots.get(reservation);
            int picked = reservation.getPickedQuantity();
            lot.setReservedQuantity(lot.getReservedQuantity() - reservation.getReservedQuantity() + picked);
            lotBalanceRepository.save(lot);
            if (picked == 0) {
                reservationRepository.delete(reservation);
            } else {
                reservation.setReservedQuantity(picked);
                reservation.setPackedQuantity(0);
                reservation.setLoadedQuantity(0);
                reservationRepository.save(reservation);
            }
        }
        order.setStatus(DeliveryOrderStatus.PICKED);
        for (PickTask task : tasks) {
            task.setStatus(PickTaskStatus.COMPLETED);
            task.setCompletedAt(LocalDateTime.now());
            pickTaskRepository.save(task);
        }
        deliveryOrderRepository.save(order);
        return true;
    }

    @Transactional
    public boolean recordPickWithBarcodes(Integer orderId, Integer lineId, String itemQrPayload,
                                          String locationBarcode, int quantity, Integer pickerId) {
        BarcodePayload.Item item = BarcodePayload.parseItem(itemQrPayload);
        String rackCode = BarcodePayload.parseLocation(locationBarcode);
        if (item == null || rackCode == null) return false;
        return recordPickValidated(orderId, lineId, item.sku(), rackCode, item.batchCode(),
                item.expiryDate(), quantity, pickerId);
    }

    @Transactional
    public boolean recordFulfillmentScan(Integer orderId, Integer lineId, String deliveryOrderBarcode,
                                         String itemQrPayload, int quantity, Integer actorId) {
        BarcodePayload.Item item = BarcodePayload.parseItem(itemQrPayload);
        if (item == null || orderId == null || lineId == null || quantity <= 0 || actorId == null) return false;
        DeliveryOrder order = deliveryOrderRepository.findByIdWithLinesForUpdate(orderId).orElse(null);
        User actor = userRepository.findById(actorId).orElse(null);
        boolean packing = order != null && order.getStatus() == DeliveryOrderStatus.PICKED;
        boolean loading = order != null && order.getStatus() == DeliveryOrderStatus.PACKED;
        if (order == null || actor == null || (!packing && !loading)) return false;
        if (loading && (deliveryOrderBarcode == null
                || !order.getOrderCode().equalsIgnoreCase(deliveryOrderBarcode.trim()))) return false;
        DeliveryOrderLine line = order.getLines().stream()
                .filter(value -> Objects.equals(value.getId(), lineId)).findFirst().orElse(null);
        if (line == null || !line.getBarang().getKodeBarang().equalsIgnoreCase(item.sku().trim())) return false;

        for (DeliveryOrderLotReservation reservation : reservationRepository.findByLineIdForUpdate(lineId)) {
            InventoryLotBalance lot = reservation.getLotBalance();
            if (lot == null || !lot.getBatchCode().equalsIgnoreCase(item.batchCode().trim())
                    || !Objects.equals(lot.getExpiryDate(), item.expiryDate())) continue;
            int available = packing
                    ? reservation.getPickedQuantity() - reservation.getPackedQuantity()
                    : reservation.getPackedQuantity() - reservation.getLoadedQuantity();
            if (quantity > available) return false;
            if (packing) reservation.setPackedQuantity(reservation.getPackedQuantity() + quantity);
            else reservation.setLoadedQuantity(reservation.getLoadedQuantity() + quantity);
            reservationRepository.save(reservation);
            fulfillmentAuditRepository.save(new DeliveryOrderFulfillmentAudit(order, line, actor,
                    packing ? "PACKING_SCAN" : "LOADING_SCAN", quantity, LocalDateTime.now()));
            return true;
        }
        return false;
    }

    private boolean recordPickValidated(Integer orderId, Integer lineId, String scannedSku, String scannedRack,
                                        String scannedBatch, LocalDate scannedExpiry, int quantity, Integer pickerId) {
        if (orderId == null || lineId == null || quantity <= 0 || pickerId == null) return false;
        DeliveryOrder order = deliveryOrderRepository.findByIdWithLinesForUpdate(orderId).orElse(null);
        User picker = userRepository.findById(pickerId).orElse(null);
        if (order == null || picker == null || order.getStatus() != DeliveryOrderStatus.PICKING) return false;
        DeliveryOrderLine line = order.getLines().stream()
            .filter(value -> java.util.Objects.equals(value.getId(), lineId))
            .findFirst().orElse(null);
        if (line == null || scannedSku == null
            || !line.getBarang().getKodeBarang().equalsIgnoreCase(scannedSku.trim())) return false;
        List<PickTask> tasks = pickTaskRepository.findActiveByOrderId(orderId);
        if (tasks.stream().noneMatch(task -> task.getStatus() == PickTaskStatus.IN_PROGRESS
                && task.getAssignedPicker() != null
                && java.util.Objects.equals(task.getAssignedPicker().getId(), pickerId))) return false;

        List<DeliveryOrderLotReservation> reservations = reservationRepository.findByLineIdForUpdate(lineId);
        for (DeliveryOrderLotReservation reservation : reservations) {
            InventoryLotBalance lot = reservation.getLotBalance();
            if (lot.getRak() == null || scannedRack == null || scannedBatch == null
                || lot.getRak().getSection() == null
                || !lot.getRak().getKodeRak().equalsIgnoreCase(scannedRack.trim())
                || !lot.getBatchCode().equalsIgnoreCase(scannedBatch.trim())
                || !java.util.Objects.equals(scannedExpiry, lot.getExpiryDate())) continue;
            Integer sectionId = lot.getRak().getSection().getId();
            boolean assignedHere = tasks.stream().anyMatch(task -> task.getStatus() == PickTaskStatus.IN_PROGRESS
                    && task.getAssignedPicker() != null && task.getAssignedSection() != null
                    && java.util.Objects.equals(task.getAssignedPicker().getId(), pickerId)
                    && java.util.Objects.equals(task.getAssignedSection().getId(), sectionId));
            if (!assignedHere) continue;
            int nextPicked = reservation.getPickedQuantity() + quantity;
            if (nextPicked > reservation.getReservedQuantity()) return false;
            reservation.setPickedQuantity(nextPicked);
            reservationRepository.save(reservation);
            pickScanRepository.save(new DeliveryOrderPickScan(order, line, picker, lot,
                    line.getBarang().getKodeBarang(), lot.getRak().getKodeRak(), lot.getBatchCode(),
                    scannedExpiry,
                    quantity, LocalDateTime.now()));
            return true;
        }
        return false;
    }

    @Transactional
    public boolean completePacking(Integer orderId, Map<Integer, Integer> packedQuantities, Integer actorId) {
        return completeFulfillmentStage(orderId, packedQuantities,
                DeliveryOrderStatus.PICKED, DeliveryOrderStatus.PACKED, false, actorId);
    }

    @Transactional
    public boolean completeLoading(Integer orderId, Map<Integer, Integer> loadedQuantities, Integer actorId) {
        return completeFulfillmentStage(orderId, loadedQuantities,
                DeliveryOrderStatus.PACKED, DeliveryOrderStatus.LOADED, true, actorId);
    }

    @Transactional
    public boolean dispatch(Integer orderId, Integer userId) {
        DeliveryOrder order = deliveryOrderRepository.findByIdWithLinesForUpdate(orderId).orElse(null);
        User user = userId == null ? null : userRepository.findById(userId).orElse(null);
        if (order == null || user == null || order.getStatus() != DeliveryOrderStatus.LOADED) return false;

        List<DeliveryOrderLotReservation> reservations = reservationRepository.findByOrderIdForUpdate(orderId);
        int totalLoaded = 0;
        Map<DeliveryOrderLine, Integer> loadedByLine = new LinkedHashMap<>();
        for (DeliveryOrderLine line : order.getLines()) {
            int loaded = reservations.stream()
                        .filter(reservation -> Objects.equals(
                            reservation.getDeliveryOrderLine().getId(), line.getId()))
                    .mapToInt(DeliveryOrderLotReservation::getLoadedQuantity).sum();
            if (loaded < 0 || loaded > line.getRemainingQuantity()) return false;
            loadedByLine.put(line, loaded);
            totalLoaded = Math.addExact(totalLoaded, loaded);
        }
        if (totalLoaded == 0) return false;

        for (DeliveryOrderLotReservation reservation : reservations) {
            if (reservation.getLoadedQuantity() > reservation.getPackedQuantity()
                    || reservation.getPackedQuantity() > reservation.getPickedQuantity()
                    || reservation.getPickedQuantity() > reservation.getReservedQuantity()) {
                throw new IllegalStateException("Jumlah fulfilment lot tidak valid; dispatch dibatalkan.");
            }
            InventoryLotBalance lot = lotBalanceRepository
                    .findByIdForUpdate(reservation.getLotBalance().getId()).orElse(null);
            if (lot == null) throw new IllegalStateException("Lot reservasi tidak ditemukan; dispatch dibatalkan.");
            Rak rack = lot.getRak();
            if (rack == null || lot.getReservedQuantity() < reservation.getReservedQuantity()) {
                throw new IllegalStateException("Reservasi lot tidak valid; dispatch dibatalkan.");
            }
            lot.setReservedQuantity(lot.getReservedQuantity() - reservation.getReservedQuantity());
            lotBalanceRepository.save(lot);
            if (reservation.getLoadedQuantity() > 0) {
                boolean posted = stockPoster.postOutbound(lot.getBarang().getId(), rack.getId(), lot.getBatchCode(),
                        reservation.getLoadedQuantity(), "Goods Issue " + order.getOrderCode(), userId);
                if (!posted) throw new IllegalStateException("Goods Issue gagal; seluruh dispatch dibatalkan.");
            }
        }

        for (Map.Entry<DeliveryOrderLine, Integer> entry : loadedByLine.entrySet()) {
            entry.getKey().setShippedQuantity(
                    Math.addExact(entry.getKey().getShippedQuantity(), entry.getValue()));
        }
        reservationRepository.deleteAll(reservations);
        boolean outstanding = order.getLines().stream().anyMatch(line -> line.getRemainingQuantity() > 0);
        order.setStatus(outstanding
                ? DeliveryOrderStatus.PARTIALLY_DISPATCHED : DeliveryOrderStatus.DISPATCHED);
        deliveryOrderRepository.save(order);
        return true;
    }

    private boolean completeFulfillmentStage(Integer orderId, Map<Integer, Integer> quantities,
                                             DeliveryOrderStatus expected, DeliveryOrderStatus next,
                                             boolean loading, Integer actorId) {
        if (quantities == null || actorId == null) return false;
        DeliveryOrder order = deliveryOrderRepository.findByIdWithLinesForUpdate(orderId).orElse(null);
        User actor = userRepository.findById(actorId).orElse(null);
        if (order == null || actor == null || order.getStatus() != expected) return false;

        List<DeliveryOrderLotReservation> reservations = reservationRepository.findByOrderIdForUpdate(orderId);
        Map<Integer, Integer> scannedTotals = new HashMap<>();
        for (DeliveryOrderLotReservation reservation : reservations) {
            int scannedQuantity = loading ? reservation.getLoadedQuantity() : reservation.getPackedQuantity();
            if (scannedQuantity > 0) {
                scannedTotals.merge(reservation.getDeliveryOrderLine().getId(), scannedQuantity, Math::addExact);
            }
        }
        Set<Integer> lineIds = new HashSet<>();
        int totalQuantity = 0;
        for (DeliveryOrderLine line : order.getLines()) {
            Integer quantity = quantities.get(line.getId());
            if (quantity == null || quantity < 0) return false;
            if (!scannedTotals.isEmpty()
                    && quantity != scannedTotals.getOrDefault(line.getId(), 0)) return false;
            lineIds.add(line.getId());
            int available = reservations.stream()
                    .filter(reservation -> Objects.equals(
                            reservation.getDeliveryOrderLine().getId(), line.getId()))
                    .mapToInt(loading
                            ? DeliveryOrderLotReservation::getPackedQuantity
                            : DeliveryOrderLotReservation::getPickedQuantity)
                    .sum();
            if (quantity > available) return false;
            totalQuantity = Math.addExact(totalQuantity, quantity);
        }
        if (totalQuantity == 0 || !lineIds.equals(quantities.keySet())) return false;

        for (DeliveryOrderLine line : order.getLines()) {
            int remaining = quantities.get(line.getId());
            for (DeliveryOrderLotReservation reservation : reservations) {
                if (!Objects.equals(reservation.getDeliveryOrderLine().getId(), line.getId())) continue;
                int capacity = loading
                        ? reservation.getPackedQuantity()
                        : reservation.getPickedQuantity();
                int assigned = Math.min(remaining, capacity);
                if (loading) {
                    reservation.setLoadedQuantity(assigned);
                } else {
                    reservation.setPackedQuantity(assigned);
                    reservation.setLoadedQuantity(0);
                }
                reservationRepository.save(reservation);
                remaining -= assigned;
            }
            if (remaining != 0) return false;
            fulfillmentAuditRepository.save(new DeliveryOrderFulfillmentAudit(
                    order, line, actor, loading ? "LOADING" : "PACKING",
                    quantities.get(line.getId()), LocalDateTime.now()));
        }
        order.setStatus(next);
        deliveryOrderRepository.save(order);
        return true;
    }
}
