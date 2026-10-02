package com.mycompany.tugas_akhir.model;

import jakarta.persistence.*;

@Entity
@Table(name = "customer")
public class Customer {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(name = "nama_customer", nullable = false, length = 150)
    private String namaCustomer;

    @Column(length = 100)
    private String posisi;

    @Column(columnDefinition = "TEXT")
    private String alamat;

    @Column(length = 20)
    private String kontak;

    @Column(length = 150)
    private String email;

    @Column(length = 20)
    private String status; // 'Aktif' or 'Nonaktif'

    public Customer() {}

    public Customer(Integer id, String namaCustomer, String posisi, String alamat, String kontak, String email, String status) {
        this.id = id;
        this.namaCustomer = namaCustomer;
        this.posisi = posisi;
        this.alamat = alamat;
        this.kontak = kontak;
        this.email = email;
        this.status = status;
    }

    public Integer getId() { return id; }
    public void setId(Integer id) { this.id = id; }

    public String getNamaCustomer() { return namaCustomer; }
    public void setNamaCustomer(String namaCustomer) { this.namaCustomer = namaCustomer; }

    public String getPosisi() { return posisi; }
    public void setPosisi(String posisi) { this.posisi = posisi; }

    public String getAlamat() { return alamat; }
    public void setAlamat(String alamat) { this.alamat = alamat; }

    public String getKontak() { return kontak; }
    public void setKontak(String kontak) { this.kontak = kontak; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    @Override
    public String toString() {
        return namaCustomer;
    }

    public static CustomerBuilder builder() {
        return new CustomerBuilder();
    }

    public static class CustomerBuilder {
        private Integer id;
        private String namaCustomer;
        private String posisi;
        private String alamat;
        private String kontak;
        private String email;
        private String status;

        public CustomerBuilder id(Integer id) { this.id = id; return this; }
        public CustomerBuilder namaCustomer(String namaCustomer) { this.namaCustomer = namaCustomer; return this; }
        public CustomerBuilder posisi(String posisi) { this.posisi = posisi; return this; }
        public CustomerBuilder alamat(String alamat) { this.alamat = alamat; return this; }
        public CustomerBuilder kontak(String kontak) { this.kontak = kontak; return this; }
        public CustomerBuilder email(String email) { this.email = email; return this; }
        public CustomerBuilder status(String status) { this.status = status; return this; }

        public Customer build() {
            return new Customer(id, namaCustomer, posisi, alamat, kontak, email, status);
        }
    }
}
