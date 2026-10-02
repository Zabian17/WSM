package com.mycompany.tugas_akhir;

<<<<<<< HEAD
import com.mycompany.tugas_akhir.model.Barang;
import com.mycompany.tugas_akhir.model.InventoryBalance;
import com.mycompany.tugas_akhir.service.BarangService;
import com.mycompany.tugas_akhir.service.InventoryBalanceService;
=======
import java.net.URL;
import java.util.Optional;
import java.util.ResourceBundle;
>>>>>>> origin/main
import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
<<<<<<< HEAD
import javafx.collections.transformation.FilteredList;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.net.URL;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.ResourceBundle;

@Component
public class StorageController implements Initializable {

    private static final Logger log = LoggerFactory.getLogger(StorageController.class);
    private final BarangService barangService;
    private final InventoryBalanceService inventoryBalanceService;
    private final ObservableList<Barang> barangList = FXCollections.observableArrayList();

    @FXML private TextField searchField;
    @FXML private Label lblTotalBarang;
    @FXML private Label lblStokRendah;
    @FXML private Label lblStokHabis;

    @FXML private TableView<Barang> tableBarang;
    @FXML private TableColumn<Barang, String> colKode;
    @FXML private TableColumn<Barang, String> colNama;
    @FXML private TableColumn<Barang, String> colKategori;
    @FXML private TableColumn<Barang, Number> colStok;
    @FXML private TableColumn<Barang, String> colStatus;
    @FXML private TableColumn<Barang, Void> colAksi;

    public StorageController(BarangService barangService, InventoryBalanceService inventoryBalanceService) {
        this.barangService = barangService;
        this.inventoryBalanceService = inventoryBalanceService;
    }

    @Override
    public void initialize(URL url, ResourceBundle rb) {
        setupColumns();
        loadData();
    }

    private void setupColumns() {
        tableBarang.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_FLEX_LAST_COLUMN);
        colKode.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().getKodeBarang()));
        colNama.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().getNamaBarang()));
        colKategori.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().getKategori()));
        colStok.setCellValueFactory(d -> new SimpleIntegerProperty(d.getValue().getStok()));

=======
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.geometry.Insets;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.stage.FileChooser;


public class StorageController implements Initializable {

    private final BarangDAO barangDAO = new BarangDAO();
    private final ObservableList<BarangDAO.Barang> barangList = FXCollections.observableArrayList();

    
    @FXML private TextField  searchField;
    @FXML private ComboBox<String> comboFilter1;
    @FXML private ComboBox<String> comboStatusFilter;
    @FXML private ComboBox<String> comboCategoryFilter;
    
    @FXML private Label      lblTotalBarang;
    @FXML private Label      lblStokSehat;
    @FXML private Label      lblStokRendah;
    @FXML private Label      lblStokHabis;

    @FXML private TableView<BarangDAO.Barang>             tableBarang;
    @FXML private TableColumn<BarangDAO.Barang, Number>   colNo;
    @FXML private TableColumn<BarangDAO.Barang, String>   colKode;
    @FXML private TableColumn<BarangDAO.Barang, String>   colKategori;
    @FXML private TableColumn<BarangDAO.Barang, String>   colLokasi;
    @FXML private TableColumn<BarangDAO.Barang, Number>   colStok;
    @FXML private TableColumn<BarangDAO.Barang, String>   colStokMin;
    @FXML private TableColumn<BarangDAO.Barang, String>   colStatus;
    @FXML private TableColumn<BarangDAO.Barang, String>   colLastUpdate;

    
    @Override
    public void initialize(URL url, ResourceBundle rb) {
        initializeFilters();
        setupColumns();
        loadData();
    }
    
    private void initializeFilters() {
        comboFilter1.setItems(FXCollections.observableArrayList("All"));
        comboFilter1.setValue("All");
        
        comboStatusFilter.setItems(FXCollections.observableArrayList(
            "All status", "In Stock", "Low Stock", "No Stock"));
        comboStatusFilter.setValue("All status");
        
        comboCategoryFilter.setItems(FXCollections.observableArrayList("All categories"));
        comboCategoryFilter.setValue("All categories");
    }

