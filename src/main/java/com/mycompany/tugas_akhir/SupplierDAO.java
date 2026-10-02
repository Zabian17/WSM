package com.mycompany.tugas_akhir;
<<<<<<< HEAD
// Obsolete legacy DAO - Replaced by SupplierRepository & SupplierService
=======

import java.sql.*;
import java.util.ArrayList;
import java.util.List;


public class SupplierDAO {

    
    public static class Supplier {
        public int    id;
        public String namaSupplier;
        public String kontak;
        public String email;

        public Supplier(int id, String namaSupplier, String kontak, String email) {
            this.id           = id;
            this.namaSupplier = namaSupplier;
            this.kontak       = kontak != null ? kontak : "";
            this.email        = email  != null ? email  : "";
        }

        @Override
        public String toString() {
            return namaSupplier;
        }
    }

    
    public List<Supplier> getAllSupplier() {
        List<Supplier> list = new ArrayList<>();
        String sql = "SELECT id, nama_supplier, kontak, email FROM supplier ORDER BY nama_supplier";
        try (Connection conn = DatabaseConnection.getInstance().getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                list.add(new Supplier(
                    rs.getInt("id"),
                    rs.getString("nama_supplier"),
                    rs.getString("kontak"),
                    rs.getString("email")
                ));
            }
        } catch (SQLException e) {
            System.err.println("[SupplierDAO] getAllSupplier: " + e.getMessage());
        }
        return list;
    }
}
>>>>>>> origin/main
