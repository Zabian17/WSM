package com.mycompany.tugas_akhir.service;

import com.mycompany.tugas_akhir.model.*;
import com.mycompany.tugas_akhir.repository.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.*;

@Service
public class StockCountService {

    private static final List<StockCountStatus> ACTIVE_STATUSES =
            List.of(StockCountStatus.COUNTING, StockCountStatus.COUNTED);

    private final StockCountRepository stockCountRepository;
    private final BarangRepository barangRepository;
    private final RakRepository rakRepository;
    private final UserRepository userRepository;
    private final InventoryBalanceRepository balanceRepository;
    private final InventoryLotBalanceRepository lotBalanceRepository;

    public StockCountService(StockCountRepository stockCountRepository, BarangRepository barangRepository,
                            RakRepository rakRepository, UserRepository userRepository,
                            InventoryBalanceRepository balanceRepository,
                            InventoryLotBalanceRepository lotBalanceRepository) {
        this.stockCountRepository = stockCountRepository;
        this.barangRepository = barangRepository;
        this.rakRepository = rakRepository;
        this.userRepository = userRepository;
        this.balanceRepository = balanceRepository;
        this.lotBalanceRepository = lotBalanceRepository;
    }

    @Transactional
    public StockCount createCycleCount(Integer rackId, Integer userId) {
        if (rackId == null || userId == null) {
            throw new IllegalArgumentException("Rak dan pengguna wajib dipilih.");
        }
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("Pengguna tidak ditemukan."));
        Rak rack = rakRepository.findByIdForUpdate(rackId)
                .orElseThrow(() -> new IllegalArgumentException("Rak tidak ditemukan."));
        if (stockCountRepository.existsByRakIdAndStatusIn(rackId, ACTIVE_STATUSES)) {
            throw new IllegalStateException("Rak ini sudah memiliki sesi opname yang belum diposting.");
        }

