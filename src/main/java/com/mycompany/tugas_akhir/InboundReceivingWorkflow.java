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
public class InboundReceivingWorkflow {
    private static final Logger log = LoggerFactory.getLogger(InboundReceivingWorkflow.class);
    private InboundReceiptService inboundReceiptService;
    private PurchaseOrderService purchaseOrderService;
    private User currentUser;
    private List<Barang> barangItems;
    private List<Supplier> supplierItems;
    private Runnable refreshTransactions;

    public InboundReceivingWorkflow(InboundReceiptService inboundReceiptService, PurchaseOrderService purchaseOrderService) {
        this.inboundReceiptService = inboundReceiptService;
        this.purchaseOrderService = purchaseOrderService;
    }

    public void open(User user, List<Barang> items, List<Supplier> suppliers, Runnable refreshTransactions) {
        this.currentUser = user;
        this.barangItems = items;
        this.supplierItems = suppliers;
        this.refreshTransactions = refreshTransactions;
        handleOpenReceiving();
    }

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
                refreshTransactions.run();
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


    private void showAlert(Alert.AlertType type, String title, String msg) {
        Alert alert = new Alert(type);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(msg);
        alert.showAndWait();
    }
}
