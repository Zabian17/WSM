package com.mycompany.tugas_akhir;

import com.mycompany.tugas_akhir.model.Barang;
import com.mycompany.tugas_akhir.model.InventoryBalance;
import com.mycompany.tugas_akhir.model.Rak;
import com.mycompany.tugas_akhir.model.Section;
import com.mycompany.tugas_akhir.model.User;
import com.mycompany.tugas_akhir.service.SectionService;
import com.mycompany.tugas_akhir.service.BarangService;
import com.mycompany.tugas_akhir.service.InventoryBalanceService;
import com.mycompany.tugas_akhir.service.StockCountService;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
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
import java.util.Comparator;
import java.util.stream.Collectors;

@Component
public class StorageController implements Initializable {

    private static final Logger log = LoggerFactory.getLogger(StorageController.class);
    private final BarangService barangService;
    private final InventoryBalanceService inventoryBalanceService;
    private final SectionService sectionService;
    private final StockCountService stockCountService;
    private final ObservableList<Barang> barangList = FXCollections.observableArrayList();
    private FilteredList<Barang> filteredBarangList;
    private User currentUser;

    @FXML private TextField searchField;
    @FXML private ComboBox<String> categoryFilter;
    @FXML private ComboBox<String> stockStatusFilter;
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

    public StorageController(BarangService barangService, InventoryBalanceService inventoryBalanceService,
                             SectionService sectionService, StockCountService stockCountService) {
        this.barangService = barangService;
        this.inventoryBalanceService = inventoryBalanceService;
        this.sectionService = sectionService;
        this.stockCountService = stockCountService;
    }

    public void initUser(User user) {
        currentUser = user;
    }

    @Override
    public void initialize(URL url, ResourceBundle rb) {
        setupColumns();
        configureFilters();
        loadData();
    }

