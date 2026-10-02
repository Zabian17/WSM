package com.mycompany.tugas_akhir;

import java.io.InputStream;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import net.sf.jasperreports.engine.JRException;
import net.sf.jasperreports.engine.JasperCompileManager;
import net.sf.jasperreports.engine.JasperFillManager;
import net.sf.jasperreports.engine.JasperPrint;
import net.sf.jasperreports.engine.JasperReport;
import net.sf.jasperreports.engine.data.JRMapCollectionDataSource;
import net.sf.jasperreports.view.JasperViewer;


public class ReportGeneratorService {

    private static final DateTimeFormatter DATE_FORMAT =
            DateTimeFormatter.ofPattern("dd MMMM yyyy HH:mm:ss");

    
    private static JasperReport movementsAllReport;
    private static JasperReport movementsStockInReport;
    private static JasperReport movementsStockOutReport;
    private static JasperReport movementsAdjustmentReport;
    private static JasperReport inventarisReport;
    private static JasperReport reportReport;

    
    
    

    public static void showMovementsAllReport(List<TransaksiDAO.Transaksi> dataList,
                                              String title) throws Exception {
        JasperReport report = getMovementsAllReport();

        Map<String, Object> params = new HashMap<>();
        params.put("reportTitle", title);
        params.put("reportDate", LocalDateTime.now().format(DATE_FORMAT));
        params.put("totalTransaksi", dataList.size());
        params.put("totalMasuk", dataList.stream()
                .filter(t -> "masuk".equals(t.tipe)).count());
        params.put("totalKeluar", dataList.stream()
                .filter(t -> "keluar".equals(t.tipe)).count());

        List<Map<String, ?>> rows = new ArrayList<>();
        int no = 1;
        for (TransaksiDAO.Transaksi t : dataList) {
            Map<String, Object> row = new HashMap<>();
            row.put("no", no++);
            row.put("kodeTransaksi", t.kodeTransaksi);
            row.put("tipe", "masuk".equals(t.tipe) ? "Masuk" : "Keluar");
            row.put("namaBarang", t.namaBarang);
            row.put("supplier", t.namaSupplier != null ? t.namaSupplier : "-");
            row.put("keterangan", t.keterangan != null ? t.keterangan : "-");
            row.put("jumlah", t.jumlah);
            row.put("tanggal", t.tanggal);
            rows.add(row);
        }

        JRMapCollectionDataSource ds = new JRMapCollectionDataSource(rows);
        JasperPrint jp = JasperFillManager.fillReport(report, params, ds);
        showViewer(jp, title);
    }

    
    
    

    public static void showMovementsStockInReport(List<TransaksiDAO.Transaksi> dataList,
                                                  String title) throws Exception {
        JasperReport report = getMovementsStockInReport();

        Map<String, Object> params = new HashMap<>();
        params.put("reportTitle", title);
        params.put("reportDate", LocalDateTime.now().format(DATE_FORMAT));
        params.put("totalTransaksi", dataList.size());

        List<Map<String, ?>> rows = new ArrayList<>();
        int no = 1;
        for (TransaksiDAO.Transaksi t : dataList) {
            Map<String, Object> row = new HashMap<>();
            row.put("no", no++);
            row.put("kodeTransaksi", t.kodeTransaksi);
            row.put("namaBarang", t.namaBarang);
            row.put("supplier", t.namaSupplier != null ? t.namaSupplier : "-");
            row.put("keterangan", t.keterangan != null ? t.keterangan : "-");
            row.put("jumlah", t.jumlah);
            row.put("tanggal", t.tanggal);
            rows.add(row);
        }

        JRMapCollectionDataSource ds = new JRMapCollectionDataSource(rows);
        JasperPrint jp = JasperFillManager.fillReport(report, params, ds);
        showViewer(jp, title);
    }

    
    
    

