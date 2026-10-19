package com.mycompany.tugas_akhir;


import com.mycompany.tugas_akhir.model.DeliveryOrderLine;
import com.mycompany.tugas_akhir.model.ReturnRequest;
import com.mycompany.tugas_akhir.model.ReturnRequestLine;
import com.mycompany.tugas_akhir.model.ReturnType;
import com.mycompany.tugas_akhir.model.PurchaseOrderLine;
import com.mycompany.tugas_akhir.model.Rak;
import com.mycompany.tugas_akhir.model.User;
import com.mycompany.tugas_akhir.service.DeliveryOrderService;
import com.mycompany.tugas_akhir.service.InboundReceiptService;
import com.mycompany.tugas_akhir.service.PurchaseOrderService;
import com.mycompany.tugas_akhir.service.ReturnRequestService;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.concurrent.Task;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.util.StringConverter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Component
public class ReturnWorkflow {
    private static final Logger log = LoggerFactory.getLogger(ReturnWorkflow.class);
    private final ReturnRequestService returnRequestService;
    private final PurchaseOrderService purchaseOrderService;
    private final DeliveryOrderService deliveryOrderService;
    private final InboundReceiptService inboundReceiptService;
    private User currentUser;
    private List<Rak> rakItems;
    private Runnable refreshTransactions;

    public ReturnWorkflow(ReturnRequestService returnRequestService, PurchaseOrderService purchaseOrderService, DeliveryOrderService deliveryOrderService, InboundReceiptService inboundReceiptService) {
        this.returnRequestService = returnRequestService;
        this.purchaseOrderService = purchaseOrderService;
        this.deliveryOrderService = deliveryOrderService;
        this.inboundReceiptService = inboundReceiptService;
    }

