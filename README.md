# GudangKu

GudangKu adalah sistem manajemen gudang berbasis desktop untuk mengelola persediaan, penerimaan, pengeluaran, perpindahan barang, dan pengendalian stok. Antarmuka desktop menggunakan JavaFX. Spring Boot dan Spring Data JPA menyediakan layanan aplikasi dan akses data dengan MySQL.

Aplikasi menyediakan klien desktop JavaFX dan klien web untuk perangkat seluler. Fitur gudang seluler mencakup penghitungan stok, putaway, pemenuhan DO, pemetikan MRO, retur yang disetujui, dan pemetikan STO. Integrasi Accurate telah menyediakan autentikasi dan transport API; sinkronisasi dokumen PO/SO belum tersedia.

## Fitur

### Persediaan dan operasional dasar

- Pendaftaran dan autentikasi pengguna dengan hashing BCrypt. Saat startup, aplikasi memigrasikan kata sandi plaintext lama; saat login, aplikasi memigrasikan hash SHA-256 bersalt.
- Admin dapat mereset kata sandi pengguna melalui Settings. Kata sandi baru harus memiliki sedikitnya 12 karakter dan panjang maksimal 72 byte UTF-8.
- Pengelolaan barang, pelanggan, pemasok, gudang, section, dan rak.
- Saldo persediaan berdasarkan lokasi penyimpanan (SLOC), batch, dan tanggal kedaluwarsa.
- Transaksi barang masuk dan keluar dicatat pada ledger persediaan.
- Transfer antar-SLOC menyimpan riwayat alokasi lot dan dapat memvalidasi QR barang serta barcode SLOC asal dan tujuan.
- Strategi alokasi lot FIFO, FEFO, atau LEFO dapat digunakan bersama untuk pengeluaran stok, reservasi SO/DO, Goods Issue MRO, dan pengiriman STO. FEFO menjadi pilihan bawaan.
- Peta gudang skematis, filter persediaan, dan peringatan stok.
- Penghitungan siklus harian dan berkala dapat dibagi ke beberapa petugas. Jadwal bulanan menggunakan hari kerja Senin–Jumat dan menyimpan snapshot stok saat penghitungan dimulai.
- Penghitungan stok per rak mencatat stok buku, hasil hitung fisik, petugas, tinjauan selisih, dan posting selisih ke ledger beserta alokasi lot. Sistem juga menyediakan rekomendasi prioritas hitung.
- Impor Excel `.xlsx` dan templat untuk data barang, pelanggan, dan pemasok. Jika ada baris tidak valid, seluruh impor dibatalkan. Stok barang hasil impor dimulai dari nol.

### Penerimaan dan pembelian

- Purchase Order (PO) dengan persetujuan maker-checker, mata uang, harga per baris, referensi tender, syarat pembayaran, tanggal pengiriman yang diharapkan, dan keterkaitan ke penerimaan barang.
- Penerimaan, pemeriksaan kualitas (QC), dan proses putaway.
- Rekomendasi putaway berdasarkan kategori section, jenis penyimpanan, dan kapasitas rak.
- Urutan oldest-first untuk antrean QC dapat dikonfigurasi. Alokasi lot pengeluaran mendukung FIFO, FEFO, atau LEFO.
- Data volume CBM barang dan kapasitas CBM rak mendukung pemeringkatan lokasi putaway.
- Pemindaian barcode pada desktop memvalidasi QR barang dan SLOC tujuan, termasuk SKU, batch, dan tanggal kedaluwarsa.
- Pembuatan label QR untuk barang pada receipt dan lokasi gudang.

### Pengeluaran dan pemenuhan pesanan

