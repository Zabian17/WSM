package com.mycompany.tugas_akhir.service;

import com.mycompany.tugas_akhir.model.Barang;
import com.mycompany.tugas_akhir.repository.BarangRepository;
import com.mycompany.tugas_akhir.repository.InventoryLotBalanceRepository;
import com.mycompany.tugas_akhir.repository.InventoryBalanceRepository;
import com.mycompany.tugas_akhir.repository.TransaksiRepository;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.time.YearMonth;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class StockSuggestionServiceTest {

    @Test
    void averagesCompletedMonthsAndSuggestsMaxOnlyWhenAvailableIsBelowBuffer() {
        Barang item = Barang.builder().id(1).kodeBarang("SKU-1").namaBarang("Item")
                .stok(12).stokMin(5).build();
        item.setStokMax(20);
        BarangRepository items = mock(BarangRepository.class);
        TransaksiRepository transactions = mock(TransaksiRepository.class);
        InventoryLotBalanceRepository lots = mock(InventoryLotBalanceRepository.class);
        LocalDateTime end = YearMonth.now().atDay(1).atStartOfDay();
        when(items.findAll()).thenReturn(List.of(item));
        when(transactions.sumOutboundQuantityByItemBetween(end.minusMonths(3), end))
                .thenReturn(List.<Object[]>of(new Object[]{1, 18L}));
        when(lots.sumReservedQuantityByItem()).thenReturn(List.<Object[]>of(new Object[]{1, 8L}));

        var suggestion = new StockSuggestionService(items, transactions, lots, 3)
                .getSuggestions().getFirst();

        assertEquals(6.0, suggestion.monthlyAverageDemand());
        assertEquals(4, suggestion.availableQuantity());
        assertEquals(16, suggestion.suggestedPurchaseQuantity());
    }

    @Test
    void zeroMaxDisablesReplenishmentSuggestion() {
        Barang item = Barang.builder().id(2).kodeBarang("SKU-2").namaBarang("No target")
                .stok(1).stokMin(4).build();
        BarangRepository items = mock(BarangRepository.class);
        TransaksiRepository transactions = mock(TransaksiRepository.class);
        InventoryLotBalanceRepository lots = mock(InventoryLotBalanceRepository.class);
        when(items.findAll()).thenReturn(List.of(item));

        var suggestion = new StockSuggestionService(items, transactions, lots, 3)
                .getSuggestions().getFirst();

        assertEquals(0, suggestion.suggestedPurchaseQuantity());
    }

    @Test
    void itemStockTargetsCanBeUpdatedAndMaximumCannotBeBelowBuffer() {
        Barang item = Barang.builder().id(1).kodeBarang("SKU-1").namaBarang("Item")
                .stok(8).stokMin(3).build();
        BarangRepository items = mock(BarangRepository.class);
        when(items.findByIdForUpdate(1)).thenReturn(java.util.Optional.of(item));
        BarangService service = new BarangService(items, mock(InventoryBalanceRepository.class),
                mock(InventoryLotBalanceRepository.class));

        assertTrue(service.updateStockTargets(1, 6, 25));
        assertEquals(6, item.getStokMin());
        assertEquals(25, item.getStokMax());
        assertFalse(service.updateStockTargets(1, 12, 10));
    }
}
