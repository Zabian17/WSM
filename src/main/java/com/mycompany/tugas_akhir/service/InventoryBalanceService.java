package com.mycompany.tugas_akhir.service;

import com.mycompany.tugas_akhir.model.Barang;
import com.mycompany.tugas_akhir.model.InventoryBalance;
import com.mycompany.tugas_akhir.model.InventoryLotBalance;
import com.mycompany.tugas_akhir.repository.BarangRepository;
import com.mycompany.tugas_akhir.repository.InventoryBalanceRepository;
import com.mycompany.tugas_akhir.repository.InventoryLotBalanceRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.time.LocalDateTime;

@Service
public class InventoryBalanceService implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(InventoryBalanceService.class);
    private final BarangRepository barangRepository;
    private final InventoryBalanceRepository balanceRepository;
    private final InventoryLotBalanceRepository lotBalanceRepository;

    public InventoryBalanceService(BarangRepository barangRepository, InventoryBalanceRepository balanceRepository,
                                   InventoryLotBalanceRepository lotBalanceRepository) {
        this.barangRepository = barangRepository;
        this.balanceRepository = balanceRepository;
        this.lotBalanceRepository = lotBalanceRepository;
    }

    @Transactional(readOnly = true)
    public List<InventoryBalance> getAllBalancesByLocation() {
        return balanceRepository.findAllForLocationReport();
    }

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        for (Barang listedBarang : barangRepository.findAll()) {
            Barang barang = barangRepository.findByIdForUpdate(listedBarang.getId()).orElse(null);
                if (barang == null || balanceRepository.existsByBarangId(barang.getId())) {
                continue;
            }
            if (barang.getStok() < 0) {
                log.warn("Stok lama negatif untuk barang {}; saldo lokasi awal tidak dibuat.", barang.getId());
                continue;
            }
            balanceRepository.save(new InventoryBalance(
                    barang, null, InventoryBalance.UNASSIGNED_LOCATION, barang.getStok()));
        }

        for (InventoryBalance balance : balanceRepository.findAllForLocationReport()) {
            if (lotBalanceRepository.existsByBarangIdAndLocationKey(
                    balance.getBarang().getId(), balance.getLocationKey())) {
                continue;
            }
            lotBalanceRepository.save(new InventoryLotBalance(balance.getBarang(), balance.getRak(),
                    balance.getLocationKey(), InventoryLotBalance.OPENING_BATCH_CODE, null,
                    LocalDateTime.of(1970, 1, 1, 0, 0), balance.getQuantity()));
        }
    }
}