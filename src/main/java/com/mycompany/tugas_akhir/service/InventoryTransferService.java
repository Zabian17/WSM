package com.mycompany.tugas_akhir.service;

import com.mycompany.tugas_akhir.model.Barang;
import com.mycompany.tugas_akhir.model.InventoryBalance;
import com.mycompany.tugas_akhir.model.InventoryLotBalance;
import com.mycompany.tugas_akhir.model.InventoryTransferLotAllocation;
import com.mycompany.tugas_akhir.model.InventoryTransfer;
import com.mycompany.tugas_akhir.model.Rak;
import com.mycompany.tugas_akhir.model.User;
import com.mycompany.tugas_akhir.repository.BarangRepository;
import com.mycompany.tugas_akhir.repository.InventoryBalanceRepository;
import com.mycompany.tugas_akhir.repository.InventoryLotBalanceRepository;
import com.mycompany.tugas_akhir.repository.InventoryTransferRepository;
import com.mycompany.tugas_akhir.repository.InventoryTransferLotAllocationRepository;
import com.mycompany.tugas_akhir.repository.RakRepository;
import com.mycompany.tugas_akhir.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.LinkedHashMap;
import java.util.UUID;

@Service
public class InventoryTransferService {

    private static final Logger log = LoggerFactory.getLogger(InventoryTransferService.class);
    private final BarangRepository barangRepository;
    private final InventoryBalanceRepository balanceRepository;
    private final InventoryTransferRepository transferRepository;
    private final RakRepository rakRepository;
    private final UserRepository userRepository;
    private final InventoryLotBalanceRepository lotBalanceRepository;
    private final InventoryTransferLotAllocationRepository lotAllocationRepository;

    public InventoryTransferService(BarangRepository barangRepository,
                                    InventoryBalanceRepository balanceRepository,
                                    InventoryTransferRepository transferRepository,
                                    RakRepository rakRepository,
                                    UserRepository userRepository,
                                    InventoryLotBalanceRepository lotBalanceRepository,
                                    InventoryTransferLotAllocationRepository lotAllocationRepository) {
        this.barangRepository = barangRepository;
        this.balanceRepository = balanceRepository;
        this.transferRepository = transferRepository;
        this.rakRepository = rakRepository;
        this.userRepository = userRepository;
        this.lotBalanceRepository = lotBalanceRepository;
        this.lotAllocationRepository = lotAllocationRepository;
    }

    @Transactional(readOnly = true)
    public List<InventoryTransfer> getRecentTransfers() {
        return transferRepository.findTop200ByOrderByCreatedAtDesc();
    }

    @Transactional
    public boolean transferStock(Integer barangId, Integer sourceRakId, Integer destinationRakId,
                                 int quantity, String notes, Integer userId) {
        return transferStock(barangId, sourceRakId, destinationRakId, quantity, notes, userId, null, null);
    }

    @Transactional
    public boolean transferStockWithBarcodes(String itemQrPayload, String sourceLocationBarcode,
                                             String destinationLocationBarcode, int quantity,
                                             String notes, Integer userId) {
        BarcodePayload.Item itemBarcode = BarcodePayload.parseItem(itemQrPayload);
        String sourceCode = BarcodePayload.parseLocation(sourceLocationBarcode);
        String destinationCode = BarcodePayload.parseLocation(destinationLocationBarcode);
        if (itemBarcode == null || sourceCode == null || destinationCode == null
                || sourceCode.equalsIgnoreCase(destinationCode)) return false;
        Barang item = barangRepository.findByKodeBarang(itemBarcode.sku()).orElse(null);
        Rak source = rakRepository.findByKodeRak(sourceCode).orElse(null);
        Rak destination = rakRepository.findByKodeRak(destinationCode).orElse(null);
        if (item == null || source == null || destination == null) return false;
        return transferStock(item.getId(), source.getId(), destination.getId(), quantity, notes, userId,
                itemBarcode.batchCode(), itemBarcode.expiryDate());
    }

