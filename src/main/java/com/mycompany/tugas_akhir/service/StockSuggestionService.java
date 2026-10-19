package com.mycompany.tugas_akhir.service;

import com.mycompany.tugas_akhir.model.Barang;
import com.mycompany.tugas_akhir.repository.BarangRepository;
import com.mycompany.tugas_akhir.repository.InventoryLotBalanceRepository;
import com.mycompany.tugas_akhir.repository.TransaksiRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.YearMonth;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class StockSuggestionService {

    private final BarangRepository barangRepository;
    private final TransaksiRepository transaksiRepository;
    private final InventoryLotBalanceRepository lotBalanceRepository;
    private final int historyMonths;

    public StockSuggestionService(BarangRepository barangRepository,
                                  TransaksiRepository transaksiRepository,
                                  InventoryLotBalanceRepository lotBalanceRepository,
                                  @org.springframework.beans.factory.annotation.Value(
                                          "${gudangku.inventory.demand-history-months:3}") int historyMonths) {
        if (historyMonths < 1 || historyMonths > 24)
            throw new IllegalArgumentException("Demand history must be between 1 and 24 months.");
        this.barangRepository = barangRepository;
        this.transaksiRepository = transaksiRepository;
        this.lotBalanceRepository = lotBalanceRepository;
        this.historyMonths = historyMonths;
    }

    @Transactional(readOnly = true)
    public List<StockSuggestion> getSuggestions() {
        LocalDateTime end = YearMonth.now().atDay(1).atStartOfDay();
        LocalDateTime start = end.minusMonths(historyMonths);
        Map<Integer, Long> demandByItem = sumsByItem(
                transaksiRepository.sumOutboundQuantityByItemBetween(start, end));
        Map<Integer, Long> reservedByItem = sumsByItem(lotBalanceRepository.sumReservedQuantityByItem());
        return barangRepository.findAll().stream().map(item -> {
            int stock = Math.max(0, value(item.getStok()));
            int reserved = (int) Math.min(Integer.MAX_VALUE, reservedByItem.getOrDefault(item.getId(), 0L));
            int available = Math.max(0, stock - reserved);
            int buffer = Math.max(0, value(item.getStokMin()));
            int maximum = Math.max(0, value(item.getStokMax()));
            int suggested = maximum > 0 && maximum >= buffer && available < buffer
                    ? maximum - available : 0;
            double average = demandByItem.getOrDefault(item.getId(), 0L) / (double) historyMonths;
            return new StockSuggestion(item.getId(), item.getKodeBarang(), item.getNamaBarang(),
                    stock, reserved, available, average, buffer, maximum, suggested);
        }).toList();
    }

    public int getHistoryMonths() {
        return historyMonths;
    }

    private Map<Integer, Long> sumsByItem(List<Object[]> rows) {
        Map<Integer, Long> sums = new HashMap<>();
        for (Object[] row : rows) {
            Integer itemId = ((Number) row[0]).intValue();
            long quantity = ((Number) row[1]).longValue();
            sums.put(itemId, quantity);
        }
        return sums;
    }

    private int value(Integer quantity) {
        return quantity == null ? 0 : quantity;
    }

    public record StockSuggestion(Integer itemId, String sku, String itemName, int stock,
                                  int reservedQuantity, int availableQuantity, double monthlyAverageDemand,
                                  int bufferStock, int maximumStock, int suggestedPurchaseQuantity) {}
}
