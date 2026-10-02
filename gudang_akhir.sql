-- phpMyAdmin SQL Dump
-- version 5.2.2
-- https://www.phpmyadmin.net/
--
-- Host: localhost:3306
<<<<<<< HEAD
-- Generation Time: Oct 01, 2026 at 04:00 AM
-- Server version: 8.4.11
-- PHP Version: 8.1.10
=======
-- Generation Time: Jun 08, 2026 at 06:00 AM
-- Server version: 8.4.3
-- PHP Version: 8.3.16
>>>>>>> origin/main

SET SQL_MODE = "NO_AUTO_VALUE_ON_ZERO";
START TRANSACTION;
SET time_zone = "+00:00";

<<<<<<< HEAD

=======
>>>>>>> origin/main
/*!40101 SET @OLD_CHARACTER_SET_CLIENT=@@CHARACTER_SET_CLIENT */;
/*!40101 SET @OLD_CHARACTER_SET_RESULTS=@@CHARACTER_SET_RESULTS */;
/*!40101 SET @OLD_COLLATION_CONNECTION=@@COLLATION_CONNECTION */;
/*!40101 SET NAMES utf8mb4 */;

--
-- Database: `gudang_akhir`
--

-- --------------------------------------------------------

--
-- Table structure for table `barang`
--