    private boolean transferStock(Integer barangId, Integer sourceRakId, Integer destinationRakId,
                                  int quantity, String notes, Integer userId,
                                  String scannedBatchCode, LocalDate scannedExpiryDate) {
        if (barangId == null || destinationRakId == null || userId == null || quantity <= 0
                || destinationRakId.equals(sourceRakId)) {
            return false;
        }

        Barang barang = barangRepository.findByIdForUpdate(barangId).orElse(null);
        Rak source = null;
        Rak destination;
        if (sourceRakId == null) {
            destination = rakRepository.findByIdForUpdate(destinationRakId).orElse(null);
        } else if (sourceRakId < destinationRakId) {
            source = rakRepository.findByIdForUpdate(sourceRakId).orElse(null);
            destination = rakRepository.findByIdForUpdate(destinationRakId).orElse(null);
        } else {
            destination = rakRepository.findByIdForUpdate(destinationRakId).orElse(null);
            source = rakRepository.findByIdForUpdate(sourceRakId).orElse(null);
        }
        if (barang == null || destination == null || (sourceRakId != null && source == null)) {
            return false;
        }

        if ((source != null && source.getTerisi() < quantity)
                || destination.getTerisi() + (long) quantity > destination.getKapasitas()) {
            return false;
        }

        String sourceKey = source == null ? InventoryBalance.UNASSIGNED_LOCATION : "RAK-" + source.getId();
        String destinationKey = "RAK-" + destination.getId();
        InventoryBalance sourceBalance = balanceRepository
                .findByBarangIdAndLocationKeyForUpdate(barangId, sourceKey).orElse(null);
        if (sourceBalance == null || sourceBalance.getQuantity() < quantity) {
            return false;
        }

        InventoryBalance destinationBalance = balanceRepository
                .findByBarangIdAndLocationKeyForUpdate(barangId, destinationKey).orElse(null);
        User user = userRepository.findById(userId).orElse(null);
        if (user == null) {
            return false;
        }

        List<InventoryLotBalance> sourceLots = lotBalanceRepository
                .findAllPositiveForUpdate(barangId, sourceKey);
        if (scannedBatchCode != null) {
            sourceLots = sourceLots.stream()
                    .filter(lot -> lot.getBatchCode().equalsIgnoreCase(scannedBatchCode)
                            && java.util.Objects.equals(lot.getExpiryDate(), scannedExpiryDate))
                    .toList();
        }
        LinkedHashMap<InventoryLotBalance, Integer> lotMoves = new LinkedHashMap<>();
        LinkedHashMap<InventoryLotBalance, InventoryLotBalance> destinationLots = new LinkedHashMap<>();
        LinkedHashMap<InventoryLotBalance, Integer> destinationLotQuantities = new LinkedHashMap<>();
        int remaining = quantity;
        try {
            for (InventoryLotBalance sourceLot : sourceLots) {
                int moved = Math.min(remaining, sourceLot.getQuantity());
                if (moved == 0) continue;
                InventoryLotBalance destinationLot = lotBalanceRepository
                        .findByBarangIdAndLocationKeyAndBatchCodeForUpdate(
                                barangId, destinationKey, sourceLot.getBatchCode()).orElse(null);
                if (destinationLot != null
                        && !java.util.Objects.equals(destinationLot.getExpiryDate(), sourceLot.getExpiryDate())) {
                    return false;
                }
                int newDestinationQuantity = Math.addExact(
                        destinationLot == null ? 0 : destinationLot.getQuantity(), moved);
                lotMoves.put(sourceLot, moved);
                destinationLots.put(sourceLot, destinationLot);
                destinationLotQuantities.put(sourceLot, newDestinationQuantity);
                remaining -= moved;
                if (remaining == 0) break;
            }
        } catch (ArithmeticException e) {
            return false;
        }
        if (remaining > 0) return false;

        int destinationQuantity;
        try {
            destinationQuantity = Math.addExact(
                    destinationBalance == null ? 0 : destinationBalance.getQuantity(), quantity);
        } catch (ArithmeticException e) {
            log.warn("Saldo tujuan melampaui batas integer untuk barang {}.", barangId);
            return false;
        }

        String code = "TRF-" + UUID.randomUUID().toString().replace("-", "").substring(0, 16)
            .toUpperCase(java.util.Locale.ROOT);
        InventoryTransfer transfer = transferRepository.save(new InventoryTransfer(code, barang, source, destination,
            quantity, notes, user, LocalDateTime.now()));

        sourceBalance.setQuantity(sourceBalance.getQuantity() - quantity);
        balanceRepository.save(sourceBalance);
        if (destinationBalance == null) {
            destinationBalance = new InventoryBalance(barang, destination, destinationKey, destinationQuantity);
        } else {
            destinationBalance.setQuantity(destinationQuantity);
        }
        balanceRepository.save(destinationBalance);

        for (var lotMove : lotMoves.entrySet()) {
            InventoryLotBalance sourceLot = lotMove.getKey();
            int moved = lotMove.getValue();
            sourceLot.setQuantity(sourceLot.getQuantity() - moved);
            lotBalanceRepository.save(sourceLot);

            InventoryLotBalance destinationLot = destinationLots.get(sourceLot);
            if (destinationLot == null) {
                destinationLot = new InventoryLotBalance(barang, destination, destinationKey,
                        sourceLot.getBatchCode(), sourceLot.getExpiryDate(), sourceLot.getReceivedAt(),
                        destinationLotQuantities.get(sourceLot));
            } else {
                destinationLot.setQuantity(destinationLotQuantities.get(sourceLot));
            }
            lotBalanceRepository.save(destinationLot);
            lotAllocationRepository.save(new InventoryTransferLotAllocation(transfer, sourceLot, moved));
        }

        if (source != null) {
            source.setTerisi(source.getTerisi() - quantity);
            rakRepository.save(source);
        }
        destination.setTerisi(destination.getTerisi() + quantity);
        rakRepository.save(destination);

        return true;
    }
}