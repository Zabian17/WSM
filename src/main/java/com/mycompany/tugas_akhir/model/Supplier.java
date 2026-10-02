package com.mycompany.tugas_akhir.model;

import jakarta.persistence.*;

@Entity
@Table(name = "supplier")
public class Supplier {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(name = "nama_supplier", nullable = false, length = 150)
    private String namaSupplier;

    @Column(length = 20)
    private String kontak;

    @Column(length = 150)
    private String email;

    @Column(columnDefinition = "TEXT")
    private String alamat;

    public Supplier() {}

    public Supplier(Integer id, String namaSupplier, String kontak, String email, String alamat) {
        this.id = id;
        this.namaSupplier = namaSupplier;
        this.kontak = kontak;
        this.email = email;
        this.alamat = alamat;
    }

    public Integer getId() { return id; }
    public void setId(Integer id) { this.id = id; }

    public String getNamaSupplier() { return namaSupplier; }
    public void setNamaSupplier(String namaSupplier) { this.namaSupplier = namaSupplier; }

    public String getKontak() { return kontak; }
    public void setKontak(String kontak) { this.kontak = kontak; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public String getAlamat() { return alamat; }
    public void setAlamat(String alamat) { this.alamat = alamat; }

    @Override
    public String toString() {
        return namaSupplier;
    }

    public static SupplierBuilder builder() {
        return new SupplierBuilder();
    }

    public static class SupplierBuilder {
        private Integer id;
        private String namaSupplier;
        private String kontak;
        private String email;
        private String alamat;

        public SupplierBuilder id(Integer id) { this.id = id; return this; }
        public SupplierBuilder namaSupplier(String namaSupplier) { this.namaSupplier = namaSupplier; return this; }
        public SupplierBuilder kontak(String kontak) { this.kontak = kontak; return this; }
        public SupplierBuilder email(String email) { this.email = email; return this; }
        public SupplierBuilder alamat(String alamat) { this.alamat = alamat; return this; }

        public Supplier build() {
            return new Supplier(id, namaSupplier, kontak, email, alamat);
        }
    }
}
