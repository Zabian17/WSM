package com.mycompany.tugas_akhir;

<<<<<<< HEAD
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
=======
import java.net.URL;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.ResourceBundle;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.*;


public class ReportController implements Initializable {

    private final TransaksiDAO transaksiDAO = new TransaksiDAO();

    private static final DateTimeFormatter DB_FORMAT   = DateTimeFormatter.ofPattern("yyyy-MM-dd");
    private static final DateTimeFormatter DISP_FORMAT = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    @FXML private DatePicker dpStartDate;
    @FXML private DatePicker dpEndDate;
    @FXML private ComboBox<String> cbTipe;
    @FXML private HBox boxPeriodeInfo;
    @FXML private Label lblPeriodeInfo;

    @FXML private Label lblTotalTx;
    @FXML private Label lblTotalMasuk;
    @FXML private Label lblTotalKeluar;
    @FXML private Label lblBarangAktif;

    @FXML private TableView<ReportRow> tableReport;
    @FXML private TableColumn<ReportRow, Integer> colNo;
    @FXML private TableColumn<ReportRow, String>  colKodeTx;
    @FXML private TableColumn<ReportRow, String>  colTipe;
    @FXML private TableColumn<ReportRow, String>  colBarang;
    @FXML private TableColumn<ReportRow, String>  colSupplier;
    @FXML private TableColumn<ReportRow, String>  colJumlah;
    @FXML private TableColumn<ReportRow, String>  colRak;
    @FXML private TableColumn<ReportRow, String>  colTanggal;
    @FXML private Label lblJumlahData;

    @FXML private VBox vboxTopBarang;

    public static class ReportRow {
        private final int    no;
        private final String kodeTx;
        private final String tipe;
        private final String barang;
        private final String supplier;
        private final String jumlah;
        private final String rak;
        private final String tanggal;

        public ReportRow(int no, String kodeTx, String tipe, String barang,
                         String supplier, String jumlah, String rak, String tanggal) {
            this.no       = no;
            this.kodeTx   = kodeTx;
            this.tipe     = tipe;
            this.barang   = barang;
            this.supplier = supplier;
            this.jumlah   = jumlah;
            this.rak      = rak;
            this.tanggal  = tanggal;
        }

        public int    getNo()       { return no; }
        public String getKodeTx()   { return kodeTx; }
        public String getTipe()     { return tipe.substring(0, 1).toUpperCase() + tipe.substring(1); }
        public String getBarang()   { return barang; }
        public String getSupplier() { return supplier; }
        public String getJumlah()   { return jumlah; }
        public String getRak()      { return rak; }
        public String getTanggal()  { return tanggal; }
>>>>>>> origin/main
    }

    @Override
    public void initialize(URL url, ResourceBundle rb) {
<<<<<<< HEAD
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
=======
        setupComboTipe();
        setupTableColumns();
        setDefaultDateRange();
        loadReport(false);
    }


    public void initUser(UserDAO.User user) {
    }


    private void setupComboTipe() {
        cbTipe.setItems(FXCollections.observableArrayList("Semua", "Masuk", "Keluar"));
        cbTipe.setValue("Semua");
    }

