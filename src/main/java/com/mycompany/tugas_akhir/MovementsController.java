package com.mycompany.tugas_akhir;

import com.mycompany.tugas_akhir.model.Barang;
import com.mycompany.tugas_akhir.model.Customer;
import com.mycompany.tugas_akhir.model.MaterialUsage;
import com.mycompany.tugas_akhir.model.MaterialUsageScan;
import com.mycompany.tugas_akhir.model.Rak;
import com.mycompany.tugas_akhir.model.Supplier;
import com.mycompany.tugas_akhir.model.Transaksi;
import com.mycompany.tugas_akhir.model.User;
import com.mycompany.tugas_akhir.service.BarangService;
import com.mycompany.tugas_akhir.service.CustomerService;
import com.mycompany.tugas_akhir.service.MaterialUsageService;
import com.mycompany.tugas_akhir.service.SectionService;
import com.mycompany.tugas_akhir.service.SupplierService;
import com.mycompany.tugas_akhir.service.TransaksiService;
import com.mycompany.tugas_akhir.service.UserService;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.VBox;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.net.URL;
import java.nio.file.Path;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import java.util.ResourceBundle;

@Component
public class MovementsController implements Initializable {

    private static final Logger log = LoggerFactory.getLogger(MovementsController.class);
    private final TransaksiService transaksiService;
    private final BarangService barangService;
    private final SupplierService supplierService;
    private final SectionService sectionService;
    private final InventoryTransferWorkflow transferWorkflow;
    private final InboundReceivingWorkflow receivingWorkflow;
    private final ReturnWorkflow returnWorkflow;
    private final OutboundWorkflow outboundWorkflow;
    private final CustomerService customerService;
    private final MaterialUsageService materialUsageService;
    private final StockTransferWorkflow stockTransferWorkflow;
    private final UserService userService;

    private User currentUser;
    private final ObservableList<Transaksi> txList = FXCollections.observableArrayList();
    private List<Barang> barangItems;
    private List<Rak> rakItems;
    private List<Supplier> supplierItems;
    private List<Customer> customerItems;

    @FXML private ComboBox<String> cbTipe;
    @FXML private ComboBox<Barang> cbBarang;
    @FXML private ComboBox<Supplier> cbSupplier;
    @FXML private ComboBox<String> cbLokasi;
    @FXML private VBox boxSupplier;
    @FXML private VBox boxBatch;
    @FXML private HBox boxInfoStok;
    @FXML private Label lblInfoStok;
    @FXML private TextField tfJumlah;
    @FXML private TextField tfBatchCode;
    @FXML private DatePicker dpExpiryDate;
    @FXML private TextArea taKeterangan;

    @FXML private Label lblTotalTx;
    @FXML private TableView<Transaksi> tableTransaksi;
    @FXML private TableColumn<Transaksi, String> colKodeTx;
    @FXML private TableColumn<Transaksi, String> colTipeTx;
    @FXML private TableColumn<Transaksi, String> colNamaBarang;
    @FXML private TableColumn<Transaksi, String> colLokasi;
    @FXML private TableColumn<Transaksi, String> colJumlahTx;
    @FXML private TableColumn<Transaksi, String> colTanggal;

    public MovementsController(TransaksiService transaksiService,
                               BarangService barangService,
                               SupplierService supplierService,
                               SectionService sectionService,
                               InventoryTransferWorkflow transferWorkflow,
                               InboundReceivingWorkflow receivingWorkflow,
                               ReturnWorkflow returnWorkflow,
                               OutboundWorkflow outboundWorkflow,
                               CustomerService customerService,
                               MaterialUsageService materialUsageService,
                               StockTransferWorkflow stockTransferWorkflow,
                               UserService userService) {
        this.transaksiService = transaksiService;
        this.barangService = barangService;
        this.supplierService = supplierService;
        this.sectionService = sectionService;
        this.transferWorkflow = transferWorkflow;
        this.receivingWorkflow = receivingWorkflow;
        this.returnWorkflow = returnWorkflow;
        this.outboundWorkflow = outboundWorkflow;
        this.customerService = customerService;
        this.materialUsageService = materialUsageService;
        this.stockTransferWorkflow = stockTransferWorkflow;
        this.userService = userService;
    }

    @Override
    public void initialize(URL url, ResourceBundle rb) {
        setupForm();
        setupTable();
        loadTransaksi();
    }

    public void initUser(User user) {
        this.currentUser = user;
    }

