package com.mycompany.tugas_akhir.service;

import com.mycompany.tugas_akhir.model.*;
import com.mycompany.tugas_akhir.repository.*;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.temporal.ChronoUnit;
import java.util.*;

@Service
public class StockCountService {

    private static final List<StockCountStatus> ACTIVE_STATUSES =
            List.of(StockCountStatus.SCHEDULED, StockCountStatus.COUNTING, StockCountStatus.COUNTED);

    private final StockCountRepository stockCountRepository;
    private final StockCountLineRepository stockCountLineRepository;
    private final BarangRepository barangRepository;
    private final RakRepository rakRepository;
    private final UserRepository userRepository;
    private final InventoryBalanceRepository balanceRepository;
    private final InventoryLotBalanceRepository lotBalanceRepository;
    private final TransaksiRepository transaksiRepository;
    private final InventoryTransferRepository inventoryTransferRepository;

    public StockCountService(StockCountRepository stockCountRepository,
                            StockCountLineRepository stockCountLineRepository,
                            BarangRepository barangRepository,
                            RakRepository rakRepository, UserRepository userRepository,
                            InventoryBalanceRepository balanceRepository,
                            InventoryLotBalanceRepository lotBalanceRepository,
                            TransaksiRepository transaksiRepository,
                            InventoryTransferRepository inventoryTransferRepository) {
        this.stockCountRepository = stockCountRepository;
        this.stockCountLineRepository = stockCountLineRepository;
        this.barangRepository = barangRepository;
        this.rakRepository = rakRepository;
        this.userRepository = userRepository;
        this.balanceRepository = balanceRepository;
        this.lotBalanceRepository = lotBalanceRepository;
        this.transaksiRepository = transaksiRepository;
        this.inventoryTransferRepository = inventoryTransferRepository;
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

    @Transactional
    public List<StockCount> createPeriodicCycleCount(Integer createdById) {
        User creator = userRepository.findById(createdById)
                .orElseThrow(() -> new IllegalArgumentException("Pengguna tidak ditemukan."));
        List<User> operators = userRepository.findAll().stream()
                .sorted(Comparator.comparing(User::getId)).toList();
        if (operators.isEmpty()) throw new IllegalStateException("Belum ada pengguna untuk pembagian opname.");
        List<Rak> racks = rakRepository.findAll().stream()
                .sorted(Comparator.comparing(Rak::getId)).toList();
        if (racks.isEmpty()) throw new IllegalStateException("Belum ada rak untuk dihitung.");
        List<Barang> items = barangRepository.findAll().stream()
                .sorted(Comparator.comparing(Barang::getId)).toList();
        if (items.isEmpty()) throw new IllegalStateException("Belum ada master barang untuk dihitung.");

        List<Rak> lockedRacks = new ArrayList<>();
        for (Rak rack : racks) {
            Rak locked = rakRepository.findByIdForUpdate(rack.getId())
                    .orElseThrow(() -> new IllegalStateException("Rak opname tidak ditemukan."));
            if (stockCountRepository.existsByRakIdAndStatusIn(locked.getId(), ACTIVE_STATUSES)) {
                throw new IllegalStateException("Rak " + locked.getKodeRak()
                        + " masih memiliki sesi opname yang belum diposting.");
            }
            lockedRacks.add(locked);
        }

        Map<String, Integer> quantities = new HashMap<>();
        for (InventoryBalance balance : balanceRepository.findAllForLocationReport()) {
            if (balance.getRak() != null) {
                quantities.put(balanceKey(balance.getRak().getId(), balance.getBarang().getId()), balance.getQuantity());
            }
        }
        String cycleCode = "PC-" + UUID.randomUUID().toString().replace("-", "")
                .substring(0, 16).toUpperCase(Locale.ROOT);
        List<StockCount> created = new ArrayList<>();
        int assignmentIndex = 0;
        for (Rak rack : lockedRacks) {
            StockCount count = new StockCount("SC-" + UUID.randomUUID().toString()
                    .replace("-", "").substring(0, 16).toUpperCase(Locale.ROOT), rack, creator,
                    LocalDateTime.now());
            count.setCycle(cycleCode, StockCountCycleType.PERIODIC);
            for (Barang item : items) {
                StockCountLine line = new StockCountLine(item,
                        quantities.getOrDefault(balanceKey(rack.getId(), item.getId()), 0));
                line.setAssignedUser(operators.get(assignmentIndex++ % operators.size()));
                count.addLine(line);
            }
            created.add(stockCountRepository.save(count));
        }
        return List.copyOf(created);
    }

    @Transactional
    public List<StockCount> createDailyCycleCount(Integer createdById) {
        User creator = userRepository.findById(createdById)
                .orElseThrow(() -> new IllegalArgumentException("Pengguna tidak ditemukan."));
        LocalDate today = LocalDate.now();
        String cycleCode = "DC-" + today.toString().replace("-", "");
        if (stockCountRepository.existsByCycleCode(cycleCode)) {
            throw new IllegalStateException("Opname harian " + today + " sudah dibuat.");
        }

        LocalDateTime startOfDay = today.atStartOfDay();
        Map<Integer, Long> mutationsByRack = new HashMap<>();
        addMutationCounts(mutationsByRack, transaksiRepository.countMutationsByRackSince(startOfDay));
        addMutationCounts(mutationsByRack, inventoryTransferRepository.countMutationsFromRackSince(startOfDay));
        addMutationCounts(mutationsByRack, inventoryTransferRepository.countMutationsIntoRackSince(startOfDay));
        if (mutationsByRack.isEmpty()) return List.of();

        List<Rak> affectedRacks = rakRepository.findAll().stream()
                .filter(rack -> mutationsByRack.getOrDefault(rack.getId(), 0L) > 0)
                .sorted(Comparator.comparing(Rak::getId)).toList();
        if (affectedRacks.isEmpty()) return List.of();

        List<Rak> lockedRacks = new ArrayList<>();
        for (Rak rack : affectedRacks) {
            Rak locked = rakRepository.findByIdForUpdate(rack.getId())
                    .orElseThrow(() -> new IllegalStateException("Rak opname tidak ditemukan."));
            if (stockCountRepository.existsByRakIdAndStatusIn(locked.getId(), ACTIVE_STATUSES)) {
                throw new IllegalStateException("Rak " + locked.getKodeRak()
                        + " masih memiliki sesi opname yang belum diposting.");
            }
            lockedRacks.add(locked);
        }

        List<Barang> items = barangRepository.findAll().stream()
                .sorted(Comparator.comparing(Barang::getId)).toList();
        if (items.isEmpty()) throw new IllegalStateException("Belum ada master barang untuk dihitung.");
        Map<String, Integer> quantities = new HashMap<>();
        for (InventoryBalance balance : balanceRepository.findAllForLocationReport()) {
            if (balance.getRak() != null) {
                quantities.put(balanceKey(balance.getRak().getId(), balance.getBarang().getId()),
                        balance.getQuantity());
            }
        }

        List<StockCount> created = new ArrayList<>();
        for (Rak rack : lockedRacks) {
            StockCount count = new StockCount("SC-" + UUID.randomUUID().toString()
                    .replace("-", "").substring(0, 16).toUpperCase(Locale.ROOT), rack, creator,
                    LocalDateTime.now());
            count.setCycle(cycleCode, StockCountCycleType.DAILY);
            for (Barang item : items) {
                count.addLine(new StockCountLine(item,
                        quantities.getOrDefault(balanceKey(rack.getId(), item.getId()), 0)));
            }
            created.add(stockCountRepository.save(count));
        }
        return List.copyOf(created);
    }

    @Transactional
    public List<StockCount> createMonthlySchedule(YearMonth month, Integer createdById) {
        if (month == null) throw new IllegalArgumentException("Bulan jadwal wajib dipilih.");
        User creator = userRepository.findById(createdById)
                .orElseThrow(() -> new IllegalArgumentException("Pengguna tidak ditemukan."));
        String cycleCode = "MC-" + month.toString().replace("-", "");
        if (stockCountRepository.existsByCycleCode(cycleCode))
            throw new IllegalStateException("Jadwal opname " + month + " sudah dibuat.");
        List<LocalDate> operatingDays = month.atDay(1)
                .datesUntil(month.plusMonths(1).atDay(1))
                .filter(date -> date.getDayOfWeek().getValue() <= 5).toList();
        if (operatingDays.isEmpty()) throw new IllegalStateException("Bulan tidak memiliki hari kerja.");
        List<Rak> racks = rakRepository.findAll().stream()
                .sorted(Comparator.comparing(Rak::getId)).toList();
        if (racks.isEmpty()) throw new IllegalStateException("Belum ada rak untuk dijadwalkan.");
        List<Rak> lockedRacks = new ArrayList<>();
        for (Rak rack : racks) {
            Rak locked = rakRepository.findByIdForUpdate(rack.getId())
                    .orElseThrow(() -> new IllegalStateException("Rak opname tidak ditemukan."));
            if (stockCountRepository.existsByRakIdAndStatusIn(locked.getId(), ACTIVE_STATUSES))
                throw new IllegalStateException("Rak " + locked.getKodeRak()
                        + " masih memiliki sesi opname yang belum diposting.");
            lockedRacks.add(locked);
        }
        List<StockCount> scheduled = new ArrayList<>();
        for (int i = 0; i < lockedRacks.size(); i++) {
            Rak rack = lockedRacks.get(i);
            int dayIndex = (int) ((long) i * operatingDays.size() / lockedRacks.size());
            StockCount count = new StockCount("SC-" + UUID.randomUUID().toString()
                    .replace("-", "").substring(0, 16).toUpperCase(Locale.ROOT), rack, creator,
                    LocalDateTime.now());
            count.setCycle(cycleCode, StockCountCycleType.MONTHLY);
            count.setDueDate(operatingDays.get(dayIndex));
            count.setStatus(StockCountStatus.SCHEDULED);
            scheduled.add(stockCountRepository.save(count));
        }
        return List.copyOf(scheduled);
    }

    @Transactional
    public StockCount startScheduledCount(Integer countId) {
        StockCount count = stockCountRepository.findByIdForUpdate(countId)
                .orElseThrow(() -> new IllegalArgumentException("Jadwal opname tidak ditemukan."));
        if (count.getStatus() != StockCountStatus.SCHEDULED
                || count.getCycleType() != StockCountCycleType.MONTHLY)
            throw new IllegalStateException("Sesi ini bukan jadwal opname bulanan yang dapat dimulai.");
        if (count.getDueDate().isAfter(LocalDate.now()))
            throw new IllegalStateException("Opname rak ini baru dapat dimulai pada " + count.getDueDate() + ".");
        Rak rack = rakRepository.findByIdForUpdate(count.getRak().getId())
                .orElseThrow(() -> new IllegalStateException("Rak opname tidak ditemukan."));
        List<Barang> items = barangRepository.findAll().stream()
                .sorted(Comparator.comparing(Barang::getId)).toList();
        if (items.isEmpty()) throw new IllegalStateException("Belum ada master barang untuk dihitung.");
        Map<String, Integer> quantities = new HashMap<>();
        for (InventoryBalance balance : balanceRepository.findAllForLocationReport()) {
            if (balance.getRak() != null)
                quantities.put(balanceKey(balance.getRak().getId(), balance.getBarang().getId()), balance.getQuantity());
        }
        count.getLines().clear();
        for (Barang item : items) {
            count.addLine(new StockCountLine(item,
                    quantities.getOrDefault(balanceKey(rack.getId(), item.getId()), 0)));
        }
        count.setStatus(StockCountStatus.COUNTING);
        return stockCountRepository.save(count);
    }

    private String balanceKey(Integer rackId, Integer itemId) {
        return rackId + ":" + itemId;
    }

    @Transactional(readOnly = true)
    public List<StockCount> getRecentCounts() {
        return stockCountRepository.findAllByOrderByCreatedAtDesc();
    }

    @Transactional(readOnly = true)
    public Page<StockCountSummaryProjection> getMobileActiveCounts(Integer userId, Pageable pageable) {
        return stockCountRepository.findMobileActiveCounts(userId,
                List.of(StockCountStatus.SCHEDULED, StockCountStatus.COUNTING),
                StockCountStatus.SCHEDULED, pageable);
    }

    @Transactional(readOnly = true)
    public Page<StockCountLineProjection> getMobileCountLines(Integer countId, Integer userId,
                                                              Pageable pageable) {
        return stockCountLineRepository.findOpenLinesForUser(countId, userId, pageable);
    }

    @Transactional(readOnly = true)
    public void validateMobileCountBarcode(Integer countId, Integer lineId, Integer userId,
                                           String barcodePayload) {
        if (barcodePayload == null || barcodePayload.length() > 512) {
            throw new IllegalArgumentException("Barcode barang tidak valid.");
        }
        BarcodePayload.Item scanned = BarcodePayload.parseItem(barcodePayload);
        if (scanned == null) throw new IllegalArgumentException("Barcode barang tidak valid.");
        String expectedSku = stockCountLineRepository.findOpenSkuForUser(countId, lineId, userId)
                .orElseThrow(() -> new IllegalArgumentException("SKU tidak ditugaskan pada sesi ini."));
        if (!expectedSku.equalsIgnoreCase(scanned.sku())) {
            throw new IllegalStateException("Barcode tidak cocok dengan SKU yang sedang dihitung.");
        }
    }

    @Transactional(readOnly = true)
    public StockCountStatus getCountStatus(Integer countId) {
        return stockCountRepository.findStatusById(countId)
                .orElseThrow(() -> new IllegalArgumentException("Sesi opname tidak ditemukan."));
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

        LocalDateTime startOfDay = today.atStartOfDay();
        Map<Integer, Long> mutationsByRack = new HashMap<>();
        addMutationCounts(mutationsByRack, transaksiRepository.countMutationsByRackSince(startOfDay));
        addMutationCounts(mutationsByRack, inventoryTransferRepository.countMutationsFromRackSince(startOfDay));
        addMutationCounts(mutationsByRack, inventoryTransferRepository.countMutationsIntoRackSince(startOfDay));

        List<CycleCountRecommendation> recommendations = new ArrayList<>();
        for (Rak rack : rakRepository.findAll()) {
            long dailyMutationCount = mutationsByRack.getOrDefault(rack.getId(), 0L);
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
                    + Math.toIntExact(Math.min(30, varianceItems * 10))
                    + Math.toIntExact(Math.min(8L, dailyMutationCount) * 5L);
            String priority = score >= 80 ? "URGENT" : score >= 50 ? "HIGH"
                    : score >= 25 ? "NORMAL" : "LOW";
            List<String> reasons = new ArrayList<>();
            if (lastCount == null) reasons.add("belum pernah dihitung");
            else if (daysSinceCount >= 7) reasons.add("terakhir dihitung " + daysSinceCount + " hari lalu");
            if (riskItems > 0) reasons.add(riskItems + " SKU stok kritis");
            if (varianceItems > 0) reasons.add(varianceItems + " selisih pada opname terakhir");
            if (dailyMutationCount > 0) reasons.add(dailyMutationCount + " mutasi stok hari ini");
            if (reasons.isEmpty()) reasons.add("opname masih baru dan tanpa risiko terdeteksi");
            recommendations.add(new CycleCountRecommendation(rack, score, priority, String.join("; ", reasons),
                    lastCount == null ? null : lastCount.getPostedAt(), (int) riskItems, (int) varianceItems,
                    Math.toIntExact(Math.min(Integer.MAX_VALUE, dailyMutationCount))));
        }
        recommendations.sort(Comparator.comparingInt(CycleCountRecommendation::priorityScore).reversed()
                .thenComparing(recommendation -> recommendation.lastCountedAt() == null
                        ? LocalDateTime.MIN : recommendation.lastCountedAt())
                .thenComparing(recommendation -> recommendation.rack().getKodeRak(),
                        String.CASE_INSENSITIVE_ORDER));
        return List.copyOf(recommendations);
    }

    private void addMutationCounts(Map<Integer, Long> target, List<RackMutationCount> mutations) {
        for (RackMutationCount mutation : mutations) {
            target.merge(mutation.getRackId(), mutation.getMutationCount(), Long::sum);
        }
    }

    @Transactional(readOnly = true)
    public List<CountLineView> getCountLines(Integer countId) {
        return getCountLines(countId, null, false);
    }

    @Transactional(readOnly = true)
    public List<CountLineView> getCountLines(Integer countId, Integer userId, boolean editable) {
        StockCount count = getCount(countId);
        return count.getLines().stream()
                .filter(line -> !editable || line.getCountedQuantity() == null)
                .filter(line -> !editable || line.getAssignedUser() == null
                        || Objects.equals(line.getAssignedUser().getId(), userId))
                .sorted(Comparator.comparing(line -> line.getBarang().getKodeBarang(),
                        String.CASE_INSENSITIVE_ORDER))
                .map(line -> new CountLineView(line.getId(), line.getBarang().getKodeBarang(),
                        line.getBarang().getNamaBarang(), line.getExpectedQuantity(), line.getCountedQuantity(),
                        line.getBarang().getSatuan(), line.getAssignedUser() == null
                        ? null : line.getAssignedUser().getFullName(), line.getCountedBy() == null
                        ? null : line.getCountedBy().getFullName()))
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
        if (countedQuantities == null || countedQuantities.isEmpty())
            throw new IllegalArgumentException("Tidak ada baris opname yang dikirim.");
        List<StockCountLine> assignedLines = count.getLines().stream()
                .filter(line -> line.getCountedQuantity() == null)
                .filter(line -> line.getAssignedUser() == null
                        || Objects.equals(line.getAssignedUser().getId(), counter.getId())).toList();
        if (countedQuantities.size() > assignedLines.size())
            throw new IllegalArgumentException("Hasil hitung melebihi SKU yang masih ditugaskan kepada Anda.");
        Map<Integer, StockCountLine> linesById = new HashMap<>();
        for (StockCountLine line : assignedLines) linesById.put(line.getId(), line);
        for (Map.Entry<Integer, Integer> entry : countedQuantities.entrySet()) {
            StockCountLine line = linesById.get(entry.getKey());
            Integer quantity = entry.getValue();
            if (line == null || quantity == null || quantity < 0) {
                throw new IllegalArgumentException("Hasil hitung berisi baris yang tidak sah.");
            }
            line.setCountedQuantity(quantity);
            line.setCountedBy(counter);
        }
        boolean complete = count.getLines().stream().allMatch(line -> line.getCountedQuantity() != null);
        if (complete) {
            count.setCountedBy(counter);
            count.setStatus(StockCountStatus.COUNTED);
        }
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
                && count.getStatus() != StockCountStatus.COUNTED
                && count.getStatus() != StockCountStatus.SCHEDULED) {
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
                                Integer expectedQuantity, Integer countedQuantity, String unit,
                                String assignedUserName, String countedByName) {}

    public record CycleCountRecommendation(Rak rack, int priorityScore, String priority,
                                           String reason, LocalDateTime lastCountedAt,
                                           int riskItemCount, int previousVarianceCount,
                                           int dailyMutationCount) {}

    private record LockedLine(StockCountLine line, Barang item, InventoryBalance balance,
                              List<InventoryLotBalance> availableLots, int variance) {}
}