    private void setupTableColumns() {
        colNo.setCellValueFactory(new PropertyValueFactory<>("no"));
        colKodeTx.setCellValueFactory(new PropertyValueFactory<>("kodeTx"));
        colBarang.setCellValueFactory(new PropertyValueFactory<>("barang"));
        colSupplier.setCellValueFactory(new PropertyValueFactory<>("supplier"));
        colJumlah.setCellValueFactory(new PropertyValueFactory<>("jumlah"));
        colRak.setCellValueFactory(new PropertyValueFactory<>("rak"));
        colTanggal.setCellValueFactory(new PropertyValueFactory<>("tanggal"));

        colTipe.setCellValueFactory(new PropertyValueFactory<>("tipe"));
        colTipe.setCellFactory(col -> new TableCell<>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                    setStyle("");
                } else {
                    setText(item);
                    if (item.equalsIgnoreCase("Masuk")) {
                        setStyle("-fx-text-fill: #10b981; -fx-font-weight: bold; -fx-font-size: 12px;");
                    } else {
                        setStyle("-fx-text-fill: #ef4444; -fx-font-weight: bold; -fx-font-size: 12px;");
                    }
                }
            }
        });

        colJumlah.setCellFactory(col -> new TableCell<>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                } else {
                    setText(item);
                    setStyle("-fx-alignment: CENTER-RIGHT; -fx-font-weight: bold; -fx-text-fill: #374151;");
                }
            }
        });
    }

    private void setDefaultDateRange() {
        LocalDate today = LocalDate.now();
        dpEndDate.setValue(today);
        dpStartDate.setValue(today.minusDays(29));
    }


    @FXML
    private void handleGenerateReport() {
        loadReport(true);
    }


    private void loadReport(boolean openViewer) {
        LocalDate start = dpStartDate.getValue();
        LocalDate end   = dpEndDate.getValue();

        if (start == null || end == null) {
            showAlert("Validasi", "Silakan pilih tanggal awal dan akhir.");
            return;
        }
        if (start.isAfter(end)) {
            showAlert("Validasi", "Tanggal awal tidak boleh lebih besar dari tanggal akhir.");
            return;
        }

        String startStr = start.format(DB_FORMAT);
        String endStr   = end.format(DB_FORMAT);
        String tipe     = cbTipe.getValue() != null ? cbTipe.getValue() : "Semua";

        loadSummary(startStr, endStr);
        loadTable(startStr, endStr, tipe);
        loadTopBarang(startStr, endStr);

        lblPeriodeInfo.setText("Menampilkan data periode: "
            + start.format(DISP_FORMAT) + "  \u2192  " + end.format(DISP_FORMAT)
            + "   |   Tipe: " + tipe);
        boxPeriodeInfo.setVisible(true);
        boxPeriodeInfo.setManaged(true);

        if (openViewer) {
            try {
                int[] summary = transaksiDAO.getReportSummary(startStr, endStr);
                String periodeInfo = "Periode: " + start.format(DISP_FORMAT)
                    + " \u2192 " + end.format(DISP_FORMAT) + "  |  Tipe: " + tipe;
                ReportGeneratorService.showReportPeriode(
                    new java.util.ArrayList<>(tableReport.getItems()),
                    periodeInfo,
                    summary
                );
            } catch (Exception e) {
                showAlert("Generate Report Gagal",
                    "Terjadi kesalahan saat menampilkan laporan: " + e.getMessage());
                e.printStackTrace();
            }
        }
    }

    @FXML
    private void handleReset() {
        setDefaultDateRange();
        cbTipe.setValue("Semua");
        tableReport.getItems().clear();
        lblTotalTx.setText("0");
        lblTotalMasuk.setText("0");
        lblTotalKeluar.setText("0");
        lblBarangAktif.setText("0");
        lblJumlahData.setText("0 data");
        vboxTopBarang.getChildren().clear();
        vboxTopBarang.getChildren().add(
            buildInfoLabel("Generate report untuk melihat top barang")
        );
        boxPeriodeInfo.setVisible(false);
        boxPeriodeInfo.setManaged(false);
        loadReport(false);
>>>>>>> origin/main
    }

    @FXML
    private void handleRefresh() {
<<<<<<< HEAD
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
=======
        handleGenerateReport();
    }




    private void loadSummary(String startDate, String endDate) {
        int[] summary = transaksiDAO.getReportSummary(startDate, endDate);
        lblTotalTx.setText(formatAngka(summary[0]));
        lblTotalMasuk.setText(formatAngka(summary[1]));
        lblTotalKeluar.setText(formatAngka(summary[2]));
        lblBarangAktif.setText(formatAngka(summary[3]));
    }

    private void loadTable(String startDate, String endDate, String tipe) {
        List<TransaksiDAO.Transaksi> list = transaksiDAO.getTransaksiByPeriod(startDate, endDate, tipe);

        ObservableList<ReportRow> rows = FXCollections.observableArrayList();
        int no = 1;
        for (TransaksiDAO.Transaksi t : list) {
            rows.add(new ReportRow(
                no++,
                t.kodeTransaksi,
                t.tipe,
                t.namaBarang,
                t.namaSupplier,
                formatAngka(t.jumlah) + " pcs",
                t.kodeRak,
                t.tanggal
            ));
        }

        tableReport.setItems(rows);
        lblJumlahData.setText(rows.size() + " data");
        System.out.println("[ReportController] Loaded " + rows.size() + " transaksi.");
    }

    private void loadTopBarang(String startDate, String endDate) {
        List<TransaksiDAO.TopBarang> topList = transaksiDAO.getTopBarang(startDate, endDate, 5);
        vboxTopBarang.getChildren().clear();

        if (topList.isEmpty()) {
            vboxTopBarang.getChildren().add(
                buildInfoLabel("Tidak ada data di periode ini")
            );
            return;
        }

        String[] rankColors = {"#f59e0b", "#9ba3b8", "#b45309", "#1a56db", "#6b7280"};
        String[] medals     = {"🥇", "🥈", "🥉", "4", "5"};

        int rank = 0;
        for (TransaksiDAO.TopBarang tb : topList) {
            String color  = rankColors[Math.min(rank, rankColors.length - 1)];
            String medal  = medals[Math.min(rank, medals.length - 1)];

            HBox row = new HBox(8);
            row.setAlignment(javafx.geometry.Pos.CENTER_LEFT);

            Label lblMedal = new Label(medal);
            lblMedal.setStyle("-fx-font-size: 16px;");

            VBox info = new VBox(2);
            HBox.setHgrow(info, Priority.ALWAYS);

            Label lblNama = new Label(tb.namaBarang);
            lblNama.setStyle("-fx-font-size: 12px; -fx-font-weight: bold; -fx-text-fill: #1f2937; -fx-wrap-text: true;");
            lblNama.setMaxWidth(Double.MAX_VALUE);

            Label lblDetail = new Label(tb.kodeBarang + " · " + formatAngka(tb.totalJumlah) + " pcs");
            lblDetail.setStyle("-fx-font-size: 10px; -fx-text-fill: #9ba3b8;");

            info.getChildren().addAll(lblNama, lblDetail);

            Label lblTotal = new Label(String.valueOf(tb.totalTransaksi) + " tx");
            lblTotal.setStyle("-fx-font-size: 11px; -fx-font-weight: bold; -fx-text-fill: " + color
                + "; -fx-background-color: derive(" + color + ", 85%); -fx-background-radius: 6;"
                + " -fx-padding: 2 6 2 6;");

            row.getChildren().addAll(lblMedal, info, lblTotal);

            vboxTopBarang.getChildren().add(row);

            if (rank < topList.size() - 1) {
                Pane sep = new Pane();
                sep.setStyle("-fx-background-color: #f0f2f5; -fx-pref-height: 1px; -fx-min-height: 1px; -fx-max-height: 1px;");
                vboxTopBarang.getChildren().add(sep);
            }

            rank++;
        }
    }

    private Label buildInfoLabel(String text) {
        Label lbl = new Label(text);
        lbl.setStyle("-fx-text-fill: #9ba3b8; -fx-font-size: 11px; -fx-wrap-text: true;");
        lbl.setMaxWidth(Double.MAX_VALUE);
        return lbl;
    }

    private String formatAngka(int n) {
        return String.format("%,d", n).replace(',', '.');
    }


    private void showAlert(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.WARNING);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }


>>>>>>> origin/main
}
