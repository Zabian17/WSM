package com.mycompany.tugas_akhir;

import java.time.LocalDateTime;

public class TransaksiDAO {

    public static class Transaksi {
        public String kodeTransaksi;
        public String tipe;
        public String namaBarang;
        public String namaSupplier;
        public String keterangan;
        public int jumlah;
        public LocalDateTime tanggal;
    }
}
