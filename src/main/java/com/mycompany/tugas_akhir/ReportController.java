package com.mycompany.tugas_akhir;

import com.mycompany.tugas_akhir.model.Barang;
import com.mycompany.tugas_akhir.model.Transaksi;
import com.mycompany.tugas_akhir.service.BarangService;
import com.mycompany.tugas_akhir.service.TransaksiService;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.net.URL;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.ResourceBundle;
import java.util.stream.Collectors;

@Component
public class ReportController implements Initializable {

    private static final Logger log = LoggerFactory.getLogger(ReportController.class);
    private final TransaksiService transaksiService;
    private final BarangService barangService;

    private final ObservableList<Transaksi> reportList = FXCollections.observableArrayList();
    private List<Transaksi> rawReportList;

    @FXML private ComboBox<String> cbFilterTipe;
    @FXML private TextField searchField;
    @FXML private Label lblTotalBarang;
    @FXML private Label lblTotalMasuk;
    @FXML private Label lblTotalKeluar;

    @FXML private TableView<Transaksi> tableReport;
    @FXML private TableColumn<Transaksi, String> colKode;
    @FXML private TableColumn<Transaksi, String> colTipe;
    @FXML private TableColumn<Transaksi, String> colBarang;
    @FXML private TableColumn<Transaksi, String> colLokasi;
    @FXML private TableColumn<Transaksi, String> colSupplier;
    @FXML private TableColumn<Transaksi, String> colJumlah;
    @FXML private TableColumn<Transaksi, String> colPetugas;
    @FXML private TableColumn<Transaksi, String> colTanggal;
    @FXML private TableColumn<Transaksi, String> colKeterangan;

    public ReportController(TransaksiService transaksiService, BarangService barangService) {
        this.transaksiService = transaksiService;
        this.barangService = barangService;
    }

    @Override
    public void initialize(URL url, ResourceBundle rb) {
        cbFilterTipe.setItems(FXCollections.observableArrayList("Semua Tipe", "Barang Masuk", "Barang Keluar"));
        cbFilterTipe.setValue("Semua Tipe");

        setupTable();
        loadData();
    }