    public void open(User user, List<Rak> rackItems, Runnable refreshTransactions) {
        this.currentUser = user;
        this.rakItems = rackItems;
        this.refreshTransactions = refreshTransactions;
        handleOpenReturns();
    }

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
        CheckBox scanBarcodes = new CheckBox("Validasi item dan SLOC dengan scan barcode");
        VBox entriesBox = new VBox(6);
        List<ReturnEntry> entries = new ArrayList<>();
        Button addLine = new Button("Tambah baris barang");
        addLine.setOnAction(event -> {
            if (entries.size() >= 100) {
                showAlert(Alert.AlertType.WARNING, "Batas Baris", "Satu dokumen retur maksimal 100 baris.");
                return;
            }
            ReturnEntry entry = new ReturnEntry(sources, rakItems, type);
            entry.setScanMode(scanBarcodes.isSelected());
            Button removeLine = new Button("Hapus");
            removeLine.setOnAction(removeEvent -> {
                entries.remove(entry);
                entriesBox.getChildren().remove(entry.row());
            });
            entry.setRemoveButton(removeLine);
            entries.add(entry);
            entriesBox.getChildren().add(entry.row());
        });
        scanBarcodes.selectedProperty().addListener((observable, oldValue, selected) ->
                entries.forEach(entry -> entry.setScanMode(selected)));
        addLine.fire();
        TextArea reasonField = new TextArea();
        reasonField.setPromptText("Alasan retur / hasil pemeriksaan");
        reasonField.setPrefRowCount(2);
        ScrollPane rowsPane = new ScrollPane(entriesBox);
        rowsPane.setFitToWidth(true);
        rowsPane.setPrefViewportHeight(180);
        VBox form = new VBox(8, new Label("Pilih sumber, lokasi, batch, dan jumlah untuk setiap barang."),
                scanBarcodes, rowsPane, addLine, new Label("Alasan retur"), reasonField);
        form.setPadding(new javafx.geometry.Insets(12));
        Dialog<ButtonType> dialog = new Dialog<>();
        dialog.setTitle(type == ReturnType.PURCHASE ? "Buat Purchase Return" : "Buat Sales Return");
        dialog.getDialogPane().setContent(form);
        dialog.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);
        Optional<ButtonType> result = dialog.showAndWait();
        if (result.isEmpty() || result.get() != ButtonType.OK) return;
        boolean useScannedBarcodes = scanBarcodes.isSelected();
        if (entries.isEmpty() || reasonField.getText().isBlank()) {
            showAlert(Alert.AlertType.WARNING, "Data Belum Lengkap",
                    "Tambahkan minimal satu baris retur dan isi alasan.");
            return;
        }
        List<ReturnRequestService.ReturnLineInput> manualLines = new ArrayList<>();
        List<ReturnRequestService.BarcodeReturnLineInput> scannedLines = new ArrayList<>();
        for (ReturnEntry entry : entries) {
            if (entry.source().getValue() == null) {
                showAlert(Alert.AlertType.WARNING, "Sumber Wajib", "Pilih dokumen asal pada setiap baris.");
                return;
            }
            int quantity;
            try {
                quantity = Integer.parseInt(entry.quantity().getText().trim());
                if (quantity <= 0) throw new NumberFormatException();
            } catch (NumberFormatException exception) {
                showAlert(Alert.AlertType.WARNING, "Jumlah Tidak Valid", "Masukkan jumlah bulat lebih dari nol pada semua baris.");
                return;
            }
            Integer sourceId = type == ReturnType.PURCHASE
                    ? ((PurchaseOrderLine) entry.source().getValue()).getId()
                    : ((DeliveryOrderLine) entry.source().getValue()).getId();
            if (useScannedBarcodes) {
                if (entry.itemQr().getText().isBlank() || entry.locationBarcode().getText().isBlank()) {
                    showAlert(Alert.AlertType.WARNING, "Barcode Belum Lengkap", "Scan QR barang dan barcode SLOC pada semua baris.");
                    return;
                }
                scannedLines.add(new ReturnRequestService.BarcodeReturnLineInput(sourceId,
                        entry.itemQr().getText().trim(), entry.locationBarcode().getText().trim(), quantity));
            } else {
                if (entry.rack().getValue() == null || entry.batch().getText().isBlank()) {
                    showAlert(Alert.AlertType.WARNING, "Data Belum Lengkap", "Pilih SLOC dan isi batch pada semua baris.");
                    return;
                }
                manualLines.add(new ReturnRequestService.ReturnLineInput(sourceId, entry.rack().getValue().getId(),
                        entry.batch().getText().trim(), type == ReturnType.SALES ? entry.expiry().getValue() : null,
                        quantity));
            }
        }
        String reason = reasonField.getText().trim();
        Integer creatorId = currentUser.getId();
        Task<ReturnRequest> task = new Task<>() {
            @Override
            protected ReturnRequest call() {
                if (useScannedBarcodes) {
                    return type == ReturnType.PURCHASE
                            ? returnRequestService.createPurchaseReturnWithBarcodes(scannedLines, reason, creatorId)
                            : returnRequestService.createSalesReturnWithBarcodes(scannedLines, reason, creatorId);
                }
                return type == ReturnType.PURCHASE
                        ? returnRequestService.createPurchaseReturn(manualLines, reason, creatorId)
                        : returnRequestService.createSalesReturn(manualLines, reason, creatorId);
            }
        };
        task.setOnSucceeded(event -> {
            if (task.getValue() == null) {
                showAlert(Alert.AlertType.ERROR, "Retur Ditolak",
                        "Salah satu baris tidak valid, melampaui kuantitas sumber, atau tidak cocok dengan scan.");
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

    private static final class ReturnEntry {
        private final ComboBox<Object> source = new ComboBox<>();
        private final ComboBox<Rak> rack = new ComboBox<>();
        private final TextField batch = new TextField();
        private final DatePicker expiry = new DatePicker();
        private final TextField quantity = new TextField();
        private final TextField itemQr = new TextField();
        private final TextField locationBarcode = new TextField();
        private final VBox row = new VBox(5);
        private final HBox manualRow = new HBox(6);
        private final HBox scanRow = new HBox(6);

        private ReturnEntry(List<?> sources, List<Rak> racks, ReturnType type) {
            source.setItems(FXCollections.observableArrayList(sources));
            source.setPromptText(type == ReturnType.PURCHASE ? "Baris PO" : "Baris DO");
            source.setAccessibleText("Baris sumber retur");
            source.setConverter(new StringConverter<>() {
                @Override public String toString(Object value) {
                    if (value instanceof PurchaseOrderLine line) {
                        return line.getPurchaseOrder().getOrderCode() + " · " + line.getBarang().getKodeBarang();
                    }
                    if (value instanceof DeliveryOrderLine line) {
                        return line.getDeliveryOrder().getOrderCode() + " · " + line.getBarang().getKodeBarang();
                    }
                    return "";
                }
                @Override public Object fromString(String value) { return null; }
            });
            rack.setItems(FXCollections.observableArrayList(racks));
            rack.setPromptText("SLOC");
            rack.setAccessibleText("SLOC retur");
            batch.setPromptText("Batch");
            batch.setAccessibleText("Batch retur");
            expiry.setPromptText("Expiry");
            expiry.setAccessibleText("Tanggal kedaluwarsa");
            expiry.setVisible(type == ReturnType.SALES);
            expiry.setManaged(type == ReturnType.SALES);
            quantity.setPromptText("Jumlah");
            quantity.setAccessibleText("Jumlah retur");
            itemQr.setPromptText("QR barang");
            itemQr.setAccessibleText("QR barang yang dipindai");
            locationBarcode.setPromptText("Barcode SLOC");
            locationBarcode.setAccessibleText("Barcode SLOC yang dipindai");
            manualRow.getChildren().addAll(source, rack, batch, expiry, quantity);
            row.getChildren().addAll(manualRow, scanRow);
            scanRow.getChildren().addAll(itemQr, locationBarcode);
            source.setPrefWidth(220);
            rack.setPrefWidth(140);
            batch.setPrefWidth(115);
            expiry.setPrefWidth(130);
            quantity.setPrefWidth(80);
            itemQr.setPrefWidth(150);
            locationBarcode.setPrefWidth(150);
        }

        private void setRemoveButton(Button button) {
            button.setAccessibleText("Hapus baris retur");
            scanRow.getChildren().add(button);
        }
        private void setScanMode(boolean enabled) {
            rack.setDisable(enabled);
            batch.setDisable(enabled);
            expiry.setDisable(enabled);
            itemQr.setDisable(!enabled);
            locationBarcode.setDisable(!enabled);
            manualRow.setVisible(!enabled);
            manualRow.setManaged(!enabled);
            scanRow.setVisible(enabled);
            scanRow.setManaged(enabled);
        }
        private VBox row() { return row; }
        private ComboBox<Object> source() { return source; }
        private ComboBox<Rak> rack() { return rack; }
        private TextField batch() { return batch; }
        private DatePicker expiry() { return expiry; }
        private TextField quantity() { return quantity; }
        private TextField itemQr() { return itemQr; }
        private TextField locationBarcode() { return locationBarcode; }
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
                refreshTransactions.run();
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


    private void showAlert(Alert.AlertType type, String title, String msg) {
        Alert alert = new Alert(type);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(msg);
        alert.showAndWait();
    }
}
