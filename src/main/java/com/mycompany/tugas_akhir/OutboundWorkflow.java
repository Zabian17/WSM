package com.mycompany.tugas_akhir;


import com.mycompany.tugas_akhir.model.Barang;
import com.mycompany.tugas_akhir.model.Customer;
import com.mycompany.tugas_akhir.model.DeliveryOrder;
import com.mycompany.tugas_akhir.model.DeliveryOrderLine;
import com.mycompany.tugas_akhir.model.DeliveryOrderLotReservation;
import com.mycompany.tugas_akhir.model.DeliveryOrderPickScan;
import com.mycompany.tugas_akhir.model.DeliveryOrderFulfillmentAudit;
import com.mycompany.tugas_akhir.model.PickTask;
import com.mycompany.tugas_akhir.model.User;
import com.mycompany.tugas_akhir.service.DeliveryOrderService;
import com.mycompany.tugas_akhir.service.DeliveryOrderService.PickerAssignment;
import com.mycompany.tugas_akhir.service.SectionService;
import com.mycompany.tugas_akhir.service.UserService;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.VBox;
import javafx.stage.FileChooser;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.time.format.DateTimeFormatter;
import java.io.ByteArrayOutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;

@Component
public class OutboundWorkflow {
    private static final Logger log = LoggerFactory.getLogger(OutboundWorkflow.class);
    private final DeliveryOrderService deliveryOrderService;
    private final SalesOrderWorkflow salesOrderWorkflow;
    private final UserService userService;
    private final SectionService sectionService;
    private User currentUser;
    private List<Barang> barangItems;
    private List<Customer> customerItems;

    private record PickerAssignmentOptions(List<User> pickers, List<com.mycompany.tugas_akhir.model.Section> sections) {}
    private record PickerAssignmentRow(ComboBox<User> picker,
                                       ComboBox<com.mycompany.tugas_akhir.model.Section> section,
                                       TextField shift) {}

    public OutboundWorkflow(DeliveryOrderService deliveryOrderService, SalesOrderWorkflow salesOrderWorkflow, UserService userService, SectionService sectionService) {
        this.deliveryOrderService = deliveryOrderService;
        this.salesOrderWorkflow = salesOrderWorkflow;
        this.userService = userService;
        this.sectionService = sectionService;
    }