        StockCount count = new StockCount("SC-" + UUID.randomUUID().toString()
                .replace("-", "").substring(0, 16).toUpperCase(Locale.ROOT), rack, user, LocalDateTime.now());
        List<Barang> items = barangRepository.findAll();
        if (items.isEmpty()) {
            throw new IllegalStateException("Belum ada master barang untuk dihitung.");
        }
        for (Barang listedItem : items) {
            Barang item = listedItem;
            String locationKey = "RAK-" + rack.getId();
            int expected = balanceRepository.findByBarangIdAndLocationKey(item.getId(), locationKey)
                    .map(InventoryBalance::getQuantity).orElse(0);
            count.addLine(new StockCountLine(item, expected));
        }
        return stockCountRepository.save(count);
    }

    @Transactional(readOnly = true)
    public List<StockCount> getRecentCounts() {
        return stockCountRepository.findAllByOrderByCreatedAtDesc();
    }

    @Transactional(readOnly = true)
    public List<CycleCountRecommendation> getCycleCountRecommendations() {
        LocalDate today = LocalDate.now();
        Map<Integer, StockCount> lastCountByRack = new HashMap<>();
        for (StockCount count : stockCountRepository.findAllByStatusOrderByPostedAtDesc(StockCountStatus.POSTED)) {
            lastCountByRack.putIfAbsent(count.getRak().getId(), count);
        }

        Map<Integer, List<InventoryBalance>> balancesByRack = new HashMap<>();
        for (InventoryBalance balance : balanceRepository.findAllForLocationReport()) {
            if (balance.getRak() != null) {
                balancesByRack.computeIfAbsent(balance.getRak().getId(), ignored -> new ArrayList<>()).add(balance);
            }
        }

        List<CycleCountRecommendation> recommendations = new ArrayList<>();
        for (Rak rack : rakRepository.findAll()) {
            StockCount lastCount = lastCountByRack.get(rack.getId());
            long daysSinceCount = lastCount == null ? Long.MAX_VALUE
                    : ChronoUnit.DAYS.between(lastCount.getPostedAt().toLocalDate(), today);
            long riskItems = balancesByRack.getOrDefault(rack.getId(), List.of()).stream()
                    .filter(balance -> balance.getQuantity() > 0)
                    .map(InventoryBalance::getBarang)
                    .distinct()
                    .filter(item -> item.getStok() <= item.getStokMin())
                    .count();
            long varianceItems = lastCount == null ? 0 : lastCount.getLines().stream()
                    .filter(line -> line.getCountedQuantity() != null
                            && !line.getCountedQuantity().equals(line.getExpectedQuantity()))
                    .count();
            int score = (lastCount == null ? 80 : daysSinceCount >= 30 ? 70
                    : daysSinceCount >= 14 ? 50 : daysSinceCount >= 7 ? 25 : 0)
                    + Math.toIntExact(Math.min(40, riskItems * 10))
                    + Math.toIntExact(Math.min(30, varianceItems * 10));
            String priority = score >= 80 ? "URGENT" : score >= 50 ? "HIGH"
                    : score >= 25 ? "NORMAL" : "LOW";
            List<String> reasons = new ArrayList<>();
            if (lastCount == null) reasons.add("belum pernah dihitung");
            else if (daysSinceCount >= 7) reasons.add("terakhir dihitung " + daysSinceCount + " hari lalu");
            if (riskItems > 0) reasons.add(riskItems + " SKU stok kritis");
            if (varianceItems > 0) reasons.add(varianceItems + " selisih pada opname terakhir");
            if (reasons.isEmpty()) reasons.add("opname masih baru dan tanpa risiko terdeteksi");
            recommendations.add(new CycleCountRecommendation(rack, score, priority, String.join("; ", reasons),
                    lastCount == null ? null : lastCount.getPostedAt(), (int) riskItems, (int) varianceItems));
        }
        recommendations.sort(Comparator.comparingInt(CycleCountRecommendation::priorityScore).reversed()
                .thenComparing(recommendation -> recommendation.lastCountedAt() == null
                        ? LocalDateTime.MIN : recommendation.lastCountedAt())
                .thenComparing(recommendation -> recommendation.rack().getKodeRak(),
                        String.CASE_INSENSITIVE_ORDER));
        return List.copyOf(recommendations);
    }

    @Transactional(readOnly = true)
    public List<CountLineView> getCountLines(Integer countId) {
        StockCount count = getCount(countId);
        return count.getLines().stream()
                .sorted(Comparator.comparing(line -> line.getBarang().getKodeBarang(),
                        String.CASE_INSENSITIVE_ORDER))
                .map(line -> new CountLineView(line.getId(), line.getBarang().getKodeBarang(),
                        line.getBarang().getNamaBarang(), line.getExpectedQuantity(), line.getCountedQuantity(),
                        line.getBarang().getSatuan()))
                .toList();
    }

    @Transactional
    public void submitCounts(Integer countId, Map<Integer, Integer> countedQuantities, Integer userId) {
        StockCount count = stockCountRepository.findByIdForUpdate(countId)
                .orElseThrow(() -> new IllegalArgumentException("Sesi opname tidak ditemukan."));
        if (count.getStatus() != StockCountStatus.COUNTING) {
            throw new IllegalStateException("Sesi opname ini tidak lagi menerima hasil hitung.");
        }
        User counter = userId == null ? null : userRepository.findById(userId).orElse(null);
        if (counter == null) throw new IllegalArgumentException("Pengguna penghitung tidak ditemukan.");
        if (countedQuantities == null || countedQuantities.size() != count.getLines().size()) {
            throw new IllegalArgumentException("Hasil hitung harus diisi untuk seluruh barang pada sesi ini.");
        }
        Map<Integer, StockCountLine> linesById = new HashMap<>();
        for (StockCountLine line : count.getLines()) linesById.put(line.getId(), line);
        for (Map.Entry<Integer, Integer> entry : countedQuantities.entrySet()) {
            StockCountLine line = linesById.get(entry.getKey());
            Integer quantity = entry.getValue();
            if (line == null || quantity == null || quantity < 0) {
                throw new IllegalArgumentException("Hasil hitung berisi baris yang tidak sah.");
            }
            line.setCountedQuantity(quantity);
        }
        count.setCountedBy(counter);
        count.setStatus(StockCountStatus.COUNTED);
        stockCountRepository.save(count);
    }

    @Transactional
    public void postCount(Integer countId, Integer userId) {
        if (userId == null) throw new IllegalArgumentException("Pengguna wajib tersedia untuk posting.");
        StockCount count = stockCountRepository.findByIdForUpdate(countId)
                .orElseThrow(() -> new IllegalArgumentException("Sesi opname tidak ditemukan."));
        if (count.getStatus() != StockCountStatus.COUNTED) {
            throw new IllegalStateException("Sesi harus selesai dihitung sebelum penyesuaian diposting.");
        }
        User poster = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("Pengguna tidak ditemukan."));
        List<StockCountLine> orderedLines = count.getLines().stream()
                .sorted(Comparator.comparing(line -> line.getBarang().getId())).toList();
        Map<Integer, Barang> lockedItems = new HashMap<>();
        for (StockCountLine line : orderedLines) {
            Barang item = barangRepository.findByIdForUpdate(line.getBarang().getId())
                    .orElseThrow(() -> new IllegalStateException("Barang opname tidak ditemukan."));
            lockedItems.put(item.getId(), item);
        }
        Rak rack = rakRepository.findByIdForUpdate(count.getRak().getId())
                .orElseThrow(() -> new IllegalStateException("Rak opname tidak ditemukan."));
        String locationKey = "RAK-" + rack.getId();
        List<LockedLine> lockedLines = new ArrayList<>();
        long totalVariance = 0;

        for (StockCountLine line : orderedLines) {
            Barang item = lockedItems.get(line.getBarang().getId());
            InventoryBalance balance = balanceRepository.findByBarangIdAndLocationKeyForUpdate(
                    item.getId(), locationKey).orElse(null);
            int currentQuantity = balance == null ? 0 : balance.getQuantity();
            if (currentQuantity != line.getExpectedQuantity()) {
                throw new IllegalStateException("Stok " + item.getKodeBarang()
                        + " berubah sejak opname dimulai. Buat sesi baru sebelum posting.");
            }
            if (line.getCountedQuantity() == null || line.getCountedQuantity() < 0) {
                throw new IllegalStateException("Sesi opname belum memiliki hasil hitung lengkap.");
            }
            int variance = line.getCountedQuantity() - line.getExpectedQuantity();
            totalVariance += variance;
            if (variance < 0) {
                List<InventoryLotBalance> availableLots = lotBalanceRepository.findAllPositiveForUpdate(
                        item.getId(), locationKey);
                long freeQuantity = availableLots.stream().mapToLong(InventoryLotBalance::getAvailableQuantity).sum();
                if (freeQuantity < -(long) variance) {
                    throw new IllegalStateException("Penyesuaian " + item.getKodeBarang()
                            + " akan mengurangi stok yang sudah direservasi.");
                }
                lockedLines.add(new LockedLine(line, item, balance, availableLots, variance));
            } else {
                lockedLines.add(new LockedLine(line, item, balance, List.of(), variance));
            }
        }

        long occupiedAfter = (long) rack.getTerisi() + totalVariance;
        if (occupiedAfter < 0) {
            throw new IllegalStateException("Penyesuaian membuat keterisian rak menjadi negatif.");
        }
        if (totalVariance > 0 && occupiedAfter > rack.getKapasitas()) {
            throw new IllegalStateException("Penyesuaian opname melebihi kapasitas rak.");
        }

        for (LockedLine locked : lockedLines) {
            int variance = locked.variance();
            if (variance == 0) continue;
            StockCountLine line = locked.line();
            Barang item = locked.item();
            InventoryBalance balance = locked.balance();
            int counted = line.getCountedQuantity();
            if (balance == null) {
                balance = new InventoryBalance(item, rack, locationKey, counted);
            } else {
                balance.setQuantity(counted);
            }
            balanceRepository.save(balance);

            if (variance > 0) {
                String batchCode = "COUNT-" + count.getCountCode();
                InventoryLotBalance lot = lotBalanceRepository
                        .findByBarangIdAndLocationKeyAndBatchCodeForUpdate(item.getId(), locationKey, batchCode)
                        .orElseGet(() -> new InventoryLotBalance(item, rack, locationKey, batchCode,
                                null, LocalDateTime.now(), 0));
                lot.setQuantity(Math.addExact(lot.getQuantity(), variance));
                lotBalanceRepository.save(lot);
            } else {
                int remaining = -variance;
                for (InventoryLotBalance lot : locked.availableLots()) {
                    int deduction = Math.min(remaining, lot.getAvailableQuantity());
                    if (deduction <= 0) continue;
                    lot.setQuantity(lot.getQuantity() - deduction);
                    lotBalanceRepository.save(lot);
                    remaining -= deduction;
                    if (remaining == 0) break;
                }
                if (remaining != 0) {
                    throw new IllegalStateException("Stok lot berubah saat posting " + item.getKodeBarang() + ".");
                }
            }

            item.setStok(Math.addExact(item.getStok(), variance));
            if (item.getStok() < 0) {
                throw new IllegalStateException("Penyesuaian membuat stok total barang menjadi negatif.");
            }
            barangRepository.save(item);
        }

        rack.setTerisi((int) occupiedAfter);
        rakRepository.save(rack);
        count.setStatus(StockCountStatus.POSTED);
        count.setPostedBy(poster);
        count.setPostedAt(LocalDateTime.now());
        stockCountRepository.save(count);
    }

    @Transactional
    public void cancelCount(Integer countId) {
        StockCount count = stockCountRepository.findByIdForUpdate(countId)
                .orElseThrow(() -> new IllegalArgumentException("Sesi opname tidak ditemukan."));
        if (count.getStatus() != StockCountStatus.COUNTING
                && count.getStatus() != StockCountStatus.COUNTED) {
            throw new IllegalStateException("Sesi opname ini tidak dapat dibatalkan.");
        }
        count.setStatus(StockCountStatus.CANCELLED);
        stockCountRepository.save(count);
    }

    private StockCount getCount(Integer countId) {
        return stockCountRepository.findById(countId)
                .orElseThrow(() -> new IllegalArgumentException("Sesi opname tidak ditemukan."));
    }

    public record CountLineView(Integer lineId, String itemCode, String itemName,
                                Integer expectedQuantity, Integer countedQuantity, String unit) {}

    public record CycleCountRecommendation(Rak rack, int priorityScore, String priority,
                                           String reason, LocalDateTime lastCountedAt,
                                           int riskItemCount, int previousVarianceCount) {}

    private record LockedLine(StockCountLine line, Barang item, InventoryBalance balance,
                              List<InventoryLotBalance> availableLots, int variance) {}
}