    private void setupColumns() {
        tableBarang.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_FLEX_LAST_COLUMN);
        colKode.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().getKodeBarang()));
        colNama.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().getNamaBarang()));
        colKategori.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().getKategori()));
        colStok.setCellValueFactory(d -> new SimpleIntegerProperty(d.getValue().getStok()));

        colStok.setCellFactory(col -> new TableCell<>() {
            @Override
            protected void updateItem(Number n, boolean empty) {
                super.updateItem(n, empty);
                if (empty || n == null) { setText(null); setStyle(""); return; }
                Barang b = getTableView().getItems().get(getIndex());
                setText(n.toString() + " " + (b.getSatuan() != null ? b.getSatuan() : "pcs"));
                setStyle(b.getStok() < b.getStokMin()
                    ? "-fx-text-fill: #ef4444; -fx-font-weight: bold;"
                    : "-fx-text-fill: #374151;");
            }
        });

        colStatus.setCellValueFactory(d -> new SimpleStringProperty(stockStatus(d.getValue())));
        colStatus.setCellFactory(col -> new TableCell<>() {
            @Override
            protected void updateItem(String s, boolean empty) {
                super.updateItem(s, empty);
                if (empty || s == null) { setGraphic(null); return; }
                Label badge = new Label(s);
                badge.getStyleClass().setAll("badge",
                    switch (s) {
                        case "Habis", "Kritis" -> "badge-danger";
                        case "Rendah"          -> "badge-warn";
                        default                -> "badge-ok";
                    });
                setGraphic(badge);
                setText(null);
            }
        });

        colAksi.setCellFactory(col -> new TableCell<>() {
            private final Button btnEdit = new Button("✏");
            private final Button btnDel  = new Button("×");
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
        });

        filteredBarangList = new FilteredList<>(barangList, barang -> true);
        tableBarang.setItems(filteredBarangList);
    }

    private void configureFilters() {
        categoryFilter.setItems(FXCollections.observableArrayList("Semua Kategori"));
        categoryFilter.getSelectionModel().selectFirst();
        stockStatusFilter.setItems(FXCollections.observableArrayList(
                "Semua Status", "Habis", "Kritis", "Rendah", "Aman"));
        stockStatusFilter.getSelectionModel().selectFirst();
        categoryFilter.valueProperty().addListener((observable, oldValue, newValue) -> applyInventoryFilters());
        stockStatusFilter.valueProperty().addListener((observable, oldValue, newValue) -> applyInventoryFilters());
    }

    private void loadData() {
        Task<List<Barang>> task = new Task<>() {
            @Override
            protected List<Barang> call() {
                return barangService.getAllBarang();
            }
        };

        task.setOnSucceeded(e -> {
            barangList.setAll(task.getValue());
            List<String> categories = barangList.stream()
                    .map(barang -> barang.getKategori() == null || barang.getKategori().isBlank()
                            ? "Umum" : barang.getKategori().trim())
                    .distinct()
                    .sorted(String.CASE_INSENSITIVE_ORDER)
                    .toList();
            categoryFilter.getItems().setAll("Semua Kategori");
            categoryFilter.getItems().addAll(categories);
            if (categoryFilter.getValue() == null || !categoryFilter.getItems().contains(categoryFilter.getValue())) {
                categoryFilter.getSelectionModel().selectFirst();
            }
            applyInventoryFilters();
            updateStats();
        });

        task.setOnFailed(e -> log.error("Gagal load data barang", task.getException()));

        Thread th = new Thread(task);
        th.setDaemon(true);
        th.start();
    }

    private void updateStats() {
        List<Barang> visibleItems = filteredBarangList == null ? List.copyOf(barangList)
                : filteredBarangList.stream().toList();
        long total  = visibleItems.size();
        long habis  = visibleItems.stream().filter(b -> b.getStok() <= 0).count();
        long rendah = visibleItems.stream().filter(b -> b.getStok() > 0 && b.getStok() < b.getStokMin()).count();
        lblTotalBarang.setText(String.valueOf(total));
        lblStokHabis.setText(String.valueOf(habis));
        lblStokRendah.setText(String.valueOf(rendah));
    }

    @FXML
    private void handleSearch() {
        applyInventoryFilters();
    }

    private void applyInventoryFilters() {
        if (filteredBarangList == null) return;
        String keyword = searchField.getText() == null ? "" : searchField.getText().trim().toLowerCase(Locale.ROOT);
        String category = categoryFilter.getValue();
        String status = stockStatusFilter.getValue();
        filteredBarangList.setPredicate(barang -> {
            boolean matchesKeyword = keyword.isEmpty()
                    || contains(barang.getKodeBarang(), keyword)
                    || contains(barang.getNamaBarang(), keyword)
                    || contains(barang.getKategori(), keyword);
            String itemCategory = barang.getKategori() == null || barang.getKategori().isBlank()
                    ? "Umum" : barang.getKategori().trim();
            boolean matchesCategory = category == null || "Semua Kategori".equals(category)
                    || category.equalsIgnoreCase(itemCategory);
            boolean matchesStatus = status == null || "Semua Status".equals(status)
                    || status.equals(stockStatus(barang));
            return matchesKeyword && matchesCategory && matchesStatus;
        });
        updateStats();
    }

    private boolean contains(String value, String keyword) {
        return value != null && value.toLowerCase(Locale.ROOT).contains(keyword);
    }

    private String stockStatus(Barang barang) {
        if (barang.getStok() <= 0) return "Habis";
        if (barang.getStok() < barang.getStokMin()) return "Kritis";
        if ((long) barang.getStok() < (long) barang.getStokMin() * 2) return "Rendah";
        return "Aman";
    }

    @FXML
    private void handleTambah() {
        showBarangDialog(null);
    }

    @FXML
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

    @FXML
    private void handleWarehouseLayout() {
        Dialog<ButtonType> dialog = new Dialog<>();
        dialog.setTitle("Layout Gudang 2D");
        dialog.getDialogPane().getButtonTypes().add(ButtonType.CLOSE);

        VBox layout = new VBox(14);
        Label help = new Label("Pilih rak untuk melihat ringkasan stok. Warna menunjukkan tingkat keterisian.");
        GridPane sectionsGrid = new GridPane();
        sectionsGrid.setHgap(14);
        sectionsGrid.setVgap(14);
        VBox rackDetails = new VBox(8);
        rackDetails.getStyleClass().add("card");
        rackDetails.getChildren().add(new Label("Pilih rak pada layout untuk melihat isinya."));
        layout.getChildren().addAll(help, sectionsGrid, rackDetails);
        ScrollPane scroll = new ScrollPane(layout);
        scroll.setFitToWidth(true);
        dialog.getDialogPane().setContent(scroll);
        dialog.getDialogPane().setPrefSize(900, 620);
        dialog.show();

        Task<WarehouseViewData> task = new Task<>() {
            @Override
            protected WarehouseViewData call() {
                return new WarehouseViewData(sectionService.getAllSections(),
                        inventoryBalanceService.getAllBalancesByLocation());
            }
        };
        task.setOnSucceeded(event -> populateWarehouseLayout(sectionsGrid, rackDetails, task.getValue()));
        task.setOnFailed(event -> {
            log.error("Gagal memuat layout gudang", task.getException());
            help.setText("Layout gudang gagal dimuat.");
        });
        Thread thread = new Thread(task, "warehouse-layout");
        thread.setDaemon(true);
        thread.start();
    }

    private void populateWarehouseLayout(GridPane sectionsGrid, VBox rackDetails, WarehouseViewData data) {
        sectionsGrid.getChildren().clear();
        List<Section> sections = data.sections().stream()
                .sorted(Comparator.comparing(Section::getKodeSection, String.CASE_INSENSITIVE_ORDER)).toList();
        for (int sectionIndex = 0; sectionIndex < sections.size(); sectionIndex++) {
            Section section = sections.get(sectionIndex);
            VBox sectionCard = new VBox(10);
            sectionCard.getStyleClass().add("card");
            Label sectionTitle = new Label(section.getKodeSection() + " - " + section.getNamaSection());
            sectionTitle.getStyleClass().add("card-title");
            GridPane rackGrid = new GridPane();
            rackGrid.setHgap(8);
            rackGrid.setVgap(8);
            List<Rak> raks = section.getRaks().stream()
                    .sorted(Comparator.comparing(Rak::getKodeRak, String.CASE_INSENSITIVE_ORDER)).toList();
            for (int rackIndex = 0; rackIndex < raks.size(); rackIndex++) {
                Rak rack = raks.get(rackIndex);
                Button rackButton = createRackButton(rack, () -> showRackBalances(rackDetails, rack, data.balances()));
                rackGrid.add(rackButton, rackIndex % 4, rackIndex / 4);
            }
            sectionCard.getChildren().addAll(sectionTitle, rackGrid);
            sectionsGrid.add(sectionCard, sectionIndex % 2, sectionIndex / 2);
        }
    }

    private Button createRackButton(Rak rack, Runnable onSelected) {
        int capacity = Math.max(rack.getKapasitas(), 1);
        double fill = Math.max(0, Math.min(1, (double) rack.getTerisi() / capacity));
        String fillColor = fill >= 0.9 ? "#fee2e2" : fill >= 0.75 ? "#fef3c7" : "#dcfce7";
        String fillText = fill >= 0.9 ? "#b91c1c" : fill >= 0.75 ? "#92400e" : "#166534";
        Button button = new Button(rack.getKodeRak() + "\n" + rack.getTerisi() + "/" + rack.getKapasitas());
        button.setWrapText(true);
        button.setMinSize(105, 58);
        button.setStyle("-fx-background-color: " + fillColor + "; -fx-text-fill: " + fillText
                + "; -fx-font-weight: bold; -fx-background-radius: 8; -fx-cursor: hand;");
        button.setTooltip(new Tooltip(rack.getTerisi() + " dari " + rack.getKapasitas()
                + " unit (" + rack.getPersentase() + "%)"));
        button.setOnAction(event -> onSelected.run());
        return button;
    }

    private void showRackBalances(VBox rackDetails, Rak rack, List<InventoryBalance> balances) {
        rackDetails.getChildren().clear();
        Label title = new Label("Stok di " + rack.getKodeRak());
        title.getStyleClass().add("card-title");
        TableView<InventoryBalance> table = new TableView<>();
        table.setPrefHeight(230);
        table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_FLEX_LAST_COLUMN);
        table.getColumns().add(balanceColumn("Kode", balance -> balance.getBarang().getKodeBarang()));
        table.getColumns().add(balanceColumn("Barang", balance -> balance.getBarang().getNamaBarang()));
        table.getColumns().add(balanceColumn("Saldo", balance -> balance.getQuantity() + " "
                + (balance.getBarang().getSatuan() == null ? "pcs" : balance.getBarang().getSatuan())));
        List<InventoryBalance> rackBalances = balances.stream()
                .filter(balance -> balance.getRak() != null && rack.getId().equals(balance.getRak().getId()))
                .filter(balance -> balance.getQuantity() > 0)
                .sorted(Comparator.comparing(balance -> balance.getBarang().getNamaBarang(),
                        String.CASE_INSENSITIVE_ORDER)).collect(Collectors.toList());
        table.setItems(FXCollections.observableArrayList(rackBalances));
        table.setPlaceholder(new Label("Rak ini belum memiliki saldo barang."));
        rackDetails.getChildren().addAll(title, table);
    }

    private record WarehouseViewData(List<Section> sections, List<InventoryBalance> balances) {}

    @FXML
    private void handleStockCount() {
        if (currentUser == null || currentUser.getId() == null) {
            showAlert(Alert.AlertType.ERROR, "Sesi tidak tersedia", "Login ulang sebelum memulai stock opname.");
            return;
        }
        new StockCountDialog(stockCountService, currentUser)
                .show(searchField.getScene().getWindow(), this::loadData);
    }

    private TableColumn<InventoryBalance, String> balanceColumn(
            String title, java.util.function.Function<InventoryBalance, String> value) {
        TableColumn<InventoryBalance, String> column = new TableColumn<>(title);
        column.setCellValueFactory(cell -> new SimpleStringProperty(value.apply(cell.getValue())));
        return column;
    }

    @FXML
    private void handleRefresh() {
        searchField.clear();
        loadData();
    }

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
        boolean isEdit = existing != null;

        Dialog<ButtonType> dlg = new Dialog<>();
        dlg.setTitle(isEdit ? "Edit Barang" : "Tambah Barang Baru");

        GridPane grid = new GridPane();
        grid.setHgap(12);
        grid.setVgap(14);
        grid.setPadding(new Insets(20, 28, 20, 28));

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

        dlg.getDialogPane().setContent(grid);
        dlg.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);

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
}