- Sales Order (SO) dapat dikonversi menjadi Delivery Order (DO) dengan referensi dokumen sumber dan sisa kuantitas.
- Pelepasan DO membuat reservasi lot berdasarkan strategi alokasi, tugas picking, serta penugasan picker dan section.
- Admin dapat mengecualikan zona tertentu dari reservasi DO dan transfer stok, atau membatasi zona yang tersedia untuk picking Stock Transfer dan Purchase Return.
- Daftar packing DO dapat dicetak dengan barcode Code 128 untuk DO dan label QR untuk lot yang sudah dikemas.
- Pemindaian picking desktop memvalidasi QR barang, SLOC, batch, tanggal kedaluwarsa, picker, dan section.
- Pemeriksaan jumlah saat packing dan loading mencatat operator dan waktu.
- Dispatch dan Goods Issue memperbarui ledger persediaan serta mendukung pengiriman parsial.
- Mobile DO Fulfillment memvalidasi pemindaian saat packing, loading, dan dispatch.

### Material Usage, retur, dan transfer antargudang

- Permintaan Material Usage/MRO menggunakan persetujuan maker-checker dan alokasi lot. Permintaan dapat memuat beberapa barang dan diproses bertahap melalui Goods Issue.
- Tugas picking MRO mencatat pemindaian QR barang, SLOC asal, jumlah, dan riwayat pemindaian. Stok tidak direservasi saat tugas dibuka; ketersediaan diperiksa kembali saat Goods Issue.
- Purchase Return terhubung ke baris PO yang diterima. Sales Return terhubung ke baris DO yang dikirim.
- Proses retur mencatat persetujuan, alasan, validasi kuantitas sumber, dan posting ke ledger.
- Mobile Return hanya memproses retur yang telah disetujui. Pemindaian memvalidasi barang dan lokasi sebelum posting.
- Label QR sales return yang sudah diposting dapat menggunakan QR barang yang sesuai atau membuat dan mencetak ulang label pengganti.
- Data gudang dan pemetaan section ke gudang mendukung Stock Transfer Order (STO).
- STO mendukung persetujuan, penugasan picker, validasi pemindaian barang dan SLOC, pengiriman dan penerimaan parsial, riwayat pemindaian, serta posting ledger yang mempertahankan batch dan tanggal kedaluwarsa.
- Mobile STO mendukung pemetikan oleh picker yang ditugaskan, pemindaian barang dan lokasi sumber, serta Goods Issue.

### Klien web seluler

- API seluler memiliki autentikasi, token sesi, pembatasan laju login, validasi jaringan, dan respons berbentuk halaman.
- Alur seluler mencakup putaway, picking/pemenuhan DO, penghitungan siklus, picking MRO, posting retur yang disetujui, dan picking STO.
- Pemindaian kamera memerlukan HTTPS (atau localhost) dan dukungan `BarcodeDetector` pada browser. Pemindai keyboard-wedge dan entri manual tersedia sebagai alternatif.
- Token akses hanya disimpan selama tab browser aktif dan tidak disimpan oleh browser.

### Dasbor dan laporan

- Dasbor menyajikan metrik fulfillment, produktivitas picker, waktu penyelesaian QC pemasok, packing, dan loading.
- Admin dapat mengatur target fulfillment, packing, loading, waktu pemasok, serta keluaran picker.
- Periode dasbor dapat dipilih: 7, 30, atau 90 hari; bulan berjalan; atau rentang tanggal khusus.
- Saran persediaan memakai rata-rata permintaan bulanan dari jumlah bulan kalender selesai yang dapat dikonfigurasi serta mempertimbangkan stok buffer, stok maksimum, dan jumlah yang sudah direservasi.
- Laporan mencakup transaksi dan aktivitas operasional seperti QC/penerimaan, fulfillment, STO, pemindaian picker, waktu proses pemasok, serta audit packing/loading.
- Filter laporan menggunakan rentang tanggal inklusif dan kategori. Ekspor transaksi dan laporan operasional tersedia dalam CSV; laporan transaksi juga mendukung PDF.

### Integrasi ERP

