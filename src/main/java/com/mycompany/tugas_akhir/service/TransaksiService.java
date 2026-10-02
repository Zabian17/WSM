package com.mycompany.tugas_akhir.service;

import com.mycompany.tugas_akhir.model.Barang;
import com.mycompany.tugas_akhir.model.InventoryBalance;
import com.mycompany.tugas_akhir.model.InventoryLotBalance;
import com.mycompany.tugas_akhir.model.Rak;
import com.mycompany.tugas_akhir.model.Supplier;
import com.mycompany.tugas_akhir.model.Transaksi;
import com.mycompany.tugas_akhir.model.TransactionLotAllocation;
import com.mycompany.tugas_akhir.model.User;
import com.mycompany.tugas_akhir.repository.BarangRepository;
import com.mycompany.tugas_akhir.repository.InventoryBalanceRepository;
import com.mycompany.tugas_akhir.repository.InventoryLotBalanceRepository;
import com.mycompany.tugas_akhir.repository.RakRepository;
import com.mycompany.tugas_akhir.repository.SupplierRepository;
import com.mycompany.tugas_akhir.repository.TransaksiRepository;
import com.mycompany.tugas_akhir.repository.TransactionLotAllocationRepository;
import com.mycompany.tugas_akhir.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.LocalDate;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

@Service
public class TransaksiService implements InboundStockPoster, OutboundStockPoster {

    private static final Logger log = LoggerFactory.getLogger(TransaksiService.class);
    private final TransaksiRepository transaksiRepository;
    private final BarangRepository barangRepository;
    private final SupplierRepository supplierRepository;
    private final UserRepository userRepository;
    private final InventoryBalanceRepository balanceRepository;
    private final RakRepository rakRepository;
    private final InventoryLotBalanceRepository lotBalanceRepository;
    private final TransactionLotAllocationRepository lotAllocationRepository;

    @Value("${gudangku.inventory.picking-strategy:FEFO}")
    private String pickingStrategy = "FEFO";

    public TransaksiService(TransaksiRepository transaksiRepository,
                            BarangRepository barangRepository,
                            SupplierRepository supplierRepository,
                            UserRepository userRepository,
                            InventoryBalanceRepository balanceRepository,
                            RakRepository rakRepository,
                            InventoryLotBalanceRepository lotBalanceRepository,
                            TransactionLotAllocationRepository lotAllocationRepository) {
        this.transaksiRepository = transaksiRepository;
        this.barangRepository = barangRepository;
        this.supplierRepository = supplierRepository;
        this.userRepository = userRepository;
        this.balanceRepository = balanceRepository;
        this.rakRepository = rakRepository;
        this.lotBalanceRepository = lotBalanceRepository;
        this.lotAllocationRepository = lotAllocationRepository;
    }

    @Transactional(readOnly = true)
    public List<Transaksi> getAllTransaksi() {
        return transaksiRepository.findTop200ByOrderByTanggalDesc();
    }

    @Override
    public boolean postInbound(Integer barangId, Integer supplierId, Integer rakId, String batchCode,
                               LocalDate expiryDate, int quantity, String notes, Integer userId) {
        if (barangId == null || userId == null) return false;
        return addTransaksi("masuk", barangId, supplierId, rakId, batchCode, expiryDate,
                quantity, notes, userId);
    }

    @Override
    public boolean postOutbound(Integer barangId, Integer rackId, String batchCode,
                                int quantity, String notes, Integer userId) {
        if (barangId == null || userId == null) return false;
        return addTransaksi("keluar", barangId, null, rackId, batchCode, null, quantity, notes, userId);
    }

