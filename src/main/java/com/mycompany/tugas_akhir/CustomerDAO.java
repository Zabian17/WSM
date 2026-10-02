package com.mycompany.tugas_akhir;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;


public class CustomerDAO {

    public static class Customer {
        public int    id;
        public String namaCustomer;
        public String posisi;
        public String alamat;
        public String kontak;
        public String email;
        public String status;

        public Customer(int id, String namaCustomer, String posisi, String alamat, 
                       String kontak, String email, String status) {
            this.id            = id;
            this.namaCustomer  = namaCustomer != null ? namaCustomer : "";
            this.posisi        = posisi != null ? posisi : "";
            this.alamat        = alamat != null ? alamat : "";
            this.kontak        = kontak != null ? kontak : "";
            this.email         = email != null ? email : "";
            this.status        = status != null ? status : "Aktif";
        }

        @Override
        public String toString() {
            return namaCustomer;
        }
    }

    public List<Customer> getAllCustomer() {
        List<Customer> list = new ArrayList<>();
        String sql = "SELECT id, nama_customer, posisi, alamat, kontak, email, status FROM customer ORDER BY nama_customer";
        try (Connection conn = DatabaseConnection.getInstance().getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                list.add(new Customer(
                    rs.getInt("id"),
                    rs.getString("nama_customer"),
                    rs.getString("posisi"),
                    rs.getString("alamat"),
                    rs.getString("kontak"),
                    rs.getString("email"),
                    rs.getString("status")
                ));
            }
        } catch (SQLException e) {
            System.err.println("[CustomerDAO] getAllCustomer: " + e.getMessage());
        }
        return list;
    }

    public List<Customer> searchCustomer(String keyword) {
        List<Customer> list = new ArrayList<>();
        String sql = "SELECT id, nama_customer, posisi, alamat, kontak, email, status FROM customer " +
                     "WHERE nama_customer LIKE ? OR posisi LIKE ? OR alamat LIKE ? OR kontak LIKE ? " +
                     "ORDER BY nama_customer";
        try (Connection conn = DatabaseConnection.getInstance().getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            String searchTerm = "%" + keyword + "%";
            ps.setString(1, searchTerm);
            ps.setString(2, searchTerm);
            ps.setString(3, searchTerm);
            ps.setString(4, searchTerm);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                list.add(new Customer(
                    rs.getInt("id"),
                    rs.getString("nama_customer"),
                    rs.getString("posisi"),
                    rs.getString("alamat"),
                    rs.getString("kontak"),
                    rs.getString("email"),
                    rs.getString("status")
                ));
            }
        } catch (SQLException e) {
            System.err.println("[CustomerDAO] searchCustomer: " + e.getMessage());
        }
        return list;
    }

    public List<Customer> getCustomerByStatus(String status) {
        List<Customer> list = new ArrayList<>();
        String sql = "SELECT id, nama_customer, posisi, alamat, kontak, email, status FROM customer " +
                     "WHERE status = ? ORDER BY nama_customer";
        try (Connection conn = DatabaseConnection.getInstance().getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, status);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                list.add(new Customer(
                    rs.getInt("id"),
                    rs.getString("nama_customer"),
                    rs.getString("posisi"),
                    rs.getString("alamat"),
                    rs.getString("kontak"),
                    rs.getString("email"),
                    rs.getString("status")
                ));
            }
        } catch (SQLException e) {
            System.err.println("[CustomerDAO] getCustomerByStatus: " + e.getMessage());
        }
        return list;
    }

    public boolean addCustomer(String namaCustomer, String posisi, String alamat, 
                              String kontak, String email) {
        String sql = "INSERT INTO customer (nama_customer, posisi, alamat, kontak, email, status) " +
                    "VALUES (?, ?, ?, ?, ?, 'Aktif')";
        try (Connection conn = DatabaseConnection.getInstance().getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, namaCustomer);
            ps.setString(2, posisi);
            ps.setString(3, alamat);
            ps.setString(4, kontak);
            ps.setString(5, email);
            ps.executeUpdate();
            return true;
        } catch (SQLException e) {
            System.err.println("[CustomerDAO] addCustomer: " + e.getMessage());
            return false;
        }
    }

    public boolean updateCustomer(int id, String namaCustomer, String posisi, String alamat, 
                                 String kontak, String email, String status) {
        String sql = "UPDATE customer SET nama_customer = ?, posisi = ?, alamat = ?, kontak = ?, email = ?, status = ? WHERE id = ?";
        try (Connection conn = DatabaseConnection.getInstance().getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, namaCustomer);
            ps.setString(2, posisi);
            ps.setString(3, alamat);
            ps.setString(4, kontak);
            ps.setString(5, email);
            ps.setString(6, status);
            ps.setInt(7, id);
            ps.executeUpdate();
            return true;
        } catch (SQLException e) {
            System.err.println("[CustomerDAO] updateCustomer: " + e.getMessage());
            return false;
        }
    }

    public boolean deleteCustomer(int id) {
        String sql = "DELETE FROM customer WHERE id = ?";
        try (Connection conn = DatabaseConnection.getInstance().getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, id);
            ps.executeUpdate();
            return true;
        } catch (SQLException e) {
            System.err.println("[CustomerDAO] deleteCustomer: " + e.getMessage());
            return false;
        }
    }
}
