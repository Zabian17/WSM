package com.mycompany.tugas_akhir;
<<<<<<< HEAD
// Obsolete legacy DAO - Replaced by TransaksiRepository & TransaksiService
=======

import java.sql.*;
import java.util.ArrayList;
import java.util.List;


public class TransaksiDAO {

    
    public static class Transaksi {
        public int    id;
        public String kodeTransaksi;
        public String tipe;
        public int    barangId;
        public String namaBarang;
        public String namaSupplier;
        public String kodeRak;
        public int    jumlah;
        public String keterangan;
        public String alasan;
        public String tanggal;

        public Transaksi(int id, String kodeTransaksi, String tipe, int barangId,
                         String namaBarang, String namaSupplier, String kodeRak,
                         int jumlah, String keterangan, String alasan, String tanggal) {
            this.id             = id;
            this.kodeTransaksi  = kodeTransaksi;
            this.tipe           = tipe;
            this.barangId       = barangId;
            this.namaBarang     = namaBarang;
            this.namaSupplier   = namaSupplier;
            this.kodeRak        = kodeRak;
            this.jumlah         = jumlah;
            this.keterangan     = keterangan;
            this.alasan         = alasan;
            this.tanggal        = tanggal;
        }
    }

    
    public List<Transaksi> getAllTransaksi() {
        List<Transaksi> list = new ArrayList<>();
        String sql = "SELECT t.id, t.kode_transaksi, t.tipe, t.barang_id, "
                   + "b.nama_barang, "
                   + "COALESCE(s.nama_supplier, '-') AS supplier, "
                   + "COALESCE(r.kode_rak, '-') AS kode_rak, "
                   + "t.jumlah, COALESCE(t.keterangan, '') AS keterangan, "
                   + "'' AS alasan, "
                   + "DATE_FORMAT(t.tanggal, '%d/%m/%Y %H:%i') AS tgl "
                   + "FROM transaksi t "
                   + "JOIN barang b ON t.barang_id = b.id "
                   + "LEFT JOIN supplier s ON t.supplier_id = s.id "
                   + "LEFT JOIN rak r ON t.rak_id = r.id "
                   + "ORDER BY t.tanggal DESC LIMIT 200";
        try (Connection conn = DatabaseConnection.getInstance().getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                list.add(new Transaksi(
                    rs.getInt("id"),
                    rs.getString("kode_transaksi"),
                    rs.getString("tipe"),
                    rs.getInt("barang_id"),
                    rs.getString("nama_barang"),
                    rs.getString("supplier"),
                    rs.getString("kode_rak"),
                    rs.getInt("jumlah"),
                    rs.getString("keterangan"),
                    rs.getString("alasan"),
                    rs.getString("tgl")
                ));
            }
        } catch (SQLException e) {
            System.err.println("[TransaksiDAO] getAllTransaksi: " + e.getMessage());
        }
        return list;
    }

    
    
    public boolean addTransaksi(String tipe, int barangId, Integer supplierId,
                                Integer rakId, int jumlah, String keterangan, int userId) {
        Connection conn = DatabaseConnection.getInstance().getConnection();
        try {
            conn.setAutoCommit(false);

            
            String kode = generateKode(conn);

            
            String sqlIns = "INSERT INTO transaksi "
                          + "(kode_transaksi, tipe, barang_id, supplier_id, rak_id, jumlah, keterangan, user_id) "
                          + "VALUES (?,?,?,?,?,?,?,?)";
            try (PreparedStatement ps = conn.prepareStatement(sqlIns)) {
                ps.setString(1, kode);
                ps.setString(2, tipe);
                ps.setInt(3, barangId);
                if (supplierId != null) ps.setInt(4, supplierId);  else ps.setNull(4, Types.INTEGER);
                if (rakId     != null) ps.setInt(5, rakId);        else ps.setNull(5, Types.INTEGER);
                ps.setInt(6, jumlah);
                ps.setString(7, keterangan.isEmpty() ? null : keterangan);
                ps.setInt(8, userId);
                ps.executeUpdate();
            }

            
            if ("masuk".equals(tipe)) {
                String sqlUp = "UPDATE barang SET stok = stok + ? WHERE id = ?";
                try (PreparedStatement ps = conn.prepareStatement(sqlUp)) {
                    ps.setInt(1, jumlah);
                    ps.setInt(2, barangId);
                    ps.executeUpdate();
                }
            } else {
                
                String sqlUp = "UPDATE barang SET stok = stok - ? WHERE id = ? AND stok >= ?";
                try (PreparedStatement ps = conn.prepareStatement(sqlUp)) {
                    ps.setInt(1, jumlah);
                    ps.setInt(2, barangId);
                    ps.setInt(3, jumlah);
                    int rows = ps.executeUpdate();
                    if (rows == 0) {
                        conn.rollback();
                        conn.setAutoCommit(true);
                        System.err.println("[TransaksiDAO] Stok tidak mencukupi, barang_id=" + barangId);
                        return false;
                    }
                }
            }

            conn.commit();
            conn.setAutoCommit(true);
            System.out.println("[TransaksiDAO] Transaksi berhasil: " + kode);
            return true;

        } catch (SQLException e) {
            System.err.println("[TransaksiDAO] addTransaksi: " + e.getMessage());
            try { conn.rollback(); conn.setAutoCommit(true); } catch (SQLException ex) {  }
            return false;
        }
    }

    
    
    public int[] getStatistik24H() {
        int masuk = 0, keluar = 0;
        String sql = "SELECT tipe, COALESCE(SUM(jumlah), 0) AS total FROM transaksi "
                   + "WHERE tanggal >= DATE_SUB(NOW(), INTERVAL 24 HOUR) GROUP BY tipe";
        try (Connection conn = DatabaseConnection.getInstance().getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                if ("masuk".equals(rs.getString("tipe")))  masuk  = rs.getInt("total");
                else                                        keluar = rs.getInt("total");
            }
        } catch (SQLException e) {
            System.err.println("[TransaksiDAO] getStatistik24H: " + e.getMessage());
        }
        return new int[]{masuk, keluar};
    }

    
    
    public int[] getReportSummary(String startDate, String endDate) {
        int totalTx = 0, totalMasuk = 0, totalKeluar = 0, barangAktif = 0;
        String sql = "SELECT "
                   + "COUNT(*) as total_tx, "
                   + "COALESCE(SUM(CASE WHEN tipe='masuk' THEN jumlah ELSE 0 END), 0) as total_masuk, "
                   + "COALESCE(SUM(CASE WHEN tipe='keluar' THEN jumlah ELSE 0 END), 0) as total_keluar, "
                   + "COUNT(DISTINCT barang_id) as barang_aktif "
                   + "FROM transaksi "
                   + "WHERE DATE(tanggal) BETWEEN ? AND ?";
        try (Connection conn = DatabaseConnection.getInstance().getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, startDate);
            ps.setString(2, endDate);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                totalTx      = rs.getInt("total_tx");
                totalMasuk   = rs.getInt("total_masuk");
                totalKeluar  = rs.getInt("total_keluar");
                barangAktif  = rs.getInt("barang_aktif");
            }
        } catch (SQLException e) {
            System.err.println("[TransaksiDAO] getReportSummary: " + e.getMessage());
        }
        return new int[]{totalTx, totalMasuk, totalKeluar, barangAktif};
    }

    
    public List<Transaksi> getTransaksiByPeriod(String startDate, String endDate, String tipe) {
        List<Transaksi> list = new ArrayList<>();
        StringBuilder sql = new StringBuilder(
            "SELECT t.id, t.kode_transaksi, t.tipe, t.barang_id, "
          + "b.nama_barang, "
          + "COALESCE(s.nama_supplier, '-') AS supplier, "
          + "COALESCE(r.kode_rak, '-') AS kode_rak, "
          + "t.jumlah, COALESCE(t.keterangan, '') AS keterangan, "
          + "'' AS alasan, "
          + "DATE_FORMAT(t.tanggal, '%d/%m/%Y %H:%i') AS tgl "
          + "FROM transaksi t "
          + "JOIN barang b ON t.barang_id = b.id "
          + "LEFT JOIN supplier s ON t.supplier_id = s.id "
          + "LEFT JOIN rak r ON t.rak_id = r.id "
          + "WHERE DATE(t.tanggal) BETWEEN ? AND ?"
        );
        if (tipe != null && !tipe.isEmpty() && !tipe.equalsIgnoreCase("semua")) {
            sql.append(" AND t.tipe = ?");
        }
        sql.append(" ORDER BY t.tanggal DESC LIMIT 500");

        try (Connection conn = DatabaseConnection.getInstance().getConnection();
             PreparedStatement ps = conn.prepareStatement(sql.toString())) {
            ps.setString(1, startDate);
            ps.setString(2, endDate);
            if (tipe != null && !tipe.isEmpty() && !tipe.equalsIgnoreCase("semua")) {
                ps.setString(3, tipe.toLowerCase());
            }
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                list.add(new Transaksi(
                    rs.getInt("id"),
                    rs.getString("kode_transaksi"),
                    rs.getString("tipe"),
                    rs.getInt("barang_id"),
                    rs.getString("nama_barang"),
                    rs.getString("supplier"),
                    rs.getString("kode_rak"),
                    rs.getInt("jumlah"),
                    rs.getString("keterangan"),
                    rs.getString("alasan"),
                    rs.getString("tgl")
                ));
            }
        } catch (SQLException e) {
            System.err.println("[TransaksiDAO] getTransaksiByPeriod: " + e.getMessage());
        }
        return list;
    }

    
    public static class TopBarang {
        public String namaBarang;
        public String kodeBarang;
        public int    totalJumlah;
        public int    totalTransaksi;

        public TopBarang(String namaBarang, String kodeBarang, int totalJumlah, int totalTransaksi) {
            this.namaBarang     = namaBarang;
            this.kodeBarang     = kodeBarang;
            this.totalJumlah    = totalJumlah;
            this.totalTransaksi = totalTransaksi;
        }
    }

    
    public List<TopBarang> getTopBarang(String startDate, String endDate, int limit) {
        List<TopBarang> list = new ArrayList<>();
        String sql = "SELECT b.nama_barang, b.kode_barang, "
                   + "SUM(t.jumlah) as total_jumlah, COUNT(t.id) as total_transaksi "
                   + "FROM transaksi t "
                   + "JOIN barang b ON t.barang_id = b.id "
                   + "WHERE DATE(t.tanggal) BETWEEN ? AND ? "
                   + "GROUP BY b.id "
                   + "ORDER BY total_jumlah DESC "
                   + "LIMIT ?";
        try (Connection conn = DatabaseConnection.getInstance().getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, startDate);
            ps.setString(2, endDate);
            ps.setInt(3, limit);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                list.add(new TopBarang(
                    rs.getString("nama_barang"),
                    rs.getString("kode_barang"),
                    rs.getInt("total_jumlah"),
                    rs.getInt("total_transaksi")
                ));
            }
        } catch (SQLException e) {
            System.err.println("[TransaksiDAO] getTopBarang: " + e.getMessage());
        }
        return list;
    }

    
    private String generateKode(Connection conn) throws SQLException {
        String sql = "SELECT COALESCE(MAX(CAST(SUBSTRING(kode_transaksi, 5) AS UNSIGNED)), 0) "
                   + "FROM transaksi WHERE kode_transaksi REGEXP '^TRX-'";
        try (PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            if (rs.next()) return String.format("TRX-%05d", rs.getInt(1) + 1);
        }
        return "TRX-00001";
    }
}
>>>>>>> origin/main
