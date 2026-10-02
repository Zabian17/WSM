package com.mycompany.tugas_akhir;
<<<<<<< HEAD
// Obsolete legacy Database Connection - Replaced by Spring Boot DataSource & HikariCP
=======

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;


public class DatabaseConnection {

    private static final String HOST     = "localhost";
    private static final String PORT     = "3306";
    private static final String DATABASE = "gudang_akhir";
    private static final String USER     = "root";
    private static final String PASSWORD = "12345";

    private static final String URL =
            "jdbc:mysql://" + HOST + ":" + PORT + "/" + DATABASE
            + "?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=Asia/Jakarta";

    private static DatabaseConnection instance;
    private Connection connection;

    private DatabaseConnection() {
        connect();
    }


    public static DatabaseConnection getInstance() {
        if (instance == null) {
            instance = new DatabaseConnection();
        }
        return instance;
    }


    private void connect() {
        try {
            Class.forName("com.mysql.cj.jdbc.Driver");
            connection = DriverManager.getConnection(URL, USER, PASSWORD);
            System.out.println("[DB] ✓ Koneksi berhasil ke database: " + DATABASE);
        } catch (ClassNotFoundException e) {
            System.err.println("[DB] ✗ Driver MySQL tidak ditemukan: " + e.getMessage());
            connection = null;
        } catch (SQLException e) {
            System.err.println("[DB] ✗ Gagal konek ke MySQL: " + e.getMessage());
            System.err.println("[DB] Pastikan MySQL server berjalan di " + HOST + ":" + PORT);
            connection = null;
        }
    }


    public Connection getConnection() {
        try {
            if (connection == null || connection.isClosed()) {
                System.out.println("[DB] Reconnecting...");
                connect();
            }
        } catch (SQLException e) {
            System.err.println("[DB] Gagal cek status koneksi: " + e.getMessage());
        }
        return connection;
    }


    public void closeConnection() {
        try {
            if (connection != null && !connection.isClosed()) {
                connection.close();
                System.out.println("[DB] Koneksi ditutup.");
            }
        } catch (SQLException e) {
            System.err.println("[DB] Gagal menutup koneksi: " + e.getMessage());
        }
    }


    public boolean isConnected() {
        try {
            return connection != null && !connection.isClosed();
        } catch (SQLException e) {
            return false;
        }
    }
}
>>>>>>> origin/main
