package com.mycompany.tugas_akhir;

import com.mycompany.tugas_akhir.model.Barang;
import com.mycompany.tugas_akhir.model.Customer;
import com.mycompany.tugas_akhir.model.DeliveryOrder;
import com.mycompany.tugas_akhir.model.DeliveryOrderLine;
import com.mycompany.tugas_akhir.model.InventoryTransfer;
import com.mycompany.tugas_akhir.model.InboundReceipt;
import com.mycompany.tugas_akhir.model.Rak;
import com.mycompany.tugas_akhir.model.Supplier;
import com.mycompany.tugas_akhir.model.Transaksi;
import com.mycompany.tugas_akhir.model.User;
import com.mycompany.tugas_akhir.service.BarangService;
import com.mycompany.tugas_akhir.service.CustomerService;
import com.mycompany.tugas_akhir.service.DeliveryOrderService;
import com.mycompany.tugas_akhir.service.InventoryTransferService;
import com.mycompany.tugas_akhir.service.InboundReceiptService;
import com.mycompany.tugas_akhir.service.SectionService;
import com.mycompany.tugas_akhir.service.SupplierService;
import com.mycompany.tugas_akhir.service.TransaksiService;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
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
    private final InventoryTransferService transferService;
    private final InboundReceiptService inboundReceiptService;
    private final DeliveryOrderService deliveryOrderService;
    private final CustomerService customerService;

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
                               InventoryTransferService transferService,
                               InboundReceiptService inboundReceiptService,
                               DeliveryOrderService deliveryOrderService,
                               CustomerService customerService) {
        this.transaksiService = transaksiService;
        this.barangService = barangService;
        this.supplierService = supplierService;
        this.sectionService = sectionService;
        this.transferService = transferService;
        this.inboundReceiptService = inboundReceiptService;
        this.deliveryOrderService = deliveryOrderService;
        this.customerService = customerService;
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
        if (currentUser == null || currentUser.getId() == null) {
            showAlert(Alert.AlertType.ERROR, "Sesi Tidak Valid", "Silakan login kembali untuk membuka receiving.");
            return;
        }

        Dialog<ButtonType> dialog = new Dialog<>();
        dialog.setTitle("Inbound Receiving & QC");
        dialog.getDialogPane().getButtonTypes().add(ButtonType.CLOSE);
        TableView<InboundReceipt> table = new TableView<>();
        table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_FLEX_LAST_COLUMN);
        table.getColumns().add(receiptColumn("Receipt", InboundReceipt::getReceiptCode));
        table.getColumns().add(receiptColumn("Barang", receipt -> receipt.getBarang().getNamaBarang()));
        table.getColumns().add(receiptColumn("Supplier", receipt -> receipt.getSupplier().getNamaSupplier()));
        table.getColumns().add(receiptColumn("Jumlah", receipt -> receipt.getQuantity() + ""));
        table.getColumns().add(receiptColumn("Batch", InboundReceipt::getBatchCode));
        table.getColumns().add(receiptColumn("Expiry", receipt -> receipt.getExpiryDate() == null
                ? "-" : receipt.getExpiryDate().toString()));
        table.getColumns().add(receiptColumn("Status", receipt -> receipt.getStatus().name()));
        table.setPlaceholder(new Label("Memuat dokumen inbound..."));

        Button createButton = new Button("Receipt Baru");
        Button approveButton = new Button("QC Lulus");
        Button rejectButton = new Button("QC Tolak");
        Button putawayButton = new Button("Putaway");
        Button qrButton = new Button("QR Label");
        createButton.setOnAction(event -> handleCreateReceipt(table));
        approveButton.setOnAction(event -> handleReceiptQc(table, true));
        rejectButton.setOnAction(event -> handleReceiptQc(table, false));
        putawayButton.setOnAction(event -> handleReceiptPutaway(table));
        qrButton.setOnAction(event -> handleReceiptQr(table));

        VBox content = new VBox(10, new HBox(8, createButton, approveButton, rejectButton, putawayButton, qrButton), table);
        content.setPadding(new javafx.geometry.Insets(8));
        VBox.setVgrow(table, javafx.scene.layout.Priority.ALWAYS);
        dialog.getDialogPane().setContent(content);
        dialog.getDialogPane().setPrefSize(1050, 560);
        dialog.show();
        loadInboundReceipts(table);
    }

    private void loadInboundReceipts(TableView<InboundReceipt> table) {
        Task<List<InboundReceipt>> task = new Task<>() {
            @Override
            protected List<InboundReceipt> call() {
                return inboundReceiptService.getRecentReceipts();
            }
        };
        task.setOnSucceeded(event -> {
            table.getItems().setAll(task.getValue());
            table.setPlaceholder(new Label("Belum ada dokumen receiving."));
        });
        task.setOnFailed(event -> {
            log.error("Gagal memuat receipt inbound", task.getException());
            table.setPlaceholder(new Label("Dokumen inbound gagal dimuat."));
        });
        Thread thread = new Thread(task, "inbound-receipt-list");
        thread.setDaemon(true);
        thread.start();
    }

    private void handleCreateReceipt(TableView<InboundReceipt> table) {
        if (barangItems == null || supplierItems == null) {
            showAlert(Alert.AlertType.INFORMATION, "Data Sedang Dimuat", "Tunggu hingga master barang dan supplier tersedia.");
            return;
        }
        Dialog<ButtonType> dialog = new Dialog<>();
        dialog.setTitle("Buat Inbound Receipt");
        ComboBox<Barang> barangBox = new ComboBox<>(FXCollections.observableArrayList(barangItems));
        ComboBox<Supplier> supplierBox = new ComboBox<>(FXCollections.observableArrayList(supplierItems));
        TextField quantityField = new TextField("1");
        TextField batchField = new TextField();
        DatePicker expiryPicker = new DatePicker();
        TextArea notesArea = new TextArea();
        notesArea.setPrefRowCount(2);
        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(10);
        grid.setPadding(new javafx.geometry.Insets(16));
        grid.addRow(0, new Label("Barang *"), barangBox);
        grid.addRow(1, new Label("Supplier *"), supplierBox);
        grid.addRow(2, new Label("Jumlah *"), quantityField);
        grid.addRow(3, new Label("Kode Batch *"), batchField);
        grid.addRow(4, new Label("Expiry"), expiryPicker);
        grid.addRow(5, new Label("Catatan"), notesArea);
        dialog.getDialogPane().setContent(grid);
        dialog.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);
        Optional<ButtonType> result = dialog.showAndWait();
        if (result.isEmpty() || result.get() != ButtonType.OK) return;

        Barang selectedBarang = barangBox.getValue();
        Supplier selectedSupplier = supplierBox.getValue();
        int quantity;
        try {
            quantity = Integer.parseInt(quantityField.getText().trim());
            if (quantity <= 0) throw new NumberFormatException();
        } catch (NumberFormatException ex) {
            showAlert(Alert.AlertType.WARNING, "Jumlah Tidak Valid", "Jumlah harus berupa angka positif.");
            return;
        }
        if (selectedBarang == null || selectedSupplier == null || batchField.getText().isBlank()) {
            showAlert(Alert.AlertType.WARNING, "Data Belum Lengkap", "Barang, supplier, jumlah, dan batch wajib diisi.");
            return;
        }

        String batch = batchField.getText().trim();
        java.time.LocalDate expiry = expiryPicker.getValue();
        String notes = notesArea.getText().trim();
        Integer userId = currentUser.getId();
        Task<InboundReceipt> task = new Task<>() {
            @Override
            protected InboundReceipt call() {
                return inboundReceiptService.createReceipt(selectedBarang.getId(), selectedSupplier.getId(),
                        quantity, batch, expiry, notes, userId);
            }
        };
        task.setOnSucceeded(event -> {
            if (task.getValue() == null) {
                showAlert(Alert.AlertType.ERROR, "Receipt Ditolak", "Data receipt tidak valid.");
                return;
            }
            loadInboundReceipts(table);
            showAlert(Alert.AlertType.INFORMATION, "Receipt Dibuat",
                    task.getValue().getReceiptCode() + " tersimpan dengan status QC_PENDING. Stok belum berubah.");
        });
        task.setOnFailed(event -> {
            log.error("Gagal membuat inbound receipt", task.getException());
            showAlert(Alert.AlertType.ERROR, "Gagal", "Inbound receipt gagal disimpan.");
        });
        Thread thread = new Thread(task, "inbound-receipt-create");
        thread.setDaemon(true);
        thread.start();
    }

    private void handleReceiptQc(TableView<InboundReceipt> table, boolean accepted) {
        InboundReceipt selected = table.getSelectionModel().getSelectedItem();
        if (selected == null) {
            showAlert(Alert.AlertType.WARNING, "Pilih Receipt", "Pilih satu receipt untuk diperiksa.");
            return;
        }
        TextInputDialog notesDialog = new TextInputDialog();
        notesDialog.setTitle(accepted ? "QC Lulus" : "QC Tolak");
        notesDialog.setHeaderText(selected.getReceiptCode());
        notesDialog.setContentText("Catatan QC:");
        notesDialog.showAndWait().ifPresent(notes -> {
            Integer userId = currentUser.getId();
            Task<Boolean> task = new Task<>() {
                @Override
                protected Boolean call() {
                    return inboundReceiptService.completeQualityCheck(selected.getId(), userId, accepted, notes);
                }
            };
            task.setOnSucceeded(event -> {
                if (task.getValue()) loadInboundReceipts(table);
                else showAlert(Alert.AlertType.ERROR, "QC Ditolak", "Receipt tidak lagi berstatus QC_PENDING.");
            });
            task.setOnFailed(event -> log.error("Gagal menyimpan hasil QC", task.getException()));
            Thread thread = new Thread(task, "inbound-qc");
            thread.setDaemon(true);
            thread.start();
        });
    }

    private void handleReceiptPutaway(TableView<InboundReceipt> table) {
        InboundReceipt selected = table.getSelectionModel().getSelectedItem();
        if (selected == null || rakItems == null) {
            showAlert(Alert.AlertType.WARNING, "Pilih Receipt", "Pilih receipt dan pastikan data rak sudah dimuat.");
            return;
        }
        ComboBox<String> rackBox = new ComboBox<>(FXCollections.observableArrayList(
                rakItems.stream().map(Rak::getKodeRak).toList()));
        Dialog<ButtonType> dialog = new Dialog<>();
        dialog.setTitle("Putaway " + selected.getReceiptCode());
        dialog.getDialogPane().setContent(new VBox(8, new Label("Lokasi tujuan"), rackBox));
        dialog.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);
        Optional<ButtonType> result = dialog.showAndWait();
        if (result.isEmpty() || result.get() != ButtonType.OK || rackBox.getValue() == null) return;

        Integer rackId = rakItems.stream().filter(rak -> rak.getKodeRak().equals(rackBox.getValue()))
                .map(Rak::getId).findFirst().orElse(null);
        Integer userId = currentUser.getId();
        Task<Boolean> task = new Task<>() {
            @Override
            protected Boolean call() {
                return inboundReceiptService.putaway(selected.getId(), rackId, userId);
            }
        };
        task.setOnSucceeded(event -> {
            if (task.getValue()) {
                loadInboundReceipts(table);
                loadTransaksi();
                showAlert(Alert.AlertType.INFORMATION, "Putaway Berhasil", "Stok batch sudah masuk ke ledger.");
            } else {
                showAlert(Alert.AlertType.ERROR, "Putaway Ditolak", "Receipt belum lulus QC atau kapasitas/saldo tidak valid.");
            }
        });
        task.setOnFailed(event -> log.error("Gagal melakukan putaway", task.getException()));
        Thread thread = new Thread(task, "inbound-putaway");
        thread.setDaemon(true);
        thread.start();
    }

    private void handleReceiptQr(TableView<InboundReceipt> table) {
        InboundReceipt selected = table.getSelectionModel().getSelectedItem();
        if (selected == null) {
            showAlert(Alert.AlertType.WARNING, "Pilih Receipt", "Pilih receipt untuk membuat QR label.");
            return;
        }
        Task<Path> task = new Task<>() {
            @Override
            protected Path call() throws Exception {
                return inboundReceiptService.generateQrLabel(selected.getId());
            }
        };
        task.setOnSucceeded(event -> {
            ImageView image = new ImageView(new Image(task.getValue().toUri().toString()));
            image.setFitWidth(320);
            image.setFitHeight(320);
            image.setPreserveRatio(true);
            Alert preview = new Alert(Alert.AlertType.INFORMATION);
            preview.setTitle("QR Label " + selected.getBatchCode());
            preview.setHeaderText(selected.getBarang().getKodeBarang() + " | Expiry: "
                    + (selected.getExpiryDate() == null ? "-" : selected.getExpiryDate()));
            preview.getDialogPane().setContent(image);
            preview.showAndWait();
        });
        task.setOnFailed(event -> showAlert(Alert.AlertType.ERROR, "QR Gagal", "QR label gagal dibuat."));
        Thread thread = new Thread(task, "inbound-qr-label");
        thread.setDaemon(true);
        thread.start();
    }

    private TableColumn<InboundReceipt, String> receiptColumn(
            String title, java.util.function.Function<InboundReceipt, String> value) {
        TableColumn<InboundReceipt, String> column = new TableColumn<>(title);
        column.setCellValueFactory(cell -> new SimpleStringProperty(value.apply(cell.getValue())));
        return column;
    }

    @FXML
    private void handleOpenDeliveryOrders() {
        if (currentUser == null || currentUser.getId() == null) {
            showAlert(Alert.AlertType.ERROR, "Sesi Tidak Valid", "Silakan login kembali untuk membuka delivery order.");
            return;
        }
        Dialog<ButtonType> dialog = new Dialog<>();
        dialog.setTitle("Delivery Orders");
        dialog.getDialogPane().getButtonTypes().add(ButtonType.CLOSE);
        TableView<DeliveryOrder> table = new TableView<>();
        table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_FLEX_LAST_COLUMN);
        table.getColumns().add(orderColumn("DO", DeliveryOrder::getOrderCode));
        table.getColumns().add(orderColumn("Customer", order -> order.getCustomer().getNamaCustomer()));
        table.getColumns().add(orderColumn("Lines", order -> String.valueOf(order.getLines().size())));
        table.getColumns().add(orderColumn("Qty", order -> String.valueOf(order.getLines().stream()
                .mapToInt(DeliveryOrderLine -> DeliveryOrderLine.getOrderedQuantity()).sum())));
        table.getColumns().add(orderColumn("Status", order -> order.getStatus().name()));
        table.setPlaceholder(new Label("Memuat delivery order..."));

        Button create = new Button("DO Baru");
        Button release = new Button("Release ke Picking");
        Button start = new Button("Mulai Picking");
        Button scanPick = new Button("Scan Pick");
        Button picked = new Button("Picking Selesai");
        Button packed = new Button("Packing Selesai");
        Button loaded = new Button("Loading Selesai");
        Button dispatch = new Button("Dispatch / GI");
        create.setOnAction(event -> handleCreateDeliveryOrder(table));
        release.setOnAction(event -> withSelectedOrder(table, "DO dirilis", orderId -> deliveryOrderService.releaseToPicking(orderId)));
        start.setOnAction(event -> withSelectedOrder(table, "Picking dimulai", orderId -> deliveryOrderService.startPicking(orderId)));
        scanPick.setOnAction(event -> handleRecordPick(table));
        picked.setOnAction(event -> withSelectedOrder(table, "Picking selesai", orderId -> deliveryOrderService.completePicking(orderId)));
        packed.setOnAction(event -> withSelectedOrder(table, "Packing selesai", orderId -> deliveryOrderService.completePacking(orderId)));
        loaded.setOnAction(event -> withSelectedOrder(table, "Loading selesai", orderId -> deliveryOrderService.completeLoading(orderId)));
        dispatch.setOnAction(event -> {
            Integer userId = currentUser.getId();
            withSelectedOrder(table, "Goods Issue berhasil dicatat", orderId -> deliveryOrderService.dispatch(orderId, userId));
        });

        javafx.scene.layout.FlowPane actions = new javafx.scene.layout.FlowPane(
            6, 6, create, release, start, scanPick, picked, packed, loaded, dispatch);
        VBox content = new VBox(10, actions, table);
        content.setPadding(new javafx.geometry.Insets(8));
        VBox.setVgrow(table, javafx.scene.layout.Priority.ALWAYS);
        dialog.getDialogPane().setContent(content);
        dialog.getDialogPane().setPrefSize(1120, 560);
        dialog.show();
        loadDeliveryOrders(table);
    }

    private void loadDeliveryOrders(TableView<DeliveryOrder> table) {
        Task<List<DeliveryOrder>> task = new Task<>() {
            @Override
            protected List<DeliveryOrder> call() {
                return deliveryOrderService.getRecentOrders();
            }
        };
        task.setOnSucceeded(event -> {
            table.getItems().setAll(task.getValue());
            table.setPlaceholder(new Label("Belum ada delivery order."));
        });
        task.setOnFailed(event -> {
            log.error("Gagal memuat delivery order", task.getException());
            table.setPlaceholder(new Label("Delivery order gagal dimuat."));
        });
        Thread thread = new Thread(task, "delivery-order-list");
        thread.setDaemon(true);
        thread.start();
    }

    private void handleCreateDeliveryOrder(TableView<DeliveryOrder> table) {
        if (barangItems == null || customerItems == null) {
            showAlert(Alert.AlertType.INFORMATION, "Data Sedang Dimuat", "Tunggu hingga master barang dan customer tersedia.");
            return;
        }
        List<Customer> activeCustomers = customerItems.stream()
                .filter(customer -> !"Nonaktif".equalsIgnoreCase(customer.getStatus())).toList();
        if (activeCustomers.isEmpty()) {
            showAlert(Alert.AlertType.WARNING, "Customer Tidak Tersedia", "Tambahkan customer aktif terlebih dahulu.");
            return;
        }

        Dialog<ButtonType> dialog = new Dialog<>();
        dialog.setTitle("Buat Delivery Order");
        ComboBox<Customer> customerBox = new ComboBox<>(FXCollections.observableArrayList(activeCustomers));
        ComboBox<Barang> barangBox = new ComboBox<>(FXCollections.observableArrayList(barangItems));
        TextField quantityField = new TextField("1");
        TextArea notesArea = new TextArea();
        notesArea.setPrefRowCount(2);
        Map<Integer, Integer> quantities = new LinkedHashMap<>();
        ObservableList<String> summary = FXCollections.observableArrayList();
        ListView<String> lineList = new ListView<>(summary);
        lineList.setPrefHeight(150);

        Button addLine = new Button("Tambah Barang");
        addLine.setOnAction(event -> {
            Barang selected = barangBox.getValue();
            int quantity;
            try {
                quantity = Integer.parseInt(quantityField.getText().trim());
                if (quantity <= 0) throw new NumberFormatException();
            } catch (NumberFormatException ex) {
                showAlert(Alert.AlertType.WARNING, "Jumlah Tidak Valid", "Jumlah harus berupa angka positif.");
                return;
            }
            if (selected == null) {
                showAlert(Alert.AlertType.WARNING, "Pilih Barang", "Pilih barang yang akan ditambahkan.");
                return;
            }
            quantities.merge(selected.getId(), quantity, Integer::sum);
            summary.setAll(quantities.entrySet().stream().map(entry -> {
                Barang item = barangItems.stream().filter(value -> value.getId().equals(entry.getKey()))
                        .findFirst().orElseThrow();
                return item.getKodeBarang() + " - " + item.getNamaBarang() + " | " + entry.getValue();
            }).toList());
        });

        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(8);
        grid.setPadding(new javafx.geometry.Insets(12));
        grid.addRow(0, new Label("Customer *"), customerBox);
        grid.addRow(1, new Label("Barang"), barangBox);
        grid.addRow(2, new Label("Jumlah"), quantityField, addLine);
        grid.addRow(3, new Label("Detail DO"), lineList);
        grid.addRow(4, new Label("Catatan"), notesArea);
        dialog.getDialogPane().setContent(grid);
        dialog.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);
        Optional<ButtonType> result = dialog.showAndWait();
        Customer customer = customerBox.getValue();
        if (result.isEmpty() || result.get() != ButtonType.OK) return;
        if (customer == null || quantities.isEmpty()) {
            showAlert(Alert.AlertType.WARNING, "DO Belum Lengkap", "Pilih customer dan tambahkan minimal satu barang.");
            return;
        }

        Integer userId = currentUser.getId();
        String notes = notesArea.getText().trim();
        Task<DeliveryOrder> task = new Task<>() {
            @Override
            protected DeliveryOrder call() {
                return deliveryOrderService.createDraft(customer.getId(), userId, Map.copyOf(quantities), notes);
            }
        };
        task.setOnSucceeded(event -> {
            if (task.getValue() == null) {
                showAlert(Alert.AlertType.ERROR, "DO Ditolak", "Customer, barang, atau jumlah tidak valid.");
            } else {
                loadDeliveryOrders(table);
            }
        });
        task.setOnFailed(event -> log.error("Gagal membuat delivery order", task.getException()));
        Thread thread = new Thread(task, "delivery-order-create");
        thread.setDaemon(true);
        thread.start();
    }

    private void withSelectedOrder(TableView<DeliveryOrder> table, String successMessage, OrderAction action) {
        DeliveryOrder selected = table.getSelectionModel().getSelectedItem();
        if (selected == null) {
            showAlert(Alert.AlertType.WARNING, "Pilih DO", "Pilih delivery order terlebih dahulu.");
            return;
        }
        Task<Boolean> task = new Task<>() {
            @Override
            protected Boolean call() {
                return action.run(selected.getId());
            }
        };
        task.setOnSucceeded(event -> {
            if (task.getValue()) {
                loadDeliveryOrders(table);
                if (successMessage.contains("Goods Issue")) showAlert(Alert.AlertType.INFORMATION, "Dispatch", successMessage);
            } else {
                showAlert(Alert.AlertType.ERROR, "Transisi Ditolak", "Periksa status DO dan ketersediaan stok di SLOC.");
            }
        });
        task.setOnFailed(event -> {
            log.error("Aksi DO gagal: {}", successMessage, task.getException());
            showAlert(Alert.AlertType.ERROR, "DO Gagal", "Aksi gagal dan perubahan stok dibatalkan.");
        });
        Thread thread = new Thread(task, "delivery-order-action");
        thread.setDaemon(true);
        thread.start();
    }

    private void handleRecordPick(TableView<DeliveryOrder> table) {
        DeliveryOrder order = table.getSelectionModel().getSelectedItem();
        if (order == null || rakItems == null) {
            showAlert(Alert.AlertType.WARNING, "Pilih DO", "Pilih DO dan pastikan daftar SLOC tersedia.");
            return;
        }
        ComboBox<DeliveryOrderLine> lineBox = new ComboBox<>(FXCollections.observableArrayList(order.getLines()));
        ComboBox<String> rackBox = new ComboBox<>(FXCollections.observableArrayList(
                rakItems.stream().map(Rak::getKodeRak).toList()));
        TextField skuField = new TextField();
        TextField batchField = new TextField();
        TextField quantityField = new TextField("1");
        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(8);
        grid.setPadding(new javafx.geometry.Insets(12));
        grid.addRow(0, new Label("DO line"), lineBox);
        grid.addRow(1, new Label("Scan SKU"), skuField);
        grid.addRow(2, new Label("Scan SLOC"), rackBox);
        grid.addRow(3, new Label("Scan batch"), batchField);
        grid.addRow(4, new Label("Jumlah scan"), quantityField);
        Dialog<ButtonType> dialog = new Dialog<>();
        dialog.setTitle("Pick Scan " + order.getOrderCode());
        dialog.getDialogPane().setContent(grid);
        dialog.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);
        Optional<ButtonType> result = dialog.showAndWait();
        if (result.isEmpty() || result.get() != ButtonType.OK) return;

        DeliveryOrderLine line = lineBox.getValue();
        String rackCode = rackBox.getValue();
        int quantity;
        try {
            quantity = Integer.parseInt(quantityField.getText().trim());
            if (quantity <= 0) throw new NumberFormatException();
        } catch (NumberFormatException ex) {
            showAlert(Alert.AlertType.WARNING, "Jumlah Tidak Valid", "Jumlah scan harus berupa angka positif.");
            return;
        }
        if (line == null || rackCode == null || skuField.getText().isBlank() || batchField.getText().isBlank()) {
            showAlert(Alert.AlertType.WARNING, "Scan Tidak Lengkap", "DO line, SKU, SLOC, batch, dan jumlah wajib diisi.");
            return;
        }
        Rak rack = rakItems.stream().filter(value -> value.getKodeRak().equals(rackCode)).findFirst().orElse(null);
        if (rack == null) return;
        Integer pickerId = currentUser == null ? null : currentUser.getId();
        if (pickerId == null) return;

        Task<Boolean> task = new Task<>() {
            @Override
            protected Boolean call() {
                return deliveryOrderService.recordPick(order.getId(), line.getId(), skuField.getText().trim(),
                        rackCode, batchField.getText().trim(), quantity, pickerId);
            }
        };
        task.setOnSucceeded(event -> {
            if (task.getValue()) {
                showAlert(Alert.AlertType.INFORMATION, "Scan Valid", quantity + " unit tercatat untuk " + line);
            } else {
                showAlert(Alert.AlertType.ERROR, "Scan Ditolak", "SKU, SLOC, batch, status DO, atau jumlah tidak cocok dengan reservasi.");
            }
        });
        task.setOnFailed(event -> {
            log.error("Pick scan gagal", task.getException());
            showAlert(Alert.AlertType.ERROR, "Scan Gagal", "Scan tidak dapat dicatat.");
        });
        Thread thread = new Thread(task, "delivery-order-pick-scan");
        thread.setDaemon(true);
        thread.start();
    }

    private TableColumn<DeliveryOrder, String> orderColumn(
            String title, java.util.function.Function<DeliveryOrder, String> value) {
        TableColumn<DeliveryOrder, String> column = new TableColumn<>(title);
        column.setCellValueFactory(cell -> new SimpleStringProperty(value.apply(cell.getValue())));
        return column;
    }

    @FunctionalInterface
    private interface OrderAction {
        boolean run(Integer orderId);
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
                    "✅ " + tipe + " berhasil dicatat!\n"
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
        List<Rak> rackItems = this.rakItems;
        if (currentUser == null || currentUser.getId() == null) {
            showAlert(Alert.AlertType.ERROR, "Sesi Tidak Valid", "Silakan login kembali sebelum transfer stok.");
            return;
        }
        if (barangItems == null || rakItems == null) {
            showAlert(Alert.AlertType.INFORMATION, "Data Sedang Dimuat", "Tunggu hingga data barang dan lokasi selesai dimuat.");
            return;
        }

        Dialog<ButtonType> dialog = new Dialog<>();
        dialog.setTitle("Transfer Stok Antar-SLOC");
        dialog.setHeaderText("Pindahkan stok tanpa mengubah total persediaan barang.");

        ComboBox<Barang> cbTransferBarang = new ComboBox<>(FXCollections.observableArrayList(barangItems));
        ComboBox<String> cbSource = new ComboBox<>();
        ComboBox<String> cbDestination = new ComboBox<>();
        ObservableList<String> rackCodes = FXCollections.observableArrayList(
                rakItems.stream().map(Rak::getKodeRak).toList());
        ObservableList<String> sourceCodes = FXCollections.observableArrayList(rackCodes);
        sourceCodes.add("Stok Belum Dipetakan");
        cbSource.setItems(sourceCodes);
        cbDestination.setItems(rackCodes);
        TextField tfTransferQuantity = new TextField("1");
        TextArea taTransferNotes = new TextArea();
        taTransferNotes.setPromptText("Alasan transfer (opsional)");
        taTransferNotes.setPrefRowCount(2);

        javafx.scene.layout.GridPane grid = new javafx.scene.layout.GridPane();
        grid.setHgap(10);
        grid.setVgap(10);
        grid.setPadding(new javafx.geometry.Insets(16));
        grid.add(new Label("Barang"), 0, 0);
        grid.add(cbTransferBarang, 1, 0);
        grid.add(new Label("Dari SLOC"), 0, 1);
        grid.add(cbSource, 1, 1);
        grid.add(new Label("Ke SLOC"), 0, 2);
        grid.add(cbDestination, 1, 2);
        grid.add(new Label("Jumlah"), 0, 3);
        grid.add(tfTransferQuantity, 1, 3);
        grid.add(new Label("Keterangan"), 0, 4);
        grid.add(taTransferNotes, 1, 4);
        dialog.getDialogPane().setContent(grid);
        dialog.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);

        dialog.showAndWait().ifPresent(result -> {
            if (result != ButtonType.OK) return;
            Barang barang = cbTransferBarang.getValue();
            String sourceCode = cbSource.getValue();
            String destinationCode = cbDestination.getValue();
            int quantity;
            try {
                quantity = Integer.parseInt(tfTransferQuantity.getText().trim());
                if (quantity <= 0) throw new NumberFormatException();
            } catch (NumberFormatException ex) {
                showAlert(Alert.AlertType.WARNING, "Jumlah Tidak Valid", "Jumlah harus berupa angka positif.");
                return;
            }
            if (barang == null || sourceCode == null || destinationCode == null) {
                showAlert(Alert.AlertType.WARNING, "Data Belum Lengkap", "Pilih barang, lokasi sumber, dan lokasi tujuan.");
                return;
            }

            Integer sourceRakId = "Stok Belum Dipetakan".equals(sourceCode) ? null : rackItems.stream()
                    .filter(rak -> rak.getKodeRak().equals(sourceCode)).map(Rak::getId).findFirst().orElse(null);
            Integer destinationRakId = rackItems.stream()
                    .filter(rak -> rak.getKodeRak().equals(destinationCode)).map(Rak::getId).findFirst().orElse(null);
            if (destinationRakId == null || (!"Stok Belum Dipetakan".equals(sourceCode) && sourceRakId == null)) {
                showAlert(Alert.AlertType.ERROR, "Lokasi Tidak Valid", "Lokasi yang dipilih tidak lagi tersedia.");
                return;
            }

            Integer userId = currentUser.getId();
            String notes = taTransferNotes.getText().trim();
            Task<Boolean> task = new Task<>() {
                @Override
                protected Boolean call() {
                    return transferService.transferStock(barang.getId(), sourceRakId, destinationRakId,
                            quantity, notes, userId);
                }
            };
            task.setOnSucceeded(event -> {
                if (task.getValue()) {
                    showAlert(Alert.AlertType.INFORMATION, "Transfer Berhasil",
                            quantity + " " + barang.getSatuan() + " dipindahkan dari " + sourceCode + " ke " + destinationCode + ".");
                    handleRefresh();
                } else {
                    showAlert(Alert.AlertType.ERROR, "Transfer Ditolak",
                            "Saldo sumber tidak cukup atau barang/lokasi/user sudah tidak tersedia.");
                }
            });
            task.setOnFailed(event -> {
                log.error("Gagal transfer antar-SLOC", task.getException());
                showAlert(Alert.AlertType.ERROR, "Transfer Gagal", "Transfer tidak dapat disimpan.");
            });
            Thread thread = new Thread(task, "sloc-transfer");
            thread.setDaemon(true);
            thread.start();
        });
    }

    @FXML
    private void handleRiwayatTransfer() {
        Dialog<ButtonType> dialog = new Dialog<>();
        dialog.setTitle("Riwayat Transfer SLOC");
        dialog.getDialogPane().getButtonTypes().add(ButtonType.CLOSE);
        TableView<InventoryTransfer> table = new TableView<>();
        table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_FLEX_LAST_COLUMN);
        table.getColumns().add(transferColumn("Kode", transfer -> transfer.getTransferCode()));
        table.getColumns().add(transferColumn("Barang", transfer -> transfer.getBarang().getNamaBarang()));
        table.getColumns().add(transferColumn("Dari", transfer -> transfer.getSourceRak() == null
                ? "Belum dipetakan" : transfer.getSourceRak().getKodeRak()));
        table.getColumns().add(transferColumn("Ke", transfer -> transfer.getDestinationRak().getKodeRak()));
        table.getColumns().add(transferColumn("Jumlah", transfer -> String.valueOf(transfer.getQuantity())));
        table.getColumns().add(transferColumn("Petugas", transfer -> transfer.getUser().getFullName()));
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");
        table.getColumns().add(transferColumn("Waktu", transfer -> transfer.getCreatedAt().format(formatter)));
        table.setPlaceholder(new Label("Memuat riwayat transfer..."));
        dialog.getDialogPane().setContent(table);
        dialog.getDialogPane().setPrefSize(850, 420);
        dialog.show();

        Task<List<InventoryTransfer>> task = new Task<>() {
            @Override
            protected List<InventoryTransfer> call() {
                return transferService.getRecentTransfers();
            }
        };
        task.setOnSucceeded(event -> {
            table.getItems().setAll(task.getValue());
            table.setPlaceholder(new Label("Belum ada transfer SLOC."));
        });
        task.setOnFailed(event -> {
            log.error("Gagal memuat riwayat transfer", task.getException());
            table.setPlaceholder(new Label("Riwayat transfer gagal dimuat."));
        });
        Thread thread = new Thread(task, "sloc-transfer-history");
        thread.setDaemon(true);
        thread.start();
    }

    private TableColumn<InventoryTransfer, String> transferColumn(
            String title, java.util.function.Function<InventoryTransfer, String> value) {
        TableColumn<InventoryTransfer, String> column = new TableColumn<>(title);
        column.setCellValueFactory(cell -> new SimpleStringProperty(value.apply(cell.getValue())));
        return column;
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