    private void setupColumns() {
        
        colNo.setCellValueFactory(data ->
            new SimpleIntegerProperty(tableBarang.getItems().indexOf(data.getValue()) + 1));
        colNo.setSortable(false);

        
        colKode.setCellValueFactory(d -> new SimpleStringProperty(
            d.getValue().kodeBarang + " • " + d.getValue().namaBarang));

        colKategori.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().kategori));
        colKategori.setCellFactory(col -> new TableCell<>() {
            @Override
            protected void updateItem(String s, boolean empty) {
                super.updateItem(s, empty);
                if (empty || s == null) { setGraphic(null); return; }
                Label badge = new Label(s);
                badge.getStyleClass().setAll("badge", "badge-info");
                badge.setStyle("-fx-padding: 4 8 4 8; -fx-font-size: 11px;");
                setGraphic(badge);
                setText(null);
            }
        });

        colLokasi.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().lokasi));

        colStok.setCellValueFactory(d -> new SimpleIntegerProperty(d.getValue().stok));
>>>>>>> origin/main
        colStok.setCellFactory(col -> new TableCell<>() {
            @Override
            protected void updateItem(Number n, boolean empty) {
                super.updateItem(n, empty);
                if (empty || n == null) { setText(null); setStyle(""); return; }
<<<<<<< HEAD
                Barang b = getTableView().getItems().get(getIndex());
                setText(n.toString() + " " + (b.getSatuan() != null ? b.getSatuan() : "pcs"));
                setStyle(b.getStok() < b.getStokMin()
                    ? "-fx-text-fill: #ef4444; -fx-font-weight: bold;"
                    : "-fx-text-fill: #374151;");
            }
        });

        colStatus.setCellValueFactory(d -> {
            Barang b = d.getValue();
            if (b.getStok() <= 0)               return new SimpleStringProperty("Habis");
            if (b.getStok() < b.getStokMin())     return new SimpleStringProperty("Kritis");
            if (b.getStok() < b.getStokMin() * 2) return new SimpleStringProperty("Rendah");
            return new SimpleStringProperty("Aman");
=======
                BarangDAO.Barang b = getTableView().getItems().get(getIndex());
                setText(n.toString());
                if (b.stok <= 0)
                    setStyle("-fx-text-fill: #ef4444; -fx-font-weight: bold;");
                else if (b.stok < b.stokMin)
                    setStyle("-fx-text-fill: #f59e0b; -fx-font-weight: bold;");
                else
                    setStyle("-fx-text-fill: #10b981;");
            }
        });

        
        colStokMin.setCellValueFactory(d -> new SimpleStringProperty(
            d.getValue().stokMin + " / " + d.getValue().stokMax));

        
        colStatus.setCellValueFactory(d -> {
            BarangDAO.Barang b = d.getValue();
            if (b.stok <= 0)                return new SimpleStringProperty("No Stock");
            if (b.stok < b.stokMin)         return new SimpleStringProperty("Low Stock");
            return new SimpleStringProperty("In Stock");
>>>>>>> origin/main
        });
        colStatus.setCellFactory(col -> new TableCell<>() {
            @Override
            protected void updateItem(String s, boolean empty) {
                super.updateItem(s, empty);
                if (empty || s == null) { setGraphic(null); return; }
                Label badge = new Label(s);
                badge.getStyleClass().setAll("badge",
                    switch (s) {
<<<<<<< HEAD
                        case "Habis", "Kritis" -> "badge-danger";
                        case "Rendah"          -> "badge-warn";
                        default                -> "badge-ok";
=======
                        case "No Stock"  -> "badge-danger";
                        case "Low Stock" -> "badge-warn";
                        default          -> "badge-ok";
>>>>>>> origin/main
                    });
                setGraphic(badge);
                setText(null);
            }
        });