- Kontrak ERP netral dan kunci penyedia SAP, Oracle, serta Accurate telah tersedia.
- Klien Accurate mendukung OAuth untuk daftar dan pembukaan database serta API Token dengan permintaan bertanda tangan HMAC.
- Transport Accurate mendukung GET/POST, pengalihan 308 yang dibatasi, validasi ukuran permintaan dan respons, serta batas 8 permintaan per detik dan 8 permintaan bersamaan.
- Host database yang dikembalikan Accurate harus menggunakan HTTPS pada domain Accurate yang diizinkan.
- Adapter penyedia ERP untuk sinkronisasi PO/SO dan pengiriman event gudang belum tersedia. Implementasinya menunggu dokumentasi endpoint, kredensial, dan pemetaan field dari vendor.

## Teknologi

| Komponen | Teknologi |
| --- | --- |
| Bahasa dan runtime | Java 25 |
| Antarmuka desktop | JavaFX 25, FXML, AtlantaFX Primer Light |
| Kerangka aplikasi | Spring Boot 3.5.16 |
| Persistensi | Spring Data JPA, Hibernate |
| Basis data | Server yang kompatibel dengan MySQL 8 dan MySQL Connector/J |
| Build | Maven Wrapper |
| Hashing kata sandi | Spring Security Crypto (BCrypt) |
| QR dan barcode | ZXing 3.5.3 |
| Impor Excel | Apache POI 5.4.1 |
| Pelaporan | JasperReports 6.21.3 |
| Pengujian otomatis | JUnit 5, Mockito, Spring Boot Test |

## Persyaratan

- JDK 25 atau lebih baru. Proyek menggunakan konfigurasi bahasa dan API Java 25.
- Server MySQL yang dapat diakses dari komputer pengembangan.
- Akses jaringan pada build pertama agar Maven dapat mengunduh dependensi.
- Git jika proyek dikloning dari repositori.

## Konfigurasi basis data

Konfigurasi bawaan profil pengembangan:

- Host: `localhost`
- Port: `3306`
- Database: `gudang_akhir`
- Pengguna: `root`

URL JDBC bawaan dapat membuat database jika belum tersedia. Profil `dev` mengaktifkan pembaruan skema Hibernate dan menggunakan akun Laragon lokal `root` tanpa kata sandi. Untuk lingkungan selain pengembangan, gunakan profil non-`dev`; konfigurasi dasar mewajibkan `DB_USER` dan `DB_PASSWORD`, koneksi terenkripsi, serta validasi skema tanpa perubahan otomatis.

Nilai koneksi dapat diatur melalui environment variable:

| Variabel | Nilai bawaan | Fungsi |
| --- | --- | --- |
| `DB_HOST` | `localhost` | Host MySQL |
| `DB_PORT` | `3306` | Port MySQL |
| `DB_NAME` | `gudang_akhir` | Nama database |
| `DB_USER` | `root` pada `dev`; wajib di profil lain | Nama pengguna database |
| `DB_PASSWORD` | Kosong pada `dev`; wajib di profil lain | Kata sandi database |
| `DB_SSL_MODE` | `DISABLED` pada `dev`; `REQUIRED` pada profil lain | Mode TLS MySQL |
| `ITEM_BARCODE_PREFIX` | `WMS1` | Awalan payload QR barang |
| `LOCATION_BARCODE_PREFIX` | `SLOC1` | Awalan payload barcode lokasi |

Contoh konfigurasi melalui PowerShell:

```powershell
$env:DB_HOST = 'localhost'
$env:DB_PORT = '3306'
$env:DB_NAME = 'gudang_akhir'
$env:DB_USER = 'root'
$env:DB_PASSWORD = 'kata-sandi-lokal'
$env:DB_SSL_MODE = 'VERIFY_IDENTITY'
$env:ITEM_BARCODE_PREFIX = 'WMS1'
$env:LOCATION_BARCODE_PREFIX = 'SLOC1'
$env:SPRING_PROFILES_ACTIVE = 'prod'
```