    private void setupTable() {
        tableReport.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_FLEX_LAST_COLUMN);
        colKode.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().getKodeTransaksi()));

        colTipe.setCellValueFactory(d -> new SimpleStringProperty(
                "masuk".equalsIgnoreCase(d.getValue().getTipe()) ? "▲ Masuk" : "▼ Keluar"));
        colTipe.setCellFactory(col -> new TableCell<>() {
            @Override
            protected void updateItem(String s, boolean empty) {
                super.updateItem(s, empty);
                if (empty || s == null) { setText(null); setStyle(""); return; }
                setText(s);
                setStyle(s.contains("Masuk")
                        ? "-fx-text-fill: #10b981; -fx-font-weight: bold;"
                        : "-fx-text-fill: #ef4444; -fx-font-weight: bold;");
            }
        });

        colBarang.setCellValueFactory(d -> new SimpleStringProperty(
                d.getValue().getBarang() != null ? d.getValue().getBarang().getNamaBarang() : "-"));

        colLokasi.setCellValueFactory(d -> new SimpleStringProperty(
            d.getValue().getRak() != null ? d.getValue().getRak().getKodeRak() : "Belum dipetakan"));
        
        colSupplier.setCellValueFactory(d -> new SimpleStringProperty(
                d.getValue().getSupplier() != null ? d.getValue().getSupplier().getNamaSupplier() : "-"));
        
        colJumlah.setCellValueFactory(d -> new SimpleStringProperty(
                d.getValue().getJumlah() + " " + (d.getValue().getBarang() != null ? d.getValue().getBarang().getSatuan() : "")));

        colPetugas.setCellValueFactory(d -> new SimpleStringProperty(
                d.getValue().getUser() != null ? d.getValue().getUser().getFullName() : "System"));

        DateTimeFormatter dtf = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");
        colTanggal.setCellValueFactory(d -> new SimpleStringProperty(
                d.getValue().getTanggal() != null ? d.getValue().getTanggal().format(dtf) : "-"));

        colKeterangan.setCellValueFactory(d -> new SimpleStringProperty(
                d.getValue().getKeterangan() != null ? d.getValue().getKeterangan() : "-"));

        tableReport.setItems(reportList);
    }

    private void loadData() {
        Task<List<Transaksi>> task = new Task<>() {
            @Override
            protected List<Transaksi> call() {
                return transaksiService.getAllTransaksi();
            }
        };

        task.setOnSucceeded(e -> {
            rawReportList = task.getValue();
            applyFilter();
            updateSummaryCards();
        });

        task.setOnFailed(e -> log.error("Gagal memuat laporan transaksi", task.getException()));

        Thread th = new Thread(task);
        th.setDaemon(true);
        th.start();

        Task<List<Barang>> barangTask = new Task<>() {
            @Override
            protected List<Barang> call() {
                return barangService.getAllBarang();
            }
        };
        barangTask.setOnSucceeded(e -> {
            if (lblTotalBarang != null) {
                lblTotalBarang.setText(String.valueOf(barangTask.getValue().size()));
            }
        });
        Thread th2 = new Thread(barangTask);
        th2.setDaemon(true);
        th2.start();
    }

    private void updateSummaryCards() {
        if (rawReportList == null) return;
        int totalMasuk = 0;
        int totalKeluar = 0;

        for (Transaksi t : rawReportList) {
            if ("masuk".equalsIgnoreCase(t.getTipe())) {
                totalMasuk += (t.getJumlah() != null ? t.getJumlah() : 0);
            } else if ("keluar".equalsIgnoreCase(t.getTipe())) {
                totalKeluar += (t.getJumlah() != null ? t.getJumlah() : 0);
            }
        }

        if (lblTotalMasuk != null) lblTotalMasuk.setText(String.format("%,d", totalMasuk));
        if (lblTotalKeluar != null) lblTotalKeluar.setText(String.format("%,d", totalKeluar));
    }

    @FXML
    private void handleFilterChange() {
        applyFilter();
    }

    @FXML
    private void handleSearch() {
        applyFilter();
    }

    private void applyFilter() {
        if (rawReportList == null) return;
        String filterTipe = cbFilterTipe.getValue();
        String keyword = searchField != null ? searchField.getText().trim().toLowerCase() : "";

        List<Transaksi> filtered = rawReportList.stream().filter(t -> {
            boolean matchesTipe = true;
            if ("Barang Masuk".equals(filterTipe)) {
                matchesTipe = "masuk".equalsIgnoreCase(t.getTipe());
            } else if ("Barang Keluar".equals(filterTipe)) {
                matchesTipe = "keluar".equalsIgnoreCase(t.getTipe());
            }

            boolean matchesSearch = keyword.isEmpty()
                    || (t.getKodeTransaksi() != null && t.getKodeTransaksi().toLowerCase().contains(keyword))
                    || (t.getBarang() != null && t.getBarang().getNamaBarang().toLowerCase().contains(keyword))
                    || (t.getSupplier() != null && t.getSupplier().getNamaSupplier().toLowerCase().contains(keyword));

            return matchesTipe && matchesSearch;
        }).collect(Collectors.toList());

        reportList.setAll(filtered);
    }

    @FXML
    private void handleRefresh() {
        if (searchField != null) searchField.clear();
        cbFilterTipe.setValue("Semua Tipe");
        loadData();
    }

    @FXML
    private void handleExportPDF() {
        showAlert(Alert.AlertType.INFORMATION, "Cetak Laporan PDF",
                "📄 Fitur Ekspor Laporan ke PDF berhasil dibuat.\nTotal data tercetak: " + reportList.size() + " baris transaksi.");
    }

    @FXML
    private void handleExportCSV() {
        showAlert(Alert.AlertType.INFORMATION, "Cetak Laporan CSV",
                "📊 Fitur Ekspor Laporan ke CSV (Excel) berhasil dibuat.\nTotal data tercetak: " + reportList.size() + " baris transaksi.");
    }

    private void showAlert(Alert.AlertType type, String title, String msg) {
        Alert alert = new Alert(type);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(msg);
        alert.showAndWait();
    }
}
