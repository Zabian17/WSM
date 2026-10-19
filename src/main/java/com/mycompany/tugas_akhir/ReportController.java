package com.mycompany.tugas_akhir;

import com.mycompany.tugas_akhir.model.Barang;
import com.mycompany.tugas_akhir.model.Transaksi;
import com.mycompany.tugas_akhir.service.BarangService;
import com.mycompany.tugas_akhir.service.TransaksiService;
import com.mycompany.tugas_akhir.service.OperationalReportRow;
import com.mycompany.tugas_akhir.service.OperationalReportService;
import com.mycompany.tugas_akhir.service.ReportPdfService;
import com.mycompany.tugas_akhir.service.StockSuggestionService;
import com.mycompany.tugas_akhir.service.StockSuggestionService.StockSuggestion;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import javafx.scene.layout.VBox;
import javafx.scene.layout.GridPane;
import javafx.stage.FileChooser;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.net.URL;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Optional;
import java.util.ResourceBundle;
import java.util.stream.Collectors;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.io.IOException;

@Component
public class ReportController implements Initializable {

    public static class ReportRow {
        private final int no;
        private final String kodeTx;
        private final String tipe;
        private final String barang;
        private final String supplier;
        private final String jumlah;
        private final String rak;
        private final String tanggal;

        public ReportRow(int no, String kodeTx, String tipe, String barang,
                         String supplier, String jumlah, String rak, String tanggal) {
            this.no = no;
            this.kodeTx = kodeTx;
            this.tipe = tipe;
            this.barang = barang;
            this.supplier = supplier;
            this.jumlah = jumlah;
            this.rak = rak;
            this.tanggal = tanggal;
        }

        public int getNo() { return no; }
        public String getKodeTx() { return kodeTx; }
        public String getTipe() { return tipe; }
        public String getBarang() { return barang; }
        public String getSupplier() { return supplier; }
        public String getJumlah() { return jumlah; }
        public String getRak() { return rak; }
        public String getTanggal() { return tanggal; }
    }

    private static final Logger log = LoggerFactory.getLogger(ReportController.class);
    private final TransaksiService transaksiService;
    private final BarangService barangService;
    private final OperationalReportService operationalReportService;
    private final ReportPdfService reportPdfService;
    private final StockSuggestionService stockSuggestionService;

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

    public ReportController(TransaksiService transaksiService, BarangService barangService,
                            OperationalReportService operationalReportService,
                            ReportPdfService reportPdfService,
                            StockSuggestionService stockSuggestionService) {
        this.transaksiService = transaksiService;
        this.barangService = barangService;
        this.operationalReportService = operationalReportService;
        this.reportPdfService = reportPdfService;
        this.stockSuggestionService = stockSuggestionService;
    }