    public static void showMovementsStockOutReport(List<TransaksiDAO.Transaksi> dataList,
                                                   String title) throws Exception {
        JasperReport report = getMovementsStockOutReport();

        Map<String, Object> params = new HashMap<>();
        params.put("reportTitle", title);
        params.put("reportDate", LocalDateTime.now().format(DATE_FORMAT));
        params.put("totalTransaksi", dataList.size());

        List<Map<String, ?>> rows = new ArrayList<>();
        int no = 1;
        for (TransaksiDAO.Transaksi t : dataList) {
            
            String tujuan = "-";
            String keterangan = "-";
            if (t.keterangan != null && !t.keterangan.isEmpty()) {
                if (t.keterangan.contains(" - ")) {
                    tujuan = t.keterangan.substring(0, t.keterangan.indexOf(" - "));
                    keterangan = t.keterangan.substring(t.keterangan.indexOf(" - ") + 3);
                } else {
                    tujuan = t.keterangan;
                }
            }

            Map<String, Object> row = new HashMap<>();
            row.put("no", no++);
            row.put("kodeTransaksi", t.kodeTransaksi);
            row.put("namaBarang", t.namaBarang);
            row.put("tujuan", tujuan);
            row.put("keterangan", keterangan);
            row.put("jumlah", t.jumlah);
            row.put("tanggal", t.tanggal);
            rows.add(row);
        }

        JRMapCollectionDataSource ds = new JRMapCollectionDataSource(rows);
        JasperPrint jp = JasperFillManager.fillReport(report, params, ds);
        showViewer(jp, title);
    }

    
    
    

    public static void showMovementsAdjustmentReport(List<TransaksiDAO.Transaksi> dataList,
                                                     String title) throws Exception {
        JasperReport report = getMovementsAdjustmentReport();

        Map<String, Object> params = new HashMap<>();
        params.put("reportTitle", title);
        params.put("reportDate", LocalDateTime.now().format(DATE_FORMAT));
        params.put("totalTransaksi", dataList.size());

        List<Map<String, ?>> rows = new ArrayList<>();
        int no = 1;
        for (TransaksiDAO.Transaksi t : dataList) {
            
            String alasan = "-";
            String keterangan = "-";
            if (t.keterangan != null) {
                if (t.keterangan.contains(" - ")) {
                    alasan = t.keterangan.substring(0, t.keterangan.indexOf(" - "));
                    keterangan = t.keterangan.substring(t.keterangan.indexOf(" - ") + 3);
                } else {
                    alasan = t.keterangan;
                }
            }

            Map<String, Object> row = new HashMap<>();
            row.put("no", no++);
            row.put("kodeTransaksi", t.kodeTransaksi);
            row.put("namaBarang", t.namaBarang);
            row.put("alasan", alasan);
            row.put("keterangan", keterangan);
            row.put("stokBaru", t.jumlah);
            row.put("tanggal", t.tanggal);
            rows.add(row);
        }

        JRMapCollectionDataSource ds = new JRMapCollectionDataSource(rows);
        JasperPrint jp = JasperFillManager.fillReport(report, params, ds);
        showViewer(jp, title);
    }

    
    
    

    public static void showInventarisReport(List<BarangDAO.Barang> dataList) throws Exception {
        JasperReport report = getInventarisReport();

        long inStock = dataList.stream()
                .filter(b -> b.stok > 0 && b.stok >= b.stokMin).count();
        long lowStock = dataList.stream()
                .filter(b -> b.stok > 0 && b.stok < b.stokMin).count();
        long outOfStock = dataList.stream()
                .filter(b -> b.stok <= 0).count();

        Map<String, Object> params = new HashMap<>();
        params.put("reportDate", LocalDateTime.now().format(DATE_FORMAT));
        params.put("totalBarang", dataList.size());
        params.put("inStock", inStock);
        params.put("lowStock", lowStock);
        params.put("outOfStock", outOfStock);

        List<Map<String, ?>> rows = new ArrayList<>();
        int no = 1;
        for (BarangDAO.Barang b : dataList) {
            String status = b.stok <= 0 ? "No Stock"
                    : (b.stok < b.stokMin ? "Low Stock" : "In Stock");

            Map<String, Object> row = new HashMap<>();
            row.put("no", no++);
            row.put("itemSku", b.kodeBarang + " \u2022 " + b.namaBarang);
            row.put("kategori", b.kategori);
            row.put("lokasi", b.lokasi);
            row.put("stok", b.stok);
            row.put("minMax", b.stokMin + " / " + b.stokMax);
            row.put("status", status);
            row.put("lastUpdate", formatLastUpdate(b.lastUpdate));
            rows.add(row);
        }

        JRMapCollectionDataSource ds = new JRMapCollectionDataSource(rows);
        JasperPrint jp = JasperFillManager.fillReport(report, params, ds);
        showViewer(jp, "Laporan Inventaris Barang");
    }

    
    
    