CREATE TABLE `barang` (
  `id` int NOT NULL,
<<<<<<< HEAD
  `kode_barang` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL,
  `nama_barang` varchar(150) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL,
  `kategori` varchar(80) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `satuan` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT 'pcs',
=======
  `kode_barang` varchar(20) COLLATE utf8mb4_unicode_ci NOT NULL,
  `nama_barang` varchar(150) COLLATE utf8mb4_unicode_ci NOT NULL,
  `kategori` varchar(80) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `satuan` varchar(20) COLLATE utf8mb4_unicode_ci DEFAULT 'pcs',
>>>>>>> origin/main
  `stok` int DEFAULT '0',
  `stok_min` int DEFAULT '0',
  `stok_max` int DEFAULT '0',
  `created_at` timestamp NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at` timestamp NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

--
-- Dumping data for table `barang`
--

INSERT INTO `barang` (`id`, `kode_barang`, `nama_barang`, `kategori`, `satuan`, `stok`, `stok_min`, `stok_max`, `created_at`, `updated_at`) VALUES
<<<<<<< HEAD
(1, 'BRG-001', 'Barang A', 'Umum', 'pcs', 900, 50, 100, '2026-05-21 10:44:41', '2026-07-01 06:15:16'),
(2, 'BRG-002', 'Barang B', 'Umum', 'pcs', 200, 30, 60, '2026-05-21 10:44:41', '2026-07-01 06:15:42'),
(3, 'BRG-003', 'Barang C', 'Umum', 'pcs', 100, 20, 40, '2026-05-21 10:44:41', '2026-06-17 06:09:43'),
(4, 'BRG-004', 'Barang D', 'Umum', 'pcs', 200, 25, 50, '2026-05-21 10:44:41', '2026-05-27 01:32:19'),
(5, 'BRG-005', 'Barang E', 'Umum', 'pcs', 300, 100, 200, '2026-05-21 10:44:41', '2026-07-01 06:16:20'),
(6, 'BRG-006', 'Odol', 'Alat Mandi', 'pcs', 900, 100, 200, '2026-07-01 13:31:10', '2026-07-01 13:32:19'),
(7, 'BRG-007', 'Sendok', 'Alat makan', 'pcs', 100, 10, 20, '2026-07-01 13:44:01', '2026-07-01 13:44:01');

-- --------------------------------------------------------

--
-- Table structure for table `customer`
--

CREATE TABLE `customer` (
  `id` int NOT NULL,
  `nama_customer` varchar(150) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL,
  `posisi` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `alamat` text CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci,
  `kontak` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `email` varchar(150) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `status` enum('Aktif','Nonaktif') CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT 'Aktif',
  `created_at` timestamp NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at` timestamp NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

--
-- Dumping data for table `customer`
--

INSERT INTO `customer` (`id`, `nama_customer`, `posisi`, `alamat`, `kontak`, `email`, `status`, `created_at`, `updated_at`) VALUES
(1, 'PT. Bangun Bersama', 'Tatang Sederhana', 'Jonggol', '+62 812345678', 'bangun@example.com', 'Aktif', '2026-06-17 05:50:09', '2026-06-17 05:50:09'),
(2, 'PT. Maju Jaya', 'Direktur Operasional', 'Jakarta Selatan', '+62 821234567', 'majujaya@example.com', 'Aktif', '2026-06-17 05:50:09', '2026-06-17 05:50:09'),
(3, 'PT. Sukses Mandiri', 'Manager Pembelian', 'Bandung', '+62 822234567', 'sukses@example.com', 'Aktif', '2026-06-17 05:50:09', '2026-06-17 05:50:09'),
(4, 'CV. Niaga Indo', 'Kepala Bagian', 'Surabaya', '+62 823234567', 'niaga@example.com', 'Nonaktif', '2026-06-17 05:50:09', '2026-06-17 05:50:09'),
(5, 'PT. Sejahtera Utama', 'Sales Manager', 'Medan', '+62 824234567', 'sejahtera@example.com', 'Aktif', '2026-06-17 05:50:09', '2026-06-17 05:50:09'),
(6, 'PT. Global Bisnis', 'Direktur Keuangan', 'Makassar', '+62 825234567', 'global@example.com', 'Nonaktif', '2026-06-17 05:50:09', '2026-07-01 13:17:51'),
(7, 'PT. Mitra Produksi', 'Supervisor', 'Batam', '+62 826234567', 'mitra@example.com', 'Aktif', '2026-06-17 05:50:09', '2026-06-17 05:50:09'),
(8, 'CV. Dagang Ekspor', 'General Manager', 'Semarang', '+62 827234567', 'dagang@example.com', 'Nonaktif', '2026-06-17 05:50:09', '2026-06-17 05:50:09'),
(9, 'PT. Teknologi Maju', 'IT Manager', 'Yogyakarta', '+62 828234567', 'teknologi@example.com', 'Aktif', '2026-06-17 05:50:09', '2026-06-17 05:50:09'),
(10, 'PT Cahaya', 'Manager', 'Jakarta barat', '1661', 'Perusahaan@gmail.com', 'Aktif', '2026-07-01 13:46:07', '2026-07-01 13:46:07');
=======
(1, 'BRG-001', 'Barang A', 'Umum', 'pcs', 500, 50, 100, '2026-05-21 10:44:41', '2026-05-27 01:32:19'),
(2, 'BRG-002', 'Barang B', 'Umum', 'pcs', 300, 30, 60, '2026-05-21 10:44:41', '2026-05-27 01:32:19'),
(3, 'BRG-003', 'Barang C', 'Umum', 'pcs', 150, 20, 40, '2026-05-21 10:44:41', '2026-05-27 01:32:19'),
(4, 'BRG-004', 'Barang D', 'Umum', 'pcs', 200, 25, 50, '2026-05-21 10:44:41', '2026-05-27 01:32:19'),
(5, 'BRG-005', 'Barang E', 'Umum', 'pcs', 769, 100, 200, '2026-05-21 10:44:41', '2026-05-27 01:32:19');
>>>>>>> origin/main

-- --------------------------------------------------------

--
-- Table structure for table `rak`
--

CREATE TABLE `rak` (
  `id` int NOT NULL,
  `section_id` int NOT NULL,
<<<<<<< HEAD
  `kode_rak` varchar(10) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL,
=======
  `kode_rak` varchar(10) COLLATE utf8mb4_unicode_ci NOT NULL,
>>>>>>> origin/main
  `kapasitas` int DEFAULT '100',
  `terisi` int DEFAULT '0',
  `created_at` timestamp NULL DEFAULT CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

--
-- Dumping data for table `rak`
--

INSERT INTO `rak` (`id`, `section_id`, `kode_rak`, `kapasitas`, `terisi`, `created_at`) VALUES
(1, 1, 'A-01', 100, 95, '2026-05-21 10:44:41'),
(2, 1, 'A-02', 100, 40, '2026-05-21 10:44:41'),
(3, 1, 'A-03', 100, 20, '2026-05-21 10:44:41'),
(4, 1, 'A-04', 100, 10, '2026-05-21 10:44:41'),
(5, 1, 'A-05', 100, 0, '2026-05-21 10:44:41'),
(6, 1, 'A-06', 100, 0, '2026-05-21 10:44:41'),
(7, 1, 'A-07', 100, 0, '2026-05-21 10:44:41'),
(8, 1, 'A-08', 100, 0, '2026-05-21 10:44:41'),
(9, 1, 'A-09', 100, 0, '2026-05-21 10:44:41'),
(10, 1, 'A-10', 100, 0, '2026-05-21 10:44:41'),
(11, 1, 'A-11', 100, 0, '2026-05-21 10:44:41'),
(12, 1, 'A-12', 100, 0, '2026-05-21 10:44:41'),
(13, 2, 'B-01', 100, 100, '2026-05-21 10:44:41'),
(14, 2, 'B-02', 100, 80, '2026-05-21 10:44:41'),
(15, 2, 'B-03', 100, 60, '2026-05-21 10:44:41'),
(16, 2, 'B-04', 100, 50, '2026-05-21 10:44:41'),
(17, 2, 'B-05', 100, 0, '2026-05-21 10:44:41'),
(18, 2, 'B-06', 100, 0, '2026-05-21 10:44:41'),
(19, 2, 'B-07', 100, 0, '2026-05-21 10:44:41'),
(20, 2, 'B-08', 100, 0, '2026-05-21 10:44:41'),
(21, 2, 'B-09', 100, 0, '2026-05-21 10:44:41'),
(22, 2, 'B-10', 100, 0, '2026-05-21 10:44:41'),
(23, 2, 'B-11', 100, 0, '2026-05-21 10:44:41'),
(24, 2, 'B-12', 100, 0, '2026-05-21 10:44:41'),
(25, 3, 'C-01', 100, 45, '2026-05-21 10:44:41'),
(26, 3, 'C-02', 100, 100, '2026-05-21 10:44:41'),
(27, 3, 'C-03', 100, 100, '2026-05-21 10:44:41'),
(28, 3, 'C-04', 100, 100, '2026-05-21 10:44:41'),
(29, 3, 'C-05', 100, 100, '2026-05-21 10:44:41'),
(30, 3, 'C-06', 100, 100, '2026-05-21 10:44:41'),
(31, 3, 'C-07', 100, 100, '2026-05-21 10:44:41'),
(32, 3, 'C-08', 100, 100, '2026-05-21 10:44:41'),
(33, 3, 'C-09', 100, 100, '2026-05-21 10:44:41'),
(34, 3, 'C-10', 100, 100, '2026-05-21 10:44:41'),
(35, 3, 'C-11', 100, 100, '2026-05-21 10:44:41'),
(36, 3, 'C-12', 100, 100, '2026-05-21 10:44:41');

-- --------------------------------------------------------

--
-- Table structure for table `section`
--

CREATE TABLE `section` (
  `id` int NOT NULL,
<<<<<<< HEAD
  `kode_section` varchar(10) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL,
  `nama_section` varchar(80) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL,
=======
  `kode_section` varchar(10) COLLATE utf8mb4_unicode_ci NOT NULL,
  `nama_section` varchar(80) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
>>>>>>> origin/main
  `kapasitas` int DEFAULT '12',
  `created_at` timestamp NULL DEFAULT CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

--
-- Dumping data for table `section`
--

INSERT INTO `section` (`id`, `kode_section`, `nama_section`, `kapasitas`, `created_at`) VALUES
(1, 'A', 'Section A', 12, '2026-05-21 10:44:41'),
(2, 'B', 'Section B', 12, '2026-05-21 10:44:41'),
(3, 'C', 'Section C', 12, '2026-05-21 10:44:41');

-- --------------------------------------------------------

--
<<<<<<< HEAD
=======
-- Table structure for table `customer`
--

CREATE TABLE `customer` (
  `id` int NOT NULL,
  `nama_customer` varchar(150) COLLATE utf8mb4_unicode_ci NOT NULL,
  `posisi` varchar(100) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `alamat` text COLLATE utf8mb4_unicode_ci,
  `kontak` varchar(20) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `email` varchar(150) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `status` enum('Aktif','Nonaktif') COLLATE utf8mb4_unicode_ci DEFAULT 'Aktif',
  `created_at` timestamp NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at` timestamp NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

--
-- Dumping data for table `customer`
--

INSERT INTO `customer` (`id`, `nama_customer`, `posisi`, `alamat`, `kontak`, `email`, `status`) VALUES
(1, 'PT. Bangun Bersama', 'Tatang Sederhana', 'Jonggol', '+62 812345678', 'bangun@example.com', 'Aktif'),
(2, 'PT. Maju Jaya', 'Direktur Operasional', 'Jakarta Selatan', '+62 821234567', 'majujaya@example.com', 'Aktif'),
(3, 'PT. Sukses Mandiri', 'Manager Pembelian', 'Bandung', '+62 822234567', 'sukses@example.com', 'Aktif'),
(4, 'CV. Niaga Indo', 'Kepala Bagian', 'Surabaya', '+62 823234567', 'niaga@example.com', 'Nonaktif'),
(5, 'PT. Sejahtera Utama', 'Sales Manager', 'Medan', '+62 824234567', 'sejahtera@example.com', 'Aktif'),
(6, 'PT. Global Bisnis', 'Direktur Keuangan', 'Makassar', '+62 825234567', 'global@example.com', 'Aktif'),
(7, 'PT. Mitra Produksi', 'Supervisor', 'Batam', '+62 826234567', 'mitra@example.com', 'Aktif'),
(8, 'CV. Dagang Ekspor', 'General Manager', 'Semarang', '+62 827234567', 'dagang@example.com', 'Nonaktif'),
(9, 'PT. Teknologi Maju', 'IT Manager', 'Yogyakarta', '+62 828234567', 'teknologi@example.com', 'Aktif');

-- --------------------------------------------------------

--
>>>>>>> origin/main
-- Table structure for table `supplier`
--

CREATE TABLE `supplier` (
  `id` int NOT NULL,
<<<<<<< HEAD
  `nama_supplier` varchar(150) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL,
  `kontak` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `email` varchar(150) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `alamat` text CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci,
=======
  `nama_supplier` varchar(150) COLLATE utf8mb4_unicode_ci NOT NULL,
  `kontak` varchar(20) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `email` varchar(150) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `alamat` text COLLATE utf8mb4_unicode_ci,
>>>>>>> origin/main
  `created_at` timestamp NULL DEFAULT CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

--
-- Dumping data for table `supplier`
--

INSERT INTO `supplier` (`id`, `nama_supplier`, `kontak`, `email`, `alamat`, `created_at`) VALUES
(1, 'PT. Satu', '08111111111', 'satu@example.com', NULL, '2026-05-21 10:44:41'),
(2, 'PT. Dua', '08222222222', 'dua@example.com', NULL, '2026-05-21 10:44:41'),
(3, 'PT. Tiga', '08333333333', 'tiga@example.com', NULL, '2026-05-21 10:44:41'),
<<<<<<< HEAD
(4, 'PT. Empat', '08444444444', 'empat@example.com', NULL, '2026-05-21 10:44:41');
=======
(4, 'PT. Empat', '08444444444', 'empat@example.com', NULL, '2026-05-21 10:44:41'),
(5, 'PT. Satu', '08111111111', 'satu@example.com', NULL, '2026-05-27 01:32:04'),
(6, 'PT. Dua', '08222222222', 'dua@example.com', NULL, '2026-05-27 01:32:04'),
(7, 'PT. Tiga', '08333333333', 'tiga@example.com', NULL, '2026-05-27 01:32:04'),
(8, 'PT. Empat', '08444444444', 'empat@example.com', NULL, '2026-05-27 01:32:04');
>>>>>>> origin/main

-- --------------------------------------------------------

--
-- Table structure for table `transaksi`
--

CREATE TABLE `transaksi` (
  `id` int NOT NULL,
<<<<<<< HEAD
  `kode_transaksi` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL,
  `tipe` varchar(10) COLLATE utf8mb4_unicode_ci NOT NULL,
=======
  `kode_transaksi` varchar(20) COLLATE utf8mb4_unicode_ci NOT NULL,
  `tipe` enum('masuk','keluar') COLLATE utf8mb4_unicode_ci NOT NULL,
>>>>>>> origin/main
  `barang_id` int NOT NULL,
  `supplier_id` int DEFAULT NULL,
  `rak_id` int DEFAULT NULL,
  `jumlah` int NOT NULL,
<<<<<<< HEAD
  `keterangan` text CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci,
=======
  `keterangan` text COLLATE utf8mb4_unicode_ci,
>>>>>>> origin/main
  `user_id` int NOT NULL,
  `tanggal` timestamp NULL DEFAULT CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

<<<<<<< HEAD
--
-- Dumping data for table `transaksi`
--

INSERT INTO `transaksi` (`id`, `kode_transaksi`, `tipe`, `barang_id`, `supplier_id`, `rak_id`, `jumlah`, `keterangan`, `user_id`, `tanggal`) VALUES
(6, 'TRX-00001', 'masuk', 1, 2, NULL, 300, 'Kekurangan barang di gudang', 2, '2026-07-01 06:15:16'),
(7, 'TRX-00002', 'keluar', 2, NULL, NULL, 100, 'Jakarta - Barang keluar (Bekasi)', 2, '2026-07-01 06:15:42'),
(8, 'TRX-00003', 'keluar', 5, NULL, NULL, 400, 'Penyesuaian Sistem - Kesalahan input', 2, '2026-07-01 06:16:20'),
(9, 'TRX-00004', 'masuk', 6, 4, NULL, 400, 'Barang masuk Gudang B', 2, '2026-07-01 13:31:42'),
(10, 'TRX-00005', 'keluar', 6, NULL, NULL, 100, 'cacat sistem - Wleeeee', 2, '2026-07-01 13:32:19');

=======
>>>>>>> origin/main
-- --------------------------------------------------------

--
-- Table structure for table `users`
--

CREATE TABLE `users` (
  `id` int NOT NULL,
<<<<<<< HEAD
  `full_name` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL,
  `email` varchar(150) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL,
  `password_hash` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL,
  `role` varchar(20) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `created_at` timestamp NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at` timestamp NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `display_name` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `phone` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `bio` text CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci,
  `profile_picture_path` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL
=======
  `full_name` varchar(100) COLLATE utf8mb4_unicode_ci NOT NULL,
  `email` varchar(150) COLLATE utf8mb4_unicode_ci NOT NULL,
  `password_hash` varchar(255) COLLATE utf8mb4_unicode_ci NOT NULL,
  `role` enum('admin','user') COLLATE utf8mb4_unicode_ci DEFAULT 'user',
  `created_at` timestamp NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at` timestamp NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
>>>>>>> origin/main
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

--
-- Dumping data for table `users`
--

<<<<<<< HEAD
INSERT INTO `users` (`id`, `full_name`, `email`, `password_hash`, `role`, `created_at`, `updated_at`, `display_name`, `phone`, `bio`, `profile_picture_path`) VALUES
(1, 'Test User', 'testuser@example.com', 'Testing123', 'user', '2026-05-21 10:44:42', '2026-05-21 10:44:42', NULL, NULL, NULL, NULL),
(2, 'Alza', 'alzabilly123@gmail.com', 'Admin123', 'user', '2026-05-25 04:58:23', '2026-07-01 13:47:55', 'Delores O\'Riordan', NULL, NULL, 'C:\\Users\\Axioo\\.gudangku\\avatars\\user_2.png');
=======
INSERT INTO `users` (`id`, `full_name`, `email`, `password_hash`, `role`, `created_at`, `updated_at`) VALUES
(1, 'Test User', 'testuser@example.com', 'Testing123', 'user', '2026-05-21 10:44:42', '2026-05-21 10:44:42'),
(2, 'Alza', 'alzabilly123@gmail.com', 'Admin123', 'user', '2026-05-25 04:58:23', '2026-05-25 04:58:23');
>>>>>>> origin/main

--
-- Indexes for dumped tables
--

--
-- Indexes for table `barang`
--
ALTER TABLE `barang`
  ADD PRIMARY KEY (`id`),
  ADD UNIQUE KEY `kode_barang` (`kode_barang`),
  ADD KEY `idx_barang_stok` (`stok`,`stok_min`);

--
<<<<<<< HEAD
-- Indexes for table `customer`
--
ALTER TABLE `customer`
  ADD PRIMARY KEY (`id`),
  ADD KEY `idx_customer_status` (`status`),
  ADD KEY `idx_customer_nama` (`nama_customer`);

--
=======
>>>>>>> origin/main
-- Indexes for table `rak`
--
ALTER TABLE `rak`
  ADD PRIMARY KEY (`id`),
  ADD UNIQUE KEY `kode_rak` (`kode_rak`),
  ADD KEY `section_id` (`section_id`);

--
-- Indexes for table `section`
--
ALTER TABLE `section`
  ADD PRIMARY KEY (`id`),
  ADD UNIQUE KEY `kode_section` (`kode_section`);

--
<<<<<<< HEAD
=======
-- Indexes for table `customer`
--
ALTER TABLE `customer`
  ADD PRIMARY KEY (`id`),
  ADD KEY `idx_customer_status` (`status`),
  ADD KEY `idx_customer_nama` (`nama_customer`);

--
>>>>>>> origin/main
-- Indexes for table `supplier`
--
ALTER TABLE `supplier`
  ADD PRIMARY KEY (`id`);

--
-- Indexes for table `transaksi`
--
ALTER TABLE `transaksi`
  ADD PRIMARY KEY (`id`),
  ADD UNIQUE KEY `kode_transaksi` (`kode_transaksi`),
  ADD KEY `barang_id` (`barang_id`),
  ADD KEY `supplier_id` (`supplier_id`),
  ADD KEY `rak_id` (`rak_id`),
  ADD KEY `user_id` (`user_id`);

--
-- Indexes for table `users`
--
ALTER TABLE `users`
  ADD PRIMARY KEY (`id`),
  ADD UNIQUE KEY `email` (`email`);

--
-- AUTO_INCREMENT for dumped tables
--

--
-- AUTO_INCREMENT for table `barang`
--
ALTER TABLE `barang`
<<<<<<< HEAD
  MODIFY `id` int NOT NULL AUTO_INCREMENT, AUTO_INCREMENT=8;

--
-- AUTO_INCREMENT for table `customer`
--
ALTER TABLE `customer`
  MODIFY `id` int NOT NULL AUTO_INCREMENT, AUTO_INCREMENT=11;
=======
  MODIFY `id` int NOT NULL AUTO_INCREMENT, AUTO_INCREMENT=6;
>>>>>>> origin/main

--
-- AUTO_INCREMENT for table `rak`
--
ALTER TABLE `rak`
  MODIFY `id` int NOT NULL AUTO_INCREMENT, AUTO_INCREMENT=62;

--
-- AUTO_INCREMENT for table `section`
--
ALTER TABLE `section`
  MODIFY `id` int NOT NULL AUTO_INCREMENT, AUTO_INCREMENT=5;

--
<<<<<<< HEAD
=======
-- AUTO_INCREMENT for table `customer`
--
ALTER TABLE `customer`
  MODIFY `id` int NOT NULL AUTO_INCREMENT, AUTO_INCREMENT=10;

--
>>>>>>> origin/main
-- AUTO_INCREMENT for table `supplier`
--
ALTER TABLE `supplier`
  MODIFY `id` int NOT NULL AUTO_INCREMENT, AUTO_INCREMENT=9;

--
-- AUTO_INCREMENT for table `transaksi`
--
ALTER TABLE `transaksi`
<<<<<<< HEAD
  MODIFY `id` int NOT NULL AUTO_INCREMENT, AUTO_INCREMENT=11;
=======
  MODIFY `id` int NOT NULL AUTO_INCREMENT;
>>>>>>> origin/main

--
-- AUTO_INCREMENT for table `users`
--
ALTER TABLE `users`
  MODIFY `id` int NOT NULL AUTO_INCREMENT, AUTO_INCREMENT=4;

--
-- Constraints for dumped tables
--

--
-- Constraints for table `rak`
--
ALTER TABLE `rak`
  ADD CONSTRAINT `rak_ibfk_1` FOREIGN KEY (`section_id`) REFERENCES `section` (`id`) ON DELETE CASCADE;

--
-- Constraints for table `transaksi`
--
ALTER TABLE `transaksi`
  ADD CONSTRAINT `transaksi_ibfk_1` FOREIGN KEY (`barang_id`) REFERENCES `barang` (`id`),
  ADD CONSTRAINT `transaksi_ibfk_2` FOREIGN KEY (`supplier_id`) REFERENCES `supplier` (`id`),
  ADD CONSTRAINT `transaksi_ibfk_3` FOREIGN KEY (`rak_id`) REFERENCES `rak` (`id`),
  ADD CONSTRAINT `transaksi_ibfk_4` FOREIGN KEY (`user_id`) REFERENCES `users` (`id`);
<<<<<<< HEAD
=======

>>>>>>> origin/main
COMMIT;

/*!40101 SET CHARACTER_SET_CLIENT=@OLD_CHARACTER_SET_CLIENT */;
/*!40101 SET CHARACTER_SET_RESULTS=@OLD_CHARACTER_SET_RESULTS */;
/*!40101 SET COLLATION_CONNECTION=@OLD_COLLATION_CONNECTION */;