    @FXML
    private void handleOpenStockSuggestions() {
        TableView<StockSuggestion> table = new TableView<>();
        table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_FLEX_LAST_COLUMN);
        table.getColumns().add(stockSuggestionColumn("SKU", StockSuggestion::sku));
        table.getColumns().add(stockSuggestionColumn("Barang", StockSuggestion::itemName));
        table.getColumns().add(stockSuggestionColumn("Stok", value -> String.valueOf(value.stock())));
        table.getColumns().add(stockSuggestionColumn("Reservasi", value -> String.valueOf(value.reservedQuantity())));
        table.getColumns().add(stockSuggestionColumn("Tersedia", value -> String.valueOf(value.availableQuantity())));
        table.getColumns().add(stockSuggestionColumn("Rata-rata/bln", value -> String.format("%.1f", value.monthlyAverageDemand())));
        table.getColumns().add(stockSuggestionColumn("Buffer", value -> String.valueOf(value.bufferStock())));
        table.getColumns().add(stockSuggestionColumn("Maksimum", value -> String.valueOf(value.maximumStock())));
        table.getColumns().add(stockSuggestionColumn("Saran beli", value -> String.valueOf(value.suggestedPurchaseQuantity())));
        table.setPlaceholder(new Label("Belum ada barang."));
        Button configure = new Button("Atur buffer / maksimum");
        Button refresh = new Button("Muat ulang");
        configure.setDisable(true);
        table.getSelectionModel().selectedItemProperty().addListener((observable, oldValue, selected) ->
                configure.setDisable(selected == null));
        configure.setOnAction(event -> editStockTargets(table));
        refresh.setOnAction(event -> loadStockSuggestions(table));
        Dialog<ButtonType> dialog = new Dialog<>();
        dialog.setTitle("Forecast dan Saran Stok");
        dialog.setHeaderText("Rata-rata dari " + stockSuggestionService.getHistoryMonths()
                + " bulan kalender selesai; saran beli muncul saat stok tersedia di bawah buffer.");
        dialog.getDialogPane().setContent(new VBox(10, new javafx.scene.layout.HBox(8, configure, refresh), table));
        dialog.getDialogPane().getButtonTypes().add(ButtonType.CLOSE);
        dialog.getDialogPane().setPrefSize(1050, 560);
        loadStockSuggestions(table);
        dialog.showAndWait();
    }

    private TableColumn<StockSuggestion, String> stockSuggestionColumn(
            String title, java.util.function.Function<StockSuggestion, String> value) {
        TableColumn<StockSuggestion, String> column = new TableColumn<>(title);
        column.setCellValueFactory(cell -> new SimpleStringProperty(value.apply(cell.getValue())));
        return column;
    }

    private void loadStockSuggestions(TableView<StockSuggestion> table) {
        Task<List<StockSuggestion>> task = new Task<>() {
            @Override
            protected List<StockSuggestion> call() {
                return stockSuggestionService.getSuggestions();
            }
        };
        task.setOnSucceeded(event -> table.setItems(FXCollections.observableArrayList(task.getValue())));
        task.setOnFailed(event -> {
            log.error("Gagal memuat saran stok", task.getException());
            showAlert(Alert.AlertType.ERROR, "Gagal", "Saran stok tidak dapat dimuat.");
        });
        Thread thread = new Thread(task, "stock-suggestions-load");
        thread.setDaemon(true);
        thread.start();
    }

    private void editStockTargets(TableView<StockSuggestion> table) {
        StockSuggestion selected = table.getSelectionModel().getSelectedItem();
        if (selected == null) return;
        TextField bufferField = new TextField(String.valueOf(selected.bufferStock()));
        TextField maximumField = new TextField(String.valueOf(selected.maximumStock()));
        Dialog<ButtonType> dialog = new Dialog<>();
        dialog.setTitle("Atur Target Stok");
        dialog.setHeaderText(selected.sku() + " — " + selected.itemName()
                + ". Maksimum 0 menonaktifkan saran pembelian.");
        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(8);
        grid.addRow(0, new Label("Buffer / stok minimum"), bufferField);
        grid.addRow(1, new Label("Maksimum"), maximumField);
        dialog.getDialogPane().setContent(grid);
        dialog.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);
        Optional<ButtonType> result = dialog.showAndWait();
        if (result.isEmpty() || result.get() != ButtonType.OK) return;
        int buffer;
        int maximum;
        try {
            buffer = Integer.parseInt(bufferField.getText().trim());
            maximum = Integer.parseInt(maximumField.getText().trim());
            if (buffer < 0 || maximum < 0 || maximum > 0 && maximum < buffer) throw new NumberFormatException();
        } catch (NumberFormatException exception) {
            showAlert(Alert.AlertType.WARNING, "Target Tidak Valid",
                    "Isi bilangan bulat nonnegatif; maksimum harus sama atau lebih besar dari buffer.");
            return;
        }
        Task<Boolean> task = new Task<>() {
            @Override
            protected Boolean call() {
                return barangService.updateStockTargets(selected.itemId(), buffer, maximum);
            }
        };
        task.setOnSucceeded(event -> {
            if (task.getValue()) loadStockSuggestions(table);
            else showAlert(Alert.AlertType.ERROR, "Target Tidak Tersimpan", "Barang tidak ditemukan.");
        });
        task.setOnFailed(event -> {
            log.error("Gagal menyimpan target stok {}", selected.sku(), task.getException());
            showAlert(Alert.AlertType.ERROR, "Gagal", "Target stok tidak dapat disimpan.");
        });
        Thread thread = new Thread(task, "stock-target-update");
        thread.setDaemon(true);
        thread.start();
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
        FileChooser chooser = new FileChooser();
        chooser.setTitle("Simpan Laporan Transaksi PDF");
        chooser.setInitialFileName("laporan-transaksi.pdf");
        chooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("PDF", "*.pdf"));
        java.io.File file = chooser.showSaveDialog(tableReport.getScene().getWindow());
        if (file == null) return;
        try {
            reportPdfService.exportTransactions(List.copyOf(reportList), file.toPath());
            showAlert(Alert.AlertType.INFORMATION, "PDF Tersimpan", "Laporan berhasil disimpan.");
        } catch (Exception exception) {
            log.error("Gagal menulis laporan transaksi PDF ke {}", file.getAbsolutePath(), exception);
            showAlert(Alert.AlertType.ERROR, "Ekspor Gagal", "File PDF tidak dapat disimpan.");
        }
    }

    @FXML
    private void handleExportCSV() {
        FileChooser chooser = new FileChooser();
        chooser.setTitle("Simpan Laporan Transaksi CSV");
        chooser.setInitialFileName("laporan-transaksi.csv");
        chooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("CSV", "*.csv"));
        java.io.File file = chooser.showSaveDialog(tableReport.getScene().getWindow());
        if (file == null) return;
        try (var writer = Files.newBufferedWriter(file.toPath(), StandardCharsets.UTF_8)) {
            writer.write("Kode Transaksi,Tipe,Barang,Supplier,Jumlah,SLOC,Petugas,Tanggal,Keterangan");
            writer.newLine();
            for (Transaksi transaction : reportList) {
                writeCsvRow(writer, List.of(
                        value(transaction.getKodeTransaksi()),
                        value(transaction.getTipe()),
                        transaction.getBarang() == null ? "-" : value(transaction.getBarang().getNamaBarang()),
                        transaction.getSupplier() == null ? "-" : value(transaction.getSupplier().getNamaSupplier()),
                        String.valueOf(transaction.getJumlah()),
                        transaction.getRak() == null ? "-" : value(transaction.getRak().getKodeRak()),
                        transaction.getUser() == null ? "-" : value(transaction.getUser().getFullName()),
                        transaction.getTanggal() == null ? "-" : value(transaction.getTanggal().toString()),
                        value(transaction.getKeterangan())));
            }
            showAlert(Alert.AlertType.INFORMATION, "CSV Tersimpan", "Laporan berhasil disimpan.");
        } catch (IOException exception) {
            log.error("Gagal menulis laporan transaksi CSV ke {}", file.getAbsolutePath(), exception);
            showAlert(Alert.AlertType.ERROR, "Ekspor Gagal", "File CSV tidak dapat disimpan.");
        }
    }

    @FXML
    private void handleOpenOperationalReports() {
        Task<List<OperationalReportRow>> task = new Task<>() {
            @Override
            protected List<OperationalReportRow> call() {
                return operationalReportService.getRecentOperationalReport();
            }
        };
        task.setOnSucceeded(event -> showOperationalReports(task.getValue()));
        task.setOnFailed(event -> {
            log.error("Gagal memuat laporan operasional", task.getException());
            showAlert(Alert.AlertType.ERROR, "Gagal", "Laporan operasional tidak dapat dimuat.");
        });
        Thread thread = new Thread(task, "operational-report-load");
        thread.setDaemon(true);
        thread.start();
    }

    private void showOperationalReports(List<OperationalReportRow> rows) {
        ObservableList<OperationalReportRow> allRows = FXCollections.observableArrayList(rows);
        ComboBox<String> category = new ComboBox<>();
        category.getItems().add("Semua");
        rows.stream().map(OperationalReportRow::category).distinct().sorted().forEach(category.getItems()::add);
        category.setValue("Semua");
        DatePicker fromDate = new DatePicker();
        fromDate.setPromptText("Dari tanggal");
        DatePicker toDate = new DatePicker();
        toDate.setPromptText("Sampai tanggal");
        TableView<OperationalReportRow> table = new TableView<>();
        table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_FLEX_LAST_COLUMN);
        table.getColumns().add(operationalColumn("Jenis", OperationalReportRow::category));
        table.getColumns().add(operationalColumn("Dokumen", OperationalReportRow::documentCode));
        table.getColumns().add(operationalColumn("Status", OperationalReportRow::status));
        table.getColumns().add(operationalColumn("Mitra / Petugas", OperationalReportRow::partner));
        table.getColumns().add(operationalColumn("SKU", OperationalReportRow::itemCode));
        table.getColumns().add(operationalColumn("Jumlah", row -> String.valueOf(row.quantity())));
        table.getColumns().add(operationalColumn("Metrik / Detail", OperationalReportRow::metric));
        table.getColumns().add(operationalColumn("Waktu", row -> row.timestamp() == null ? "-"
                : row.timestamp().format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm"))));
        table.getItems().setAll(allRows);
        category.setOnAction(event -> applyOperationalCategory(allRows, table, category));
        Button applyFilters = new Button("Terapkan Filter");
        applyFilters.setOnAction(event -> loadOperationalReport(
                fromDate.getValue(), toDate.getValue(), allRows, table, category));
        Button export = new Button("Ekspor CSV");
        export.setOnAction(event -> exportOperationalCsv(table.getItems()));
        VBox content = new VBox(10, new javafx.scene.layout.HBox(10,
                new Label("Kategori"), category, new Label("Dari"), fromDate,
                new Label("Sampai"), toDate, applyFilters, export), table);
        Dialog<ButtonType> dialog = new Dialog<>();
        dialog.setTitle("Laporan Operasional WMS");
        dialog.getDialogPane().setContent(content);
        dialog.getDialogPane().getButtonTypes().add(ButtonType.CLOSE);
        dialog.getDialogPane().setPrefSize(1120, 600);
        dialog.showAndWait();
    }

    private void loadOperationalReport(LocalDate from, LocalDate to,
                                       ObservableList<OperationalReportRow> allRows,
                                       TableView<OperationalReportRow> table,
                                       ComboBox<String> category) {
        if (from != null && to != null && from.isAfter(to)) {
            showAlert(Alert.AlertType.WARNING, "Rentang Tanggal Tidak Valid",
                    "Tanggal awal harus sama dengan atau sebelum tanggal akhir.");
            return;
        }
        Task<List<OperationalReportRow>> task = new Task<>() {
            @Override
            protected List<OperationalReportRow> call() {
                return operationalReportService.getRecentOperationalReport(from, to);
            }
        };
        task.setOnSucceeded(event -> {
            allRows.setAll(task.getValue());
            String selectedCategory = category.getValue();
            category.getItems().setAll("Semua");
            allRows.stream().map(OperationalReportRow::category).distinct().sorted()
                    .forEach(category.getItems()::add);
            if (selectedCategory == null || !category.getItems().contains(selectedCategory)) {
                category.setValue("Semua");
            }
            applyOperationalCategory(allRows, table, category);
        });
        task.setOnFailed(event -> {
            log.error("Gagal memfilter laporan operasional", task.getException());
            showAlert(Alert.AlertType.ERROR, "Gagal", "Laporan tidak dapat difilter.");
        });
        Thread thread = new Thread(task, "operational-report-filter");
        thread.setDaemon(true);
        thread.start();
    }

    private void applyOperationalCategory(List<OperationalReportRow> rows,
                                          TableView<OperationalReportRow> table,
                                          ComboBox<String> category) {
        String selected = category.getValue();
        table.getItems().setAll(rows.stream().filter(row -> "Semua".equals(selected)
                || row.category().equals(selected)).toList());
    }

    private TableColumn<OperationalReportRow, String> operationalColumn(
            String title, java.util.function.Function<OperationalReportRow, String> value) {
        TableColumn<OperationalReportRow, String> column = new TableColumn<>(title);
        column.setCellValueFactory(cell -> new SimpleStringProperty(value.apply(cell.getValue())));
        return column;
    }

    private void exportOperationalCsv(List<OperationalReportRow> rows) {
        FileChooser chooser = new FileChooser();
        chooser.setTitle("Simpan Laporan Operasional CSV");
        chooser.setInitialFileName("laporan-operasional-wms.csv");
        chooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("CSV", "*.csv"));
        java.io.File file = chooser.showSaveDialog(tableReport.getScene().getWindow());
        if (file == null) return;
        try (var writer = Files.newBufferedWriter(file.toPath(), StandardCharsets.UTF_8)) {
            writer.write("Kategori,Dokumen,Status,Mitra,SKU,Jumlah,Metrik,Petugas,Waktu");
            writer.newLine();
            for (OperationalReportRow row : rows) {
                writeCsvRow(writer, List.of(value(row.category()), value(row.documentCode()),
                        value(row.status()), value(row.partner()), value(row.itemCode()),
                        String.valueOf(row.quantity()), value(row.metric()), value(row.actor()),
                        row.timestamp() == null ? "-" : value(row.timestamp().toString())));
            }
            showAlert(Alert.AlertType.INFORMATION, "CSV Tersimpan", "Laporan operasional berhasil disimpan.");
        } catch (IOException exception) {
            log.error("Gagal menulis laporan operasional CSV ke {}", file.getAbsolutePath(), exception);
            showAlert(Alert.AlertType.ERROR, "Ekspor Gagal", "File CSV operasional tidak dapat disimpan.");
        }
    }

    private String value(String value) { return value == null ? "-" : value; }

    private void writeCsvRow(java.io.BufferedWriter writer, List<String> values) throws IOException {
        writer.write(values.stream().map(this::csvCell)
                .collect(Collectors.joining(",")));
        writer.newLine();
    }

    private String csvCell(String value) {
        String cell = value == null ? "-" : value;
        int first = 0;
        while (first < cell.length()) {
            char character = cell.charAt(first);
            if (!Character.isWhitespace(character) && !Character.isSpaceChar(character)
                    && !Character.isISOControl(character) && character != '\uFEFF') break;
            first++;
        }
        if (first < cell.length()) {
            char prefix = cell.charAt(first);
            boolean formula = prefix == '=' || prefix == '+' || prefix == '@'
                    || (prefix == '-' && !cell.substring(first).matches("-\\d+(\\.\\d+)?"));
            if (formula) cell = "'" + cell;
        }
        return "\"" + cell.replace("\"", "\"\"") + "\"";
    }

    private void showAlert(Alert.AlertType type, String title, String msg) {
        Alert alert = new Alert(type);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(msg);
        alert.showAndWait();
    }
}