Mode TLS yang didukung: `REQUIRED`, `VERIFY_CA`, `VERIFY_IDENTITY`, dan `DISABLED` hanya untuk pengembangan lokal. Gunakan `VERIFY_IDENTITY` untuk server remote dengan sertifikat tepercaya. Gunakan `REQUIRED` jika enkripsi diperlukan tetapi verifikasi sertifikat belum dikonfigurasi.

Awalan barcode menerima 1–16 huruf, angka, garis bawah, atau tanda hubung. Scanner tetap menerima label lama berawalan `WMS1` dan `SLOC1`; label baru memakai awalan yang dikonfigurasi.

Jangan menyimpan kredensial atau data produksi di repositori. [`database/gudang_akhir.sql`](./database/gudang_akhir.sql) adalah ekspor untuk pengembangan lokal, bukan cadangan produksi.

### Migrasi SQL

Flyway mengelola skema mulai versi 15. Database lama yang tidak kosong dan belum memiliki riwayat Flyway diadopsi dengan baseline versi 15. Database baru yang kosong menjalankan skema awal versi 15, lalu migrasi berikutnya hingga versi 25. Migrasi dijalankan saat aplikasi dimulai.

Jika database lama belum lengkap saat diadopsi, Hibernate dapat gagal memvalidasi skema. Periksa kondisi database dan pulihkan dari cadangan terverifikasi sebelum mengubah baseline atau skema. Jangan menjalankan skrip impor database pada data yang perlu dipertahankan.

Skrip di `src/main/resources/sql/` mencatat perubahan manual historis hingga `migration_v15.sql`; Flyway tidak menjalankan ulang skrip tersebut. Migrasi baru harus ditambahkan sebagai `V26__deskripsi.sql` atau versi lebih tinggi di `src/main/resources/db/migration`. Cadangkan database sebelum memperbarui skema.

File [`scripts/windows/import_db.bat`](./scripts/windows/import_db.bat) menghapus dan mengimpor ulang database pengembangan yang dipilih. Skrip tersebut bersifat destruktif dan tidak boleh digunakan pada database berisi data penting atau data produksi.

## Menjalankan aplikasi desktop

Jalankan MySQL terlebih dahulu, lalu buka PowerShell di direktori utama repositori:

```powershell
.\mvnw.cmd spring-boot:run
```

Jendela login JavaFX akan terbuka setelah Spring Boot selesai melakukan inisialisasi. Maven mengunduh dependensi yang diperlukan saat build pertama.

## Menjalankan klien seluler

API seluler menggunakan alamat `127.0.0.1:8765` secara bawaan. Kontrak OpenAPI tersedia di [`docs/mobile-api-v1.yaml`](./docs/mobile-api-v1.yaml). Antarmuka web dapat dibuka di `/mobile/` melalui server yang sama.

Untuk mengakses API dari jaringan gudang, atur `MOBILE_API_BIND_ADDRESS` ke alamat LAN server, aktifkan `MOBILE_API_TLS_ENABLED=true`, lalu konfigurasi environment variable `SERVER_SSL_KEY_STORE`, `SERVER_SSL_KEY_STORE_PASSWORD`, `SERVER_SSL_KEY_STORE_TYPE`, dan `SERVER_SSL_KEY_ALIAS`. Aplikasi menolak binding non-loopback tanpa TLS.

Pemindaian kamera memerlukan HTTPS (atau localhost) dan browser yang mendukung `BarcodeDetector`. Pemindai keyboard-wedge dan entri manual tetap tersedia.

Launcher Windows opsional tersedia di `scripts/windows/run.ps1`. Aksi proyek NetBeans pada `nbactions.xml` juga dikonfigurasi untuk menjalankan aplikasi JavaFX.