    @Transactional
    public boolean addTransaksi(String tipe, int barangId, Integer supplierId, Integer rakId,
                                String batchCode, LocalDate expiryDate,
                                int jumlah, String keterangan, int userId) {
        if (jumlah <= 0 || (!"masuk".equalsIgnoreCase(tipe) && !"keluar".equalsIgnoreCase(tipe))) {
            return false;
        }
        Barang barang = barangRepository.findByIdForUpdate(barangId).orElse(null);
        if (barang == null) return false;
        boolean isInbound = "masuk".equalsIgnoreCase(tipe);
        String normalizedBatchCode = batchCode == null ? null : batchCode.trim().toUpperCase(Locale.ROOT);
        if (isInbound && (rakId == null || normalizedBatchCode == null || normalizedBatchCode.isBlank()
                || normalizedBatchCode.length() > 64
                || InventoryLotBalance.OPENING_BATCH_CODE.equals(normalizedBatchCode)
                || (expiryDate != null && expiryDate.isBefore(LocalDate.now())))) {
            return false;
        }
        Rak rak = rakId == null ? null : rakRepository.findByIdForUpdate(rakId).orElse(null);
        if (rakId != null && rak == null) return false;

        int occupiedAfter = 0;
        if (rak != null) {
            try {
                occupiedAfter = isInbound
                        ? Math.addExact(rak.getTerisi(), jumlah)
                        : rak.getTerisi() - jumlah;
            } catch (ArithmeticException e) {
                return false;
            }
            if (occupiedAfter < 0 || occupiedAfter > rak.getKapasitas()) {
                log.warn("[TransaksiService] Kapasitas rak tidak cukup: rakId={}, terisi={}, kapasitas={}, jumlah={}",
                        rakId, rak.getTerisi(), rak.getKapasitas(), jumlah);
                return false;
            }
        }

        String locationKey = rak == null ? InventoryBalance.UNASSIGNED_LOCATION : "RAK-" + rak.getId();
        InventoryBalance balance = balanceRepository.findByBarangIdAndLocationKeyForUpdate(barangId, locationKey)
                .orElse(null);
        if (!isInbound && (balance == null || balance.getQuantity() < jumlah)) {
            log.warn("[TransaksiService] Saldo lokasi tidak mencukupi: barangId={}, lokasi={}", barangId, locationKey);
            return false;
        }
        if (!isInbound && barang.getStok() < jumlah) {
            log.warn("[TransaksiService] Stok tidak mencukupi untuk barang id {}: stok={}, diminta={}", barangId, barang.getStok(), jumlah);
            return false;
        }

        InventoryLotBalance inboundLot = null;
        int inboundLotQuantity = 0;
        List<InventoryLotBalance> outboundLots = List.of();
        java.util.Map<InventoryLotBalance, Integer> outboundAllocations = new java.util.LinkedHashMap<>();
        if (isInbound) {
            inboundLot = lotBalanceRepository.findByBarangIdAndLocationKeyAndBatchCodeForUpdate(
                    barangId, locationKey, normalizedBatchCode).orElse(null);
            if (inboundLot != null && !java.util.Objects.equals(inboundLot.getExpiryDate(), expiryDate)) {
                log.warn("[TransaksiService] Expiry batch tidak cocok dengan batch yang sudah tercatat: {}", normalizedBatchCode);
                return false;
            }
            try {
                inboundLotQuantity = Math.addExact(inboundLot == null ? 0 : inboundLot.getQuantity(), jumlah);
            } catch (ArithmeticException e) {
                return false;
            }
        } else if (normalizedBatchCode != null && !normalizedBatchCode.isBlank()) {
            InventoryLotBalance lot = lotBalanceRepository.findByBarangIdAndLocationKeyAndBatchCodeForUpdate(
                    barangId, locationKey, normalizedBatchCode).orElse(null);
            if (lot == null || lot.getAvailableQuantity() < jumlah
                    || (lot.getExpiryDate() != null && lot.getExpiryDate().isBefore(LocalDate.now()))) {
                return false;
            }
            outboundAllocations.put(lot, jumlah);
        } else {
            String strategy = pickingStrategy == null || pickingStrategy.isBlank()
                    ? "FEFO" : pickingStrategy.trim().toUpperCase(Locale.ROOT);
            if ("FIFO".equals(strategy)) {
                outboundLots = lotBalanceRepository.findAvailableFifoForUpdate(barangId, locationKey, LocalDate.now());
            } else if ("LEFO".equals(strategy)) {
                outboundLots = lotBalanceRepository.findAvailableLefoForUpdate(barangId, locationKey, LocalDate.now());
            } else {
                outboundLots = lotBalanceRepository.findAvailableFefoForUpdate(barangId, locationKey, LocalDate.now());
            }
            int remaining = jumlah;
            for (InventoryLotBalance lot : outboundLots) {
                int allocated = Math.min(remaining, lot.getAvailableQuantity());
                if (allocated > 0) {
                    outboundAllocations.put(lot, allocated);
                    remaining -= allocated;
                }
                if (remaining == 0) break;
            }
            if (remaining > 0) {
                log.warn("[TransaksiService] Stok eligible batch tidak mencukupi: barangId={}, lokasi={}", barangId, locationKey);
                return false;
            }
        }

        Supplier supplier = null;
        if (supplierId != null) {
            supplier = supplierRepository.findById(supplierId).orElse(null);
            if (supplier == null) {
                log.warn("[TransaksiService] Supplier tidak ditemukan: supplierId={}", supplierId);
                return false;
            }
        }

        User user = userRepository.findById(userId).orElse(null);
        if (user == null) {
            log.error("[TransaksiService] User tidak ditemukan di database.");
            return false;
        }

        String kodeTransaksi = "TRX-" + UUID.randomUUID().toString().replace("-", "").substring(0, 16).toUpperCase(java.util.Locale.ROOT);
        int stokBaru;
        int saldoLokasiBaru;
        try {
            stokBaru = isInbound ? Math.addExact(barang.getStok(), jumlah) : barang.getStok() - jumlah;
            saldoLokasiBaru = isInbound
                    ? Math.addExact(balance == null ? 0 : balance.getQuantity(), jumlah)
                    : balance.getQuantity() - jumlah;
        } catch (ArithmeticException e) {
            log.warn("[TransaksiService] Jumlah transaksi melampaui batas stok: barangId={}", barangId);
            return false;
        }

        Transaksi tx = Transaksi.builder()
                .kodeTransaksi(kodeTransaksi)
                .tipe(tipe.toLowerCase(java.util.Locale.ROOT))
                .barang(barang)
                .supplier(supplier)
                .rak(rak)
                .jumlah(jumlah)
                .keterangan(keterangan)
                .user(user)
                .tanggal(LocalDateTime.now())
                .build();

        Transaksi savedTx = transaksiRepository.save(tx);
        if (balance == null) {
            balance = new InventoryBalance(barang, rak, locationKey, saldoLokasiBaru);
        } else {
            balance.setQuantity(saldoLokasiBaru);
        }
        balanceRepository.save(balance);

        if (isInbound) {
            if (inboundLot == null) {
                inboundLot = new InventoryLotBalance(barang, rak, locationKey, normalizedBatchCode,
                        expiryDate, LocalDateTime.now(), inboundLotQuantity);
            } else {
                inboundLot.setQuantity(inboundLotQuantity);
            }
            inboundLot = lotBalanceRepository.save(inboundLot);
            lotAllocationRepository.save(new TransactionLotAllocation(savedTx, inboundLot, jumlah));
        } else {
            for (java.util.Map.Entry<InventoryLotBalance, Integer> allocation : outboundAllocations.entrySet()) {
                InventoryLotBalance lot = allocation.getKey();
                lot.setQuantity(lot.getQuantity() - allocation.getValue());
                lotBalanceRepository.save(lot);
                lotAllocationRepository.save(new TransactionLotAllocation(savedTx, lot, allocation.getValue()));
            }
        }

        barang.setStok(stokBaru);
        barangRepository.save(barang);
        if (rak != null) {
            rak.setTerisi(occupiedAfter);
            rakRepository.save(rak);
        }

        log.info("[TransaksiService] Transaksi {} berhasil dicatat: {} ({})", tipe, kodeTransaksi, barang.getNamaBarang());
        return true;
    }

    @Transactional(readOnly = true)
    public int[] getStatistik24H() {
        int masuk = 0;
        int keluar = 0;
        LocalDateTime since = LocalDateTime.now().minusHours(24);
        List<Object[]> rows = transaksiRepository.getStatistikSince(since);

        for (Object[] row : rows) {
            String tipe = (String) row[0];
            Number total = (Number) row[1];
            if ("masuk".equalsIgnoreCase(tipe)) {
                masuk = total.intValue();
            } else if ("keluar".equalsIgnoreCase(tipe)) {
                keluar = total.intValue();
            }
        }
        return new int[]{masuk, keluar};
    }
}
