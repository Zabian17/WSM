package com.mycompany.tugas_akhir.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "transaksi")
public class Transaksi {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(name = "kode_transaksi", nullable = false, unique = true, length = 20)
    private String kodeTransaksi;

    @Column(nullable = false, length = 10)
    private String tipe; // "masuk" or "keluar"

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "barang_id", nullable = false)
    private Barang barang;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "supplier_id")
    private Supplier supplier;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "rak_id")
    private Rak rak;

    @Column(nullable = false)
    private Integer jumlah;

    @Column(columnDefinition = "TEXT")
    private String keterangan;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(nullable = false)
    private LocalDateTime tanggal;

    public Transaksi() {}

    public Transaksi(Integer id, String kodeTransaksi, String tipe, Barang barang, Supplier supplier, Rak rak, Integer jumlah, String keterangan, User user, LocalDateTime tanggal) {
        this.id = id;
        this.kodeTransaksi = kodeTransaksi;
        this.tipe = tipe;
        this.barang = barang;
        this.supplier = supplier;
        this.rak = rak;
        this.jumlah = jumlah;
        this.keterangan = keterangan;
        this.user = user;
        this.tanggal = tanggal;
    }

    @PrePersist
    protected void onCreate() {
        if (tanggal == null) {
            tanggal = LocalDateTime.now();
        }
    }

    public Integer getId() { return id; }
    public void setId(Integer id) { this.id = id; }

    public String getKodeTransaksi() { return kodeTransaksi; }
    public void setKodeTransaksi(String kodeTransaksi) { this.kodeTransaksi = kodeTransaksi; }

    public String getTipe() { return tipe; }
    public void setTipe(String tipe) { this.tipe = tipe; }

    public Barang getBarang() { return barang; }
    public void setBarang(Barang barang) { this.barang = barang; }

    public Supplier getSupplier() { return supplier; }
    public void setSupplier(Supplier supplier) { this.supplier = supplier; }

    public Rak getRak() { return rak; }
    public void setRak(Rak rak) { this.rak = rak; }

    public Integer getJumlah() { return jumlah; }
    public void setJumlah(Integer jumlah) { this.jumlah = jumlah; }

    public String getKeterangan() { return keterangan; }
    public void setKeterangan(String keterangan) { this.keterangan = keterangan; }

    public User getUser() { return user; }
    public void setUser(User user) { this.user = user; }

    public LocalDateTime getTanggal() { return tanggal; }
    public void setTanggal(LocalDateTime tanggal) { this.tanggal = tanggal; }

    public static TransaksiBuilder builder() {
        return new TransaksiBuilder();
    }

    public static class TransaksiBuilder {
        private Integer id;
        private String kodeTransaksi;
        private String tipe;
        private Barang barang;
        private Supplier supplier;
        private Rak rak;
        private Integer jumlah;
        private String keterangan;
        private User user;
        private LocalDateTime tanggal;

        public TransaksiBuilder id(Integer id) { this.id = id; return this; }
        public TransaksiBuilder kodeTransaksi(String kodeTransaksi) { this.kodeTransaksi = kodeTransaksi; return this; }
        public TransaksiBuilder tipe(String tipe) { this.tipe = tipe; return this; }
        public TransaksiBuilder barang(Barang barang) { this.barang = barang; return this; }
        public TransaksiBuilder supplier(Supplier supplier) { this.supplier = supplier; return this; }
        public TransaksiBuilder rak(Rak rak) { this.rak = rak; return this; }
        public TransaksiBuilder jumlah(Integer jumlah) { this.jumlah = jumlah; return this; }
        public TransaksiBuilder keterangan(String keterangan) { this.keterangan = keterangan; return this; }
        public TransaksiBuilder user(User user) { this.user = user; return this; }
        public TransaksiBuilder tanggal(LocalDateTime tanggal) { this.tanggal = tanggal; return this; }

        public Transaksi build() {
            return new Transaksi(id, kodeTransaksi, tipe, barang, supplier, rak, jumlah, keterangan, user, tanggal);
        }
    }
}
