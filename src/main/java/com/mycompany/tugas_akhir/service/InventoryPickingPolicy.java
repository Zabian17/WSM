package com.mycompany.tugas_akhir.service;

import com.mycompany.tugas_akhir.model.InventoryLotBalance;
import com.mycompany.tugas_akhir.repository.InventoryLotBalanceRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.List;
import java.util.Locale;

@Service
public class InventoryPickingPolicy {

    private enum Strategy {
        FIFO,
        FEFO,
        LEFO
    }

    private final InventoryLotBalanceRepository lotBalanceRepository;
    private final Strategy strategy;

    public InventoryPickingPolicy(InventoryLotBalanceRepository lotBalanceRepository,
                                  @Value("${gudangku.inventory.picking-strategy:FEFO}") String configuredStrategy) {
        this.lotBalanceRepository = lotBalanceRepository;
        this.strategy = parseStrategy(configuredStrategy);
    }

    public List<InventoryLotBalance> availableAt(Integer itemId, String locationKey, LocalDate today) {
        return switch (strategy) {
            case FIFO -> lotBalanceRepository.findAvailableFifoForUpdate(itemId, locationKey, today);
            case FEFO -> lotBalanceRepository.findAvailableFefoForUpdate(itemId, locationKey, today);
            case LEFO -> lotBalanceRepository.findAvailableLefoForUpdate(itemId, locationKey, today);
        };
    }

    public List<InventoryLotBalance> availableAcrossLocations(Integer itemId, LocalDate today) {
        return switch (strategy) {
            case FIFO -> lotBalanceRepository.findAvailableFifoAcrossLocationsForUpdate(itemId, today);
            case FEFO -> lotBalanceRepository.findAvailableFefoAcrossLocationsForUpdate(itemId, today);
            case LEFO -> lotBalanceRepository.findAvailableLefoAcrossLocationsForUpdate(itemId, today);
        };
    }

    private Strategy parseStrategy(String configuredStrategy) {
        String normalized = configuredStrategy == null || configuredStrategy.isBlank()
                ? "FEFO" : configuredStrategy.trim().toUpperCase(Locale.ROOT);
        try {
            return Strategy.valueOf(normalized);
        } catch (IllegalArgumentException exception) {
            throw new IllegalArgumentException(
                    "PICKING_STRATEGY must be FIFO, FEFO, or LEFO; got: " + normalized, exception);
        }
    }
}
