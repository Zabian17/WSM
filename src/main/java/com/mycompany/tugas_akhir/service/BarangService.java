package com.mycompany.tugas_akhir.service;

import com.mycompany.tugas_akhir.model.Barang;
import com.mycompany.tugas_akhir.model.InventoryBalance;
import com.mycompany.tugas_akhir.model.InventoryLotBalance;
import com.mycompany.tugas_akhir.repository.BarangRepository;
import com.mycompany.tugas_akhir.repository.InventoryBalanceRepository;
import com.mycompany.tugas_akhir.repository.InventoryLotBalanceRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Service
public class BarangService {

    private static final Logger log = LoggerFactory.getLogger(BarangService.class);
    private final BarangRepository barangRepository;
    private final InventoryBalanceRepository balanceRepository;
    private final InventoryLotBalanceRepository lotBalanceRepository;

    public BarangService(BarangRepository barangRepository, InventoryBalanceRepository balanceRepository,
                         InventoryLotBalanceRepository lotBalanceRepository) {
        this.barangRepository = barangRepository;
        this.balanceRepository = balanceRepository;
        this.lotBalanceRepository = lotBalanceRepository;
    }

    @Transactional(readOnly = true)
    public List<Barang> getAllBarang() {
        return barangRepository.findAll();
    }

    @Transactional(readOnly = true)
    public List<Barang> searchBarang(String keyword) {
        if (keyword == null || keyword.isBlank()) {
            return getAllBarang();
        }
        return barangRepository.searchByKeyword(keyword);
    }

    @Transactional
    public Barang addBarang(String kode, String nama, String kategori, String satuan, int stok, int stokMin) {
        return addBarang(kode, nama, kategori, satuan, stok, stokMin, null);
    }

    @Transactional
    public Barang addBarang(String kode, String nama, String kategori, String satuan,
                            int stok, int stokMin, BigDecimal volumeCbm) {
        return addBarang(kode, nama, kategori, satuan, stok, stokMin, volumeCbm, null);
    }

    @Transactional
    public Barang addBarang(String kode, String nama, String kategori, String satuan,
                            int stok, int stokMin, BigDecimal volumeCbm, String storageType) {
        if (stok < 0 || stokMin < 0) {
            throw new IllegalArgumentException("Stok dan stok minimum tidak boleh negatif.");
        }
        validateVolumeCbm(volumeCbm);
        if (storageType != null && storageType.trim().length() > 40)
            throw new IllegalArgumentException("Tipe penyimpanan maksimal 40 karakter.");
        Barang b = Barang.builder()
                .kodeBarang(kode)
                .namaBarang(nama)
                .kategori((kategori == null || kategori.isBlank()) ? "Umum" : kategori)
                .satuan((satuan == null || satuan.isBlank()) ? "pcs" : satuan)
                .stok(stok)
                .stokMin(stokMin)
                .build();
            b.setVolumeCbm(volumeCbm);
            b.setStorageType(normalizeStorageType(storageType));
            Barang saved = barangRepository.save(b);
            balanceRepository.save(new InventoryBalance(saved, null,
                InventoryBalance.UNASSIGNED_LOCATION, stok));
            lotBalanceRepository.save(new InventoryLotBalance(saved, null,
                    InventoryBalance.UNASSIGNED_LOCATION, InventoryLotBalance.OPENING_BATCH_CODE,
                    null, LocalDateTime.now(), stok));
            return saved;
    }

    @Transactional
    public boolean updateBarang(int id, String nama, String kategori, String satuan, int stok, int stokMin) {
        return updateBarang(id, nama, kategori, satuan, stok, stokMin, null);
    }

    @Transactional
    public boolean updateBarang(int id, String nama, String kategori, String satuan,
                                int stok, int stokMin, BigDecimal volumeCbm) {
        return updateBarang(id, nama, kategori, satuan, stok, stokMin, volumeCbm, null);
    }

    @Transactional
    public boolean updateBarang(int id, String nama, String kategori, String satuan,
                                int stok, int stokMin, BigDecimal volumeCbm, String storageType) {
        if (stok < 0 || stokMin < 0) {
            return false;
        }
        try {
            validateVolumeCbm(volumeCbm);
            if (storageType != null && storageType.trim().length() > 40) return false;
        } catch (IllegalArgumentException exception) {
            return false;
        }
        return barangRepository.findById(id).map(existing -> {
            existing.setNamaBarang(nama);
            existing.setKategori((kategori == null || kategori.isBlank()) ? "Umum" : kategori);
            existing.setSatuan((satuan == null || satuan.isBlank()) ? "pcs" : satuan);
            existing.setStokMin(stokMin);
            existing.setVolumeCbm(volumeCbm);
            existing.setStorageType(normalizeStorageType(storageType));
            barangRepository.save(existing);
            return true;
        }).orElse(false);
    }

    private void validateVolumeCbm(BigDecimal volumeCbm) {
        if (volumeCbm != null && (volumeCbm.signum() <= 0 || volumeCbm.precision() > 12
                || Math.max(0, volumeCbm.stripTrailingZeros().scale()) > 6)) {
            throw new IllegalArgumentException("CBM barang harus positif dengan maksimal 6 desimal.");
        }
    }

    private String normalizeStorageType(String storageType) {
        return storageType == null || storageType.isBlank() ? null
                : storageType.trim().toUpperCase(java.util.Locale.ROOT);
    }

    @Transactional
    public boolean deleteBarang(int id) {
        try {
            if (barangRepository.existsById(id)) {
                barangRepository.deleteById(id);
                return true;
            }
        } catch (Exception e) {
            log.error("[BarangService] Gagal menghapus barang id {}: {}", id, e.getMessage());
        }
        return false;
    }

    @Transactional(readOnly = true)
    public String generateKodeBarang() {
        int nextSeq = barangRepository.findMaxKodeSequence() + 1;
        return String.format("BRG-%03d", nextSeq);
    }
}
