package com.mycompany.tugas_akhir;

import com.mycompany.tugas_akhir.model.Barang;
import com.mycompany.tugas_akhir.model.Customer;
import com.mycompany.tugas_akhir.model.DeliveryOrder;
import com.mycompany.tugas_akhir.model.DeliveryOrderLine;
import com.mycompany.tugas_akhir.model.DeliveryOrderLotReservation;
import com.mycompany.tugas_akhir.model.DeliveryOrderPickScan;
import com.mycompany.tugas_akhir.model.DeliveryOrderFulfillmentAudit;
import com.mycompany.tugas_akhir.model.PickTask;
import com.mycompany.tugas_akhir.model.MaterialUsage;
import com.mycompany.tugas_akhir.model.MaterialUsageScan;
import com.mycompany.tugas_akhir.model.ReturnRequest;
import com.mycompany.tugas_akhir.model.ReturnRequestLine;
import com.mycompany.tugas_akhir.model.ReturnType;
import com.mycompany.tugas_akhir.model.PurchaseOrderLine;
import com.mycompany.tugas_akhir.model.StockTransferOrder;
import com.mycompany.tugas_akhir.model.StockTransferOrderScan;
import com.mycompany.tugas_akhir.model.Warehouse;
import com.mycompany.tugas_akhir.model.InventoryTransfer;
import com.mycompany.tugas_akhir.model.InboundReceipt;
import com.mycompany.tugas_akhir.model.PurchaseOrder;
import com.mycompany.tugas_akhir.model.PurchaseOrderLine;
import com.mycompany.tugas_akhir.model.SalesOrder;
import com.mycompany.tugas_akhir.model.SalesOrderLine;
import com.mycompany.tugas_akhir.model.Rak;
import com.mycompany.tugas_akhir.model.Supplier;
import com.mycompany.tugas_akhir.model.Transaksi;
import com.mycompany.tugas_akhir.model.User;
import com.mycompany.tugas_akhir.service.BarangService;
import com.mycompany.tugas_akhir.service.CustomerService;
import com.mycompany.tugas_akhir.service.DeliveryOrderService;
import com.mycompany.tugas_akhir.service.InventoryTransferService;
import com.mycompany.tugas_akhir.service.InboundReceiptService;
import com.mycompany.tugas_akhir.service.InboundReceiptService.PutawayRecommendation;
import com.mycompany.tugas_akhir.service.PurchaseOrderService;
import com.mycompany.tugas_akhir.service.SalesOrderService;
import com.mycompany.tugas_akhir.service.MaterialUsageService;
import com.mycompany.tugas_akhir.service.ReturnRequestService;
import com.mycompany.tugas_akhir.service.StockTransferOrderService;
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
    private final PurchaseOrderService purchaseOrderService;
    private final DeliveryOrderService deliveryOrderService;
    private final CustomerService customerService;
    private final SalesOrderService salesOrderService;
    private final MaterialUsageService materialUsageService;
    private final ReturnRequestService returnRequestService;
    private final StockTransferOrderService stockTransferOrderService;
    private final UserService userService;

    private User currentUser;
    private final ObservableList<Transaksi> txList = FXCollections.observableArrayList();
    private List<Barang> barangItems;
    private List<Rak> rakItems;
    private List<Supplier> supplierItems;
    private List<Customer> customerItems;

    private record PickerAssignmentOptions(List<User> pickers,
                                          List<com.mycompany.tugas_akhir.model.Section> sections) {}

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
                               PurchaseOrderService purchaseOrderService,
                               DeliveryOrderService deliveryOrderService,
                               CustomerService customerService,
                               SalesOrderService salesOrderService,
                               MaterialUsageService materialUsageService,
                               ReturnRequestService returnRequestService,
                               StockTransferOrderService stockTransferOrderService,
                               UserService userService) {
        this.transaksiService = transaksiService;
        this.barangService = barangService;
        this.supplierService = supplierService;
        this.sectionService = sectionService;
        this.transferService = transferService;
        this.inboundReceiptService = inboundReceiptService;
        this.purchaseOrderService = purchaseOrderService;
        this.deliveryOrderService = deliveryOrderService;
        this.customerService = customerService;
        this.salesOrderService = salesOrderService;
        this.materialUsageService = materialUsageService;
        this.returnRequestService = returnRequestService;
        this.stockTransferOrderService = stockTransferOrderService;
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

    private javafx.util.StringConverter<Customer> customerNameConverter() {
        return new javafx.util.StringConverter<>() {
            @Override
            public String toString(Customer customer) {
                return customer == null ? "" : customer.getNamaCustomer();
            }

            @Override
            public Customer fromString(String value) {
                return null;
            }
        };
    }

    private javafx.util.StringConverter<Barang> barangNameConverter() {
        return new javafx.util.StringConverter<>() {
            @Override
            public String toString(Barang barang) {
                return barang == null ? "" : barang.getKodeBarang() + " — " + barang.getNamaBarang();
            }

            @Override
            public Barang fromString(String value) {
                return null;
            }
        };
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
        table.getColumns().add(receiptColumn("PO", receipt -> receipt.getPurchaseOrderLine() == null
                ? "-" : receipt.getPurchaseOrderLine().getPurchaseOrder().getOrderCode()));
        table.getColumns().add(receiptColumn("Barang", receipt -> receipt.getBarang().getNamaBarang()));
        table.getColumns().add(receiptColumn("Supplier", receipt -> receipt.getSupplier().getNamaSupplier()));
        table.getColumns().add(receiptColumn("Jumlah", receipt -> receipt.getQuantity() + ""));
        table.getColumns().add(receiptColumn("Batch", InboundReceipt::getBatchCode));
        table.getColumns().add(receiptColumn("Expiry", receipt -> receipt.getExpiryDate() == null
                ? "-" : receipt.getExpiryDate().toString()));
        table.getColumns().add(receiptColumn("Status", receipt -> receipt.getStatus().name()));
        table.setPlaceholder(new Label("Memuat dokumen inbound..."));

        Button createButton = new Button("Receipt Baru");
        Button createPoButton = new Button("Purchase Order Baru");
        Button viewPoButton = new Button("Daftar Purchase Order");
        Button createPoReceiptButton = new Button("Receipt dari PO");
        Button approveButton = new Button("QC Lulus");
        Button rejectButton = new Button("QC Tolak");
        Button putawayButton = new Button("Putaway");
        Button qrButton = new Button("QR Label");
        createButton.setOnAction(event -> handleCreateReceipt(table));
        createPoButton.setOnAction(event -> handleCreatePurchaseOrder(table));
        viewPoButton.setOnAction(event -> handleViewPurchaseOrders());
        createPoReceiptButton.setOnAction(event -> handleCreatePoReceipt(table));
        approveButton.setOnAction(event -> handleReceiptQc(table, true));
        rejectButton.setOnAction(event -> handleReceiptQc(table, false));
        putawayButton.setOnAction(event -> handleReceiptPutaway(table));
        qrButton.setOnAction(event -> handleReceiptQr(table));

        VBox content = new VBox(10, new HBox(8, createPoButton, viewPoButton, createPoReceiptButton,
                createButton, approveButton, rejectButton, putawayButton, qrButton), table);
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

    private void handleCreatePurchaseOrder(TableView<InboundReceipt> receiptTable) {
        if (barangItems == null || supplierItems == null || currentUser == null || currentUser.getId() == null) {
            showAlert(Alert.AlertType.INFORMATION, "Data Belum Siap",
                    "Pastikan sesi aktif dan data barang serta supplier sudah dimuat.");
            return;
        }
        ComboBox<Supplier> supplierBox = new ComboBox<>(FXCollections.observableArrayList(supplierItems));
        ComboBox<Barang> barangBox = new ComboBox<>(FXCollections.observableArrayList(barangItems));
        TextField quantityField = new TextField("1");
        ObservableList<PurchaseOrderDraftLine> draftLines = FXCollections.observableArrayList();
        TableView<PurchaseOrderDraftLine> linesTable = new TableView<>(draftLines);
        linesTable.setPrefHeight(220);
        linesTable.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_FLEX_LAST_COLUMN);
        TableColumn<PurchaseOrderDraftLine, String> itemColumn = new TableColumn<>("Barang");
        itemColumn.setCellValueFactory(cell -> new SimpleStringProperty(
                cell.getValue().barang().getKodeBarang() + " - " + cell.getValue().barang().getNamaBarang()));
        TableColumn<PurchaseOrderDraftLine, String> quantityColumn = new TableColumn<>("Jumlah");
        quantityColumn.setCellValueFactory(cell -> new SimpleStringProperty(
                String.valueOf(cell.getValue().quantity())));
        linesTable.getColumns().add(itemColumn);
        linesTable.getColumns().add(quantityColumn);
        Button addLineButton = new Button("Tambah Barang");
        Button removeLineButton = new Button("Hapus Baris");
        addLineButton.setOnAction(event -> {
            Barang item = barangBox.getValue();
            int quantity;
            try {
                quantity = Integer.parseInt(quantityField.getText().trim());
                if (quantity <= 0) throw new NumberFormatException();
            } catch (NumberFormatException ex) {
                showAlert(Alert.AlertType.WARNING, "Jumlah Tidak Valid", "Jumlah PO harus bilangan bulat positif.");
                return;
            }
            if (item == null) {
                showAlert(Alert.AlertType.WARNING, "Pilih Barang", "Pilih barang untuk ditambahkan ke PO.");
                return;
            }
            for (int index = 0; index < draftLines.size(); index++) {
                if (draftLines.get(index).barang().getId().equals(item.getId())) {
                    PurchaseOrderDraftLine existing = draftLines.get(index);
                    try {
                        draftLines.set(index, new PurchaseOrderDraftLine(existing.barang(),
                                Math.addExact(existing.quantity(), quantity)));
                    } catch (ArithmeticException ex) {
                        showAlert(Alert.AlertType.WARNING, "Jumlah Terlalu Besar",
                                "Total jumlah untuk barang ini melebihi batas yang didukung.");
                    }
                    return;
                }
            }
            draftLines.add(new PurchaseOrderDraftLine(item, quantity));
            quantityField.setText("1");
        });
        removeLineButton.setOnAction(event ->
                draftLines.remove(linesTable.getSelectionModel().getSelectedItem()));

        TextArea notesArea = new TextArea();
        notesArea.setPromptText("Catatan PO (opsional)");
        notesArea.setPrefRowCount(2);
        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(8);
        grid.setPadding(new javafx.geometry.Insets(12));
        grid.addRow(0, new Label("Supplier *"), supplierBox);
        grid.addRow(1, new Label("Barang"), barangBox);
        grid.addRow(2, new Label("Jumlah"), quantityField);
        grid.addRow(3, addLineButton, removeLineButton);
        grid.add(linesTable, 0, 4, 2, 1);
        grid.addRow(5, new Label("Catatan"), notesArea);

        Dialog<ButtonType> dialog = new Dialog<>();
        dialog.setTitle("Buat Purchase Order");
        dialog.setHeaderText("Buat PO berdasarkan supplier dan daftar barang yang dipesan.");
        dialog.getDialogPane().setContent(grid);
        dialog.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);
        Optional<ButtonType> result = dialog.showAndWait();
        if (result.isEmpty() || result.get() != ButtonType.OK) return;
        Supplier supplier = supplierBox.getValue();
        if (supplier == null || draftLines.isEmpty()) {
            showAlert(Alert.AlertType.WARNING, "PO Belum Lengkap",
                    "Pilih supplier dan tambahkan minimal satu barang.");
            return;
        }
        Map<Integer, Integer> requested = new LinkedHashMap<>();
        draftLines.forEach(line -> requested.put(line.barang().getId(), line.quantity()));
        Integer userId = currentUser.getId();
        String notes = notesArea.getText().trim();
        Task<PurchaseOrder> task = new Task<>() {
            @Override
            protected PurchaseOrder call() {
                return purchaseOrderService.createPurchaseOrder(
                        supplier.getId(), userId, Map.copyOf(requested), notes);
            }
        };
        task.setOnSucceeded(event -> {
            PurchaseOrder order = task.getValue();
            if (order == null) {
                showAlert(Alert.AlertType.ERROR, "PO Ditolak", "Purchase order tidak valid.");
                return;
            }
            loadInboundReceipts(receiptTable);
            showAlert(Alert.AlertType.INFORMATION, "PO Dibuat",
                    order.getOrderCode() + " dibuat dan menunggu persetujuan.");
        });
        task.setOnFailed(event -> {
            log.error("Gagal membuat purchase order", task.getException());
            showAlert(Alert.AlertType.ERROR, "Gagal", "Purchase order tidak dapat disimpan.");
        });
        Thread thread = new Thread(task, "purchase-order-create");
        thread.setDaemon(true);
        thread.start();
    }

    private void handleViewPurchaseOrders() {
        Task<List<PurchaseOrder>> task = new Task<>() {
            @Override
            protected List<PurchaseOrder> call() {
                return purchaseOrderService.getRecentPurchaseOrders();
            }
        };
        task.setOnSucceeded(event -> {
            TableView<PurchaseOrder> table = new TableView<>(
                    FXCollections.observableArrayList(task.getValue()));
            table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_FLEX_LAST_COLUMN);
            table.getColumns().add(purchaseOrderColumn("PO", PurchaseOrder::getOrderCode));
            table.getColumns().add(purchaseOrderColumn("Supplier",
                    order -> order.getSupplier().getNamaSupplier()));
            table.getColumns().add(purchaseOrderColumn("Barang", order -> String.valueOf(order.getLines().size())));
            table.getColumns().add(purchaseOrderColumn("Dipesan", order -> String.valueOf(order.getLines().stream()
                    .mapToInt(PurchaseOrderLine::getOrderedQuantity).sum())));
            table.getColumns().add(purchaseOrderColumn("Diterima", order -> String.valueOf(order.getLines().stream()
                    .mapToInt(PurchaseOrderLine::getReceivedQuantity).sum())));
            table.getColumns().add(purchaseOrderColumn("Status", order -> order.getStatus().name()));
            table.getColumns().add(purchaseOrderColumn("Ditinjau Oleh", order -> order.getReviewedBy() == null
                    ? "-" : order.getReviewedBy().getFullName()));
            table.setPlaceholder(new Label("Belum ada purchase order."));
            Dialog<ButtonType> dialog = new Dialog<>();
            dialog.setTitle("Daftar Purchase Order");
            dialog.getDialogPane().getButtonTypes().add(ButtonType.CLOSE);
            Button approveButton = new Button("Setujui");
            Button rejectButton = new Button("Tolak");
            approveButton.setDisable(true);
            rejectButton.setDisable(true);
            table.getSelectionModel().selectedItemProperty().addListener((observable, oldValue, selected) -> {
                boolean pending = selected != null
                        && selected.getStatus() == com.mycompany.tugas_akhir.model.PurchaseOrderStatus.PENDING_APPROVAL;
                boolean canReview = pending && currentUser != null && currentUser.getId() != null
                        && !currentUser.getId().equals(selected.getCreatedBy().getId());
                approveButton.setDisable(!canReview);
                rejectButton.setDisable(!canReview);
            });
            approveButton.setOnAction(action -> reviewPurchaseOrder(table, true));
            rejectButton.setOnAction(action -> reviewPurchaseOrder(table, false));
            dialog.getDialogPane().setContent(new VBox(10, new HBox(8, approveButton, rejectButton), table));
            dialog.getDialogPane().setPrefSize(800, 420);
            dialog.showAndWait();
        });
        task.setOnFailed(event -> {
            log.error("Gagal memuat purchase order", task.getException());
            showAlert(Alert.AlertType.ERROR, "Gagal", "Daftar purchase order tidak dapat dimuat.");
        });
        Thread thread = new Thread(task, "purchase-order-list");
        thread.setDaemon(true);
        thread.start();
    }

    private void reviewPurchaseOrder(TableView<PurchaseOrder> table, boolean approve) {
        PurchaseOrder selected = table.getSelectionModel().getSelectedItem();
        if (selected == null || currentUser == null || currentUser.getId() == null) return;
        String reason = null;
        if (!approve) {
            TextInputDialog reasonDialog = new TextInputDialog();
            reasonDialog.setTitle("Tolak Purchase Order");
            reasonDialog.setHeaderText("Masukkan alasan penolakan");
            reasonDialog.setContentText("Alasan:");
            reason = reasonDialog.showAndWait().orElse(null);
            if (reason == null) return;
            if (reason.isBlank()) {
                showAlert(Alert.AlertType.WARNING, "Alasan Wajib", "Alasan penolakan tidak boleh kosong.");
                return;
            }
        }
        final String rejectionReason = reason;
        Integer reviewerId = currentUser.getId();
        Task<Boolean> task = new Task<>() {
            @Override
            protected Boolean call() {
                return approve
                        ? purchaseOrderService.approvePurchaseOrder(selected.getId(), reviewerId)
                        : purchaseOrderService.rejectPurchaseOrder(selected.getId(), reviewerId, rejectionReason);
            }
        };
        task.setOnSucceeded(event -> {
            if (!task.getValue()) {
                showAlert(Alert.AlertType.ERROR, "Tinjauan Ditolak",
                        "PO mungkin sudah ditinjau atau pembuat PO tidak dapat menyetujui PO sendiri.");
                return;
            }
            showAlert(Alert.AlertType.INFORMATION, "Purchase Order",
                    "PO " + selected.getOrderCode() + (approve ? " disetujui." : " ditolak."));
            Task<List<PurchaseOrder>> refreshTask = new Task<>() {
                @Override
                protected List<PurchaseOrder> call() {
                    return purchaseOrderService.getRecentPurchaseOrders();
                }
            };
            refreshTask.setOnSucceeded(refreshEvent -> table.getItems().setAll(refreshTask.getValue()));
            refreshTask.setOnFailed(refreshEvent ->
                    log.error("Gagal memuat ulang purchase order", refreshTask.getException()));
            Thread refreshThread = new Thread(refreshTask, "purchase-order-refresh");
            refreshThread.setDaemon(true);
            refreshThread.start();
        });
        task.setOnFailed(event -> {
            log.error("Gagal meninjau purchase order {}", selected.getOrderCode(), task.getException());
            showAlert(Alert.AlertType.ERROR, "Gagal", "Purchase order tidak dapat ditinjau.");
        });
        Thread thread = new Thread(task, "purchase-order-review");
        thread.setDaemon(true);
        thread.start();
    }

    private void handleCreatePoReceipt(TableView<InboundReceipt> receiptTable) {
        Task<List<PurchaseOrderLine>> task = new Task<>() {
            @Override
            protected List<PurchaseOrderLine> call() {
                return purchaseOrderService.getOpenLines();
            }
        };
        task.setOnSucceeded(event -> {
            if (task.getValue().isEmpty()) {
                showAlert(Alert.AlertType.INFORMATION, "Tidak Ada PO Terbuka",
                        "Semua baris PO sudah diterima atau belum ada purchase order.");
                return;
            }
            showCreatePoReceiptDialog(receiptTable, task.getValue());
        });
        task.setOnFailed(event -> {
            log.error("Gagal memuat sisa baris purchase order", task.getException());
            showAlert(Alert.AlertType.ERROR, "Gagal", "Baris purchase order tidak dapat dimuat.");
        });
        Thread thread = new Thread(task, "purchase-order-open-lines");
        thread.setDaemon(true);
        thread.start();
    }

    private void showCreatePoReceiptDialog(TableView<InboundReceipt> receiptTable,
                                           List<PurchaseOrderLine> openLines) {
        ComboBox<PurchaseOrderLine> lineBox = new ComboBox<>(FXCollections.observableArrayList(openLines));
        TextField quantityField = new TextField("1");
        TextField batchField = new TextField();
        DatePicker expiryPicker = new DatePicker();
        TextArea notesArea = new TextArea();
        notesArea.setPrefRowCount(2);
        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(8);
        grid.setPadding(new javafx.geometry.Insets(12));
        grid.addRow(0, new Label("PO / Barang *"), lineBox);
        grid.addRow(1, new Label("Jumlah *"), quantityField);
        grid.addRow(2, new Label("Kode Batch *"), batchField);
        grid.addRow(3, new Label("Expiry"), expiryPicker);
        grid.addRow(4, new Label("Catatan"), notesArea);
        Dialog<ButtonType> dialog = new Dialog<>();
        dialog.setTitle("Receipt dari Purchase Order");
        dialog.getDialogPane().setContent(grid);
        dialog.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);
        Optional<ButtonType> result = dialog.showAndWait();
        if (result.isEmpty() || result.get() != ButtonType.OK) return;
        PurchaseOrderLine line = lineBox.getValue();
        int quantity;
        try {
            quantity = Integer.parseInt(quantityField.getText().trim());
            if (quantity <= 0) throw new NumberFormatException();
        } catch (NumberFormatException ex) {
            showAlert(Alert.AlertType.WARNING, "Jumlah Tidak Valid", "Jumlah receipt harus bilangan bulat positif.");
            return;
        }
        if (line == null || batchField.getText().isBlank()) {
            showAlert(Alert.AlertType.WARNING, "Receipt Belum Lengkap",
                    "Pilih baris PO dan masukkan kode batch.");
            return;
        }

        Integer userId = currentUser == null ? null : currentUser.getId();
        String batch = batchField.getText().trim();
        java.time.LocalDate expiry = expiryPicker.getValue();
        String notes = notesArea.getText().trim();
        Task<InboundReceipt> createTask = new Task<>() {
            @Override
            protected InboundReceipt call() {
                return inboundReceiptService.createReceiptFromPurchaseOrder(
                        line.getId(), quantity, batch, expiry, notes, userId);
            }
        };
        createTask.setOnSucceeded(event -> {
            InboundReceipt receipt = createTask.getValue();
            if (receipt == null) {
                showAlert(Alert.AlertType.ERROR, "Receipt Ditolak",
                        "Jumlah melebihi sisa PO, atau data receipt sudah tidak valid.");
                return;
            }
            loadInboundReceipts(receiptTable);
            showAlert(Alert.AlertType.INFORMATION, "Receipt Dibuat",
                    receipt.getReceiptCode() + " dibuat dari " + line.getPurchaseOrder().getOrderCode()
                            + " dengan status QC_PENDING.");
        });
        createTask.setOnFailed(event -> {
            log.error("Gagal membuat receipt dari purchase order", createTask.getException());
            showAlert(Alert.AlertType.ERROR, "Gagal", "Receipt dari purchase order tidak dapat disimpan.");
        });
        Thread thread = new Thread(createTask, "purchase-order-receipt-create");
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
        if (selected == null || currentUser == null || currentUser.getId() == null) {
            showAlert(Alert.AlertType.WARNING, "Pilih Receipt", "Pilih receipt dan pastikan sesi pengguna aktif.");
            return;
        }
        Task<List<PutawayRecommendation>> task = new Task<>() {
            @Override
            protected List<PutawayRecommendation> call() {
                return inboundReceiptService.recommendPutawayLocations(selected.getId());
            }
        };
        task.setOnSucceeded(event -> showPutawayRecommendations(table, selected, task.getValue()));
        task.setOnFailed(event -> {
            log.error("Gagal menyusun rekomendasi putaway", task.getException());
            showAlert(Alert.AlertType.ERROR, "Rekomendasi Gagal",
                    "Sistem tidak dapat memeriksa lokasi putaway yang tersedia.");
        });
        Thread thread = new Thread(task, "inbound-putaway-recommendations");
        thread.setDaemon(true);
        thread.start();
    }

    private void showPutawayRecommendations(TableView<InboundReceipt> table, InboundReceipt selected,
                                             List<PutawayRecommendation> recommendations) {
        if (recommendations.isEmpty()) {
            showAlert(Alert.AlertType.WARNING, "Tidak Ada Lokasi Sesuai",
                    "Tidak ada rak dengan kapasitas cukup untuk jumlah receipt ini.");
            return;
        }
        ComboBox<PutawayRecommendation> rackBox =
                new ComboBox<>(FXCollections.observableArrayList(recommendations));
        rackBox.setConverter(new javafx.util.StringConverter<>() {
            @Override
            public String toString(PutawayRecommendation recommendation) {
                if (recommendation == null) return "";
                return recommendation.rack().getKodeRak() + " — "
                        + recommendation.getPreferenceLabel() + " — kapasitas tersedia "
                        + recommendation.availableCapacity();
            }

            @Override
            public PutawayRecommendation fromString(String value) {
                return recommendations.stream()
                        .filter(recommendation -> toString(recommendation).equals(value))
                        .findFirst().orElse(null);
            }
        });
        rackBox.getSelectionModel().selectFirst();
        TextField itemQrField = new TextField();
        itemQrField.setPromptText("Pindai QR item / batch / expiry");
        TextField locationBarcodeField = new TextField();
        locationBarcodeField.setPromptText("Pindai barcode lokasi SLOC");
        Button showLocationQr = new Button("Tampilkan QR Lokasi");
        showLocationQr.setOnAction(event -> {
            PutawayRecommendation recommendation = rackBox.getValue();
            if (recommendation == null) return;
            Task<Path> qrTask = new Task<>() {
                @Override
                protected Path call() throws Exception {
                    return inboundReceiptService.generateLocationQrLabel(recommendation.rack().getId());
                }
            };
            qrTask.setOnSucceeded(qrEvent -> {
                ImageView image = new ImageView(new Image(qrTask.getValue().toUri().toString()));
                image.setFitWidth(300);
                image.setFitHeight(300);
                image.setPreserveRatio(true);
                Alert preview = new Alert(Alert.AlertType.INFORMATION);
                preview.setTitle("QR Lokasi " + recommendation.rack().getKodeRak());
                preview.setHeaderText("SLOC " + recommendation.rack().getKodeRak());
                preview.setContentText("Gunakan QR ini pada label lokasi agar barcode scanner dapat mengisinya.");
                preview.getDialogPane().setContent(new VBox(8, image, new Label(qrTask.getValue().toString())));
                preview.showAndWait();
            });
            qrTask.setOnFailed(qrEvent -> {
                log.error("Gagal membuat QR lokasi", qrTask.getException());
                showAlert(Alert.AlertType.ERROR, "QR Lokasi Gagal", "QR lokasi tidak dapat dibuat.");
            });
            Thread qrThread = new Thread(qrTask, "inbound-location-qr");
            qrThread.setDaemon(true);
            qrThread.start();
        });
        Label guidance = new Label("Urutan saran memakai target kategori section, kapasitas saat ini, "
                + "dan kode rak. Kecocokan kategori adalah preferensi; CBM/volume barang belum tersedia. "
                + "Barcode scanner keyboard dapat mengisi kolom scan.");
        guidance.setWrapText(true);
        Dialog<ButtonType> dialog = new Dialog<>();
        dialog.setTitle("Putaway " + selected.getReceiptCode());
        dialog.getDialogPane().setContent(new VBox(8,
                new Label("Lokasi tujuan (saran terbaik dipilih terlebih dahulu)"),
                rackBox,
                new Label("Scan QR barang dari receipt"),
                itemQrField,
                new Label("Scan barcode lokasi tujuan"),
                locationBarcodeField,
                showLocationQr,
                guidance));
        dialog.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);
        Optional<ButtonType> result = dialog.showAndWait();
        if (result.isEmpty() || result.get() != ButtonType.OK || rackBox.getValue() == null) return;

        Integer rackId = rackBox.getValue().rack().getId();
        Integer userId = currentUser.getId();
        String itemQrPayload = itemQrField.getText();
        String locationBarcode = locationBarcodeField.getText();
        String selectedRackCode = rackBox.getValue().rack().getKodeRak();
        Task<Boolean> task = new Task<>() {
            @Override
            protected Boolean call() {
                return inboundReceiptService.putawayWithScannedBarcodes(
                        selected.getId(), itemQrPayload, locationBarcode, rackId, userId);
            }
        };
        task.setOnSucceeded(event -> {
            if (task.getValue()) {
                loadInboundReceipts(table);
                loadTransaksi();
                showAlert(Alert.AlertType.INFORMATION, "Putaway Berhasil", "Stok batch sudah masuk ke ledger.");
            } else {
                showAlert(Alert.AlertType.ERROR, "Putaway Ditolak",
                        "Scan barang harus cocok dengan receipt dan scan lokasi harus cocok dengan "
                                + selectedRackCode + ". Pastikan receipt lulus QC dan kapasitas lokasi cukup.");
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

    private TableColumn<PurchaseOrder, String> purchaseOrderColumn(
            String title, java.util.function.Function<PurchaseOrder, String> value) {
        TableColumn<PurchaseOrder, String> column = new TableColumn<>(title);
        column.setCellValueFactory(cell -> new SimpleStringProperty(value.apply(cell.getValue())));
        return column;
    }

    private record PurchaseOrderDraftLine(Barang barang, int quantity) {}

    @FXML
    private void handleOpenDeliveryOrders() {
        if (currentUser == null || currentUser.getId() == null) {
            showAlert(Alert.AlertType.ERROR, "Sesi Tidak Valid", "Silakan login kembali untuk membuka delivery order.");
            return;
        }
        Dialog<ButtonType> dialog = new Dialog<>();
        dialog.setTitle("Pesanan Pengiriman");
        dialog.getDialogPane().getButtonTypes().add(ButtonType.CLOSE);
        TableView<DeliveryOrder> table = new TableView<>();
        table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_FLEX_LAST_COLUMN);
        table.getColumns().add(orderColumn("No. DO", DeliveryOrder::getOrderCode));
        table.getColumns().add(orderColumn("Customer", order -> order.getCustomer().getNamaCustomer()));
        table.getColumns().add(orderColumn("No. SO", order -> order.getSalesOrder() == null
                ? "-" : order.getSalesOrder().getOrderCode()));
        table.getColumns().add(orderColumn("Jenis barang", order -> String.valueOf(order.getLines().size())));
        table.getColumns().add(orderColumn("Sisa (unit)", order -> String.valueOf(order.getLines().stream()
                .mapToInt(DeliveryOrderLine::getRemainingQuantity).sum())));
        table.getColumns().add(orderColumn("Status", order -> deliveryOrderStatusLabel(order.getStatus())));
        table.setPlaceholder(new Label("Belum ada pesanan pengiriman. Pilih “DO Baru” atau buat DO dari Sales Order."));
        table.setPrefHeight(300);

        Button create = deliveryOrderButton("Buat DO", "Buat pesanan pengiriman langsung untuk customer.");
        Button salesOrders = deliveryOrderButton("Sales Order (SO)", "Lihat SO, buat SO, atau konversi sisa SO menjadi DO.");
        Button release = deliveryOrderButton("Reservasi stok", "Cek ketersediaan stok dan siapkan DO untuk picking.");
        Button assignTask = deliveryOrderButton("Tugaskan picker", "Pilih petugas dan zona setelah stok berhasil direservasi.");
        Button taskDetails = deliveryOrderButton("Lihat tugas picking", "Lihat picker, zona, dan status tugas DO.");
        Button reservations = deliveryOrderButton("Lihat alokasi stok", "Periksa SLOC, batch, dan jumlah yang dialokasikan.");
        Button start = deliveryOrderButton("Mulai picking", "Mulai picking setelah picker ditugaskan.");
        Button scanPick = deliveryOrderButton("Catat scan barang", "Catat QR barang dan barcode SLOC yang dipindai picker.");
        Button scanHistory = deliveryOrderButton("Riwayat scan", "Tinjau hasil scan barang untuk DO terpilih.");
        Button picked = deliveryOrderButton("Selesaikan picking", "Simpan jumlah yang berhasil diambil dan lanjutkan ke packing.");
        Button packed = deliveryOrderButton("Konfirmasi packing", "Masukkan jumlah aktual yang dikemas per barang.");
        Button loaded = deliveryOrderButton("Konfirmasi loading", "Masukkan jumlah aktual yang dimuat per barang.");
        Button fulfillmentAudit = deliveryOrderButton("Audit pemenuhan", "Tinjau catatan jumlah packing dan loading.");
        Button dispatch = deliveryOrderButton("Dispatch / Goods Issue", "Posting barang yang dimuat sebagai Goods Issue.");
        create.setOnAction(event -> handleCreateDeliveryOrder(table));
        salesOrders.setOnAction(event -> handleSalesOrders(table));
        assignTask.setOnAction(event -> handleAssignPicker(table));
        taskDetails.setOnAction(event -> handleViewPickTasks(table));
        reservations.setOnAction(event -> handleViewReservations(table));
        fulfillmentAudit.setOnAction(event -> handleViewFulfillmentAudit(table));
        release.setOnAction(event -> withSelectedOrder(table, "DO dirilis", orderId -> deliveryOrderService.releaseToPicking(orderId)));
        start.setOnAction(event -> withSelectedOrder(table, "Picking dimulai", orderId -> deliveryOrderService.startPicking(orderId)));
        scanPick.setOnAction(event -> handleRecordPick(table));
        picked.setOnAction(event -> withSelectedOrder(table, "Picking selesai", orderId -> deliveryOrderService.completePicking(orderId)));
        packed.setOnAction(event -> handleCompletePacking(table));
        loaded.setOnAction(event -> handleCompleteLoading(table));
        scanHistory.setOnAction(event -> handlePickScanHistory(table));
        dispatch.setOnAction(event -> {
            DeliveryOrder selected = table.getSelectionModel().getSelectedItem();
            if (selected == null) return;
            Alert confirmation = new Alert(Alert.AlertType.CONFIRMATION);
            confirmation.setTitle("Konfirmasi Goods Issue");
            confirmation.setHeaderText("Kirim barang dan catat Goods Issue?");
            confirmation.setContentText("DO " + selected.getOrderCode() + " akan mengurangi stok sesuai jumlah yang dimuat. "
                    + "Pastikan hasil loading sudah benar; tindakan ini mencatat pengeluaran stok.");
            styleDeliveryOrderDialog(confirmation);
            if (confirmation.showAndWait().filter(ButtonType.OK::equals).isEmpty()) return;
            Integer userId = currentUser.getId();
            withSelectedOrder(table, "Goods Issue berhasil dicatat",
                    orderId -> deliveryOrderService.dispatch(orderId, userId));
        });

        Label selectionHint = new Label("Pilih pesanan pada tabel untuk melihat tindakan yang tersedia.");
        selectionHint.getStyleClass().add("do-selection-hint");
        List<Button> selectedOrderActions = List.of(release, assignTask, taskDetails, reservations, start,
                scanPick, scanHistory, picked, packed, loaded, fulfillmentAudit, dispatch);
        table.getSelectionModel().selectedItemProperty().addListener((observable, oldOrder, selectedOrder) -> {
            selectionHint.setText(selectedOrder == null
                    ? "Pilih pesanan pada tabel untuk melihat tindakan yang tersedia."
                    : selectedOrder.getOrderCode() + "  ·  " + deliveryOrderStatusLabel(selectedOrder.getStatus())
                    + "  ·  " + selectedOrder.getLines().size() + " baris  ·  "
                    + selectedOrder.getLines().stream().mapToInt(DeliveryOrderLine::getRemainingQuantity).sum()
                    + " unit tersisa");
            updateDeliveryOrderActionState(selectedOrder, release, assignTask, taskDetails, reservations,
                    start, scanPick, scanHistory, picked, packed, loaded, fulfillmentAudit, dispatch);
        });
        updateDeliveryOrderActionState(null, release, assignTask, taskDetails, reservations, start,
                scanPick, scanHistory, picked, packed, loaded, fulfillmentAudit, dispatch);

        VBox createGroup = deliveryOrderActionGroup("Buat pesanan", create, salesOrders);
        VBox preparationGroup = deliveryOrderActionGroup("Persiapan", release, assignTask,
                taskDetails, reservations);
        VBox pickingGroup = deliveryOrderActionGroup("Picking", start, scanPick, scanHistory, picked);
        VBox fulfillmentGroup = deliveryOrderActionGroup("Packing & pengiriman", packed, loaded,
                fulfillmentAudit, dispatch);
        javafx.scene.layout.FlowPane actions = new javafx.scene.layout.FlowPane(
                12, 12, createGroup, preparationGroup, pickingGroup, fulfillmentGroup);
        actions.setPrefWrapLength(980);

        Button refresh = new Button("Muat ulang daftar");
        refresh.getStyleClass().add("btn-outline");
        refresh.setTooltip(new Tooltip("Muat ulang status dan daftar pesanan pengiriman."));
        refresh.setOnAction(event -> loadDeliveryOrders(table));
        HBox tableHeader = new HBox(10, new Label("Daftar pesanan"), new javafx.scene.layout.Region(), refresh);
        tableHeader.setAlignment(javafx.geometry.Pos.CENTER_LEFT);
        HBox.setHgrow(tableHeader.getChildren().get(1), javafx.scene.layout.Priority.ALWAYS);

        VBox content = new VBox(12, actions, selectionHint, tableHeader, table);
        content.getStyleClass().add("delivery-order-content");
        content.setPadding(new javafx.geometry.Insets(12));
        VBox.setVgrow(table, javafx.scene.layout.Priority.ALWAYS);
        dialog.getDialogPane().setContent(content);
        dialog.setResizable(true);
        dialog.getDialogPane().setPrefSize(1060, 650);
        dialog.getDialogPane().setMinWidth(900);
        dialog.getDialogPane().setMinHeight(580);
        dialog.setOnShown(event -> {
            javafx.stage.Window window = dialog.getDialogPane().getScene().getWindow();
            if (window instanceof javafx.stage.Stage stage) {
                stage.setWidth(1080);
                stage.setHeight(700);
                stage.setMinWidth(940);
                stage.setMinHeight(620);
            }
        });
        dialog.show();
        loadDeliveryOrders(table);
    }

    private Button deliveryOrderButton(String text, String help) {
        Button button = new Button(text);
        button.getStyleClass().add("do-action-button");
        button.setTooltip(new Tooltip(help));
        return button;
    }

    private void styleDeliveryOrderDialog(Dialog<?> dialog) {
        dialog.getDialogPane().getStylesheets().add(
                getClass().getResource("/css/dashboard.css").toExternalForm());
    }

    private void configureDeliveryOrderFormGrid(GridPane grid) {
        grid.setHgap(12);
        grid.setVgap(10);
        grid.setPadding(new javafx.geometry.Insets(16));

        javafx.scene.layout.ColumnConstraints labelColumn = new javafx.scene.layout.ColumnConstraints();
        labelColumn.setMinWidth(145);
        labelColumn.setPrefWidth(155);
        labelColumn.setMaxWidth(175);
        javafx.scene.layout.ColumnConstraints inputColumn = new javafx.scene.layout.ColumnConstraints();
        inputColumn.setHgrow(javafx.scene.layout.Priority.ALWAYS);
        inputColumn.setFillWidth(true);
        grid.getColumnConstraints().setAll(labelColumn, inputColumn);

        for (javafx.scene.Node node : grid.getChildren()) {
            Integer column = GridPane.getColumnIndex(node);
            if (column != null && column == 0 && node instanceof Label label) {
                label.getStyleClass().add("form-label");
            } else if (column != null && column == 1) {
                GridPane.setHgrow(node, javafx.scene.layout.Priority.ALWAYS);
                if (node instanceof javafx.scene.layout.Region region) {
                    region.setMaxWidth(Double.MAX_VALUE);
                }
            }
        }
    }

    private VBox deliveryOrderActionGroup(String title, Button... buttons) {
        Label heading = new Label(title);
        heading.getStyleClass().add("do-action-heading");
        javafx.scene.layout.FlowPane buttonPane = new javafx.scene.layout.FlowPane(6, 6, buttons);
        VBox group = new VBox(8, heading, buttonPane);
        group.getStyleClass().add("do-action-group");
        group.setPrefWidth(480);
        group.setMinWidth(390);
        return group;
    }

    private String deliveryOrderStatusLabel(com.mycompany.tugas_akhir.model.DeliveryOrderStatus status) {
        return switch (status) {
            case DRAFT -> "Draft";
            case READY_TO_PICK -> "Siap picking";
            case PICKING -> "Sedang picking";
            case PICKED -> "Picking selesai";
            case PACKED -> "Packing selesai";
            case LOADED -> "Siap dispatch";
            case PARTIALLY_DISPATCHED -> "Terkirim sebagian";
            case DISPATCHED -> "Terkirim";
            case CANCELLED -> "Dibatalkan";
        };
    }

    private void updateDeliveryOrderActionState(DeliveryOrder order, Button release, Button assignTask,
            Button taskDetails, Button reservations, Button start, Button scanPick, Button scanHistory,
            Button picked, Button packed, Button loaded, Button fulfillmentAudit, Button dispatch) {
        boolean selected = order != null;
        release.setDisable(!selected || (order.getStatus()
                != com.mycompany.tugas_akhir.model.DeliveryOrderStatus.DRAFT
                && order.getStatus() != com.mycompany.tugas_akhir.model.DeliveryOrderStatus.PARTIALLY_DISPATCHED));
        assignTask.setDisable(!selected || order.getStatus()
                != com.mycompany.tugas_akhir.model.DeliveryOrderStatus.READY_TO_PICK);
        taskDetails.setDisable(!selected);
        reservations.setDisable(!selected);
        start.setDisable(!selected || order.getStatus()
                != com.mycompany.tugas_akhir.model.DeliveryOrderStatus.READY_TO_PICK);
        scanPick.setDisable(!selected || order.getStatus()
                != com.mycompany.tugas_akhir.model.DeliveryOrderStatus.PICKING);
        scanHistory.setDisable(!selected);
        picked.setDisable(!selected || order.getStatus()
                != com.mycompany.tugas_akhir.model.DeliveryOrderStatus.PICKING);
        packed.setDisable(!selected || order.getStatus()
                != com.mycompany.tugas_akhir.model.DeliveryOrderStatus.PICKED);
        loaded.setDisable(!selected || order.getStatus()
                != com.mycompany.tugas_akhir.model.DeliveryOrderStatus.PACKED);
        fulfillmentAudit.setDisable(!selected);
        dispatch.setDisable(!selected || order.getStatus()
                != com.mycompany.tugas_akhir.model.DeliveryOrderStatus.LOADED);
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
        if (currentUser == null || currentUser.getId() == null) {
            showAlert(Alert.AlertType.ERROR, "Sesi Tidak Valid", "Silakan login kembali.");
            return;
        }
        TableView<ReturnRequest> table = new TableView<>();
        table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_FLEX_LAST_COLUMN);
        table.getColumns().add(returnColumn("Kode", ReturnRequest::getReturnCode));
        table.getColumns().add(returnColumn("Jenis", request -> request.getReturnType().name()));
        table.getColumns().add(returnColumn("Status", request -> request.getStatus().name()));
        table.getColumns().add(returnColumn("Alasan", ReturnRequest::getReason));
        table.getColumns().add(returnColumn("Barang", request -> request.getLines().stream()
                .map(line -> line.getBarang().getKodeBarang() + " x" + line.getQuantity())
                .collect(java.util.stream.Collectors.joining(", "))));
        Button purchase = new Button("Retur Pembelian");
        Button sales = new Button("Retur Penjualan");
        Button approve = new Button("Verifikasi / Setujui");
        Button reject = new Button("Tolak");
        Button post = new Button("Post Return");
        Button itemLabel = new Button("QR Item Retur");
        Button refresh = new Button("Muat Ulang");
        purchase.setOnAction(action -> handleCreateReturn(table, ReturnType.PURCHASE));
        sales.setOnAction(action -> handleCreateReturn(table, ReturnType.SALES));
        approve.setOnAction(action -> reviewReturn(table, true));
        reject.setOnAction(action -> reviewReturn(table, false));
        post.setOnAction(action -> postReturn(table));
        itemLabel.setOnAction(action -> showReturnItemLabel(table));
        refresh.setOnAction(action -> loadReturns(table));
        Dialog<ButtonType> dialog = new Dialog<>();
        dialog.setTitle("Purchase Return / Sales Return");
        dialog.getDialogPane().setContent(new VBox(10, new javafx.scene.layout.FlowPane(8, 8,
                purchase, sales, approve, reject, post, itemLabel, refresh), table));
        dialog.getDialogPane().getButtonTypes().add(ButtonType.CLOSE);
        dialog.getDialogPane().setPrefSize(900, 460);
        dialog.show();
        loadReturns(table);
    }

    private TableColumn<ReturnRequest, String> returnColumn(
            String title, java.util.function.Function<ReturnRequest, String> value) {
        TableColumn<ReturnRequest, String> column = new TableColumn<>(title);
        column.setCellValueFactory(cell -> new SimpleStringProperty(value.apply(cell.getValue())));
        return column;
    }

    private void loadReturns(TableView<ReturnRequest> table) {
        Task<List<ReturnRequest>> task = new Task<>() {
            @Override
            protected List<ReturnRequest> call() { return returnRequestService.getRecentReturns(); }
        };
        task.setOnSucceeded(event -> table.getItems().setAll(task.getValue()));
        task.setOnFailed(event -> {
            log.error("Gagal memuat dokumen retur", task.getException());
            showAlert(Alert.AlertType.ERROR, "Gagal", "Dokumen retur tidak dapat dimuat.");
        });
        Thread thread = new Thread(task, "return-request-list");
        thread.setDaemon(true);
        thread.start();
    }

    private void handleCreateReturn(TableView<ReturnRequest> table, ReturnType type) {
        Task<List<?>> loadTask = new Task<>() {
            @Override
            protected List<?> call() {
                if (type == ReturnType.PURCHASE) return purchaseOrderService.getReceivedLines();
                return deliveryOrderService.getRecentOrders().stream()
                        .filter(order -> order.getStatus() == com.mycompany.tugas_akhir.model.DeliveryOrderStatus.DISPATCHED
                                || order.getStatus() == com.mycompany.tugas_akhir.model.DeliveryOrderStatus.PARTIALLY_DISPATCHED)
                        .flatMap(order -> order.getLines().stream())
                        .filter(line -> line.getShippedQuantity() > 0).toList();
            }
        };
        loadTask.setOnSucceeded(event -> showCreateReturnDialog(table, type, loadTask.getValue()));
        loadTask.setOnFailed(event -> {
            log.error("Gagal memuat dokumen sumber retur", loadTask.getException());
            showAlert(Alert.AlertType.ERROR, "Gagal", "Dokumen sumber retur tidak dapat dimuat.");
        });
        Thread thread = new Thread(loadTask, "return-source-list");
        thread.setDaemon(true);
        thread.start();
    }

    private void showCreateReturnDialog(TableView<ReturnRequest> table, ReturnType type, List<?> sources) {
        if (sources.isEmpty() || rakItems == null || rakItems.isEmpty()) {
            showAlert(Alert.AlertType.INFORMATION, "Tidak Ada Dokumen Sumber",
                    sources.isEmpty()
                            ? (type == ReturnType.PURCHASE ? "Tidak ada PO yang sudah diterima."
                                    : "Tidak ada DO yang sudah dikirim.")
                            : "Data SLOC belum berhasil dimuat.");
            return;
        }
        @SuppressWarnings("unchecked")
        ComboBox<Object> sourceBox = new ComboBox<>(FXCollections.observableArrayList(sources));
        sourceBox.setPromptText(type == ReturnType.PURCHASE ? "Pilih baris PO" : "Pilih baris DO terkirim");
        ComboBox<Rak> rackBox = new ComboBox<>(FXCollections.observableArrayList(rakItems));
        rackBox.setPromptText("Pilih SLOC");
        TextField batchField = new TextField();
        batchField.setPromptText("Batch retur");
        DatePicker expiryPicker = new DatePicker();
        TextField quantityField = new TextField();
        quantityField.setPromptText("Jumlah");
        CheckBox scanBarcodes = new CheckBox("Validasi item dan SLOC dengan scan barcode");
        TextField itemQrField = new TextField();
        itemQrField.setPromptText("WMS1 item QR");
        TextField locationBarcodeField = new TextField();
        locationBarcodeField.setPromptText("SLOC1 barcode");
        itemQrField.setDisable(true);
        locationBarcodeField.setDisable(true);
        scanBarcodes.selectedProperty().addListener((observable, oldValue, selected) -> {
            itemQrField.setDisable(!selected);
            locationBarcodeField.setDisable(!selected);
            rackBox.setDisable(selected);
            batchField.setDisable(selected);
            expiryPicker.setDisable(selected);
        });
        TextArea reasonField = new TextArea();
        reasonField.setPromptText("Alasan retur / hasil pemeriksaan");
        reasonField.setPrefRowCount(2);
        GridPane form = new GridPane();
        form.setHgap(10);
        form.setVgap(8);
        form.setPadding(new javafx.geometry.Insets(12));
        form.addRow(0, new Label("Dokumen asal"), sourceBox);
        form.addRow(1, new Label("SLOC"), rackBox);
        form.addRow(2, new Label("Batch"), batchField);
        int row = 3;
        if (type == ReturnType.SALES) form.addRow(row++, new Label("Expiry (opsional)"), expiryPicker);
        form.addRow(row++, new Label("Jumlah"), quantityField);
        form.addRow(row++, new Label("Alasan"), reasonField);
        form.addRow(row++, scanBarcodes);
        form.addRow(row++, new Label("QR barang"), itemQrField);
        form.addRow(row, new Label("Barcode SLOC"), locationBarcodeField);
        Dialog<ButtonType> dialog = new Dialog<>();
        dialog.setTitle(type == ReturnType.PURCHASE ? "Buat Purchase Return" : "Buat Sales Return");
        dialog.getDialogPane().setContent(form);
        dialog.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);
        Optional<ButtonType> result = dialog.showAndWait();
        if (result.isEmpty() || result.get() != ButtonType.OK) return;
        boolean useScannedBarcodes = scanBarcodes.isSelected();
        if (sourceBox.getValue() == null || reasonField.getText().isBlank()
                || !useScannedBarcodes && (rackBox.getValue() == null || batchField.getText().isBlank())) {
            showAlert(Alert.AlertType.WARNING, "Data Belum Lengkap",
                    "Dokumen asal, SLOC, batch, jumlah, dan alasan wajib diisi.");
            return;
        }
        if (useScannedBarcodes && (itemQrField.getText().isBlank()
                || locationBarcodeField.getText().isBlank())) {
            showAlert(Alert.AlertType.WARNING, "Barcode Belum Lengkap",
                    "Scan QR barang dan barcode SLOC.");
            return;
        }
        int quantity;
        try {
            quantity = Integer.parseInt(quantityField.getText().trim());
            if (quantity <= 0) throw new NumberFormatException();
        } catch (NumberFormatException exception) {
            showAlert(Alert.AlertType.WARNING, "Jumlah Tidak Valid", "Masukkan jumlah bulat lebih dari nol.");
            return;
        }
        Integer sourceId = type == ReturnType.PURCHASE
                ? ((PurchaseOrderLine) sourceBox.getValue()).getId()
                : ((DeliveryOrderLine) sourceBox.getValue()).getId();
        Integer rackId = useScannedBarcodes ? null : rackBox.getValue().getId();
        String batch = batchField.getText().trim();
        String reason = reasonField.getText().trim();
        java.time.LocalDate expiry = expiryPicker.getValue();
        String scannedItemQr = itemQrField.getText().trim();
        String scannedLocationBarcode = locationBarcodeField.getText().trim();
        Integer creatorId = currentUser.getId();
        Task<ReturnRequest> task = new Task<>() {
            @Override
            protected ReturnRequest call() {
                if (useScannedBarcodes) {
                    return type == ReturnType.PURCHASE
                            ? returnRequestService.createPurchaseReturnWithBarcodes(sourceId, scannedItemQr,
                                    scannedLocationBarcode, quantity, reason, creatorId)
                            : returnRequestService.createSalesReturnWithBarcodes(sourceId, scannedItemQr,
                                    scannedLocationBarcode, quantity, reason, creatorId);
                }
                return type == ReturnType.PURCHASE
                        ? returnRequestService.createPurchaseReturn(sourceId, rackId, batch,
                                quantity, reason, creatorId)
                        : returnRequestService.createSalesReturn(sourceId, rackId, batch, expiry,
                                quantity, reason, creatorId);
            }
        };
        task.setOnSucceeded(event -> {
            if (task.getValue() == null) {
                showAlert(Alert.AlertType.ERROR, "Retur Ditolak",
                        "Jumlah retur melebihi penerimaan/pengiriman asal atau dokumen tidak memenuhi syarat.");
            } else loadReturns(table);
        });
        task.setOnFailed(event -> {
            log.error("Gagal membuat dokumen retur", task.getException());
            showAlert(Alert.AlertType.ERROR, "Gagal", "Dokumen retur tidak dapat disimpan.");
        });
        Thread thread = new Thread(task, "return-request-create");
        thread.setDaemon(true);
        thread.start();
    }

    private void reviewReturn(TableView<ReturnRequest> table, boolean approved) {
        ReturnRequest request = table.getSelectionModel().getSelectedItem();
        if (request == null || currentUser == null || currentUser.getId() == null) {
            showAlert(Alert.AlertType.WARNING, "Pilih Retur", "Pilih dokumen retur dan pastikan sesi aktif.");
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
        final String reviewNotes = notes;
        Integer reviewerId = currentUser.getId();
        Task<Boolean> task = new Task<>() {
            @Override
            protected Boolean call() {
                return returnRequestService.reviewReturn(request.getId(), reviewerId, approved, reviewNotes);
            }
        };
        task.setOnSucceeded(event -> {
            if (task.getValue()) loadReturns(table);
            else showAlert(Alert.AlertType.ERROR, "Review Ditolak",
                    "Hanya pengguna selain pembuat yang dapat meninjau retur yang menunggu.");
        });
        task.setOnFailed(event -> {
            log.error("Gagal meninjau retur {}", request.getReturnCode(), task.getException());
            showAlert(Alert.AlertType.ERROR, "Gagal", "Pemeriksaan retur gagal.");
        });
        Thread thread = new Thread(task, "return-request-review");
        thread.setDaemon(true);
        thread.start();
    }

    private void postReturn(TableView<ReturnRequest> table) {
        ReturnRequest request = table.getSelectionModel().getSelectedItem();
        if (request == null || currentUser == null || currentUser.getId() == null) {
            showAlert(Alert.AlertType.WARNING, "Pilih Retur", "Pilih dokumen retur dan pastikan sesi aktif.");
            return;
        }
        Integer userId = currentUser.getId();
        Task<Boolean> task = new Task<>() {
            @Override
            protected Boolean call() {
                return returnRequestService.postApprovedReturn(request.getId(), userId);
            }
        };
        task.setOnSucceeded(event -> {
            if (task.getValue()) {
                loadReturns(table);
                loadTransaksi();
                showAlert(Alert.AlertType.INFORMATION, "Retur Diposting",
                        "Perubahan stok retur berhasil dicatat melalui ledger.");
            } else showAlert(Alert.AlertType.ERROR, "Posting Ditolak",
                    "Retur harus disetujui dan kuantitas sumber masih harus valid.");
        });
        task.setOnFailed(event -> {
            log.error("Gagal mem-post retur {}", request.getReturnCode(), task.getException());
            showAlert(Alert.AlertType.ERROR, "Gagal", "Posting retur dibatalkan.");
        });
        Thread thread = new Thread(task, "return-request-post");
        thread.setDaemon(true);
        thread.start();
    }

    private void showReturnItemLabel(TableView<ReturnRequest> table) {
        ReturnRequest request = table.getSelectionModel().getSelectedItem();
        if (request == null || request.getReturnType() != ReturnType.SALES
                || request.getStatus() != com.mycompany.tugas_akhir.model.ReturnStatus.POSTED
                || request.getLines().size() != 1) {
            showAlert(Alert.AlertType.WARNING, "QR Tidak Tersedia",
                    "Pilih sales return yang sudah diposting dan berisi satu item.");
            return;
        }
        ReturnRequestLine line = request.getLines().get(0);
        String sku = line.getBarang().getKodeBarang();
        String batch = line.getBatchCode();
        java.time.LocalDate expiry = line.getExpiryDate();
        Task<Path> task = new Task<>() {
            @Override
            protected Path call() throws Exception {
                return inboundReceiptService.generateItemQrLabel(sku, batch, expiry);
            }
        };
        task.setOnSucceeded(event -> {
            ImageView image = new ImageView(new Image(task.getValue().toUri().toString()));
            image.setFitWidth(300);
            image.setFitHeight(300);
            image.setPreserveRatio(true);
            Alert preview = new Alert(Alert.AlertType.INFORMATION);
            preview.setTitle("Label Sales Return");
            preview.setHeaderText("Gunakan ulang QR item asli atau cetak label pengganti");
            preview.setContentText("QR berisi SKU, batch, dan expiry yang tercatat pada penerimaan retur.");
            preview.getDialogPane().setContent(new VBox(8, image, new Label(task.getValue().toString())));
            preview.showAndWait();
        });
        task.setOnFailed(event -> {
            log.error("Gagal membuat label sales return {}", request.getReturnCode(), task.getException());
            showAlert(Alert.AlertType.ERROR, "QR Gagal",
                    "Label retur tidak dapat dibuat. Persediaan tetap tercatat; coba ulangi pembuatan label.");
        });
        Thread thread = new Thread(task, "sales-return-item-qr");
        thread.setDaemon(true);
        thread.start();
    }

    @FXML
    private void handleOpenSto() {
        if (currentUser == null || currentUser.getId() == null) {
            showAlert(Alert.AlertType.ERROR, "Sesi Tidak Valid", "Silakan login kembali.");
            return;
        }
        TableView<StockTransferOrder> table = new TableView<>();
        table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_FLEX_LAST_COLUMN);
        table.getColumns().add(stoColumn("STO", StockTransferOrder::getOrderCode));
        table.getColumns().add(stoColumn("Asal", order -> order.getSourceWarehouse().getWarehouseCode()));
        table.getColumns().add(stoColumn("Tujuan", order -> order.getDestinationWarehouse().getWarehouseCode()));
        table.getColumns().add(stoColumn("Status", order -> order.getStatus().name()));
        table.getColumns().add(stoColumn("Picker", order -> order.getAssignedPicker() == null
                ? "-" : order.getAssignedPicker().getFullName()));
        table.getColumns().add(stoColumn("Barang", order -> order.getLines().stream()
                .map(line -> line.getBarang().getKodeBarang() + " x" + line.getQuantity())
                .collect(java.util.stream.Collectors.joining(", "))));
        Button create = new Button("Buat Transfer Plan");
        Button manage = new Button("Gudang / Section");
        Button approve = new Button("Setujui");
        Button reject = new Button("Tolak");
        Button assignPicker = new Button("Pilih Picker");
        Button dispatch = new Button("Scan Dispatch");
        Button receive = new Button("Scan STO In");
        Button legacyReceive = new Button("Terima STO lama");
        Button history = new Button("Riwayat Scan");
        Button refresh = new Button("Muat Ulang");
        create.setOnAction(action -> handleCreateStoPlan(table));
        manage.setOnAction(action -> handleManageWarehouses());
        approve.setOnAction(action -> reviewSto(table, true));
        reject.setOnAction(action -> reviewSto(table, false));
        assignPicker.setOnAction(action -> assignStoPicker(table));
        dispatch.setOnAction(action -> openStoScanDialog(table, true));
        receive.setOnAction(action -> openStoScanDialog(table, false));
        legacyReceive.setOnAction(action -> receiveLegacySto(table));
        history.setOnAction(action -> showStoScanHistory(table));
        refresh.setOnAction(action -> loadSto(table));
        Dialog<ButtonType> dialog = new Dialog<>();
        dialog.setTitle("Stock Transfer Plan / STO");
        dialog.getDialogPane().setContent(new VBox(10, new javafx.scene.layout.FlowPane(8, 8,
                create, manage, approve, reject, assignPicker, dispatch, receive,
                legacyReceive, history, refresh), table));
        dialog.getDialogPane().getButtonTypes().add(ButtonType.CLOSE);
        dialog.getDialogPane().setPrefSize(980, 470);
        dialog.show();
        loadSto(table);
    }

    private TableColumn<StockTransferOrder, String> stoColumn(
            String title, java.util.function.Function<StockTransferOrder, String> value) {
        TableColumn<StockTransferOrder, String> column = new TableColumn<>(title);
        column.setCellValueFactory(cell -> new SimpleStringProperty(value.apply(cell.getValue())));
        return column;
    }

    private void loadSto(TableView<StockTransferOrder> table) {
        Task<List<StockTransferOrder>> task = new Task<>() {
            @Override
            protected List<StockTransferOrder> call() { return stockTransferOrderService.getRecentOrders(); }
        };
        task.setOnSucceeded(event -> table.getItems().setAll(task.getValue()));
        task.setOnFailed(event -> {
            log.error("Gagal memuat STO", task.getException());
            showAlert(Alert.AlertType.ERROR, "Gagal", "Daftar STO tidak dapat dimuat.");
        });
        Thread thread = new Thread(task, "sto-list");
        thread.setDaemon(true);
        thread.start();
    }

    private void handleCreateStoPlan(TableView<StockTransferOrder> table) {
        if (barangItems == null || rakItems == null) {
            showAlert(Alert.AlertType.WARNING, "Data Belum Siap", "Barang dan SLOC belum berhasil dimuat.");
            return;
        }
        Task<List<Warehouse>> task = new Task<>() {
            @Override
            protected List<Warehouse> call() { return sectionService.getActiveWarehouses(); }
        };
        task.setOnSucceeded(event -> {
            if (task.getValue().size() < 2) {
                showAlert(Alert.AlertType.INFORMATION, "Gudang Belum Cukup",
                        "Buat minimal dua gudang, lalu tetapkan section tujuan melalui Gudang / Section.");
                return;
            }
            showCreateStoPlanDialog(table, task.getValue());
        });
        task.setOnFailed(event -> {
            log.error("Gagal memuat gudang untuk STO", task.getException());
            showAlert(Alert.AlertType.ERROR, "Gagal", "Daftar gudang tidak dapat dimuat.");
        });
        Thread thread = new Thread(task, "sto-warehouse-list");
        thread.setDaemon(true);
        thread.start();
    }

    private void showCreateStoPlanDialog(TableView<StockTransferOrder> table, List<Warehouse> warehouses) {
        ComboBox<Warehouse> sourceWarehouse = new ComboBox<>(FXCollections.observableArrayList(warehouses));
        ComboBox<Warehouse> destinationWarehouse = new ComboBox<>(FXCollections.observableArrayList(warehouses));
        ComboBox<Rak> sourceRack = new ComboBox<>();
        ComboBox<Rak> destinationRack = new ComboBox<>();
        sourceWarehouse.setPromptText("Gudang asal");
        destinationWarehouse.setPromptText("Gudang tujuan");
        sourceRack.setPromptText("SLOC asal");
        destinationRack.setPromptText("SLOC tujuan");
        sourceWarehouse.valueProperty().addListener((observable, oldValue, selected) ->
                sourceRack.setItems(FXCollections.observableArrayList(racksForWarehouse(selected))));
        destinationWarehouse.valueProperty().addListener((observable, oldValue, selected) ->
                destinationRack.setItems(FXCollections.observableArrayList(racksForWarehouse(selected))));
        ComboBox<Barang> itemBox = new ComboBox<>(FXCollections.observableArrayList(barangItems));
        itemBox.setPromptText("Barang");
        TextField quantityField = new TextField();
        quantityField.setPromptText("Jumlah");
        TextArea reasonField = new TextArea();
        reasonField.setPromptText("Alasan / tujuan transfer");
        reasonField.setPrefRowCount(2);
        GridPane form = new GridPane();
        form.setHgap(10);
        form.setVgap(8);
        form.setPadding(new javafx.geometry.Insets(12));
        form.addRow(0, new Label("Gudang asal"), sourceWarehouse);
        form.addRow(1, new Label("SLOC asal"), sourceRack);
        form.addRow(2, new Label("Gudang tujuan"), destinationWarehouse);
        form.addRow(3, new Label("SLOC tujuan"), destinationRack);
        form.addRow(4, new Label("Barang"), itemBox);
        form.addRow(5, new Label("Jumlah"), quantityField);
        form.addRow(6, new Label("Alasan"), reasonField);
        Dialog<ButtonType> dialog = new Dialog<>();
        dialog.setTitle("Buat Stock Transfer Plan");
        dialog.getDialogPane().setContent(form);
        dialog.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);
        Optional<ButtonType> result = dialog.showAndWait();
        if (result.isEmpty() || result.get() != ButtonType.OK) return;
        if (sourceWarehouse.getValue() == null || destinationWarehouse.getValue() == null
                || sourceRack.getValue() == null || destinationRack.getValue() == null
                || itemBox.getValue() == null || reasonField.getText().isBlank()) {
            showAlert(Alert.AlertType.WARNING, "Data Belum Lengkap",
                    "Gudang, SLOC, barang, jumlah, dan alasan wajib dipilih/diisi.");
            return;
        }
        int quantity;
        try {
            quantity = Integer.parseInt(quantityField.getText().trim());
            if (quantity <= 0) throw new NumberFormatException();
        } catch (NumberFormatException exception) {
            showAlert(Alert.AlertType.WARNING, "Jumlah Tidak Valid", "Masukkan jumlah bulat lebih dari nol.");
            return;
        }
        Integer sourceWarehouseId = sourceWarehouse.getValue().getId();
        Integer destinationWarehouseId = destinationWarehouse.getValue().getId();
        Integer sourceRackId = sourceRack.getValue().getId();
        Integer destinationRackId = destinationRack.getValue().getId();
        Integer itemId = itemBox.getValue().getId();
        String reason = reasonField.getText().trim();
        Integer userId = currentUser.getId();
        Task<StockTransferOrder> saveTask = new Task<>() {
            @Override
            protected StockTransferOrder call() {
                return stockTransferOrderService.createPlan(sourceWarehouseId, destinationWarehouseId,
                        sourceRackId, destinationRackId, itemId, quantity, reason, userId);
            }
        };
        saveTask.setOnSucceeded(event -> {
            if (saveTask.getValue() == null) {
                showAlert(Alert.AlertType.ERROR, "Plan Ditolak",
                        "Periksa bahwa SLOC termasuk gudang yang dipilih dan kapasitas tujuan mencukupi.");
            } else loadSto(table);
        });
        saveTask.setOnFailed(event -> {
            log.error("Gagal membuat stock transfer plan", saveTask.getException());
            showAlert(Alert.AlertType.ERROR, "Gagal", "Stock transfer plan tidak dapat dibuat.");
        });
        Thread thread = new Thread(saveTask, "sto-plan-create");
        thread.setDaemon(true);
        thread.start();
    }

    private List<Rak> racksForWarehouse(Warehouse warehouse) {
        if (warehouse == null || rakItems == null) return List.of();
        return rakItems.stream().filter(rack -> rack.getSection() != null
                && rack.getSection().getWarehouse() != null
                && warehouse.getId().equals(rack.getSection().getWarehouse().getId())).toList();
    }

    private void handleManageWarehouses() {
        Task<List<Object>> task = new Task<>() {
            @Override
            protected List<Object> call() {
                return List.of(sectionService.getActiveWarehouses(), sectionService.getAllSections());
            }
        };
        task.setOnSucceeded(event -> showWarehouseManagement(task.getValue()));
        task.setOnFailed(event -> {
            log.error("Gagal memuat konfigurasi gudang", task.getException());
            showAlert(Alert.AlertType.ERROR, "Gagal", "Konfigurasi gudang tidak dapat dimuat.");
        });
        Thread thread = new Thread(task, "warehouse-config");
        thread.setDaemon(true);
        thread.start();
    }

    @SuppressWarnings("unchecked")
    private void showWarehouseManagement(List<Object> values) {
        List<Warehouse> warehouses = (List<Warehouse>) values.get(0);
        List<com.mycompany.tugas_akhir.model.Section> sections =
                (List<com.mycompany.tugas_akhir.model.Section>) values.get(1);
        TextField code = new TextField();
        code.setPromptText("Kode gudang");
        TextField name = new TextField();
        name.setPromptText("Nama gudang");
        TextField address = new TextField();
        address.setPromptText("Alamat (opsional)");
        Button create = new Button("Tambah Gudang");
        ComboBox<com.mycompany.tugas_akhir.model.Section> sectionBox =
                new ComboBox<>(FXCollections.observableArrayList(sections));
        ComboBox<Warehouse> warehouseBox = new ComboBox<>(FXCollections.observableArrayList(warehouses));
        sectionBox.setPromptText("Pilih section");
        warehouseBox.setPromptText("Pilih gudang");
        Button assign = new Button("Tetapkan Section");
        Label resultLabel = new Label();
        Dialog<ButtonType> dialog = new Dialog<>();
        dialog.setTitle("Konfigurasi Gudang dan Section");
        GridPane form = new GridPane();
        form.setHgap(8);
        form.setVgap(8);
        form.setPadding(new javafx.geometry.Insets(12));
        form.addRow(0, new Label("Kode"), code);
        form.addRow(1, new Label("Nama"), name);
        form.addRow(2, new Label("Alamat"), address);
        form.add(create, 1, 3);
        form.addRow(4, new Label("Section"), sectionBox);
        form.addRow(5, new Label("Gudang"), warehouseBox);
        form.add(assign, 1, 6);
        form.add(resultLabel, 1, 7);
        dialog.getDialogPane().setContent(form);
        dialog.getDialogPane().getButtonTypes().add(ButtonType.CLOSE);
        create.setOnAction(action -> {
            String warehouseCode = code.getText().trim();
            String warehouseName = name.getText().trim();
            String warehouseAddress = address.getText().trim();
            Task<Warehouse> createTask = new Task<>() {
                @Override
                protected Warehouse call() {
                    return sectionService.createWarehouse(warehouseCode, warehouseName, warehouseAddress);
                }
            };
            createTask.setOnSucceeded(createEvent -> {
                if (createTask.getValue() == null) {
                    resultLabel.setText("Kode/nama tidak valid atau kode sudah digunakan.");
                } else {
                    warehouseBox.getItems().add(createTask.getValue());
                    resultLabel.setText("Gudang ditambahkan; pilih section untuk memetakannya.");
                }
            });
            createTask.setOnFailed(createEvent -> {
                log.error("Gagal membuat gudang", createTask.getException());
                resultLabel.setText("Gudang gagal dibuat.");
            });
            Thread thread = new Thread(createTask, "warehouse-create");
            thread.setDaemon(true);
            thread.start();
        });
        assign.setOnAction(action -> {
            if (sectionBox.getValue() == null || warehouseBox.getValue() == null) {
                resultLabel.setText("Pilih section dan gudang terlebih dahulu.");
                return;
            }
            Integer sectionId = sectionBox.getValue().getId();
            Integer warehouseId = warehouseBox.getValue().getId();
            Task<Boolean> assignTask = new Task<>() {
                @Override
                protected Boolean call() {
                    return sectionService.assignSectionToWarehouse(sectionId, warehouseId);
                }
            };
            assignTask.setOnSucceeded(assignEvent -> {
                if (assignTask.getValue()) {
                    resultLabel.setText("Section dipetakan ke gudang. Muat ulang untuk memperbarui pilihan SLOC.");
                    Task<List<Rak>> reloadRacks = new Task<>() {
                        @Override
                        protected List<Rak> call() { return sectionService.getAllRaks(); }
                    };
                    reloadRacks.setOnSucceeded(reloadEvent -> rakItems = reloadRacks.getValue());
                    Thread reloadThread = new Thread(reloadRacks, "sto-rack-refresh");
                    reloadThread.setDaemon(true);
                    reloadThread.start();
                } else resultLabel.setText("Pemetaan section ditolak.");
            });
            assignTask.setOnFailed(assignEvent -> {
                log.error("Gagal menetapkan section ke gudang", assignTask.getException());
                resultLabel.setText("Section gagal dipetakan.");
            });
            Thread thread = new Thread(assignTask, "warehouse-section-assign");
            thread.setDaemon(true);
            thread.start();
        });
        dialog.showAndWait();
    }

    private void reviewSto(TableView<StockTransferOrder> table, boolean approved) {
        StockTransferOrder order = table.getSelectionModel().getSelectedItem();
        if (order == null || currentUser == null || currentUser.getId() == null) {
            showAlert(Alert.AlertType.WARNING, "Pilih STO", "Pilih dokumen dan pastikan sesi aktif.");
            return;
        }
        String notes = approved ? null : new TextInputDialog().showAndWait().orElse(null);
        if (!approved && (notes == null || notes.isBlank())) return;
        Integer reviewerId = currentUser.getId();
        Task<Boolean> task = new Task<>() {
            @Override
            protected Boolean call() {
                return stockTransferOrderService.reviewPlan(order.getId(), reviewerId, approved, notes);
            }
        };
        task.setOnSucceeded(event -> {
            if (task.getValue()) loadSto(table);
            else showAlert(Alert.AlertType.ERROR, "Review Ditolak",
                    "Pembuat tidak dapat meninjau plan sendiri atau status bukan menunggu approval.");
        });
        task.setOnFailed(event -> {
            log.error("Gagal meninjau STO {}", order.getOrderCode(), task.getException());
            showAlert(Alert.AlertType.ERROR, "Gagal", "STO tidak dapat ditinjau.");
        });
        Thread thread = new Thread(task, "sto-review");
        thread.setDaemon(true);
        thread.start();
    }

    private void assignStoPicker(TableView<StockTransferOrder> table) {
        StockTransferOrder order = table.getSelectionModel().getSelectedItem();
        if (order == null || currentUser == null || currentUser.getId() == null) {
            showAlert(Alert.AlertType.WARNING, "Pilih STO", "Pilih dokumen dan pastikan sesi aktif.");
            return;
        }
        Task<List<User>> task = new Task<>() {
            @Override
            protected List<User> call() { return userService.getUsersForAssignment(); }
        };
        task.setOnSucceeded(event -> {
            if (task.getValue().isEmpty()) {
                showAlert(Alert.AlertType.WARNING, "Picker Tidak Tersedia", "Tidak ada pengguna untuk ditugaskan.");
                return;
            }
            ComboBox<User> pickerBox = new ComboBox<>(FXCollections.observableArrayList(task.getValue()));
            pickerBox.setPromptText("Pilih picker");
            pickerBox.setCellFactory(list -> userPickerCell());
            pickerBox.setButtonCell(userPickerCell());
            Dialog<ButtonType> dialog = new Dialog<>();
            dialog.setTitle("Assignment Picker STO");
            dialog.setHeaderText("Pilih picker untuk dispatch barcode STO.");
            dialog.getDialogPane().setContent(pickerBox);
            dialog.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);
            Optional<ButtonType> result = dialog.showAndWait();
            if (result.isEmpty() || result.get() != ButtonType.OK) return;
            User selectedPicker = pickerBox.getValue();
            if (selectedPicker == null) {
                showAlert(Alert.AlertType.WARNING, "Picker Belum Dipilih", "Pilih pengguna sebelum menyimpan.");
                return;
            }
            Task<Boolean> assignTask = new Task<>() {
                @Override
                protected Boolean call() {
                    return stockTransferOrderService.assignPicker(order.getId(), selectedPicker.getId());
                }
            };
            assignTask.setOnSucceeded(assignEvent -> {
                if (assignTask.getValue()) loadSto(table);
                else showAlert(Alert.AlertType.ERROR, "Assignment Ditolak",
                        "Picker hanya dapat ditetapkan untuk STO yang sudah disetujui dan belum dipindai.");
            });
            assignTask.setOnFailed(assignEvent -> {
                log.error("Gagal menetapkan picker STO {}", order.getOrderCode(), assignTask.getException());
                showAlert(Alert.AlertType.ERROR, "Gagal", "Picker STO tidak dapat ditetapkan.");
            });
            Thread assignThread = new Thread(assignTask, "sto-picker-assignment");
            assignThread.setDaemon(true);
            assignThread.start();
        });
        task.setOnFailed(event -> {
            log.error("Gagal memuat daftar picker untuk STO {}", order.getOrderCode(), task.getException());
            showAlert(Alert.AlertType.ERROR, "Gagal", "Daftar picker tidak dapat dimuat.");
        });
        Thread thread = new Thread(task, "sto-picker-list");
        thread.setDaemon(true);
        thread.start();
    }

    private void openStoScanDialog(TableView<StockTransferOrder> table, boolean dispatch) {
        StockTransferOrder order = table.getSelectionModel().getSelectedItem();
        if (order == null || currentUser == null || currentUser.getId() == null) {
            showAlert(Alert.AlertType.WARNING, "Pilih STO", "Pilih dokumen dan pastikan sesi aktif.");
            return;
        }
        TextField itemQr = new TextField();
        itemQr.setPromptText("WMS1 item QR");
        TextField locationBarcode = new TextField();
        locationBarcode.setPromptText("SLOC1 barcode");
        TextField quantityField = new TextField();
        quantityField.setPromptText("Kuantitas scan");
        Dialog<ButtonType> dialog = new Dialog<>();
        dialog.setTitle(dispatch ? "Scan Dispatch STO" : "Scan Penerimaan STO");
        dialog.setHeaderText(dispatch
                ? "Pindai item dan SLOC sumber; kuantitas dapat diproses bertahap."
                : "Pindai item dan SLOC tujuan untuk menerima kuantitas yang sudah dikirim.");
        dialog.getDialogPane().setContent(new VBox(8,
                new Label("QR item"), itemQr,
                new Label(dispatch ? "SLOC sumber" : "SLOC tujuan"), locationBarcode,
                new Label("Kuantitas"), quantityField));
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
                    "Isi QR item, barcode SLOC, dan kuantitas positif.");
            return;
        }
        Integer operatorId = currentUser.getId();
        String itemPayload = itemQr.getText().trim();
        String locationPayload = locationBarcode.getText().trim();
        Task<Boolean> task = new Task<>() {
            @Override
            protected Boolean call() {
                return dispatch
                        ? stockTransferOrderService.recordDispatchScan(
                                order.getId(), operatorId, itemPayload, locationPayload, quantity)
                        : stockTransferOrderService.recordReceiptScan(
                                order.getId(), operatorId, itemPayload, locationPayload, quantity);
            }
        };
        task.setOnSucceeded(event -> {
            if (task.getValue()) {
                loadSto(table);
                loadTransaksi();
                showAlert(Alert.AlertType.INFORMATION,
                        dispatch ? "Dispatch Dicatat" : "Penerimaan Dicatat",
                        dispatch ? "Kuantitas hasil scan sudah diposting melalui ledger."
                                : "Kuantitas hasil scan sudah diterima melalui ledger.");
            } else showAlert(Alert.AlertType.ERROR, "Scan Ditolak",
                    dispatch
                            ? "Periksa assignment picker, status STO, QR item, SLOC sumber, lot, dan sisa kuantitas."
                            : "Periksa status STO, QR item, SLOC tujuan, dan kuantitas yang belum diterima.");
        });
        task.setOnFailed(event -> {
            log.error("Gagal mencatat scan STO {}", order.getOrderCode(), task.getException());
            showAlert(Alert.AlertType.ERROR, "Gagal", "Transaksi scan STO dibatalkan.");
        });
        Thread thread = new Thread(task, dispatch ? "sto-dispatch-scan" : "sto-receipt-scan");
        thread.setDaemon(true);
        thread.start();
    }

    private void showStoScanHistory(TableView<StockTransferOrder> table) {
        StockTransferOrder order = table.getSelectionModel().getSelectedItem();
        if (order == null) {
            showAlert(Alert.AlertType.WARNING, "Pilih STO", "Pilih dokumen untuk melihat riwayat scan.");
            return;
        }
        Task<List<StockTransferOrderScan>> task = new Task<>() {
            @Override
            protected List<StockTransferOrderScan> call() {
                return stockTransferOrderService.getScans(order.getId());
            }
        };
        task.setOnSucceeded(event -> {
            TableView<StockTransferOrderScan> scans = new TableView<>(
                    FXCollections.observableArrayList(task.getValue()));
            scans.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_FLEX_LAST_COLUMN);
            scans.getColumns().add(stoScanColumn("Jenis", scan -> scan.getScanType().name()));
            scans.getColumns().add(stoScanColumn("Waktu", scan -> scan.getScannedAt().toString()));
            scans.getColumns().add(stoScanColumn("Picker / Operator", scan -> scan.getPicker().getFullName()));
            scans.getColumns().add(stoScanColumn("SKU / Batch",
                    scan -> scan.getScannedSku() + " / " + scan.getScannedBatch()));
            scans.getColumns().add(stoScanColumn("SLOC", StockTransferOrderScan::getScannedRack));
            scans.getColumns().add(stoScanColumn("Qty", scan -> String.valueOf(scan.getQuantity())));
            Dialog<ButtonType> history = new Dialog<>();
            history.setTitle("Riwayat Scan " + order.getOrderCode());
            history.getDialogPane().setContent(scans);
            history.getDialogPane().getButtonTypes().add(ButtonType.CLOSE);
            history.getDialogPane().setPrefSize(760, 420);
            history.show();
        });
        task.setOnFailed(event -> {
            log.error("Gagal memuat riwayat scan STO {}", order.getOrderCode(), task.getException());
            showAlert(Alert.AlertType.ERROR, "Gagal", "Riwayat scan STO tidak dapat dimuat.");
        });
        Thread thread = new Thread(task, "sto-scan-history");
        thread.setDaemon(true);
        thread.start();
    }

    private TableColumn<StockTransferOrderScan, String> stoScanColumn(
            String title, java.util.function.Function<StockTransferOrderScan, String> value) {
        TableColumn<StockTransferOrderScan, String> column = new TableColumn<>(title);
        column.setCellValueFactory(cell -> new SimpleStringProperty(value.apply(cell.getValue())));
        return column;
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

    private void receiveLegacySto(TableView<StockTransferOrder> table) {
        StockTransferOrder order = table.getSelectionModel().getSelectedItem();
        if (order == null || currentUser == null || currentUser.getId() == null) {
            showAlert(Alert.AlertType.WARNING, "Pilih STO", "Pilih dokumen dan pastikan sesi aktif.");
            return;
        }
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION,
                "Gunakan hanya untuk STO lama yang di-dispatch sebelum scan STO tersedia. "
                        + "Semua lot terbuka akan diterima tanpa riwayat scan.",
                ButtonType.OK, ButtonType.CANCEL);
        confirm.setTitle("Penerimaan STO Lama");
        confirm.setHeaderText(order.getOrderCode());
        if (confirm.showAndWait().orElse(ButtonType.CANCEL) != ButtonType.OK) return;
        Integer receiverId = currentUser.getId();
        Task<Boolean> task = new Task<>() {
            @Override
            protected Boolean call() {
                return stockTransferOrderService.receive(order.getId(), receiverId);
            }
        };
        task.setOnSucceeded(event -> {
            if (task.getValue()) {
                loadSto(table);
                loadTransaksi();
                showAlert(Alert.AlertType.INFORMATION, "Penerimaan STO",
                        "Lot STO lama berhasil diterima melalui ledger.");
            } else showAlert(Alert.AlertType.ERROR, "Penerimaan Ditolak",
                    "Aksi hanya berlaku untuk STO lama berstatus DISPATCHED tanpa riwayat scan.");
        });
        task.setOnFailed(event -> {
            log.error("Gagal menerima STO lama {}", order.getOrderCode(), task.getException());
            showAlert(Alert.AlertType.ERROR, "Gagal", "Penerimaan STO dibatalkan.");
        });
        Thread thread = new Thread(task, "sto-legacy-receive");
        thread.setDaemon(true);
        thread.start();
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

    private void handleSalesOrders(TableView<DeliveryOrder> deliveryOrdersTable) {
        Task<List<SalesOrder>> task = new Task<>() {
            @Override
            protected List<SalesOrder> call() {
                return salesOrderService.getRecentSalesOrders();
            }
        };
        task.setOnSucceeded(event -> {
            TableView<SalesOrder> table = new TableView<>(FXCollections.observableArrayList(task.getValue()));
            table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_FLEX_LAST_COLUMN);
            table.getColumns().add(salesOrderColumn("Nomor SO", SalesOrder::getOrderCode));
            table.getColumns().add(salesOrderColumn("Customer", order -> order.getCustomer().getNamaCustomer()));
            table.getColumns().add(salesOrderColumn("Jenis barang", order -> Integer.toString(order.getLines().size())));
            table.getColumns().add(salesOrderColumn("Jumlah belum dikonversi", order -> Integer.toString(order.getLines().stream()
                    .mapToInt(SalesOrderLine::getRemainingQuantity).sum())));
            table.getColumns().add(salesOrderColumn("Status", order -> switch (order.getStatus()) {
                case OPEN -> "Terbuka";
                case PARTIALLY_CONVERTED -> "Sebagian menjadi DO";
                case CONVERTED -> "Selesai dikonversi";
            }));
            table.setPlaceholder(new Label("Belum ada Sales Order."));
            Button create = deliveryOrderButton("Buat Sales Order", "Buat pesanan customer yang dapat dikonversi menjadi DO.");
            Button convert = deliveryOrderButton("Buat DO dari SO terpilih", "Konversi sisa jumlah Sales Order terpilih menjadi Delivery Order.");
            convert.setDisable(true);
            table.getSelectionModel().selectedItemProperty().addListener((observable, oldValue, selected) ->
                    convert.setDisable(selected == null
                            || selected.getStatus() == com.mycompany.tugas_akhir.model.SalesOrderStatus.CONVERTED));
            create.setOnAction(action -> handleCreateSalesOrder(table));
            convert.setOnAction(action -> {
                SalesOrder selected = table.getSelectionModel().getSelectedItem();
                if (selected == null) return;
                Integer userId = currentUser == null ? null : currentUser.getId();
                Task<DeliveryOrder> convertTask = new Task<>() {
                    @Override
                    protected DeliveryOrder call() {
                        return salesOrderService.convertToDeliveryOrder(selected.getId(), userId);
                    }
                };
                convertTask.setOnSucceeded(convertEvent -> {
                    DeliveryOrder deliveryOrder = convertTask.getValue();
                    if (deliveryOrder == null) {
                        showAlert(Alert.AlertType.ERROR, "Konversi Gagal", "Sales order tidak memiliki kuantitas tersisa.");
                        return;
                    }
                    loadDeliveryOrders(deliveryOrdersTable);
                    Task<List<SalesOrder>> refreshTask = new Task<>() {
                        @Override
                        protected List<SalesOrder> call() {
                            return salesOrderService.getRecentSalesOrders();
                        }
                    };
                    refreshTask.setOnSucceeded(refreshEvent -> table.getItems().setAll(refreshTask.getValue()));
                    refreshTask.setOnFailed(refreshEvent ->
                            log.error("Gagal memuat ulang sales order", refreshTask.getException()));
                    Thread refreshThread = new Thread(refreshTask, "sales-order-refresh");
                    refreshThread.setDaemon(true);
                    refreshThread.start();
                    showAlert(Alert.AlertType.INFORMATION, "DO Dibuat",
                            deliveryOrder.getOrderCode() + " dibuat dari " + selected.getOrderCode() + ".");
                });
                convertTask.setOnFailed(convertEvent -> {
                    log.error("Gagal membuat DO dari SO {}", selected.getOrderCode(), convertTask.getException());
                    showAlert(Alert.AlertType.ERROR, "Konversi Gagal", "Sales order tidak dapat dikonversi.");
                });
                Thread thread = new Thread(convertTask, "sales-order-convert");
                thread.setDaemon(true);
                thread.start();
            });

            Dialog<ButtonType> dialog = new Dialog<>();
            dialog.setTitle("Sales Order");
            dialog.setHeaderText("Pilih SO untuk melihat sisa pesanan atau membuat DO.");
            dialog.getDialogPane().getButtonTypes().add(ButtonType.CLOSE);
            HBox actions = new HBox(8, create, convert);
            VBox content = new VBox(10, actions, table);
            content.setPadding(new javafx.geometry.Insets(12));
            dialog.getDialogPane().setContent(content);
            dialog.getDialogPane().setPrefSize(850, 500);
            styleDeliveryOrderDialog(dialog);
            VBox.setVgrow(table, javafx.scene.layout.Priority.ALWAYS);
            dialog.showAndWait();
        });
        task.setOnFailed(event -> {
            log.error("Gagal memuat sales order", task.getException());
            showAlert(Alert.AlertType.ERROR, "Gagal", "Daftar sales order tidak dapat dimuat.");
        });
        Thread thread = new Thread(task, "sales-order-list");
        thread.setDaemon(true);
        thread.start();
    }

    private void handleCreateSalesOrder(TableView<SalesOrder> orderTable) {
        if (barangItems == null || customerItems == null || currentUser == null || currentUser.getId() == null) {
            showAlert(Alert.AlertType.INFORMATION, "Data Belum Siap",
                    "Pastikan sesi aktif dan data barang serta customer sudah dimuat.");
            return;
        }
        List<Customer> activeCustomers = customerItems.stream()
                .filter(customer -> !"Nonaktif".equalsIgnoreCase(customer.getStatus())).toList();
        ComboBox<Customer> customerBox = new ComboBox<>(FXCollections.observableArrayList(activeCustomers));
        ComboBox<Barang> itemBox = new ComboBox<>(FXCollections.observableArrayList(barangItems));
        customerBox.setConverter(customerNameConverter());
        itemBox.setConverter(barangNameConverter());
        TextField quantityField = new TextField("1");
        Map<Integer, Integer> quantities = new LinkedHashMap<>();
        ObservableList<String> summary = FXCollections.observableArrayList();
        Button addLine = new Button("Tambah Barang");
        addLine.setOnAction(event -> {
            Barang item = itemBox.getValue();
            int quantity;
            try {
                quantity = Integer.parseInt(quantityField.getText().trim());
                if (quantity <= 0) throw new NumberFormatException();
            } catch (NumberFormatException exception) {
                showAlert(Alert.AlertType.WARNING, "Jumlah Tidak Valid", "Jumlah harus bilangan bulat positif.");
                return;
            }
            if (item == null) {
                showAlert(Alert.AlertType.WARNING, "Pilih Barang", "Pilih barang untuk ditambahkan.");
                return;
            }
            try {
                quantities.merge(item.getId(), quantity, Math::addExact);
            } catch (ArithmeticException exception) {
                showAlert(Alert.AlertType.WARNING, "Jumlah Terlalu Besar", "Kuantitas melampaui batas.");
                return;
            }
            summary.setAll(quantities.entrySet().stream().map(entry -> {
                Barang lineItem = barangItems.stream().filter(value -> value.getId().equals(entry.getKey()))
                        .findFirst().orElseThrow();
                return lineItem.getKodeBarang() + " - " + lineItem.getNamaBarang() + " | " + entry.getValue();
            }).toList());
            quantityField.setText("1");
        });
        ListView<String> lines = new ListView<>(summary);
        lines.setPrefHeight(150);
        TextArea notes = new TextArea();
        notes.setPromptText("Catatan pesanan (opsional)");
        notes.setPrefRowCount(2);
        GridPane grid = new GridPane();
        grid.addRow(0, new Label("Customer *"), customerBox);
        grid.addRow(1, new Label("Barang"), itemBox);
        grid.addRow(2, new Label("Jumlah"), quantityField, addLine);
        grid.addRow(3, new Label("Detail SO"), lines);
        grid.addRow(4, new Label("Catatan"), notes);
        configureDeliveryOrderFormGrid(grid);
        customerBox.setPromptText("Pilih customer pemesan");
        itemBox.setPromptText("Pilih barang untuk SO");
        quantityField.setPromptText("Jumlah pesanan");
        quantityField.setPrefWidth(140);
        addLine.setTooltip(new Tooltip("Tambahkan barang dan jumlah ke daftar Sales Order."));
        lines.setPlaceholder(new Label("Belum ada barang. Pilih barang lalu tekan Tambah Barang."));
        Dialog<ButtonType> dialog = new Dialog<>();
        dialog.setTitle("Buat Sales Order");
        dialog.setHeaderText("Tentukan customer dan barang yang dipesan. SO dapat dikonversi ke DO nanti.");
        dialog.getDialogPane().setContent(grid);
        dialog.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);
        styleDeliveryOrderDialog(dialog);
        dialog.getDialogPane().setPrefSize(680, 500);
        Optional<ButtonType> result = dialog.showAndWait();
        Customer customer = customerBox.getValue();
        if (result.isEmpty() || result.get() != ButtonType.OK) return;
        if (customer == null || quantities.isEmpty()) {
            showAlert(Alert.AlertType.WARNING, "SO Belum Lengkap", "Pilih customer dan tambahkan barang.");
            return;
        }
        Integer userId = currentUser.getId();
        Task<SalesOrder> saveTask = new Task<>() {
            @Override
            protected SalesOrder call() {
                return salesOrderService.createSalesOrder(customer.getId(), userId,
                        Map.copyOf(quantities), notes.getText().trim());
            }
        };
        saveTask.setOnSucceeded(event -> {
            if (saveTask.getValue() == null) {
                showAlert(Alert.AlertType.ERROR, "SO Ditolak", "Sales order tidak valid.");
                return;
            }
            Task<List<SalesOrder>> refreshTask = new Task<>() {
                @Override
                protected List<SalesOrder> call() { return salesOrderService.getRecentSalesOrders(); }
            };
            refreshTask.setOnSucceeded(refreshEvent -> orderTable.getItems().setAll(refreshTask.getValue()));
            refreshTask.setOnFailed(refreshEvent ->
                    log.error("Gagal memuat ulang sales order", refreshTask.getException()));
            Thread thread = new Thread(refreshTask, "sales-order-refresh");
            thread.setDaemon(true);
            thread.start();
        });
        saveTask.setOnFailed(event -> {
            log.error("Gagal membuat sales order", saveTask.getException());
            showAlert(Alert.AlertType.ERROR, "Gagal", "Sales order tidak dapat disimpan.");
        });
        Thread thread = new Thread(saveTask, "sales-order-create");
        thread.setDaemon(true);
        thread.start();
    }

    private TableColumn<SalesOrder, String> salesOrderColumn(String title,
                                                             java.util.function.Function<SalesOrder, String> value) {
        TableColumn<SalesOrder, String> column = new TableColumn<>(title);
        column.setCellValueFactory(cell -> new SimpleStringProperty(value.apply(cell.getValue())));
        return column;
    }

    private void handleAssignPicker(TableView<DeliveryOrder> table) {
        DeliveryOrder order = table.getSelectionModel().getSelectedItem();
        if (order == null || currentUser == null || currentUser.getId() == null) {
            showAlert(Alert.AlertType.WARNING, "Sesi atau DO Tidak Valid",
                    "Pilih DO dan pastikan sesi pengguna masih aktif.");
            return;
        }
        Task<PickerAssignmentOptions> task = new Task<>() {
            @Override
            protected PickerAssignmentOptions call() {
                return new PickerAssignmentOptions(userService.getUsersForAssignment(),
                        sectionService.getAllSections());
            }
        };
        task.setOnSucceeded(event -> {
            ComboBox<User> pickers = new ComboBox<>(FXCollections.observableArrayList(task.getValue().pickers()));
            pickers.setConverter(new javafx.util.StringConverter<>() {
                @Override
                public String toString(User picker) {
                    return picker == null ? "" : picker.getFullName() + " (" + picker.getEmail() + ")";
                }

                @Override
                public User fromString(String value) {
                    return null;
                }
            });
            pickers.setValue(task.getValue().pickers().stream()
                    .filter(picker -> java.util.Objects.equals(picker.getId(), currentUser.getId()))
                    .findFirst().orElse(null));
            pickers.setPromptText("Pilih picker");
            ComboBox<com.mycompany.tugas_akhir.model.Section> sections =
                    new ComboBox<>(FXCollections.observableArrayList(task.getValue().sections()));
            sections.setConverter(new javafx.util.StringConverter<>() {
                @Override
                public String toString(com.mycompany.tugas_akhir.model.Section section) {
                    return section == null ? "" : section.getKodeSection() + " — " + section.getNamaSection();
                }

                @Override
                public com.mycompany.tugas_akhir.model.Section fromString(String value) {
                    return null;
                }
            });
            sections.setPromptText("Pilih zona / section");
            pickers.setMaxWidth(Double.MAX_VALUE);
            sections.setMaxWidth(Double.MAX_VALUE);
            Dialog<ButtonType> dialog = new Dialog<>();
            dialog.setTitle("Tugaskan Picker");
            dialog.setHeaderText("DO: " + order.getOrderCode());
            dialog.getDialogPane().setContent(new VBox(8,
                    new Label("Pilih petugas yang akan mengambil barang."), new Label("Picker"), pickers,
                    new Label("Pilih area gudang yang menjadi tanggung jawab picker."),
                    new Label("Zona / Section"), sections));
            dialog.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);
            styleDeliveryOrderDialog(dialog);
            dialog.getDialogPane().setPrefWidth(480);
            Optional<ButtonType> result = dialog.showAndWait();
            if (result.isEmpty() || result.get() != ButtonType.OK) return;
            if (pickers.getValue() == null || sections.getValue() == null) {
                showAlert(Alert.AlertType.WARNING, "Data Wajib", "Pilih picker dan section untuk tugas picking.");
                return;
            }
            User selectedPicker = pickers.getValue();
            Integer pickerId = selectedPicker.getId();
            Integer sectionId = sections.getValue().getId();
            Task<Boolean> assignTask = new Task<>() {
                @Override
                protected Boolean call() {
                    return deliveryOrderService.assignPicker(order.getId(), pickerId, sectionId);
                }
            };
            assignTask.setOnSucceeded(assignEvent -> {
                if (assignTask.getValue()) {
                    showAlert(Alert.AlertType.INFORMATION, "Pick Task Ditugaskan",
                            "Tugas untuk " + order.getOrderCode() + " ditugaskan ke "
                                    + selectedPicker.getFullName() + " di section "
                                    + sections.getValue().getKodeSection() + ".");
                } else {
                    showAlert(Alert.AlertType.ERROR, "Assignment Ditolak",
                            "DO harus berstatus READY_TO_PICK dan memiliki tugas aktif.");
                }
            });
            assignTask.setOnFailed(assignEvent -> {
                log.error("Gagal assign pick task {}", order.getOrderCode(), assignTask.getException());
                showAlert(Alert.AlertType.ERROR, "Gagal", "Tugas picking tidak dapat ditugaskan.");
            });
            Thread assignThread = new Thread(assignTask, "pick-task-assign");
            assignThread.setDaemon(true);
            assignThread.start();
        });
        task.setOnFailed(event -> {
            log.error("Gagal memuat zone/section", task.getException());
            showAlert(Alert.AlertType.ERROR, "Gagal", "Daftar zone tidak dapat dimuat.");
        });
        Thread thread = new Thread(task, "pick-task-sections");
        thread.setDaemon(true);
        thread.start();
    }

    private void handleViewReservations(TableView<DeliveryOrder> table) {
        DeliveryOrder order = table.getSelectionModel().getSelectedItem();
        if (order == null) {
            showAlert(Alert.AlertType.WARNING, "Pilih DO", "Pilih delivery order terlebih dahulu.");
            return;
        }
        Task<List<DeliveryOrderLotReservation>> task = new Task<>() {
            @Override
            protected List<DeliveryOrderLotReservation> call() {
                return deliveryOrderService.getReservations(order.getId());
            }
        };
        task.setOnSucceeded(event -> {
            TableView<DeliveryOrderLotReservation> reservationTable =
                    new TableView<>(FXCollections.observableArrayList(task.getValue()));
            reservationTable.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_FLEX_LAST_COLUMN);
            reservationTable.getColumns().add(reservationColumn("Barang",
                    value -> value.getDeliveryOrderLine().getBarang().getKodeBarang()));
            reservationTable.getColumns().add(reservationColumn("Lokasi (SLOC)",
                    value -> value.getLotBalance().getRak() == null
                            ? "-" : value.getLotBalance().getRak().getKodeRak()));
            reservationTable.getColumns().add(reservationColumn("Batch",
                    value -> value.getLotBalance().getBatchCode()));
            reservationTable.getColumns().add(reservationColumn("Kedaluwarsa",
                    value -> value.getLotBalance().getExpiryDate() == null
                            ? "-" : value.getLotBalance().getExpiryDate().toString()));
            reservationTable.getColumns().add(reservationColumn("Dialokasikan",
                    value -> String.valueOf(value.getReservedQuantity())));
            reservationTable.getColumns().add(reservationColumn("Diambil",
                    value -> String.valueOf(value.getPickedQuantity())));
            reservationTable.getColumns().add(reservationColumn("Dikemas",
                    value -> String.valueOf(value.getPackedQuantity())));
            reservationTable.getColumns().add(reservationColumn("Dimuat",
                    value -> String.valueOf(value.getLoadedQuantity())));
            reservationTable.setPlaceholder(new Label("Belum ada reservasi lot untuk DO ini."));
            String sourceOrder = order.getSalesOrder() == null ? "-"
                    : order.getSalesOrder().getOrderCode();
            Dialog<ButtonType> dialog = new Dialog<>();
            dialog.setTitle("Alokasi Stok — " + order.getOrderCode());
            dialog.setHeaderText("Rincian lokasi dan batch stok untuk DO " + order.getOrderCode()
                    + " | Sales Order: " + sourceOrder);
            dialog.getDialogPane().setContent(reservationTable);
            dialog.getDialogPane().getButtonTypes().add(ButtonType.CLOSE);
            dialog.getDialogPane().setPrefSize(950, 420);
            styleDeliveryOrderDialog(dialog);
            dialog.showAndWait();
        });
        task.setOnFailed(event -> {
            log.error("Gagal memuat reservasi DO {}", order.getOrderCode(), task.getException());
            showAlert(Alert.AlertType.ERROR, "Gagal", "Rincian reservasi tidak dapat dimuat.");
        });
        Thread thread = new Thread(task, "do-reservations");
        thread.setDaemon(true);
        thread.start();
    }

    private TableColumn<DeliveryOrderLotReservation, String> reservationColumn(
            String title, java.util.function.Function<DeliveryOrderLotReservation, String> value) {
        TableColumn<DeliveryOrderLotReservation, String> column = new TableColumn<>(title);
        column.setCellValueFactory(cell -> new SimpleStringProperty(value.apply(cell.getValue())));
        return column;
    }

    private void handleViewPickTasks(TableView<DeliveryOrder> table) {
        DeliveryOrder order = table.getSelectionModel().getSelectedItem();
        if (order == null) {
            showAlert(Alert.AlertType.WARNING, "Pilih DO", "Pilih delivery order terlebih dahulu.");
            return;
        }
        Task<List<PickTask>> task = new Task<>() {
            @Override
            protected List<PickTask> call() {
                return deliveryOrderService.getPickTasks(order.getId());
            }
        };
        task.setOnSucceeded(event -> {
            TableView<PickTask> taskTable = new TableView<>(FXCollections.observableArrayList(task.getValue()));
            taskTable.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_FLEX_LAST_COLUMN);
            taskTable.getColumns().add(pickTaskColumn("Kode tugas", PickTask::getTaskCode));
            taskTable.getColumns().add(pickTaskColumn("Status", value -> value.getStatus().name().replace('_', ' ')));
            taskTable.getColumns().add(pickTaskColumn("Petugas", value -> value.getAssignedPicker() == null
                    ? "-" : value.getAssignedPicker().getFullName()));
            taskTable.getColumns().add(pickTaskColumn("Zona", value -> value.getAssignedSection() == null
                    ? "-" : value.getAssignedSection().getKodeSection()));
            taskTable.getColumns().add(pickTaskColumn("Waktu penugasan", value -> value.getAssignedAt() == null
                    ? "-" : value.getAssignedAt().format(DateTimeFormatter.ofPattern("dd MMM yyyy HH:mm"))));
            taskTable.setPlaceholder(new Label("Belum ada pick task aktif."));
            Dialog<ButtonType> dialog = new Dialog<>();
            dialog.setTitle("Tugas Picking — " + order.getOrderCode());
            dialog.setHeaderText("Tugas dan penanggung jawab picking untuk pesanan ini.");
            dialog.getDialogPane().setContent(taskTable);
            dialog.getDialogPane().getButtonTypes().add(ButtonType.CLOSE);
            dialog.getDialogPane().setPrefSize(720, 360);
            styleDeliveryOrderDialog(dialog);
            dialog.showAndWait();
        });
        task.setOnFailed(event -> {
            log.error("Gagal memuat pick task untuk {}", order.getOrderCode(), task.getException());
            showAlert(Alert.AlertType.ERROR, "Gagal", "Pick task tidak dapat dimuat.");
        });
        Thread thread = new Thread(task, "pick-task-details");
        thread.setDaemon(true);
        thread.start();
    }

    private TableColumn<PickTask, String> pickTaskColumn(String title,
                                                          java.util.function.Function<PickTask, String> value) {
        TableColumn<PickTask, String> column = new TableColumn<>(title);
        column.setCellValueFactory(cell -> new SimpleStringProperty(value.apply(cell.getValue())));
        return column;
    }

    private void handleViewFulfillmentAudit(TableView<DeliveryOrder> table) {
        DeliveryOrder order = table.getSelectionModel().getSelectedItem();
        if (order == null) {
            showAlert(Alert.AlertType.WARNING, "Pilih DO", "Pilih delivery order terlebih dahulu.");
            return;
        }
        Task<List<DeliveryOrderFulfillmentAudit>> task = new Task<>() {
            @Override
            protected List<DeliveryOrderFulfillmentAudit> call() {
                return deliveryOrderService.getFulfillmentAudit(order.getId());
            }
        };
        task.setOnSucceeded(event -> {
            TableView<DeliveryOrderFulfillmentAudit> auditTable =
                    new TableView<>(FXCollections.observableArrayList(task.getValue()));
            auditTable.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_FLEX_LAST_COLUMN);
            auditTable.getColumns().add(fulfillmentAuditColumn("Tahap", DeliveryOrderFulfillmentAudit::getStage));
            auditTable.getColumns().add(fulfillmentAuditColumn("Kode barang",
                    audit -> audit.getDeliveryOrderLine().getBarang().getKodeBarang()));
            auditTable.getColumns().add(fulfillmentAuditColumn("Jumlah",
                    audit -> String.valueOf(audit.getVerifiedQuantity())));
            auditTable.getColumns().add(fulfillmentAuditColumn("Operator",
                    audit -> audit.getActor().getFullName()));
            auditTable.getColumns().add(fulfillmentAuditColumn("Waktu pencatatan", audit ->
                    audit.getRecordedAt().format(DateTimeFormatter.ofPattern("dd MMM yyyy HH:mm"))));
            auditTable.setPlaceholder(new Label("Belum ada audit packing/loading."));
            Dialog<ButtonType> dialog = new Dialog<>();
            dialog.setTitle("Audit Packing & Loading — " + order.getOrderCode());
            dialog.setHeaderText("Riwayat verifikasi jumlah barang pada setiap tahap pemenuhan.");
            dialog.getDialogPane().setContent(auditTable);
            dialog.getDialogPane().getButtonTypes().add(ButtonType.CLOSE);
            dialog.getDialogPane().setPrefSize(760, 400);
            styleDeliveryOrderDialog(dialog);
            dialog.showAndWait();
        });
        task.setOnFailed(event -> {
            log.error("Gagal memuat audit fulfillment untuk {}", order.getOrderCode(), task.getException());
            showAlert(Alert.AlertType.ERROR, "Gagal", "Audit packing/loading tidak dapat dimuat.");
        });
        Thread thread = new Thread(task, "delivery-order-fulfillment-audit");
        thread.setDaemon(true);
        thread.start();
    }

    private TableColumn<DeliveryOrderFulfillmentAudit, String> fulfillmentAuditColumn(
            String title, java.util.function.Function<DeliveryOrderFulfillmentAudit, String> value) {
        TableColumn<DeliveryOrderFulfillmentAudit, String> column = new TableColumn<>(title);
        column.setCellValueFactory(cell -> new SimpleStringProperty(value.apply(cell.getValue())));
        return column;
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
        customerBox.setConverter(customerNameConverter());
        ComboBox<Barang> barangBox = new ComboBox<>(FXCollections.observableArrayList(barangItems));
        barangBox.setConverter(barangNameConverter());
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
        grid.addRow(0, new Label("Customer *"), customerBox);
        grid.addRow(1, new Label("Barang"), barangBox);
        grid.addRow(2, new Label("Jumlah"), quantityField, addLine);
        grid.addRow(3, new Label("Detail DO"), lineList);
        grid.addRow(4, new Label("Catatan"), notesArea);
        configureDeliveryOrderFormGrid(grid);
        customerBox.setPromptText("Pilih customer penerima");
        barangBox.setPromptText("Pilih barang untuk DO");
        quantityField.setPromptText("Jumlah pesanan");
        quantityField.setPrefWidth(140);
        addLine.setTooltip(new Tooltip("Tambahkan barang dan jumlah ke daftar DO."));
        lineList.setPlaceholder(new Label("Belum ada barang. Pilih barang lalu tekan Tambah Barang."));
        notesArea.setPromptText("Catatan pengiriman (opsional)");
        dialog.setHeaderText("Pilih customer, tambahkan barang dan jumlah, lalu simpan sebagai draft.");
        dialog.getDialogPane().setContent(grid);
        dialog.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);
        styleDeliveryOrderDialog(dialog);
        dialog.getDialogPane().setPrefSize(680, 500);
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
        if (order == null || currentUser == null || currentUser.getId() == null) {
            showAlert(Alert.AlertType.WARNING, "Pilih DO", "Pilih DO dan pastikan daftar SLOC tersedia.");
            return;
        }
        ComboBox<DeliveryOrderLine> lineBox = new ComboBox<>(FXCollections.observableArrayList(order.getLines()));
        lineBox.setConverter(new javafx.util.StringConverter<>() {
            @Override
            public String toString(DeliveryOrderLine line) {
                return line == null ? "" : line.getBarang().getKodeBarang() + " — "
                        + line.getBarang().getNamaBarang() + " | Sisa: " + line.getRemainingQuantity();
            }

            @Override
            public DeliveryOrderLine fromString(String value) {
                return null;
            }
        });
        TextField itemQrField = new TextField();
        itemQrField.setPromptText("Pindai QR barang (WMS1)");
        TextField locationBarcodeField = new TextField();
        locationBarcodeField.setPromptText("Pindai barcode lokasi (SLOC1)");
        TextField quantityField = new TextField("1");
        GridPane grid = new GridPane();
        grid.addRow(0, new Label("Baris barang DO"), lineBox);
        grid.addRow(1, new Label("Picker aktif"), new Label(currentUser.getFullName()));
        grid.addRow(2, new Label("QR barang"), itemQrField);
        grid.addRow(3, new Label("Barcode lokasi"), locationBarcodeField);
        grid.addRow(4, new Label("Jumlah dipindai"), quantityField);
        configureDeliveryOrderFormGrid(grid);
        Dialog<ButtonType> dialog = new Dialog<>();
        dialog.setTitle("Catat Scan Picking — " + order.getOrderCode());
        dialog.setHeaderText("Pilih barang pada DO, lalu pindai QR barang dan barcode lokasi asal.");
        dialog.getDialogPane().setContent(grid);
        dialog.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);
        styleDeliveryOrderDialog(dialog);
        dialog.getDialogPane().setPrefSize(680, 390);
        Optional<ButtonType> result = dialog.showAndWait();
        if (result.isEmpty() || result.get() != ButtonType.OK) return;

        DeliveryOrderLine line = lineBox.getValue();
        int quantity;
        try {
            quantity = Integer.parseInt(quantityField.getText().trim());
            if (quantity <= 0) throw new NumberFormatException();
        } catch (NumberFormatException ex) {
            showAlert(Alert.AlertType.WARNING, "Jumlah Tidak Valid", "Jumlah scan harus berupa angka positif.");
            return;
        }
        if (line == null || itemQrField.getText().isBlank() || locationBarcodeField.getText().isBlank()) {
            showAlert(Alert.AlertType.WARNING, "Scan Tidak Lengkap",
                    "DO line, QR item, barcode SLOC, dan jumlah wajib diisi.");
            return;
        }
        Integer pickerId = currentUser == null ? null : currentUser.getId();
        if (pickerId == null) return;

        Task<Boolean> task = new Task<>() {
            @Override
            protected Boolean call() {
                return deliveryOrderService.recordPickWithBarcodes(order.getId(), line.getId(),
                        itemQrField.getText().trim(), locationBarcodeField.getText().trim(), quantity, pickerId);
            }
        };
        task.setOnSucceeded(event -> {
            if (task.getValue()) {
                showAlert(Alert.AlertType.INFORMATION, "Scan Valid", quantity + " unit tercatat untuk " + line);
                loadDeliveryOrders(table);
            } else {
                showAlert(Alert.AlertType.ERROR, "Scan Ditolak",
                        "QR item/SLOC, batch/expiry, assignment picker, status DO, atau jumlah tidak cocok dengan reservasi.");
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

    private void handleCompletePacking(TableView<DeliveryOrder> table) {
        handleFulfillmentQuantities(table, false);
    }

    private void handleCompleteLoading(TableView<DeliveryOrder> table) {
        handleFulfillmentQuantities(table, true);
    }

    private void handleFulfillmentQuantities(TableView<DeliveryOrder> table, boolean loading) {
        DeliveryOrder order = table.getSelectionModel().getSelectedItem();
        Integer actorId = currentUser == null ? null : currentUser.getId();
        if (order == null || actorId == null) {
            showAlert(Alert.AlertType.WARNING, "Pilih DO", "Pilih DO dan pastikan pengguna aktif.");
            return;
        }

        Map<Integer, TextField> fields = new LinkedHashMap<>();
        GridPane grid = new GridPane();
        grid.addRow(0, new Label("Barang"), new Label("Jumlah " + (loading ? "dimuat" : "dikemas")));
        int row = 1;
        for (DeliveryOrderLine line : order.getLines()) {
            TextField quantityField = new TextField();
            quantityField.setPromptText("0 bila tidak " + (loading ? "dimuat" : "dikemas"));
            fields.put(line.getId(), quantityField);
            grid.addRow(row++, new Label(line.getBarang().getKodeBarang() + " — "
                    + line.getBarang().getNamaBarang() + " (sisa DO: "
                    + line.getRemainingQuantity() + ")"), quantityField);
        }
        configureDeliveryOrderFormGrid(grid);

        Dialog<ButtonType> dialog = new Dialog<>();
        dialog.setTitle(loading ? "Konfirmasi Barang Dimuat" : "Konfirmasi Barang Dikemas");
        dialog.setHeaderText("Masukkan jumlah aktual untuk setiap barang. Isi 0 jika tidak diproses pada tahap ini.");
        dialog.getDialogPane().setContent(grid);
        dialog.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);
        styleDeliveryOrderDialog(dialog);
        dialog.getDialogPane().setPrefSize(720, Math.min(620, 220 + fields.size() * 48));
        Optional<ButtonType> result = dialog.showAndWait();
        if (result.isEmpty() || result.get() != ButtonType.OK) return;

        Map<Integer, Integer> quantities = new LinkedHashMap<>();
        try {
            for (Map.Entry<Integer, TextField> entry : fields.entrySet()) {
                int quantity = Integer.parseInt(entry.getValue().getText().trim());
                if (quantity < 0) throw new NumberFormatException();
                quantities.put(entry.getKey(), quantity);
            }
        } catch (NumberFormatException ex) {
            showAlert(Alert.AlertType.WARNING, "Jumlah Tidak Valid",
                    "Isi setiap jumlah dengan bilangan bulat nol atau lebih.");
            return;
        }

        Task<Boolean> task = new Task<>() {
            @Override
            protected Boolean call() {
                return loading
                        ? deliveryOrderService.completeLoading(order.getId(), quantities, actorId)
                        : deliveryOrderService.completePacking(order.getId(), quantities, actorId);
            }
        };
        task.setOnSucceeded(event -> {
            if (task.getValue()) {
                loadDeliveryOrders(table);
                showAlert(Alert.AlertType.INFORMATION, "Kuantitas Tercatat",
                        "Kuantitas aktual " + (loading ? "loading" : "packing") + " berhasil dicatat.");
            } else {
                showAlert(Alert.AlertType.ERROR, "Kuantitas Ditolak",
                        "Periksa jumlah per barang; jumlah tidak boleh melebihi tahap sebelumnya dan harus ada kuantitas positif.");
            }
        });
        task.setOnFailed(event -> {
            log.error("Gagal mencatat kuantitas DO", task.getException());
            showAlert(Alert.AlertType.ERROR, "DO Gagal", "Kuantitas tidak dapat dicatat.");
        });
        Thread thread = new Thread(task, loading ? "delivery-order-loading" : "delivery-order-packing");
        thread.setDaemon(true);
        thread.start();
    }

    private void handlePickScanHistory(TableView<DeliveryOrder> table) {
        DeliveryOrder order = table.getSelectionModel().getSelectedItem();
        if (order == null) {
            showAlert(Alert.AlertType.WARNING, "Pilih DO", "Pilih delivery order terlebih dahulu.");
            return;
        }
        Task<List<DeliveryOrderPickScan>> task = new Task<>() {
            @Override
            protected List<DeliveryOrderPickScan> call() {
                return deliveryOrderService.getPickScans(order.getId());
            }
        };
        task.setOnSucceeded(event -> {
            TableView<DeliveryOrderPickScan> scans = new TableView<>();
            scans.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_FLEX_LAST_COLUMN);
            scans.getColumns().add(scanColumn("Waktu", scan -> scan.getScannedAt()
                    .format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss"))));
            scans.getColumns().add(scanColumn("Picker", scan -> scan.getPicker().getFullName()));
            scans.getColumns().add(scanColumn("Barang", scan -> scan.getScannedSku()));
            scans.getColumns().add(scanColumn("SLOC", DeliveryOrderPickScan::getScannedRack));
            scans.getColumns().add(scanColumn("Batch", DeliveryOrderPickScan::getScannedBatch));
            scans.getColumns().add(scanColumn("Jumlah", scan -> String.valueOf(scan.getQuantity())));
            scans.getItems().setAll(task.getValue());
            scans.setPlaceholder(new Label("Belum ada scan pick."));
            Dialog<ButtonType> dialog = new Dialog<>();
            dialog.setTitle("Riwayat Scan " + order.getOrderCode());
            dialog.setHeaderText("Daftar barang dan lokasi yang berhasil dipindai untuk pesanan ini.");
            dialog.getDialogPane().setContent(scans);
            dialog.getDialogPane().setPrefSize(760, 420);
            dialog.getDialogPane().getButtonTypes().add(ButtonType.CLOSE);
            styleDeliveryOrderDialog(dialog);
            dialog.showAndWait();
        });
        task.setOnFailed(event -> {
            log.error("Gagal memuat riwayat scan DO", task.getException());
            showAlert(Alert.AlertType.ERROR, "Riwayat Scan Gagal", "Riwayat scan tidak dapat dimuat.");
        });
        Thread thread = new Thread(task, "delivery-order-pick-history");
        thread.setDaemon(true);
        thread.start();
    }

    private TableColumn<DeliveryOrderPickScan, String> scanColumn(
            String title, java.util.function.Function<DeliveryOrderPickScan, String> value) {
        TableColumn<DeliveryOrderPickScan, String> column = new TableColumn<>(title);
        column.setCellValueFactory(cell -> new SimpleStringProperty(value.apply(cell.getValue())));
        return column;
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
        CheckBox useBarcodes = new CheckBox("Validasi dengan scan QR item dan barcode kedua SLOC");
        TextField itemQr = new TextField();
        itemQr.setPromptText("WMS1 item QR");
        TextField sourceLocationQr = new TextField();
        sourceLocationQr.setPromptText("SLOC1 sumber");
        TextField destinationLocationQr = new TextField();
        destinationLocationQr.setPromptText("SLOC1 tujuan");
        itemQr.setDisable(true);
        sourceLocationQr.setDisable(true);
        destinationLocationQr.setDisable(true);
        useBarcodes.selectedProperty().addListener((observable, oldValue, selected) -> {
            itemQr.setDisable(!selected);
            sourceLocationQr.setDisable(!selected);
            destinationLocationQr.setDisable(!selected);
            cbTransferBarang.setDisable(selected);
            cbSource.setDisable(selected);
            cbDestination.setDisable(selected);
        });

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
        grid.add(useBarcodes, 0, 5, 2, 1);
        grid.add(new Label("QR barang"), 0, 6);
        grid.add(itemQr, 1, 6);
        grid.add(new Label("Barcode SLOC sumber"), 0, 7);
        grid.add(sourceLocationQr, 1, 7);
        grid.add(new Label("Barcode SLOC tujuan"), 0, 8);
        grid.add(destinationLocationQr, 1, 8);
        dialog.getDialogPane().setContent(grid);
        dialog.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);

        dialog.showAndWait().ifPresent(result -> {
            if (result != ButtonType.OK) return;
            Barang barang = cbTransferBarang.getValue();
            String sourceCode = cbSource.getValue();
            String destinationCode = cbDestination.getValue();
            boolean scanWithBarcodes = useBarcodes.isSelected();
            int quantity;
            try {
                quantity = Integer.parseInt(tfTransferQuantity.getText().trim());
                if (quantity <= 0) throw new NumberFormatException();
            } catch (NumberFormatException ex) {
                showAlert(Alert.AlertType.WARNING, "Jumlah Tidak Valid", "Jumlah harus berupa angka positif.");
                return;
            }
            if (!scanWithBarcodes && (barang == null || sourceCode == null || destinationCode == null)) {
                showAlert(Alert.AlertType.WARNING, "Data Belum Lengkap", "Pilih barang, lokasi sumber, dan lokasi tujuan.");
                return;
            }
            if (scanWithBarcodes && (itemQr.getText().isBlank()
                    || sourceLocationQr.getText().isBlank() || destinationLocationQr.getText().isBlank())) {
                showAlert(Alert.AlertType.WARNING, "Barcode Belum Lengkap",
                        "Scan QR barang, SLOC sumber, dan SLOC tujuan.");
                return;
            }

            Integer sourceRakId = scanWithBarcodes ? null
                    : "Stok Belum Dipetakan".equals(sourceCode) ? null : rackItems.stream()
                            .filter(rak -> rak.getKodeRak().equals(sourceCode)).map(Rak::getId).findFirst().orElse(null);
            Integer destinationRakId = scanWithBarcodes ? null : rackItems.stream()
                    .filter(rak -> rak.getKodeRak().equals(destinationCode)).map(Rak::getId).findFirst().orElse(null);
            if (!scanWithBarcodes && (destinationRakId == null
                    || (!"Stok Belum Dipetakan".equals(sourceCode) && sourceRakId == null))) {
                showAlert(Alert.AlertType.ERROR, "Lokasi Tidak Valid", "Lokasi yang dipilih tidak lagi tersedia.");
                return;
            }

            Integer userId = currentUser.getId();
            String notes = taTransferNotes.getText().trim();
            String scannedItemQr = itemQr.getText().trim();
            String scannedSourceQr = sourceLocationQr.getText().trim();
            String scannedDestinationQr = destinationLocationQr.getText().trim();
            Task<Boolean> task = new Task<>() {
                @Override
                protected Boolean call() {
                    if (scanWithBarcodes) {
                        return transferService.transferStockWithBarcodes(scannedItemQr, scannedSourceQr,
                                scannedDestinationQr, quantity, notes, userId);
                    }
                    return transferService.transferStock(barang.getId(), sourceRakId, destinationRakId,
                            quantity, notes, userId);
                }
            };
            task.setOnSucceeded(event -> {
                if (task.getValue()) {
                    showAlert(Alert.AlertType.INFORMATION, "Transfer Berhasil",
                            scanWithBarcodes ? quantity + " unit lot tervalidasi berhasil dipindahkan."
                                    : quantity + " " + barang.getSatuan() + " dipindahkan dari "
                                            + sourceCode + " ke " + destinationCode + ".");
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
