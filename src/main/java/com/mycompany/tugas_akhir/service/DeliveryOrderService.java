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
import com.mycompany.tugas_akhir.repository.PickTaskRepository;
import com.mycompany.tugas_akhir.repository.SectionRepository;
import com.mycompany.tugas_akhir.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.data.domain.PageRequest;

import java.time.LocalDateTime;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.LinkedHashMap;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Set;
import java.util.Objects;

@Service
public class DeliveryOrderService {

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
                                InventoryPickingPolicy pickingPolicy) {
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
        if (tasks.isEmpty() || tasks.get(0).getStatus() != PickTaskStatus.ASSIGNED) return false;
        tasks.get(0).setStatus(PickTaskStatus.IN_PROGRESS);
        pickTaskRepository.save(tasks.get(0));
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
    public boolean completePicking(Integer orderId) {
        DeliveryOrder order = deliveryOrderRepository.findByIdWithLinesForUpdate(orderId).orElse(null);
        if (order == null || order.getStatus() != DeliveryOrderStatus.PICKING) return false;
        List<PickTask> tasks = pickTaskRepository.findActiveByOrderId(orderId);
        if (tasks.isEmpty() || tasks.get(0).getStatus() != PickTaskStatus.IN_PROGRESS) return false;
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
        tasks.get(0).setStatus(PickTaskStatus.COMPLETED);
        tasks.get(0).setCompletedAt(LocalDateTime.now());
        pickTaskRepository.save(tasks.get(0));
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
        if (tasks.isEmpty() || tasks.get(0).getStatus() != PickTaskStatus.IN_PROGRESS
                || tasks.get(0).getAssignedPicker() == null
                || !java.util.Objects.equals(tasks.get(0).getAssignedPicker().getId(), pickerId)
                || tasks.get(0).getAssignedSection() == null) return false;
        Integer assignedSectionId = tasks.get(0).getAssignedSection().getId();

        List<DeliveryOrderLotReservation> reservations = reservationRepository.findByLineIdForUpdate(lineId);
        for (DeliveryOrderLotReservation reservation : reservations) {
            InventoryLotBalance lot = reservation.getLotBalance();
            if (lot.getRak() == null || scannedRack == null || scannedBatch == null
                || lot.getRak().getSection() == null
                || !Objects.equals(lot.getRak().getSection().getId(), assignedSectionId)
                || !lot.getRak().getKodeRak().equalsIgnoreCase(scannedRack.trim())
                || !lot.getBatchCode().equalsIgnoreCase(scannedBatch.trim())
                || !java.util.Objects.equals(scannedExpiry, lot.getExpiryDate())) continue;
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
        Set<Integer> lineIds = new HashSet<>();
        int totalQuantity = 0;
        for (DeliveryOrderLine line : order.getLines()) {
            Integer quantity = quantities.get(line.getId());
            if (quantity == null || quantity < 0) return false;
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

    private boolean transition(Integer orderId, DeliveryOrderStatus expected, DeliveryOrderStatus next) {
        DeliveryOrder order = deliveryOrderRepository.findByIdWithLinesForUpdate(orderId).orElse(null);
        if (order == null || order.getStatus() != expected) return false;
        order.setStatus(next);
        deliveryOrderRepository.save(order);
        return true;
    }
}