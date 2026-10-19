package com.mycompany.tugas_akhir.model;

import jakarta.persistence.*;
import java.math.BigDecimal;

@Entity
@Table(name = "barang")
public class Barang {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(name = "kode_barang", nullable = false, unique = true, length = 20)
    private String kodeBarang;

    @Column(name = "nama_barang", nullable = false, length = 150)
    private String namaBarang;

    @Column(length = 80)
    private String kategori;

    @Column(length = 20)
    private String satuan;

    @Column(nullable = false)
    private Integer stok;

    @Column(name = "stok_min", nullable = false)
    private Integer stokMin;

    @Column(name = "stok_max", nullable = false)
    private Integer stokMax = 0;

    @Column(name = "volume_cbm", precision = 12, scale = 6)
    private BigDecimal volumeCbm;

    @Column(name = "storage_type", length = 40)
    private String storageType;

    public Barang() {}

    public Barang(Integer id, String kodeBarang, String namaBarang, String kategori, String satuan, Integer stok, Integer stokMin) {
        this.id = id;
        this.kodeBarang = kodeBarang;
        this.namaBarang = namaBarang;
        this.kategori = kategori;
        this.satuan = satuan;
        this.stok = stok;
        this.stokMin = stokMin;
    }

    public Integer getId() { return id; }
    public void setId(Integer id) { this.id = id; }

    public String getKodeBarang() { return kodeBarang; }
    public void setKodeBarang(String kodeBarang) { this.kodeBarang = kodeBarang; }

    public String getNamaBarang() { return namaBarang; }
    public void setNamaBarang(String namaBarang) { this.namaBarang = namaBarang; }

    public String getKategori() { return kategori; }
    public void setKategori(String kategori) { this.kategori = kategori; }

    public String getSatuan() { return satuan; }
    public void setSatuan(String satuan) { this.satuan = satuan; }

    public Integer getStok() { return stok; }
    public void setStok(Integer stok) { this.stok = stok; }

    public Integer getStokMin() { return stokMin; }
    public void setStokMin(Integer stokMin) { this.stokMin = stokMin; }
    public Integer getStokMax() { return stokMax == null ? 0 : stokMax; }
    public void setStokMax(Integer stokMax) { this.stokMax = stokMax; }
    public BigDecimal getVolumeCbm() { return volumeCbm; }
    public void setVolumeCbm(BigDecimal volumeCbm) { this.volumeCbm = volumeCbm; }
    public String getStorageType() { return storageType; }
    public void setStorageType(String storageType) { this.storageType = storageType; }

    @Override
    public String toString() {
        return namaBarang + " (" + kodeBarang + ")";
    }

    public static BarangBuilder builder() {
        return new BarangBuilder();
    }

    public static class BarangBuilder {
        private Integer id;
        private String kodeBarang;
        private String namaBarang;
        private String kategori;
        private String satuan;
        private Integer stok;
        private Integer stokMin;
        private Integer stokMax;

        public BarangBuilder id(Integer id) { this.id = id; return this; }
        public BarangBuilder kodeBarang(String kodeBarang) { this.kodeBarang = kodeBarang; return this; }
        public BarangBuilder namaBarang(String namaBarang) { this.namaBarang = namaBarang; return this; }
        public BarangBuilder kategori(String kategori) { this.kategori = kategori; return this; }
        public BarangBuilder satuan(String satuan) { this.satuan = satuan; return this; }
        public BarangBuilder stok(Integer stok) { this.stok = stok; return this; }
        public BarangBuilder stokMin(Integer stokMin) { this.stokMin = stokMin; return this; }
        public BarangBuilder stokMax(Integer stokMax) { this.stokMax = stokMax; return this; }

        public Barang build() {
            Barang barang = new Barang(id, kodeBarang, namaBarang, kategori, satuan, stok, stokMin);
            barang.setStokMax(stokMax == null ? 0 : stokMax);
            return barang;
        }
    }
}
