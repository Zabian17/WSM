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
public class StockTransferWorkflow {
    private static final Logger log = LoggerFactory.getLogger(StockTransferWorkflow.class);
    private final StockTransferOrderService stockTransferOrderService;
    private final SectionService sectionService;
    private final UserService userService;
    private User currentUser;
    private List<Barang> barangItems;
    private List<Rak> rakItems;
    private Runnable refreshTransactions;
    private java.util.function.Consumer<List<Rak>> updateRacks;

    public StockTransferWorkflow(StockTransferOrderService stockTransferOrderService, SectionService sectionService, UserService userService) {
        this.stockTransferOrderService = stockTransferOrderService;
        this.sectionService = sectionService;
        this.userService = userService;
    }

    public void open(User user, List<Barang> items, List<Rak> racks, Runnable refreshTransactions, java.util.function.Consumer<List<Rak>> updateRacks) {
        this.currentUser = user;
        this.barangItems = items;
        this.rakItems = racks;
        this.refreshTransactions = refreshTransactions;
        this.updateRacks = updateRacks;
        handleOpenSto();
    }

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
                    reloadRacks.setOnSucceeded(reloadEvent -> {
                        rakItems = reloadRacks.getValue();
                        updateRacks.accept(rakItems);
                    });
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
                refreshTransactions.run();
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
                refreshTransactions.run();
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


    private void showAlert(Alert.AlertType type, String title, String msg) {
        Alert alert = new Alert(type);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(msg);
        alert.showAndWait();
    }
}