<<<<<<< HEAD
        colAksi.setCellFactory(col -> new TableCell<>() {
            private final Button btnEdit = new Button("✏");
            private final Button btnDel  = new Button("🗑");
            private final HBox box       = new HBox(6, btnEdit, btnDel);
            {
                btnEdit.getStyleClass().add("btn-action-edit");
                btnDel.getStyleClass().add("btn-action-delete");
                box.setAlignment(Pos.CENTER);
                btnEdit.setOnAction(e -> showBarangDialog(getTableView().getItems().get(getIndex())));
                btnDel.setOnAction(e  -> handleDelete(getTableView().getItems().get(getIndex())));
            }
            @Override
            protected void updateItem(Void v, boolean empty) {
                super.updateItem(v, empty);
                setGraphic(empty ? null : box);
            }
=======
        
        colLastUpdate.setCellValueFactory(d -> {
            long timestamp = d.getValue().lastUpdate;
            String formatted = formatLastUpdate(timestamp);
            return new SimpleStringProperty(formatted);
>>>>>>> origin/main
        });

        tableBarang.setItems(barangList);
    }

<<<<<<< HEAD
    private void loadData() {
        Task<List<Barang>> task = new Task<>() {
            @Override
            protected List<Barang> call() {
                return barangService.getAllBarang();
            }
        };

        task.setOnSucceeded(e -> {
            barangList.setAll(task.getValue());
            updateStats();
        });

        task.setOnFailed(e -> log.error("Gagal load data barang", task.getException()));

        Thread th = new Thread(task);
        th.setDaemon(true);
        th.start();
=======
    
    private void loadData() {
        barangList.setAll(barangDAO.getAllBarang());
        updateStats();
>>>>>>> origin/main
    }

    private void updateStats() {
        long total  = barangList.size();
<<<<<<< HEAD
        long habis  = barangList.stream().filter(b -> b.getStok() <= 0).count();
        long rendah = barangList.stream().filter(b -> b.getStok() > 0 && b.getStok() < b.getStokMin()).count();
        lblTotalBarang.setText(String.valueOf(total));
        lblStokHabis.setText(String.valueOf(habis));
        lblStokRendah.setText(String.valueOf(rendah));
    }

    @FXML
    private void handleSearch() {
        String kw = searchField.getText().trim();
        Task<List<Barang>> searchTask = new Task<>() {
            @Override
            protected List<Barang> call() {
                return barangService.searchBarang(kw);
            }
        };

        searchTask.setOnSucceeded(e -> {
            barangList.setAll(searchTask.getValue());
            updateStats();
        });

        searchTask.setOnFailed(e -> log.error("Gagal pencarian barang", searchTask.getException()));

        Thread th = new Thread(searchTask);
        th.setDaemon(true);
        th.start();
=======
        long sehat  = barangList.stream().filter(b -> b.stok > 0 && b.stok >= b.stokMin).count();
        long rendah = barangList.stream().filter(b -> b.stok > 0 && b.stok < b.stokMin).count();
        long habis  = barangList.stream().filter(b -> b.stok <= 0).count();
        lblTotalBarang.setText(String.valueOf(total));
        lblStokSehat.setText(String.valueOf(sehat));
        lblStokRendah.setText(String.valueOf(rendah));
        lblStokHabis.setText(String.valueOf(habis));
    }

    
    @FXML
    private void handleSearch() {
        String kw = searchField.getText().trim();
        barangList.setAll(kw.isEmpty() ? barangDAO.getAllBarang() : barangDAO.searchBarang(kw));
        updateStats();
>>>>>>> origin/main
    }

    @FXML
    private void handleTambah() {
        showBarangDialog(null);
    }

    @FXML
<<<<<<< HEAD
    private void handleViewSLOCStock() {
        Dialog<ButtonType> dialog = new Dialog<>();
        dialog.setTitle("Stok per SLOC");
        dialog.getDialogPane().getButtonTypes().add(ButtonType.CLOSE);

        TextField filterField = new TextField();
        filterField.setPromptText("Cari kode/nama barang atau lokasi...");
        TableView<InventoryBalance> table = new TableView<>();
        table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_FLEX_LAST_COLUMN);
        table.getColumns().add(balanceColumn("Kode", balance -> balance.getBarang().getKodeBarang()));
        table.getColumns().add(balanceColumn("Barang", balance -> balance.getBarang().getNamaBarang()));
        table.getColumns().add(balanceColumn("Lokasi", balance -> balance.getRak() == null
                ? "Belum dipetakan" : balance.getRak().getKodeRak()));
        table.getColumns().add(balanceColumn("Saldo", balance -> balance.getQuantity() + " "
                + (balance.getBarang().getSatuan() == null ? "pcs" : balance.getBarang().getSatuan())));
        table.setPlaceholder(new Label("Memuat saldo lokasi..."));

        VBox content = new VBox(10, filterField, table);
        content.setPadding(new Insets(8));
        VBox.setVgrow(table, Priority.ALWAYS);
        dialog.getDialogPane().setContent(content);
        dialog.getDialogPane().setPrefSize(760, 500);
        dialog.show();

        Task<List<InventoryBalance>> task = new Task<>() {
            @Override
            protected List<InventoryBalance> call() {
                return inventoryBalanceService.getAllBalancesByLocation();
            }
        };
        task.setOnSucceeded(event -> {
            ObservableList<InventoryBalance> balances = FXCollections.observableArrayList(task.getValue());
            FilteredList<InventoryBalance> filtered = new FilteredList<>(balances, balance -> true);
            filterField.textProperty().addListener((observable, oldValue, newValue) -> {
                String keyword = newValue == null ? "" : newValue.trim().toLowerCase(Locale.ROOT);
                filtered.setPredicate(balance -> keyword.isEmpty()
                        || balance.getBarang().getKodeBarang().toLowerCase(Locale.ROOT).contains(keyword)
                        || balance.getBarang().getNamaBarang().toLowerCase(Locale.ROOT).contains(keyword)
                        || (balance.getRak() == null ? "belum dipetakan" : balance.getRak().getKodeRak()
                                .toLowerCase(Locale.ROOT)).contains(keyword));
            });
            table.setItems(filtered);
            table.setPlaceholder(new Label("Tidak ada saldo lokasi."));
        });
        task.setOnFailed(event -> {
            log.error("Gagal memuat stok per SLOC", task.getException());
            table.setPlaceholder(new Label("Saldo lokasi gagal dimuat."));
        });
        Thread thread = new Thread(task, "sloc-stock-report");
        thread.setDaemon(true);
        thread.start();
    }

    private TableColumn<InventoryBalance, String> balanceColumn(
            String title, java.util.function.Function<InventoryBalance, String> value) {
        TableColumn<InventoryBalance, String> column = new TableColumn<>(title);
        column.setCellValueFactory(cell -> new SimpleStringProperty(value.apply(cell.getValue())));
        return column;
    }

    @FXML
=======
>>>>>>> origin/main
    private void handleRefresh() {
        searchField.clear();
        loadData();
    }

<<<<<<< HEAD
    private void handleDelete(Barang b) {
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setTitle("Hapus Barang");
        confirm.setHeaderText(null);
        confirm.setContentText("Hapus barang \"" + b.getNamaBarang() + "\" (" + b.getKodeBarang() + ")?");
        confirm.showAndWait().ifPresent(btn -> {
            if (btn == ButtonType.OK) {
                Task<Boolean> deleteTask = new Task<>() {
                    @Override
                    protected Boolean call() {
                        return barangService.deleteBarang(b.getId());
                    }
                };

                deleteTask.setOnSucceeded(e -> {
                    if (deleteTask.getValue()) {
                        loadData();
                        showAlert(Alert.AlertType.INFORMATION, "Berhasil", "Barang berhasil dihapus.");
                    } else {
                        showAlert(Alert.AlertType.ERROR, "Gagal",
                            "Tidak bisa menghapus barang.\nBarang mungkin masih memiliki riwayat transaksi.");
                    }
                });

                deleteTask.setOnFailed(e -> log.error("Gagal hapus barang", deleteTask.getException()));

                Thread th = new Thread(deleteTask);
                th.setDaemon(true);
                th.start();
            }
        });
    }

    private void showBarangDialog(Barang existing) {
=======
    @FXML
    private void handleExport() {
        if (barangList.isEmpty()) {
            showAlert(Alert.AlertType.WARNING, "Export Gagal", "Tidak ada data barang untuk diekspor.");
            return;
        }
        try {
            ReportGeneratorService.showInventarisReport(new java.util.ArrayList<>(barangList));
        } catch (Exception e) {
            showAlert(Alert.AlertType.ERROR, "Error Report",
                "Gagal menampilkan report:\n" + e.getMessage());
            e.printStackTrace();
        }
    }

    
    private void showBarangDialog(BarangDAO.Barang existing) {
>>>>>>> origin/main
        boolean isEdit = existing != null;

        Dialog<ButtonType> dlg = new Dialog<>();
        dlg.setTitle(isEdit ? "Edit Barang" : "Tambah Barang Baru");
<<<<<<< HEAD

=======
        dlg.getDialogPane().getStylesheets().add(
            getClass().getResource("/css/dashboard.css").toExternalForm());

        
>>>>>>> origin/main
        GridPane grid = new GridPane();
        grid.setHgap(12);
        grid.setVgap(14);
        grid.setPadding(new Insets(20, 28, 20, 28));
<<<<<<< HEAD

        TextField tfKode = new TextField(isEdit ? existing.getKodeBarang() : barangService.generateKodeBarang());
        tfKode.setEditable(!isEdit);
        if (isEdit) tfKode.setStyle("-fx-background-color: #f0f2f5; -fx-text-fill: #9ba3b8;");

        TextField tfNama     = new TextField(isEdit ? existing.getNamaBarang() : "");
        tfNama.setPromptText("Nama barang...");

        TextField tfKategori = new TextField(isEdit ? existing.getKategori() : "");
        tfKategori.setPromptText("Umum, Elektronik, dll.");

        TextField tfSatuan   = new TextField(isEdit ? existing.getSatuan() : "pcs");
        TextField tfStok     = new TextField(isEdit ? String.valueOf(existing.getStok()) : "0");
        TextField tfStokMin  = new TextField(isEdit ? String.valueOf(existing.getStokMin()) : "0");
        if (isEdit) {
            tfStok.setEditable(false);
            tfStok.setTooltip(new Tooltip("Perubahan stok dicatat melalui transaksi."));
        }

        addRow(grid, 0, "Kode Barang", tfKode);
        addRow(grid, 1, "Nama Barang *", tfNama);
        addRow(grid, 2, "Kategori", tfKategori);
        addRow(grid, 3, "Satuan", tfSatuan);
        addRow(grid, 4, isEdit ? "Stok Saat Ini" : "Stok Awal", tfStok);
        addRow(grid, 5, "Stok Min. (Alert)", tfStokMin);
=======
        grid.setMinWidth(440);

        ColumnConstraints c0 = new ColumnConstraints(130);
        ColumnConstraints c1 = new ColumnConstraints(230, 230, Double.MAX_VALUE);
        c1.setHgrow(Priority.ALWAYS);
        grid.getColumnConstraints().addAll(c0, c1);

        
        TextField tfKode = styledField(isEdit ? existing.kodeBarang : barangDAO.generateKodeBarang());
        tfKode.setEditable(!isEdit);
        if (isEdit) tfKode.setStyle("-fx-background-color: #f0f2f5; -fx-text-fill: #9ba3b8; "
                                  + "-fx-background-radius: 8; -fx-border-radius: 8;");

        TextField tfNama     = styledField(isEdit ? existing.namaBarang : "");
        tfNama.setPromptText("Nama barang...");

        TextField tfKategori = styledField(isEdit ? existing.kategori : "");
        tfKategori.setPromptText("Umum, Elektronik, dll.");

        TextField tfSatuan   = styledField(isEdit ? existing.satuan : "pcs");

        TextField tfStok     = styledField(isEdit ? String.valueOf(existing.stok) : "0");
        tfStok.setPromptText("0");

        TextField tfStokMin  = styledField(isEdit ? String.valueOf(existing.stokMin) : "0");
        tfStokMin.setPromptText("0");

        addRow(grid, 0, "Kode Barang",              tfKode);
        addRow(grid, 1, "Nama Barang *",             tfNama);
        addRow(grid, 2, "Kategori",                  tfKategori);
        addRow(grid, 3, "Satuan",                    tfSatuan);
        addRow(grid, 4, isEdit ? "Stok Saat Ini" : "Stok Awal",  tfStok);
        addRow(grid, 5, "Stok Min. (Alert)",         tfStokMin);
>>>>>>> origin/main

        dlg.getDialogPane().setContent(grid);
        dlg.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);

<<<<<<< HEAD
=======
        
        Button okBtn = (Button) dlg.getDialogPane().lookupButton(ButtonType.OK);
        okBtn.setText(isEdit ? "💾  Simpan" : "➕  Tambah");
        okBtn.getStyleClass().add("btn-primary");

        
>>>>>>> origin/main
        Optional<ButtonType> result = dlg.showAndWait();
        if (result.isPresent() && result.get() == ButtonType.OK) {
            String nama = tfNama.getText().trim();
            String kode = tfKode.getText().trim();
            if (nama.isEmpty() || kode.isEmpty()) {
                showAlert(Alert.AlertType.WARNING, "Peringatan", "Kode dan Nama barang wajib diisi!");
                return;
            }
            int stok, stokMin;
            try {
                stok    = Integer.parseInt(tfStok.getText().trim());
                stokMin = Integer.parseInt(tfStokMin.getText().trim());
            } catch (NumberFormatException ex) {
                showAlert(Alert.AlertType.WARNING, "Peringatan", "Nilai stok harus berupa angka!");
                return;
            }

<<<<<<< HEAD
            Task<Boolean> saveTask = new Task<>() {
                @Override
                protected Boolean call() {
                    if (isEdit) {
                        return barangService.updateBarang(existing.getId(), nama, tfKategori.getText().trim(),
                                tfSatuan.getText().trim(), stok, stokMin);
                    } else {
                        return barangService.addBarang(kode, nama, tfKategori.getText().trim(),
                                tfSatuan.getText().trim(), stok, stokMin) != null;
                    }
                }
            };

            saveTask.setOnSucceeded(e -> {
                if (saveTask.getValue()) {
                    loadData();
                    showAlert(Alert.AlertType.INFORMATION, "Berhasil",
                        isEdit ? "Data barang berhasil diperbarui." : "Barang baru berhasil ditambahkan.");
                } else {
                    showAlert(Alert.AlertType.ERROR, "Gagal",
                        isEdit ? "Gagal memperbarui data barang."
                               : "Gagal menambah barang. Kode barang mungkin sudah digunakan.");
                }
            });

            saveTask.setOnFailed(e -> log.error("Gagal simpan barang", saveTask.getException()));

            Thread th = new Thread(saveTask);
            th.setDaemon(true);
            th.start();
        }
    }

    private void addRow(GridPane grid, int row, String labelText, TextField field) {
        Label lbl = new Label(labelText);
=======
            boolean ok = isEdit
                ? barangDAO.updateBarang(existing.id, nama, tfKategori.getText().trim(),
                                         tfSatuan.getText().trim(), stok, stokMin)
                : barangDAO.addBarang(kode, nama, tfKategori.getText().trim(),
                                      tfSatuan.getText().trim(), stok, stokMin);

            if (ok) {
                loadData();
                showAlert(Alert.AlertType.INFORMATION, "Berhasil",
                    isEdit ? "Data barang berhasil diperbarui." : "Barang baru berhasil ditambahkan.");
            } else {
                showAlert(Alert.AlertType.ERROR, "Gagal",
                    isEdit ? "Gagal memperbarui data barang."
                           : "Gagal menambah barang. Kode barang mungkin sudah digunakan.");
            }
        }
    }

    
    private TextField styledField(String value) {
        TextField tf = new TextField(value);
        tf.getStyleClass().add("form-field");
        return tf;
    }

    private void addRow(GridPane grid, int row, String labelText, TextField field) {
        Label lbl = new Label(labelText);
        lbl.getStyleClass().add("form-label");
>>>>>>> origin/main
        lbl.setMinWidth(130);
        grid.add(lbl, 0, row);
        grid.add(field, 1, row);
    }

    private void showAlert(Alert.AlertType type, String title, String msg) {
        Alert alert = new Alert(type);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(msg);
        alert.showAndWait();
    }
<<<<<<< HEAD
=======

    
    private String formatLastUpdate(long timestamp) {
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
        return ldt.format(java.time.format.DateTimeFormatter.ofPattern("dd MMM yyyy"));
    }
>>>>>>> origin/main
}