    public void open(User user, List<Barang> items, List<Customer> customers) {
        this.currentUser = user;
        this.barangItems = items;
        this.customerItems = customers;
        handleOpenDeliveryOrders();
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
        Button scanPacking = deliveryOrderButton("Scan packing", "Verifikasi QR item terhadap barang yang sudah dipick.");
        Button scanLoading = deliveryOrderButton("Scan loading", "Verifikasi QR item terhadap barang yang sudah dipacking.");
        Button packingList = deliveryOrderButton("Packing list / label", "Simpan packing list dan label QR untuk DO.");
        Button fulfillmentAudit = deliveryOrderButton("Audit pemenuhan", "Tinjau catatan jumlah packing dan loading.");
        Button dispatch = deliveryOrderButton("Dispatch / Goods Issue", "Posting barang yang dimuat sebagai Goods Issue.");
        create.setOnAction(event -> handleCreateDeliveryOrder(table));
        salesOrders.setOnAction(event -> salesOrderWorkflow.open(currentUser, barangItems, customerItems, () -> loadDeliveryOrders(table)));
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
        scanPacking.setOnAction(event -> handleRecordFulfillmentScan(table));
        scanLoading.setOnAction(event -> handleRecordFulfillmentScan(table));
        packingList.setOnAction(event -> handleExportPackingList(table));
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
        table.getSelectionModel().selectedItemProperty().addListener((observable, oldOrder, selectedOrder) -> {
            selectionHint.setText(selectedOrder == null
                    ? "Pilih pesanan pada tabel untuk melihat tindakan yang tersedia."
                    : selectedOrder.getOrderCode() + "  ·  " + deliveryOrderStatusLabel(selectedOrder.getStatus())
                    + "  ·  " + selectedOrder.getLines().size() + " baris  ·  "
                    + selectedOrder.getLines().stream().mapToInt(DeliveryOrderLine::getRemainingQuantity).sum()
                    + " unit tersisa");
            updateDeliveryOrderActionState(selectedOrder, release, assignTask, taskDetails, reservations,
                    start, scanPick, scanHistory, picked, packed, loaded, scanPacking, scanLoading,
                    packingList, fulfillmentAudit, dispatch);
        });
        updateDeliveryOrderActionState(null, release, assignTask, taskDetails, reservations, start,
                scanPick, scanHistory, picked, packed, loaded, scanPacking, scanLoading,
                packingList, fulfillmentAudit, dispatch);

        VBox createGroup = deliveryOrderActionGroup("Buat pesanan", create, salesOrders);
        VBox preparationGroup = deliveryOrderActionGroup("Persiapan", release, assignTask,
                taskDetails, reservations);
        VBox pickingGroup = deliveryOrderActionGroup("Picking", start, scanPick, scanHistory, picked);
        VBox fulfillmentGroup = deliveryOrderActionGroup("Packing & pengiriman", packed, loaded,
                scanPacking, scanLoading, packingList, fulfillmentAudit, dispatch);
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
            Button picked, Button packed, Button loaded, Button scanPacking, Button scanLoading,
            Button packingList, Button fulfillmentAudit, Button dispatch) {
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
        scanPacking.setDisable(!selected || order.getStatus()
                != com.mycompany.tugas_akhir.model.DeliveryOrderStatus.PICKED);
        scanLoading.setDisable(!selected || order.getStatus()
                != com.mycompany.tugas_akhir.model.DeliveryOrderStatus.PACKED);
        packingList.setDisable(!selected || (order.getStatus()
                != com.mycompany.tugas_akhir.model.DeliveryOrderStatus.PACKED
                && order.getStatus() != com.mycompany.tugas_akhir.model.DeliveryOrderStatus.LOADED));
        fulfillmentAudit.setDisable(!selected);
        dispatch.setDisable(!selected || order.getStatus()
                != com.mycompany.tugas_akhir.model.DeliveryOrderStatus.LOADED);
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
            List<PickerAssignmentRow> rows = new java.util.ArrayList<>();
            VBox rowsBox = new VBox(6);
            Runnable addRow = () -> {
                PickerAssignmentRow row = new PickerAssignmentRow(
                        pickerCombo(task.getValue().pickers()), sectionCombo(task.getValue().sections()),
                        new TextField("Pagi"));
                row.shift().setPromptText("Shift");
                if (rows.isEmpty()) row.picker().setValue(task.getValue().pickers().stream()
                        .filter(picker -> java.util.Objects.equals(picker.getId(), currentUser.getId()))
                        .findFirst().orElse(null));
                rows.add(row);
                rowsBox.getChildren().add(new HBox(8, row.picker(), row.section(), row.shift()));
            };
            addRow.run();
            Button addPicker = new Button("Tambah Picker");
            addPicker.setOnAction(addEvent -> {
                if (rows.size() < 24) addRow.run();
            });
            Dialog<ButtonType> dialog = new Dialog<>();
            dialog.setTitle("Tugaskan Multi-Picker");
            dialog.setHeaderText("Tugas per section dan shift — DO: " + order.getOrderCode());
            dialog.getDialogPane().setContent(new VBox(8,
                    new Label("Satu section hanya boleh ditugaskan sekali."), rowsBox, addPicker));
            dialog.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);
            styleDeliveryOrderDialog(dialog);
            dialog.getDialogPane().setPrefWidth(760);
            Optional<ButtonType> result = dialog.showAndWait();
            if (result.isEmpty() || result.get() != ButtonType.OK) return;
            if (rows.stream().anyMatch(row -> row.picker().getValue() == null
                    || row.section().getValue() == null || row.shift().getText().isBlank())) {
                showAlert(Alert.AlertType.WARNING, "Data Wajib", "Pilih picker, section, dan shift pada setiap baris.");
                return;
            }
            List<PickerAssignment> assignments = rows.stream().map(row -> new PickerAssignment(
                    row.picker().getValue().getId(), row.section().getValue().getId(), row.shift().getText()))
                    .toList();
            Task<Boolean> assignTask = new Task<>() {
                @Override
                protected Boolean call() {
                    return deliveryOrderService.assignPickers(order.getId(), assignments);
                }
            };
            assignTask.setOnSucceeded(assignEvent -> {
                if (assignTask.getValue()) {
                    showAlert(Alert.AlertType.INFORMATION, "Pick Task Ditugaskan",
                            assignments.size() + " tugas untuk " + order.getOrderCode()
                                    + " ditugaskan menurut picker, section, dan shift.");
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

    private ComboBox<User> pickerCombo(List<User> pickers) {
        ComboBox<User> combo = new ComboBox<>(FXCollections.observableArrayList(pickers));
        combo.setPromptText("Pilih picker");
        combo.setMaxWidth(Double.MAX_VALUE);
        combo.setConverter(new javafx.util.StringConverter<>() {
            @Override public String toString(User value) {
                return value == null ? "" : value.getFullName() + " (" + value.getEmail() + ")";
            }
            @Override public User fromString(String value) { return null; }
        });
        return combo;
    }

    private ComboBox<com.mycompany.tugas_akhir.model.Section> sectionCombo(
            List<com.mycompany.tugas_akhir.model.Section> sections) {
        ComboBox<com.mycompany.tugas_akhir.model.Section> combo =
                new ComboBox<>(FXCollections.observableArrayList(sections));
        combo.setPromptText("Pilih section");
        combo.setMaxWidth(Double.MAX_VALUE);
        combo.setConverter(new javafx.util.StringConverter<>() {
            @Override public String toString(com.mycompany.tugas_akhir.model.Section value) {
                return value == null ? "" : value.getKodeSection() + " — " + value.getNamaSection();
            }
            @Override public com.mycompany.tugas_akhir.model.Section fromString(String value) { return null; }
        });
        return combo;
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
            taskTable.getColumns().add(pickTaskColumn("Shift", PickTask::getAssignedShift));
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

    private void handleRecordFulfillmentScan(TableView<DeliveryOrder> table) {
        DeliveryOrder order = table.getSelectionModel().getSelectedItem();
        Integer actorId = currentUser == null ? null : currentUser.getId();
        if (order == null || actorId == null) return;
        ComboBox<DeliveryOrderLine> lineBox = new ComboBox<>(
                FXCollections.observableArrayList(order.getLines()));
        lineBox.setConverter(new javafx.util.StringConverter<>() {
            @Override
            public String toString(DeliveryOrderLine line) {
                return line == null ? "" : line.getBarang().getKodeBarang() + " — "
                        + line.getBarang().getNamaBarang();
            }

            @Override
            public DeliveryOrderLine fromString(String value) { return null; }
        });
        lineBox.getSelectionModel().selectFirst();
        boolean loading = order.getStatus() == com.mycompany.tugas_akhir.model.DeliveryOrderStatus.PACKED;
        TextField deliveryOrderBarcode = new TextField();
        deliveryOrderBarcode.setPromptText("Scan barcode DO " + order.getOrderCode());
        TextField qrField = new TextField();
        qrField.setPromptText("Pindai QR barang (WMS1)");
        TextField quantityField = new TextField("1");
        GridPane grid = new GridPane();
        int row = 0;
        if (loading) grid.addRow(row++, new Label("Barcode DO"), deliveryOrderBarcode);
        grid.addRow(row++, new Label("Barang DO"), lineBox);
        grid.addRow(row++, new Label("QR item"), qrField);
        grid.addRow(row, new Label("Jumlah scan"), quantityField);
        configureDeliveryOrderFormGrid(grid);
        boolean packing = !loading;
        Dialog<ButtonType> dialog = new Dialog<>();
        dialog.setTitle(packing ? "Scan Packing" : "Scan Loading");
        dialog.setHeaderText(loading
                ? "Scan barcode DO dan item; SKU, batch, expiry, serta jumlah harus cocok dengan packing."
                : "Scan hanya diterima jika SKU, batch, expiry, dan jumlah cocok dengan reservasi DO.");
        dialog.getDialogPane().setContent(grid);
        dialog.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);
        styleDeliveryOrderDialog(dialog);
        Optional<ButtonType> result = dialog.showAndWait();
        if (result.isEmpty() || result.get() != ButtonType.OK) return;
        DeliveryOrderLine line = lineBox.getValue();
        int quantity;
        try {
            quantity = Integer.parseInt(quantityField.getText().trim());
            if (quantity <= 0) throw new NumberFormatException();
        } catch (NumberFormatException ex) {
            showAlert(Alert.AlertType.WARNING, "Jumlah Tidak Valid", "Jumlah scan harus bilangan positif.");
            return;
        }
        if (line == null || qrField.getText().isBlank()
                || loading && deliveryOrderBarcode.getText().isBlank()) {
            showAlert(Alert.AlertType.WARNING, "Scan Tidak Lengkap",
                    "Scan barcode DO saat loading, pilih barang, dan pindai QR item.");
            return;
        }
        Task<Boolean> task = new Task<>() {
            @Override
            protected Boolean call() {
                return deliveryOrderService.recordFulfillmentScan(order.getId(), line.getId(),
                        loading ? deliveryOrderBarcode.getText().trim() : null,
                        qrField.getText().trim(), quantity, actorId);
            }
        };
        task.setOnSucceeded(event -> {
            if (task.getValue()) {
                showAlert(Alert.AlertType.INFORMATION, "Scan Tercatat", quantity + " unit terverifikasi.");
                loadDeliveryOrders(table);
            } else {
                showAlert(Alert.AlertType.ERROR, "Scan Ditolak",
                        "Barcode DO, QR item, status tahap, atau jumlah tidak cocok dengan reservasi DO.");
            }
        });
        task.setOnFailed(event -> {
            log.error("Scan fulfillment gagal", task.getException());
            showAlert(Alert.AlertType.ERROR, "Scan Gagal", "Scan tidak dapat dicatat.");
        });
        Thread thread = new Thread(task, "delivery-order-fulfillment-scan");
        thread.setDaemon(true);
        thread.start();
    }

    private void handleExportPackingList(TableView<DeliveryOrder> table) {
        DeliveryOrder order = table.getSelectionModel().getSelectedItem();
        if (order == null) return;
        FileChooser chooser = new FileChooser();
        chooser.setTitle("Simpan Packing List dan Label DO");
        chooser.setInitialFileName(order.getOrderCode().replaceAll("[^A-Za-z0-9._-]", "_")
                + "-packing.html");
        chooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("Packing List HTML", "*.html"));
        java.io.File target = chooser.showSaveDialog(table.getScene().getWindow());
        if (target == null) return;
        Task<Path> task = new Task<>() {
            @Override
            protected Path call() throws Exception {
                List<DeliveryOrderLotReservation> reservations = deliveryOrderService.getReservations(order.getId());
                if (reservations.isEmpty()) throw new IllegalStateException("DO belum memiliki alokasi lot.");
                Path destination = target.toPath();
                Files.writeString(destination, createPackingListHtml(order, reservations),
                        java.nio.charset.StandardCharsets.UTF_8);
                return destination;
            }
        };
        task.setOnSucceeded(event -> {
            try {
                if (java.awt.Desktop.isDesktopSupported()) {
                    java.awt.Desktop.getDesktop().browse(task.getValue().toUri());
                } else {
                    showAlert(Alert.AlertType.INFORMATION, "Packing List Tersimpan",
                            "File tersimpan di " + task.getValue());
                }
            } catch (java.io.IOException ex) {
                log.error("Packing list tersimpan tetapi browser gagal dibuka", ex);
                showAlert(Alert.AlertType.INFORMATION, "Packing List Tersimpan",
                        "File tersimpan di " + task.getValue());
            }
        });
        task.setOnFailed(event -> {
            log.error("Gagal membuat packing list {}", order.getOrderCode(), task.getException());
            showAlert(Alert.AlertType.ERROR, "Gagal Membuat Packing List",
                    task.getException() instanceof IllegalStateException
                            ? task.getException().getMessage() : "File packing list tidak dapat dibuat.");
        });
        Thread thread = new Thread(task, "delivery-order-packing-list");
        thread.setDaemon(true);
        thread.start();
    }

    private String createPackingListHtml(DeliveryOrder order,
                                         List<DeliveryOrderLotReservation> reservations) throws Exception {
        StringBuilder html = new StringBuilder("<!doctype html><html lang=\"id\"><head><meta charset=\"utf-8\">")
                .append("<meta name=\"viewport\" content=\"width=device-width,initial-scale=1\"><title>Packing ")
                .append(escapeHtml(order.getOrderCode())).append("</title><style>")
                .append("body{font:14px Arial,sans-serif;color:#17212b;margin:24px}h1{font-size:22px}")
                .append("table{width:100%;border-collapse:collapse;margin:16px 0}th,td{border:1px solid #9aa7b2;padding:7px;text-align:left}")
                .append("th{background:#edf2f6}.barcode{width:360px;height:90px}.label{border:1px dashed #687986;padding:16px;margin:16px 0;page-break-inside:avoid}")
                .append(".qr{width:140px;height:140px}.muted{color:#52616c}@media print{.label{page-break-inside:avoid}body{margin:12mm}}")
                .append("</style></head><body><h1>Packing List / Label</h1><p><b>DO:</b> ")
                .append(escapeHtml(order.getOrderCode())).append("<br><b>Customer:</b> ")
                .append(escapeHtml(order.getCustomer().getNamaCustomer())).append("</p><img class=\"barcode\" alt=\"Barcode DO\" src=\"data:image/png;base64,")
                .append(barcodeImage(order.getOrderCode(), false)).append("\"><table><thead><tr><th>SKU</th><th>Barang</th>")
                .append("<th>Pesanan</th><th>Picked</th><th>Packed</th></tr></thead><tbody>");
        for (DeliveryOrderLine line : order.getLines()) {
            int picked = reservations.stream().filter(value -> value.getDeliveryOrderLine().getId().equals(line.getId()))
                    .mapToInt(DeliveryOrderLotReservation::getPickedQuantity).sum();
            int packed = reservations.stream().filter(value -> value.getDeliveryOrderLine().getId().equals(line.getId()))
                    .mapToInt(DeliveryOrderLotReservation::getPackedQuantity).sum();
            html.append("<tr><td>").append(escapeHtml(line.getBarang().getKodeBarang())).append("</td><td>")
                    .append(escapeHtml(line.getBarang().getNamaBarang())).append("</td><td>")
                    .append(line.getOrderedQuantity()).append("</td><td>").append(picked).append("</td><td>")
                    .append(packed).append("</td></tr>");
        }
        html.append("</tbody></table><h2>Label per lot</h2>");
        for (DeliveryOrderLotReservation reservation : reservations) {
            int packed = reservation.getPackedQuantity();
            if (packed <= 0) continue;
            var line = reservation.getDeliveryOrderLine();
            var lot = reservation.getLotBalance();
            String expiry = lot.getExpiryDate() == null ? "-" : lot.getExpiryDate().toString();
            String payload = "WMS1|" + barcodePart(line.getBarang().getKodeBarang()) + "|"
                    + barcodePart(lot.getBatchCode()) + "|" + expiry;
            html.append("<section class=\"label\"><b>DO ").append(escapeHtml(order.getOrderCode()))
                    .append("</b><p>").append(escapeHtml(line.getBarang().getKodeBarang())).append(" — ")
                    .append(escapeHtml(line.getBarang().getNamaBarang())).append("</p><p>Batch: ")
                    .append(escapeHtml(lot.getBatchCode())).append(" | Expiry: ").append(escapeHtml(expiry))
                    .append(" | SLOC: ").append(escapeHtml(lot.getRak() == null ? "-" : lot.getRak().getKodeRak()))
                    .append(" | Qty: ").append(packed).append("</p><img class=\"qr\" alt=\"QR item\" src=\"data:image/png;base64,")
                    .append(barcodeImage(payload, true)).append("\"><p class=\"muted\">")
                    .append(escapeHtml(payload)).append("</p></section>");
        }
        return html.append("</body></html>").toString();
    }

    private String barcodeImage(String value, boolean qr) throws Exception {
        var matrix = qr
                ? new com.google.zxing.qrcode.QRCodeWriter().encode(value,
                        com.google.zxing.BarcodeFormat.QR_CODE, 240, 240)
                : new com.google.zxing.oned.Code128Writer().encode(value,
                        com.google.zxing.BarcodeFormat.CODE_128, 720, 180);
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        com.google.zxing.client.j2se.MatrixToImageWriter.writeToStream(matrix, "PNG", output);
        return Base64.getEncoder().encodeToString(output.toByteArray());
    }

    private String barcodePart(String value) {
        return Base64.getUrlEncoder().withoutPadding().encodeToString(value.getBytes(java.nio.charset.StandardCharsets.UTF_8));
    }

    private String escapeHtml(String value) {
        if (value == null) return "-";
        return value.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;")
                .replace("\"", "&quot;").replace("'", "&#39;");
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
        dialog.setHeaderText("Masukkan jumlah per barang; jika memakai scan QR, jumlah wajib sama dengan total scan.");
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
    private void showAlert(Alert.AlertType type, String title, String msg) {
        Alert alert = new Alert(type);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(msg);
        alert.showAndWait();
    }
}