    public static void showReportPeriode(List<ReportController.ReportRow> dataList,
                                         String periodeInfo,
                                         int[] summary) throws Exception {
        JasperReport report = getReportReport();

        Map<String, Object> params = new HashMap<>();
        params.put("reportDate", LocalDateTime.now().format(DATE_FORMAT));
        params.put("periodeInfo", periodeInfo);
        params.put("totalTransaksi", summary[0]);
        params.put("totalMasuk", summary[1]);
        params.put("totalKeluar", summary[2]);
        params.put("barangAktif", summary[3]);

        List<Map<String, ?>> rows = new ArrayList<>();
        for (ReportController.ReportRow r : dataList) {
            Map<String, Object> row = new HashMap<>();
            row.put("no", r.getNo());
            row.put("kodeTx", r.getKodeTx());
            row.put("tipe", r.getTipe());
            row.put("barang", r.getBarang());
            row.put("supplier", r.getSupplier());
            row.put("jumlah", r.getJumlah());
            row.put("rak", r.getRak());
            row.put("tanggal", r.getTanggal());
            rows.add(row);
        }

        JRMapCollectionDataSource ds = new JRMapCollectionDataSource(rows);
        JasperPrint jp = JasperFillManager.fillReport(report, params, ds);
        showViewer(jp, "Laporan Transaksi Gudang");
    }

    
    
    

    private static void showViewer(JasperPrint jasperPrint, String title) {
        JasperViewer viewer = new JasperViewer(jasperPrint, false);
        viewer.setTitle(title);
        viewer.setVisible(true);
    }

    private static JasperReport getMovementsAllReport() throws JRException {
        if (movementsAllReport == null) {
            movementsAllReport = compileFromResource("/reports/laporan_movements_all.jrxml");
        }
        return movementsAllReport;
    }

    private static JasperReport getMovementsStockInReport() throws JRException {
        if (movementsStockInReport == null) {
            movementsStockInReport = compileFromResource("/reports/laporan_movements_stockin.jrxml");
        }
        return movementsStockInReport;
    }

    private static JasperReport getMovementsStockOutReport() throws JRException {
        if (movementsStockOutReport == null) {
            movementsStockOutReport = compileFromResource("/reports/laporan_movements_stockout.jrxml");
        }
        return movementsStockOutReport;
    }

    private static JasperReport getMovementsAdjustmentReport() throws JRException {
        if (movementsAdjustmentReport == null) {
            movementsAdjustmentReport = compileFromResource("/reports/laporan_movements_adjustment.jrxml");
        }
        return movementsAdjustmentReport;
    }

    private static JasperReport getInventarisReport() throws JRException {
        if (inventarisReport == null) {
            inventarisReport = compileFromResource("/reports/laporan_inventaris.jrxml");
        }
        return inventarisReport;
    }

    private static JasperReport getReportReport() throws JRException {
        if (reportReport == null) {
            reportReport = compileFromResource("/reports/laporan_report.jrxml");
        }
        return reportReport;
    }

    private static JasperReport compileFromResource(String resourcePath) throws JRException {
        InputStream is = ReportGeneratorService.class.getResourceAsStream(resourcePath);
        if (is == null) {
            throw new JRException("Template tidak ditemukan: " + resourcePath);
        }
        return JasperCompileManager.compileReport(is);
    }

    
    private static String formatLastUpdate(long timestamp) {
        if (timestamp == 0) return "-";
        long now = System.currentTimeMillis();
        long diffMs = now - timestamp;
        long diffSec = diffMs / 1000;
        long diffMin = diffSec / 60;
        long diffHour = diffMin / 60;
        long diffDay = diffHour / 24;

        if (diffSec < 60) return "Just now";
        if (diffMin < 60) return diffMin + " min ago";
        if (diffHour < 24) return diffHour + " h ago";
        if (diffDay < 7) return diffDay + " d ago";

        java.time.LocalDateTime ldt = java.time.LocalDateTime
                .ofInstant(java.time.Instant.ofEpochMilli(timestamp), java.time.ZoneId.systemDefault());
        return ldt.format(DateTimeFormatter.ofPattern("dd MMM yyyy"));
    }
}