Strategi picking dapat diatur melalui `PICKING_STRATEGY` ke `FEFO`, `FIFO`, atau `LEFO`. Strategi ini berlaku untuk pengeluaran stok tanpa pemindaian, reservasi lot DO, Goods Issue MRO, dan dispatch STO. Nilai yang tidak valid menghentikan startup; jika tidak diatur, aplikasi memakai `FEFO`.

QC penerimaan diwajibkan secara bawaan. Atur `QC_REQUIRED=false` hanya jika pemeriksaan kualitas dilakukan dan dicatat di luar GudangKu. Receipt tetap belum menambah stok sampai proses putaway selesai.

## Menjalankan pengujian

Di Windows:

```powershell
.\mvnw.cmd test
```

Di macOS atau Linux:

```sh
./mvnw test
```

Suite pengujian mencakup posting persediaan, penerimaan, DO, PO, SO, stock count, impor, Material Usage, retur, STO, laporan operasional, API seluler, dan transport ERP. Sebagian besar pengujian menggunakan mock untuk persistensi; pengujian tersebut tidak menggantikan validasi alur operasional terhadap database MySQL.

## Struktur proyek

```text
database/                         Ekspor database pengembangan lokal
scripts/
  release/                        Skrip rilis Git opsional
  windows/                        Skrip Windows untuk menjalankan, membangun, dan mengelola basis data
src/
  main/
    java/com/mycompany/tugas_akhir/
      config/                     Integrasi Spring dan JavaFX
      model/                      Entitas domain JPA
      repository/                 Repository Spring Data
      service/                    Alur bisnis dan ledger persediaan
      *Controller.java            Controller layar JavaFX
      api/mobile/                 API klien seluler
      integration/erp/            Kontrak dan klien transport ERP
    resources/
      css/                        Gaya JavaFX
      fxml/                       Tampilan JavaFX
      db/migration/               Migrasi Flyway
      sql/                        Skema kanonis dan skrip SQL historis
  test/java/                      Pengujian otomatis
```

## Batasan saat ini

- Sinkronisasi PO/SO dan pengiriman event gudang ke ERP belum tersedia. Belum ada adapter penyedia SAP, Oracle, atau Accurate yang memetakan API vendor ke kontrak WMS.
- Uji sinkronisasi live Accurate belum dilakukan karena dokumentasi endpoint, kredensial, dan pemetaan field vendor belum tersedia.
- Migrasi hingga skema versi 25 dan startup aplikasi telah dijalankan pada MySQL Laragon lokal. Alur operasional lengkap dan basis data produksi belum diuji secara menyeluruh.
- Dukungan kamera bergantung pada HTTPS (atau localhost) dan `BarcodeDetector` pada browser. Perangkat scanner fisik belum diuji integrasinya.
- STO saat ini hanya mendukung satu barang per dokumen. Penjadwalan multi-picker dan shift untuk STO belum tersedia.
- Pembuatan atau cetak ulang QR sales return hanya tersedia pada desktop; pembuatan batch label multi-item belum tersedia.
- Persetujuan PO, MRO, retur, dan STO memakai matriks routing departemen di Settings. Admin perlu menetapkan departemen pengguna dan rute approver. Rute yang tidak tersedia akan memblokir persetujuan.
- Tugas picking MRO tidak mereservasi stok saat dibuka. Sistem memeriksa kembali ketersediaan pada saat Goods Issue.
- Dasbor menampilkan metrik fulfillment, picker, packing, loading, dan waktu proses pemasok. Target SLA pemasok dan kepatuhan kalender kerja belum dihitung.
- Laporan operasional dibatasi pada data terbaru: maksimal 200 receipt, 100 DO, 200 STO, serta 500 baris scan/audit. Filter tanggal bekerja di dalam batas tersebut.
- Metrik waktu pemasok hanya menghitung receipt yang terhubung ke PO. Perhitungan belum memperhitungkan kalender kerja atau target SLA.