    private void setupForm() {
        cbTipe.setItems(FXCollections.observableArrayList("Barang Masuk", "Barang Keluar"));

        Task<Void> initTask = new Task<>() {
            private List<Barang> barangs;
            private List<Supplier> suppliers;
            private List<Rak> raks;
            private List<Customer> customers;

            @Override
            protected Void call() {
                barangs = barangService.getAllBarang();
                suppliers = supplierService.getAllSupplier();
                raks = sectionService.getAllRaks();
                customers = customerService.getAllCustomer();
                return null;
            }

            @Override
            protected void succeeded() {
                barangItems = barangs;
                cbBarang.setItems(FXCollections.observableArrayList(barangItems));
                cbSupplier.setItems(FXCollections.observableArrayList(suppliers));
                supplierItems = suppliers;
                rakItems = raks;
                customerItems = customers;
                if (cbTipe.getValue() != null) handleTipeChange();
            }

            @Override
            protected void failed() {
                log.error("Gagal load dropdown data form transaksi", getException());
            }
        };

        Thread th = new Thread(initTask);
        th.setDaemon(true);
        th.start();
    }

    private void setupTable() {
        tableTransaksi.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_FLEX_LAST_COLUMN);
        colKodeTx.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().getKodeTransaksi()));

        colTipeTx.setCellValueFactory(d -> new SimpleStringProperty(
            "masuk".equalsIgnoreCase(d.getValue().getTipe()) ? "▲ Masuk" : "▼ Keluar"));
        colTipeTx.setCellFactory(col -> new TableCell<>() {
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

        colNamaBarang.setCellValueFactory(d -> new SimpleStringProperty(
                d.getValue().getBarang() != null ? d.getValue().getBarang().getNamaBarang() : "-"));
        colLokasi.setCellValueFactory(d -> new SimpleStringProperty(
            d.getValue().getRak() != null ? d.getValue().getRak().getKodeRak() : "Belum dipetakan"));
        colJumlahTx.setCellValueFactory(d -> new SimpleStringProperty(
            String.valueOf(d.getValue().getJumlah())));
        
        DateTimeFormatter dtf = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");
        colTanggal.setCellValueFactory(d -> new SimpleStringProperty(
                d.getValue().getTanggal() != null ? d.getValue().getTanggal().format(dtf) : "-"));

        tableTransaksi.setItems(txList);
    }

    @FXML
    private void handleTipeChange() {
        String tipe = cbTipe.getValue();
        boolean isMasuk = "Barang Masuk".equals(tipe);
        boxSupplier.setVisible(isMasuk);
        boxSupplier.setManaged(isMasuk);
        boxBatch.setVisible(isMasuk);
        boxBatch.setManaged(isMasuk);
        ObservableList<String> locations = FXCollections.observableArrayList();
        if (rakItems != null) {
            rakItems.stream().map(Rak::getKodeRak).forEach(locations::add);
        }
        if (!isMasuk) locations.add("Stok Belum Dipetakan");
        cbLokasi.setItems(locations);
    }

    @FXML
    private void handleBarangChange() {
        Barang selected = cbBarang.getValue();
        if (selected != null) {
            lblInfoStok.setText("Stok saat ini: " + selected.getStok() + " " + selected.getSatuan());
            boxInfoStok.setVisible(true);
            boxInfoStok.setManaged(true);
        } else {
            boxInfoStok.setVisible(false);
            boxInfoStok.setManaged(false);
        }
    }

    @FXML
    private void handleOpenReceiving() {
        receivingWorkflow.open(currentUser, barangItems, supplierItems, this::handleRefresh);
    }

    @FXML
    private void handleOpenDeliveryOrders() {
        outboundWorkflow.open(currentUser, barangItems, customerItems);
    }

    @FXML
    private void handleOpenMaterialUsage() {
        if (currentUser == null || currentUser.getId() == null) {
            showAlert(Alert.AlertType.ERROR, "Sesi Tidak Valid", "Silakan login kembali.");
            return;
        }
        TableView<MaterialUsage> table = new TableView<>();
        table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_FLEX_LAST_COLUMN);
        table.getColumns().add(materialUsageColumn("Kode", MaterialUsage::getRequestCode));
        table.getColumns().add(materialUsageColumn("Status", request -> request.getStatus().name()));
        table.getColumns().add(materialUsageColumn("Pemohon", request -> request.getCreatedBy().getFullName()));
        table.getColumns().add(materialUsageColumn("Alasan", MaterialUsage::getReason));
        table.getColumns().add(materialUsageColumn("Baris", request -> String.valueOf(request.getLines().size())));
        table.setPlaceholder(new Label("Belum ada permintaan Material Usage."));

        Button create = new Button("Buat Permintaan");
        Button approve = new Button("Setujui");
        Button reject = new Button("Tolak");
        Button assignPicker = new Button("Assign Picker");
        Button issue = new Button("Goods Issue");
        Button scanIssue = new Button("Scan GI");
        Button scanHistory = new Button("Riwayat Scan");
        Button refresh = new Button("Muat Ulang");
        create.setOnAction(action -> handleCreateMaterialUsage(table));
        approve.setOnAction(action -> reviewMaterialUsage(table, true));
        reject.setOnAction(action -> reviewMaterialUsage(table, false));
        assignPicker.setOnAction(action -> assignMaterialUsagePicker(table));
        issue.setOnAction(action -> issueMaterialUsage(table));
        scanIssue.setOnAction(action -> issueMaterialUsageWithBarcodes(table));
        scanHistory.setOnAction(action -> showMaterialUsageScanHistory(table));
        refresh.setOnAction(action -> loadMaterialUsage(table));

        Dialog<ButtonType> dialog = new Dialog<>();
        dialog.setTitle("Material Usage / MRO");
        dialog.getDialogPane().setContent(new VBox(10,
                new javafx.scene.layout.FlowPane(8, 8, create, approve, reject, assignPicker, issue,
                        scanIssue, scanHistory, refresh), table));
        dialog.getDialogPane().getButtonTypes().add(ButtonType.CLOSE);
        dialog.getDialogPane().setPrefSize(850, 460);
        dialog.show();
        loadMaterialUsage(table);
    }

    private void assignMaterialUsagePicker(TableView<MaterialUsage> table) {
        MaterialUsage request = table.getSelectionModel().getSelectedItem();
        if (request == null || currentUser == null || currentUser.getId() == null) {
            showAlert(Alert.AlertType.WARNING, "Pilih Permintaan", "Pilih permintaan dan pastikan sesi aktif.");
            return;
        }
        Task<List<User>> task = new Task<>() {
            @Override
            protected List<User> call() {
                return userService.getUsersForAssignment();
            }
        };
        task.setOnSucceeded(event -> {
            if (task.getValue().isEmpty()) {
                showAlert(Alert.AlertType.WARNING, "Picker Tidak Tersedia", "Tidak ada pengguna untuk ditugaskan.");
                return;
            }
            ComboBox<User> pickers = new ComboBox<>(FXCollections.observableArrayList(task.getValue()));
            pickers.setCellFactory(list -> userPickerCell());
            pickers.setButtonCell(userPickerCell());
            pickers.setPromptText("Pilih picker");
            pickers.setValue(task.getValue().stream()
                    .filter(picker -> java.util.Objects.equals(picker.getId(), currentUser.getId()))
                    .findFirst().orElse(null));
            Dialog<ButtonType> dialog = new Dialog<>();
            dialog.setTitle("Assign MRO Pick Task");
            dialog.setHeaderText("Pilih picker untuk " + request.getRequestCode());
            dialog.getDialogPane().setContent(new VBox(8, new Label("Picker"), pickers));
            dialog.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);
            Optional<ButtonType> result = dialog.showAndWait();
            if (result.isEmpty() || result.get() != ButtonType.OK) return;
            User selectedPicker = pickers.getValue();
            if (selectedPicker == null) {
                showAlert(Alert.AlertType.WARNING, "Picker Belum Dipilih", "Pilih pengguna sebelum menyimpan.");
                return;
            }
            Task<Boolean> assignTask = new Task<>() {
                @Override
                protected Boolean call() {
                    return materialUsageService.assignPicker(request.getId(), selectedPicker.getId());
                }
            };
            assignTask.setOnSucceeded(assignEvent -> {
                if (assignTask.getValue()) {
                    loadMaterialUsage(table);
                    showAlert(Alert.AlertType.INFORMATION, "Pick Task Ditugaskan",
                            "Tugas MRO ditugaskan kepada " + selectedPicker.getFullName() + ".");
                } else {
                    showAlert(Alert.AlertType.ERROR, "Assignment Ditolak",
                            "Permintaan harus disetujui dan belum mulai dipindai.");
                }
            });
            assignTask.setOnFailed(assignEvent -> {
                log.error("Gagal menetapkan picker MRO {}", request.getRequestCode(), assignTask.getException());
                showAlert(Alert.AlertType.ERROR, "Gagal", "Pick task MRO tidak dapat ditugaskan.");
            });
            Thread assignThread = new Thread(assignTask, "mro-pick-task-assign");
            assignThread.setDaemon(true);
            assignThread.start();
        });
        task.setOnFailed(event -> {
            log.error("Gagal memuat picker untuk MRO {}", request.getRequestCode(), task.getException());
            showAlert(Alert.AlertType.ERROR, "Gagal", "Daftar picker tidak dapat dimuat.");
        });
        Thread thread = new Thread(task, "mro-pick-task-users");
        thread.setDaemon(true);
        thread.start();
    }

    @FXML
    private void handleOpenReturns() {
        returnWorkflow.open(currentUser, rakItems, this::handleRefresh);
    }

    @FXML
    private void handleOpenSto() {
        stockTransferWorkflow.open(currentUser, barangItems, rakItems, this::handleRefresh, racks -> rakItems = racks);
    }

    private ListCell<User> userPickerCell() {
        return new ListCell<>() {
            @Override
            protected void updateItem(User user, boolean empty) {
                super.updateItem(user, empty);
                setText(empty || user == null ? null : user.getFullName());
            }
        };
    }

    private TableColumn<MaterialUsage, String> materialUsageColumn(
            String title, java.util.function.Function<MaterialUsage, String> value) {
        TableColumn<MaterialUsage, String> column = new TableColumn<>(title);
        column.setCellValueFactory(cell -> new SimpleStringProperty(value.apply(cell.getValue())));
        return column;
    }

    private void loadMaterialUsage(TableView<MaterialUsage> table) {
        Task<List<MaterialUsage>> task = new Task<>() {
            @Override
            protected List<MaterialUsage> call() { return materialUsageService.getRecentRequests(); }
        };
        task.setOnSucceeded(event -> table.getItems().setAll(task.getValue()));
        task.setOnFailed(event -> {
            log.error("Gagal memuat permintaan Material Usage", task.getException());
            showAlert(Alert.AlertType.ERROR, "Gagal", "Permintaan Material Usage tidak dapat dimuat.");
        });
        Thread thread = new Thread(task, "material-usage-list");
        thread.setDaemon(true);
        thread.start();
    }

    private void handleCreateMaterialUsage(TableView<MaterialUsage> table) {
        if (barangItems == null || barangItems.isEmpty()) {
            showAlert(Alert.AlertType.WARNING, "Barang Belum Tersedia", "Daftar barang belum berhasil dimuat.");
            return;
        }
        VBox linesBox = new VBox(6);
        List<ComboBox<Barang>> itemBoxes = new java.util.ArrayList<>();
        List<TextField> quantityFields = new java.util.ArrayList<>();
        Runnable addLine = () -> {
            ComboBox<Barang> itemBox = new ComboBox<>(FXCollections.observableArrayList(barangItems));
            itemBox.setPromptText("Pilih barang");
            TextField quantityField = new TextField();
            quantityField.setPromptText("Jumlah");
            HBox row = new HBox(8, itemBox, quantityField);
            Button remove = new Button("Hapus");
            remove.setOnAction(event -> {
                int index = linesBox.getChildren().indexOf(row);
                if (linesBox.getChildren().size() <= 1) return;
                linesBox.getChildren().remove(row);
                itemBoxes.remove(index);
                quantityFields.remove(index);
            });
            row.getChildren().add(remove);
            linesBox.getChildren().add(row);
            itemBoxes.add(itemBox);
            quantityFields.add(quantityField);
        };
        addLine.run();
        Button addAnother = new Button("Tambah Barang");
        addAnother.setOnAction(event -> {
            if (itemBoxes.size() >= 100) {
                showAlert(Alert.AlertType.WARNING, "Batas Baris",
                        "Permintaan Material Usage dibatasi 100 jenis barang.");
                return;
            }
            addLine.run();
        });
        TextArea reasonField = new TextArea();
        reasonField.setPromptText("Alasan pemakaian / nomor pekerjaan");
        reasonField.setPrefRowCount(3);
        GridPane form = new GridPane();
        form.setHgap(10);
        form.setVgap(8);
        form.setPadding(new javafx.geometry.Insets(12));
        form.addRow(0, new Label("Barang / jumlah"), new VBox(8, linesBox, addAnother));
        form.addRow(1, new Label("Alasan"), reasonField);
        Dialog<ButtonType> dialog = new Dialog<>();
        dialog.setTitle("Permintaan Material Usage");
        dialog.getDialogPane().setContent(form);
        dialog.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);
        Optional<ButtonType> result = dialog.showAndWait();
        if (result.isEmpty() || result.get() != ButtonType.OK) return;
        Map<Integer, Integer> quantities = new LinkedHashMap<>();
        for (int index = 0; index < itemBoxes.size(); index++) {
            Barang item = itemBoxes.get(index).getValue();
            int quantity;
            try {
                quantity = Integer.parseInt(quantityFields.get(index).getText().trim());
            } catch (NumberFormatException exception) {
                showAlert(Alert.AlertType.WARNING, "Jumlah Tidak Valid", "Masukkan jumlah bulat lebih dari nol.");
                return;
            }
            if (item == null || quantity <= 0 || quantities.putIfAbsent(item.getId(), quantity) != null) {
                showAlert(Alert.AlertType.WARNING, "Baris Tidak Valid",
                        "Pilih barang unik pada setiap baris dan isi jumlah positif.");
                return;
            }
        }
        if (reasonField.getText().isBlank()) {
            showAlert(Alert.AlertType.WARNING, "Data Belum Lengkap", "Alasan pemakaian wajib diisi.");
            return;
        }
        Integer userId = currentUser.getId();
        String reason = reasonField.getText().trim();
        Task<MaterialUsage> task = new Task<>() {
            @Override
            protected MaterialUsage call() {
                return materialUsageService.createRequest(userId, reason, quantities);
            }
        };
        task.setOnSucceeded(event -> {
            if (task.getValue() == null) {
                showAlert(Alert.AlertType.ERROR, "Permintaan Ditolak", "Data Material Usage tidak valid.");
            } else {
                loadMaterialUsage(table);
            }
        });
        task.setOnFailed(event -> {
            log.error("Gagal membuat Material Usage", task.getException());
            showAlert(Alert.AlertType.ERROR, "Gagal", "Permintaan Material Usage tidak dapat dibuat.");
        });
        Thread thread = new Thread(task, "material-usage-create");
        thread.setDaemon(true);
        thread.start();
    }

    private void reviewMaterialUsage(TableView<MaterialUsage> table, boolean approved) {
        MaterialUsage request = table.getSelectionModel().getSelectedItem();
        if (request == null || currentUser == null || currentUser.getId() == null) {
            showAlert(Alert.AlertType.WARNING, "Pilih Permintaan", "Pilih permintaan dan pastikan sesi aktif.");
            return;
        }
        String notes = null;
        if (!approved) {
            notes = new TextInputDialog().showAndWait().orElse(null);
            if (notes == null) return;
            if (notes.isBlank()) {
                showAlert(Alert.AlertType.WARNING, "Alasan Wajib", "Alasan penolakan tidak boleh kosong.");
                return;
            }
        }
        final String rejectionNotes = notes;
        Integer reviewerId = currentUser.getId();
        Task<Boolean> task = new Task<>() {
            @Override
            protected Boolean call() {
                return materialUsageService.reviewRequest(request.getId(), reviewerId, approved, rejectionNotes);
            }
        };
        task.setOnSucceeded(event -> {
            if (!task.getValue()) {
                showAlert(Alert.AlertType.ERROR, "Tinjauan Ditolak",
                        "Hanya pengguna selain pemohon yang dapat meninjau permintaan yang menunggu.");
            } else loadMaterialUsage(table);
        });
        task.setOnFailed(event -> {
            log.error("Gagal meninjau Material Usage {}", request.getRequestCode(), task.getException());
            showAlert(Alert.AlertType.ERROR, "Gagal", "Tinjauan Material Usage gagal.");
        });
        Thread thread = new Thread(task, "material-usage-review");
        thread.setDaemon(true);
        thread.start();
    }

    private void issueMaterialUsage(TableView<MaterialUsage> table) {
        MaterialUsage request = table.getSelectionModel().getSelectedItem();
        if (request == null || currentUser == null || currentUser.getId() == null) {
            showAlert(Alert.AlertType.WARNING, "Pilih Permintaan", "Pilih permintaan dan pastikan sesi aktif.");
            return;
        }
        Integer issuerId = currentUser.getId();
        Task<Boolean> task = new Task<>() {
            @Override
            protected Boolean call() {
                return materialUsageService.issueApprovedRequest(request.getId(), issuerId);
            }
        };
        task.setOnSucceeded(event -> {
            if (!task.getValue()) {
                showAlert(Alert.AlertType.ERROR, "Goods Issue Ditolak",
                        "Permintaan harus disetujui dan seluruh kebutuhan harus tersedia di SLOC.");
            } else {
                loadMaterialUsage(table);
                showAlert(Alert.AlertType.INFORMATION, "Goods Issue", "Pengeluaran material berhasil dicatat.");
            }
        });
        task.setOnFailed(event -> {
            log.error("Gagal Goods Issue Material Usage {}", request.getRequestCode(), task.getException());
            showAlert(Alert.AlertType.ERROR, "Gagal", "Goods Issue Material Usage dibatalkan.");
        });
        Thread thread = new Thread(task, "material-usage-issue");
        thread.setDaemon(true);
        thread.start();
    }

    private void issueMaterialUsageWithBarcodes(TableView<MaterialUsage> table) {
        MaterialUsage request = table.getSelectionModel().getSelectedItem();
        if (request == null || currentUser == null || currentUser.getId() == null) {
            showAlert(Alert.AlertType.WARNING, "Pilih Permintaan", "Pilih permintaan dan pastikan sesi aktif.");
            return;
        }
        TextField itemQr = new TextField();
        itemQr.setPromptText("WMS1 item QR");
        TextField locationBarcode = new TextField();
        locationBarcode.setPromptText("SLOC1 barcode");
        TextField quantityField = new TextField();
        quantityField.setPromptText("Kuantitas scan");
        Dialog<ButtonType> dialog = new Dialog<>();
        dialog.setTitle("Scan Material Usage");
        dialog.setHeaderText("Pindai item dan SLOC untuk memverifikasi lot yang akan dikeluarkan.");
        dialog.getDialogPane().setContent(new VBox(8,
                new Label("QR item"), itemQr, new Label("Barcode lokasi"), locationBarcode,
                new Label("Kuantitas yang dipindai"), quantityField));
        dialog.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);
        Optional<ButtonType> result = dialog.showAndWait();
        if (result.isEmpty() || result.get() != ButtonType.OK) return;
        int quantity;
        try {
            quantity = Integer.parseInt(quantityField.getText().trim());
        } catch (NumberFormatException exception) {
            showAlert(Alert.AlertType.WARNING, "Kuantitas Tidak Valid", "Masukkan kuantitas bilangan bulat positif.");
            return;
        }
        if (itemQr.getText().isBlank() || locationBarcode.getText().isBlank() || quantity <= 0) {
            showAlert(Alert.AlertType.WARNING, "Data Scan Belum Lengkap",
                    "Scan QR item dan SLOC, lalu masukkan kuantitas positif.");
            return;
        }
        Integer requestId = request.getId();
        Integer issuerId = currentUser.getId();
        String itemPayload = itemQr.getText().trim();
        String locationPayload = locationBarcode.getText().trim();
        int scannedQuantity = quantity;
        Task<Boolean> task = new Task<>() {
            @Override
            protected Boolean call() {
                return materialUsageService.issueApprovedRequestWithBarcodes(
                        requestId, issuerId, itemPayload, locationPayload, scannedQuantity);
            }
        };
        task.setOnSucceeded(event -> {
            if (!task.getValue()) {
                showAlert(Alert.AlertType.ERROR, "Goods Issue Ditolak",
                        "Pastikan request disetujui, scan cocok dengan baris dan lot, dan kuantitas belum melebihi sisa.");
            } else {
                loadMaterialUsage(table);
                showAlert(Alert.AlertType.INFORMATION, "Goods Issue", "Lot hasil scan berhasil dikeluarkan.");
            }
        });
        task.setOnFailed(event -> {
            log.error("Gagal scan Goods Issue Material Usage {}", request.getRequestCode(), task.getException());
            showAlert(Alert.AlertType.ERROR, "Gagal", "Goods Issue Material Usage dibatalkan.");
        });
        Thread thread = new Thread(task, "material-usage-scan-issue");
        thread.setDaemon(true);
        thread.start();
    }

    private void showMaterialUsageScanHistory(TableView<MaterialUsage> table) {
        MaterialUsage request = table.getSelectionModel().getSelectedItem();
        if (request == null) {
            showAlert(Alert.AlertType.WARNING, "Pilih Permintaan", "Pilih permintaan untuk melihat riwayat scan.");
            return;
        }
        Task<List<MaterialUsageScan>> task = new Task<>() {
            @Override
            protected List<MaterialUsageScan> call() {
                return materialUsageService.getScans(request.getId());
            }
        };
        task.setOnSucceeded(event -> {
            TableView<MaterialUsageScan> scans = new TableView<>(
                    FXCollections.observableArrayList(task.getValue()));
            scans.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_FLEX_LAST_COLUMN);
            scans.getColumns().add(materialUsageScanColumn("Waktu", scan -> scan.getScannedAt().toString()));
            scans.getColumns().add(materialUsageScanColumn("Issuer", scan -> scan.getIssuer().getFullName()));
            scans.getColumns().add(materialUsageScanColumn("SKU / Batch",
                    scan -> scan.getScannedSku() + " / " + scan.getScannedBatch()));
            scans.getColumns().add(materialUsageScanColumn("SLOC", MaterialUsageScan::getScannedRack));
            scans.getColumns().add(materialUsageScanColumn("Qty", scan -> scan.getQuantity().toString()));
            Dialog<ButtonType> history = new Dialog<>();
            history.setTitle("Riwayat Scan " + request.getRequestCode());
            history.getDialogPane().setContent(scans);
            history.getDialogPane().getButtonTypes().add(ButtonType.CLOSE);
            history.getDialogPane().setPrefSize(720, 400);
            history.show();
        });
        task.setOnFailed(event -> {
            log.error("Gagal memuat scan Material Usage {}", request.getRequestCode(), task.getException());
            showAlert(Alert.AlertType.ERROR, "Gagal", "Riwayat scan Material Usage tidak dapat dimuat.");
        });
        Thread thread = new Thread(task, "material-usage-scan-history");
        thread.setDaemon(true);
        thread.start();
    }

    private TableColumn<MaterialUsageScan, String> materialUsageScanColumn(
            String title, java.util.function.Function<MaterialUsageScan, String> value) {
        TableColumn<MaterialUsageScan, String> column = new TableColumn<>(title);
        column.setCellValueFactory(cell -> new SimpleStringProperty(value.apply(cell.getValue())));
        return column;
    }

    @FXML
    private void handleSimpan() {
        String tipe = cbTipe.getValue();
        Barang barang = cbBarang.getValue();
        String jumlahStr = tfJumlah.getText().trim();

        if (tipe == null) {
            showAlert(Alert.AlertType.WARNING, "Peringatan", "Pilih tipe transaksi terlebih dahulu!");
            return;
        }
        if (barang == null) {
            showAlert(Alert.AlertType.WARNING, "Peringatan", "Pilih barang terlebih dahulu!");
            return;
        }
        String locationCode = cbLokasi.getValue();
        if (locationCode == null) {
            showAlert(Alert.AlertType.WARNING, "Peringatan", "Pilih lokasi penyimpanan terlebih dahulu!");
            return;
        }
        if (jumlahStr.isEmpty()) {
            showAlert(Alert.AlertType.WARNING, "Peringatan", "Jumlah tidak boleh kosong!");
            return;
        }

        int jumlah;
        try {
            jumlah = Integer.parseInt(jumlahStr);
            if (jumlah <= 0) throw new NumberFormatException();
        } catch (NumberFormatException e) {
            showAlert(Alert.AlertType.WARNING, "Peringatan", "Jumlah harus berupa angka positif!");
            return;
        }

        boolean isMasuk = "Barang Masuk".equals(tipe);
        String batchCode = isMasuk ? tfBatchCode.getText().trim() : null;
        java.time.LocalDate expiryDate = isMasuk ? dpExpiryDate.getValue() : null;
        if (isMasuk && batchCode.isEmpty()) {
            showAlert(Alert.AlertType.WARNING, "Batch Wajib Diisi", "Masukkan kode batch untuk barang masuk.");
            return;
        }

        if (!isMasuk && barang.getStok() < jumlah) {
            showAlert(Alert.AlertType.WARNING, "Stok Tidak Cukup",
                "Stok " + barang.getNamaBarang() + " saat ini hanya " + barang.getStok() + " " + barang.getSatuan() + ".\n"
                + "Tidak bisa mengeluarkan " + jumlah + " " + barang.getSatuan() + ".");
            return;
        }

        Supplier supplier = cbSupplier.getValue();
        Integer supplierId = (supplier != null) ? supplier.getId() : null;
        String keterangan = taKeterangan.getText().trim();
        if (currentUser == null || currentUser.getId() == null) {
            showAlert(Alert.AlertType.ERROR, "Sesi Tidak Valid", "Silakan login kembali sebelum mencatat transaksi.");
            return;
        }
        int userId = currentUser.getId();
        Integer rakId = "Stok Belum Dipetakan".equals(locationCode) ? null : rakItems.stream()
            .filter(rak -> rak.getKodeRak().equals(locationCode))
            .map(Rak::getId)
            .findFirst()
            .orElse(null);
        if (!"Stok Belum Dipetakan".equals(locationCode) && rakId == null) {
            showAlert(Alert.AlertType.ERROR, "Lokasi Tidak Valid", "Lokasi yang dipilih tidak lagi tersedia. Muat ulang data.");
            return;
        }

        Task<Boolean> saveTask = new Task<>() {
            @Override
            protected Boolean call() {
                return transaksiService.addTransaksi(
                    isMasuk ? "masuk" : "keluar",
                    barang.getId(), supplierId, rakId,
                    batchCode, expiryDate, jumlah, keterangan, userId
                );
            }
        };

        saveTask.setOnSucceeded(e -> {
            if (saveTask.getValue()) {
                showAlert(Alert.AlertType.INFORMATION, "Transaksi Berhasil",
                    tipe + " berhasil dicatat.\n"
                    + "Barang : " + barang.getNamaBarang() + "\n"
                    + "Jumlah : " + jumlah + " " + barang.getSatuan());
                clearForm();
                loadTransaksi();

                Task<List<Barang>> refreshBarangTask = new Task<>() {
                    @Override
                    protected List<Barang> call() {
                        return barangService.getAllBarang();
                    }
                };
                refreshBarangTask.setOnSucceeded(ev -> {
                    barangItems = refreshBarangTask.getValue();
                    cbBarang.setItems(FXCollections.observableArrayList(barangItems));
                    boxInfoStok.setVisible(false);
                    boxInfoStok.setManaged(false);
                });
                Thread th = new Thread(refreshBarangTask);
                th.setDaemon(true);
                th.start();
            } else {
                showAlert(Alert.AlertType.ERROR, "Transaksi Gagal",
                    "Transaksi gagal disimpan.\nPeriksa koneksi database atau stok barang.");
            }
        });

        saveTask.setOnFailed(e -> log.error("Gagal simpan transaksi", saveTask.getException()));

        Thread th = new Thread(saveTask);
        th.setDaemon(true);
        th.start();
    }

    @FXML
    private void handleTransferSLOC() {
        transferWorkflow.openTransfer(barangItems, rakItems, currentUser, this::handleRefresh);
    }

    @FXML
    private void handleRiwayatTransfer() {
        transferWorkflow.openHistory();
    }

    @FXML
    private void handleRefresh() {
        loadTransaksi();
        Task<List<Barang>> refreshTask = new Task<>() {
            @Override
            protected List<Barang> call() {
                return barangService.getAllBarang();
            }
        };
        refreshTask.setOnSucceeded(e -> {
            barangItems = refreshTask.getValue();
            cbBarang.setItems(FXCollections.observableArrayList(barangItems));
        });
        Thread th = new Thread(refreshTask);
        th.setDaemon(true);
        th.start();
    }

    private void loadTransaksi() {
        Task<List<Transaksi>> task = new Task<>() {
            @Override
            protected List<Transaksi> call() {
                return transaksiService.getAllTransaksi();
            }
        };

        task.setOnSucceeded(e -> {
            txList.setAll(task.getValue());
            lblTotalTx.setText(txList.size() + " transaksi");
        });

        task.setOnFailed(e -> log.error("Gagal load transaksi", task.getException()));

        Thread th = new Thread(task);
        th.setDaemon(true);
        th.start();
    }

    private void clearForm() {
        cbTipe.setValue(null);
        cbBarang.setValue(null);
        cbSupplier.setValue(null);
        cbLokasi.setValue(null);
        tfBatchCode.clear();
        dpExpiryDate.setValue(null);
        tfJumlah.clear();
        taKeterangan.clear();
        boxSupplier.setVisible(false);
        boxSupplier.setManaged(false);
        boxBatch.setVisible(false);
        boxBatch.setManaged(false);
    }

    private void showAlert(Alert.AlertType type, String title, String msg) {
        Alert alert = new Alert(type);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(msg);
        alert.showAndWait();
    }
}
