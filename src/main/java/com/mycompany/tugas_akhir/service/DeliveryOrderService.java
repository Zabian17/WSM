package com.mycompany.tugas_akhir.service;

import com.mycompany.tugas_akhir.model.Barang;
import com.mycompany.tugas_akhir.model.Customer;
import com.mycompany.tugas_akhir.model.DeliveryOrder;
import com.mycompany.tugas_akhir.model.DeliveryOrderLine;
import com.mycompany.tugas_akhir.model.DeliveryOrderStatus;
import com.mycompany.tugas_akhir.model.InventoryLotBalance;
import com.mycompany.tugas_akhir.model.DeliveryOrderLotReservation;
import com.mycompany.tugas_akhir.model.Rak;
import com.mycompany.tugas_akhir.model.User;
import com.mycompany.tugas_akhir.repository.BarangRepository;
import com.mycompany.tugas_akhir.repository.CustomerRepository;
import com.mycompany.tugas_akhir.repository.DeliveryOrderRepository;
import com.mycompany.tugas_akhir.repository.InventoryLotBalanceRepository;
import com.mycompany.tugas_akhir.repository.DeliveryOrderLotReservationRepository;
import com.mycompany.tugas_akhir.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.LinkedHashMap;

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

    public DeliveryOrderService(DeliveryOrderRepository deliveryOrderRepository,
                                CustomerRepository customerRepository,
                                BarangRepository barangRepository,
                                UserRepository userRepository,
                                OutboundStockPoster stockPoster,
                                InventoryLotBalanceRepository lotBalanceRepository,
                                DeliveryOrderLotReservationRepository reservationRepository) {
        this.deliveryOrderRepository = deliveryOrderRepository;
        this.customerRepository = customerRepository;
        this.barangRepository = barangRepository;
        this.userRepository = userRepository;
        this.stockPoster = stockPoster;
        this.lotBalanceRepository = lotBalanceRepository;
        this.reservationRepository = reservationRepository;
    }

    @Transactional(readOnly = true)
    public List<DeliveryOrder> getRecentOrders() {
        return deliveryOrderRepository.findTop100ByOrderByCreatedAtDesc();
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
        if (order == null || order.getStatus() != DeliveryOrderStatus.DRAFT || order.getLines().isEmpty()) return false;

        LinkedHashMap<DeliveryOrderLine, LinkedHashMap<InventoryLotBalance, Integer>> planned = new LinkedHashMap<>();
        for (DeliveryOrderLine line : order.getLines()) {
            Barang barang = barangRepository.findByIdForUpdate(line.getBarang().getId()).orElse(null);
            if (barang == null) return false;
            List<InventoryLotBalance> lots = lotBalanceRepository
                    .findAvailableFefoAcrossLocationsForUpdate(barang.getId(), java.time.LocalDate.now());
            int remaining = line.getOrderedQuantity();
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
        deliveryOrderRepository.save(order);
        return true;
    }

    @Transactional
    public boolean startPicking(Integer orderId) {
        return transition(orderId, DeliveryOrderStatus.READY_TO_PICK, DeliveryOrderStatus.PICKING);
    }

    @Transactional
    public boolean completePicking(Integer orderId) {
        DeliveryOrder order = deliveryOrderRepository.findByIdWithLinesForUpdate(orderId).orElse(null);
        if (order == null || order.getStatus() != DeliveryOrderStatus.PICKING) return false;
        List<DeliveryOrderLotReservation> reservations = reservationRepository.findByOrderIdForUpdate(orderId);
        for (DeliveryOrderLine line : order.getLines()) {
            int picked = reservations.stream()
                .filter(reservation -> java.util.Objects.equals(
                    reservation.getDeliveryOrderLine().getId(), line.getId()))
                .mapToInt(DeliveryOrderLotReservation::getPickedQuantity).sum();
            if (picked != line.getOrderedQuantity()) return false;
        }
        order.setStatus(DeliveryOrderStatus.PICKED);
        deliveryOrderRepository.save(order);
        return true;
    }

    @Transactional
    public boolean recordPick(Integer orderId, Integer lineId, String scannedSku, String scannedRack,
                              String scannedBatch, int quantity, Integer pickerId) {
        if (orderId == null || lineId == null || quantity <= 0 || pickerId == null) return false;
        DeliveryOrder order = deliveryOrderRepository.findByIdWithLinesForUpdate(orderId).orElse(null);
        User picker = userRepository.findById(pickerId).orElse(null);
        if (order == null || picker == null || order.getStatus() != DeliveryOrderStatus.PICKING) return false;
        DeliveryOrderLine line = order.getLines().stream()
            .filter(value -> java.util.Objects.equals(value.getId(), lineId))
            .findFirst().orElse(null);
        if (line == null || scannedSku == null
            || !line.getBarang().getKodeBarang().equalsIgnoreCase(scannedSku.trim())) return false;

        List<DeliveryOrderLotReservation> reservations = reservationRepository.findByLineIdForUpdate(lineId);
        for (DeliveryOrderLotReservation reservation : reservations) {
            InventoryLotBalance lot = reservation.getLotBalance();
            if (lot.getRak() == null || scannedRack == null || scannedBatch == null
                || !lot.getRak().getKodeRak().equalsIgnoreCase(scannedRack.trim())
                || !lot.getBatchCode().equalsIgnoreCase(scannedBatch.trim())) continue;
            int nextPicked = reservation.getPickedQuantity() + quantity;
            if (nextPicked > reservation.getReservedQuantity()) return false;
            reservation.setPickedQuantity(nextPicked);
            reservationRepository.save(reservation);
            return true;
        }
        return false;
    }

    @Transactional
    public boolean completePacking(Integer orderId) {
        return transition(orderId, DeliveryOrderStatus.PICKED, DeliveryOrderStatus.PACKED);
    }

    @Transactional
    public boolean completeLoading(Integer orderId) {
        return transition(orderId, DeliveryOrderStatus.PACKED, DeliveryOrderStatus.LOADED);
    }

    @Transactional
    public boolean dispatch(Integer orderId, Integer userId) {
        DeliveryOrder order = deliveryOrderRepository.findByIdWithLinesForUpdate(orderId).orElse(null);
        User user = userId == null ? null : userRepository.findById(userId).orElse(null);
        if (order == null || user == null || order.getStatus() != DeliveryOrderStatus.LOADED) return false;

        List<DeliveryOrderLotReservation> reservations = reservationRepository.findByOrderIdForUpdate(orderId);
        for (DeliveryOrderLine line : order.getLines()) {
            int picked = reservations.stream()
                        .filter(reservation -> java.util.Objects.equals(
                            reservation.getDeliveryOrderLine().getId(), line.getId()))
                    .mapToInt(DeliveryOrderLotReservation::getPickedQuantity).sum();
            if (picked != line.getOrderedQuantity()) {
                throw new IllegalStateException("Picking belum lengkap; dispatch dibatalkan.");
            }
        }

        for (DeliveryOrderLotReservation reservation : reservations) {
            if (reservation.getPickedQuantity() != reservation.getReservedQuantity()) {
                throw new IllegalStateException("Jumlah pick lot tidak cocok; dispatch dibatalkan.");
            }
            InventoryLotBalance lot = reservation.getLotBalance();
            Rak rack = lot.getRak();
            if (rack == null || lot.getReservedQuantity() < reservation.getReservedQuantity()) {
                throw new IllegalStateException("Reservasi lot tidak valid; dispatch dibatalkan.");
            }
            lot.setReservedQuantity(lot.getReservedQuantity() - reservation.getReservedQuantity());
            lotBalanceRepository.save(lot);
            boolean posted = stockPoster.postOutbound(lot.getBarang().getId(), rack.getId(), lot.getBatchCode(),
                    reservation.getReservedQuantity(), "Goods Issue " + order.getOrderCode(), userId);
            if (!posted) throw new IllegalStateException("Goods Issue gagal; seluruh dispatch dibatalkan.");
        }

        order.setStatus(DeliveryOrderStatus.DISPATCHED);
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