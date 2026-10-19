package com.mycompany.tugas_akhir;



import com.mycompany.tugas_akhir.model.Barang;
import com.mycompany.tugas_akhir.model.Customer;
import com.mycompany.tugas_akhir.model.DeliveryOrder;
import com.mycompany.tugas_akhir.model.SalesOrder;
import com.mycompany.tugas_akhir.model.SalesOrderLine;
import com.mycompany.tugas_akhir.model.User;
import com.mycompany.tugas_akhir.service.SalesOrderService;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.VBox;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;

@Component
public class SalesOrderWorkflow {
    private static final Logger log = LoggerFactory.getLogger(SalesOrderWorkflow.class);
    private final SalesOrderService salesOrderService;
    private User currentUser;
    private List<Barang> barangItems;
    private List<Customer> customerItems;
    private Runnable refreshDeliveryOrders;

    public SalesOrderWorkflow(SalesOrderService salesOrderService) { this.salesOrderService = salesOrderService; }

    public void open(User user, List<Barang> items, List<Customer> customers, Runnable refreshDeliveryOrders) {
        currentUser = user;
        barangItems = items;
        customerItems = customers;
        this.refreshDeliveryOrders = refreshDeliveryOrders;
        handleSalesOrders();
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

    private void handleSalesOrders() {
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
                    refreshDeliveryOrders.run();
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


    private void showAlert(Alert.AlertType type, String title, String msg) {
        Alert alert = new Alert(type);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(msg);
        alert.showAndWait();
    }
}
