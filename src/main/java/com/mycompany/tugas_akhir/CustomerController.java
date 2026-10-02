package com.mycompany.tugas_akhir;

<<<<<<< HEAD
import com.mycompany.tugas_akhir.model.Customer;
import com.mycompany.tugas_akhir.service.CustomerService;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.net.URL;
import java.util.List;
import java.util.Optional;
import java.util.ResourceBundle;
import java.util.stream.Collectors;

@Component
public class CustomerController implements Initializable {

    private static final Logger log = LoggerFactory.getLogger(CustomerController.class);
    private final CustomerService customerService;

    private final ObservableList<Customer> customerList = FXCollections.observableArrayList();
    private List<Customer> rawCustomerList;

    @FXML private TextField searchField;
    @FXML private Label lblTotalCustomer;

    @FXML private TableView<Customer> tableCustomer;
    @FXML private TableColumn<Customer, String> colNama;
    @FXML private TableColumn<Customer, String> colKontak;
    @FXML private TableColumn<Customer, String> colEmail;
    @FXML private TableColumn<Customer, String> colStatus;
    @FXML private TableColumn<Customer, Void> colAksi;

    public CustomerController(CustomerService customerService) {
        this.customerService = customerService;
    }

    @Override
    public void initialize(URL url, ResourceBundle rb) {
        setupTable();
        loadData();
    }

    private void setupTable() {
        tableCustomer.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_FLEX_LAST_COLUMN);
        colNama.setCellValueFactory(d -> new SimpleStringProperty(
                d.getValue().getNamaCustomer() != null ? d.getValue().getNamaCustomer() : "-"));

        colKontak.setCellValueFactory(d -> new SimpleStringProperty(
                d.getValue().getKontak() != null ? d.getValue().getKontak() : "-"));

        colEmail.setCellValueFactory(d -> new SimpleStringProperty(
                d.getValue().getEmail() != null ? d.getValue().getEmail() : "-"));

        colStatus.setCellValueFactory(d -> new SimpleStringProperty(
                d.getValue().getStatus() != null ? d.getValue().getStatus() : "Aktif"));
        colStatus.setCellFactory(col -> new TableCell<>() {
            @Override
            protected void updateItem(String status, boolean empty) {
                super.updateItem(status, empty);
                if (empty || status == null) {
                    setGraphic(null);
                    return;
                }
                Label badge = new Label(status);
                badge.getStyleClass().setAll("badge", "Aktif".equalsIgnoreCase(status) ? "badge-ok" : "badge-danger");
                setGraphic(badge);
                setText(null);
            }
        });

        colAksi.setCellFactory(param -> new TableCell<>() {
            private final Button btnEdit = new Button("✏");
            private final Button btnHapus = new Button("🗑");
            private final HBox box = new HBox(6, btnEdit, btnHapus);

            {
                btnEdit.getStyleClass().add("btn-action-edit");
                btnHapus.getStyleClass().add("btn-action-delete");
                box.setAlignment(Pos.CENTER);

                btnEdit.setOnAction(e -> {
                    Customer cust = getTableView().getItems().get(getIndex());
                    handleEdit(cust);
                });

                btnHapus.setOnAction(e -> {
                    Customer cust = getTableView().getItems().get(getIndex());
                    handleHapus(cust);
                });
            }

            @Override
            protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                setGraphic(empty ? null : box);
            }
        });

        tableCustomer.setItems(customerList);
    }

    private void loadData() {
        Task<List<Customer>> task = new Task<>() {
            @Override
            protected List<Customer> call() {
                return customerService.getAllCustomer();
            }
        };

        task.setOnSucceeded(e -> {
            rawCustomerList = task.getValue();
            customerList.setAll(rawCustomerList);
            if (lblTotalCustomer != null) {
                lblTotalCustomer.setText(customerList.size() + " Customer");
            }
        });

        task.setOnFailed(e -> log.error("Gagal memuat data customer", task.getException()));

        Thread th = new Thread(task);
        th.setDaemon(true);
        th.start();
    }

    @FXML
    private void handleSearch() {
        String keyword = searchField.getText().trim().toLowerCase();
        if (rawCustomerList == null) return;

        if (keyword.isEmpty()) {
            customerList.setAll(rawCustomerList);
        } else {
            List<Customer> filtered = rawCustomerList.stream()
                    .filter(c -> (c.getNamaCustomer() != null && c.getNamaCustomer().toLowerCase().contains(keyword))
                            || (c.getPosisi() != null && c.getPosisi().toLowerCase().contains(keyword))
                            || (c.getKontak() != null && c.getKontak().toLowerCase().contains(keyword))
                            || (c.getEmail() != null && c.getEmail().toLowerCase().contains(keyword)))
                    .collect(Collectors.toList());
            customerList.setAll(filtered);
        }
        if (lblTotalCustomer != null) {
            lblTotalCustomer.setText(customerList.size() + " Customer");
        }
    }

    @FXML
    private void handleTambah() {
        showDialog(null);
    }

    @FXML
    private void handleRefresh() {
        if (searchField != null) searchField.clear();
        loadData();
    }

    private void handleEdit(Customer customer) {
        showDialog(customer);
    }

    private void handleHapus(Customer customer) {
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setTitle("Konfirmasi Hapus");
        confirm.setHeaderText(null);
        confirm.setContentText("Apakah Anda yakin ingin menghapus customer: " + customer.getNamaCustomer() + "?");

        confirm.showAndWait().ifPresent(response -> {
            if (response == ButtonType.OK) {
                Task<Void> task = new Task<>() {
                    @Override
                    protected Void call() {
                        customerService.deleteCustomer(customer.getId());
                        return null;
                    }
                };
                task.setOnSucceeded(e -> {
                    showAlert(Alert.AlertType.INFORMATION, "Sukses", "Data customer berhasil dihapus!");
                    loadData();
                });
                task.setOnFailed(e -> {
                    log.error("Gagal menghapus customer", task.getException());
                    showAlert(Alert.AlertType.ERROR, "Gagal", "Gagal menghapus data customer.");
                });
                Thread th = new Thread(task);
                th.setDaemon(true);
                th.start();
            }
        });
    }

    private void showDialog(Customer existing) {
        Dialog<Customer> dialog = new Dialog<>();
        dialog.setTitle(existing == null ? "Tambah Customer" : "Edit Customer");
        dialog.setHeaderText(existing == null ? "Isi data customer baru" : "Perbarui data customer");

        ButtonType simpanBtnType = new ButtonType("Simpan", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(simpanBtnType, ButtonType.CANCEL);

        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(10);
        grid.setPadding(new Insets(20, 40, 10, 10));

        TextField tfNama = new TextField(existing != null ? existing.getNamaCustomer() : "");
        tfNama.setPromptText("Nama Customer / Perusahaan");

        TextField tfPosisi = new TextField(existing != null ? existing.getPosisi() : "");
        tfPosisi.setPromptText("Posisi / Jabatan");

        TextField tfKontak = new TextField(existing != null ? existing.getKontak() : "");
        tfKontak.setPromptText("Kontak / HP");

        TextField tfEmail = new TextField(existing != null ? existing.getEmail() : "");
        tfEmail.setPromptText("Email");

        TextArea taAlamat = new TextArea(existing != null ? existing.getAlamat() : "");
        taAlamat.setPromptText("Alamat Lengkap");
        taAlamat.setPrefRowCount(3);

        ComboBox<String> cbStatus = new ComboBox<>(FXCollections.observableArrayList("Aktif", "Nonaktif"));
        cbStatus.setValue(existing != null && existing.getStatus() != null ? existing.getStatus() : "Aktif");

        grid.add(new Label("Nama Customer:"), 0, 0);
        grid.add(tfNama, 1, 0);
        grid.add(new Label("Posisi / Jabatan:"), 0, 1);
        grid.add(tfPosisi, 1, 1);
        grid.add(new Label("Kontak:"), 0, 2);
        grid.add(tfKontak, 1, 2);
        grid.add(new Label("Email:"), 0, 3);
        grid.add(tfEmail, 1, 3);
        grid.add(new Label("Alamat:"), 0, 4);
        grid.add(taAlamat, 1, 4);
        grid.add(new Label("Status:"), 0, 5);
        grid.add(cbStatus, 1, 5);

        dialog.getDialogPane().setContent(grid);

        dialog.setResultConverter(dialogButton -> {
            if (dialogButton == simpanBtnType) {
                if (tfNama.getText().trim().isEmpty()) {
                    showAlert(Alert.AlertType.WARNING, "Peringatan", "Nama Customer harus diisi!");
                    return null;
                }
                Customer customer = existing != null ? existing : new Customer();
                customer.setNamaCustomer(tfNama.getText().trim());
                customer.setPosisi(tfPosisi.getText().trim());
                customer.setKontak(tfKontak.getText().trim());
                customer.setEmail(tfEmail.getText().trim());
                customer.setAlamat(taAlamat.getText().trim());
                customer.setStatus(cbStatus.getValue());
                return customer;
=======
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;
import javafx.scene.layout.TilePane;
import javafx.scene.layout.VBox;
import java.net.URL;
import java.util.List;
import java.util.ResourceBundle;


public class CustomerController implements Initializable {

    @FXML private ComboBox<String> cbCategory;
    @FXML private ComboBox<String> cbStatus;
    @FXML private TextField tfSearch;
    @FXML private TilePane tileCustomers;

    private final CustomerDAO customerDAO = new CustomerDAO();
    private List<CustomerDAO.Customer> allCustomers;
    private List<CustomerDAO.Customer> filteredCustomers;

    @Override
    public void initialize(URL url, ResourceBundle rb) {
        setupFilters();
        loadCustomers();
    }

    public void initUser(UserDAO.User user) {
    }

    private void setupFilters() {
        cbCategory.getItems().addAll("All");
        cbCategory.setValue("All");

        cbStatus.getItems().addAll("All status", "Aktif", "Nonaktif");
        cbStatus.setValue("All status");
    }

    private void loadCustomers() {
        allCustomers = customerDAO.getAllCustomer();
        applyFilters();
    }

    private void applyFilters() {
        filteredCustomers = allCustomers;

        String selectedStatus = cbStatus.getValue();
        if (selectedStatus != null && !selectedStatus.equals("All status")) {
            filteredCustomers = filteredCustomers.stream()
                .filter(c -> selectedStatus.equals(c.status))
                .toList();
        }

        String searchTerm = tfSearch.getText().trim().toLowerCase();
        if (!searchTerm.isEmpty()) {
            filteredCustomers = filteredCustomers.stream()
                .filter(c -> c.namaCustomer.toLowerCase().contains(searchTerm) ||
                           c.posisi.toLowerCase().contains(searchTerm) ||
                           c.alamat.toLowerCase().contains(searchTerm) ||
                           c.kontak.toLowerCase().contains(searchTerm))
                .toList();
        }

        displayCustomers(filteredCustomers);
    }

    private void displayCustomers(List<CustomerDAO.Customer> customers) {
        tileCustomers.getChildren().clear();

        if (customers.isEmpty()) {
            Label emptyLabel = new Label("Belum ada data customer");
            emptyLabel.setStyle("-fx-text-fill: #9ba3b8; -fx-font-size: 14px;");
            tileCustomers.getChildren().add(emptyLabel);
            return;
        }

        for (CustomerDAO.Customer customer : customers) {
            tileCustomers.getChildren().add(createCustomerCard(customer));
        }
    }

    private VBox createCustomerCard(CustomerDAO.Customer customer) {
        VBox card = new VBox(12);
        card.setStyle("-fx-border-color: #e5e7eb; -fx-border-radius: 8; " +
                      "-fx-background-color: #ffffff; -fx-padding: 20; " +
                      "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.05), 4, 0, 0, 2);");
        card.setPrefWidth(300);

        Label lblName = new Label(customer.namaCustomer);
        lblName.setStyle("-fx-font-size: 16px; -fx-font-weight: bold; -fx-text-fill: #1f2937;");

        HBox positionBox = new HBox(8);
        positionBox.setAlignment(Pos.CENTER_LEFT);
        Label iconPos = new Label("👤");
        Label lblPosition = new Label(customer.posisi);
        lblPosition.setStyle("-fx-font-size: 12px; -fx-text-fill: #9ba3b8;");
        positionBox.getChildren().addAll(iconPos, lblPosition);

        Label lblAddress = new Label(customer.alamat);
        lblAddress.setStyle("-fx-font-size: 12px; -fx-text-fill: #9ba3b8;");
        lblAddress.setWrapText(true);

        Label lblPhone = new Label(customer.kontak);
        lblPhone.setStyle("-fx-font-size: 12px; -fx-text-fill: #9ba3b8;");

        Label lblStatus = new Label(customer.status);
        String statusColor = "Aktif".equals(customer.status) ? "#10b981" : "#ef4444";
        lblStatus.setStyle("-fx-font-size: 10px; -fx-text-fill: white; -fx-padding: 4 8; " +
                          "-fx-background-color: " + statusColor + "; -fx-background-radius: 4;");

        HBox buttonBox = new HBox(10);
        buttonBox.setAlignment(Pos.CENTER);

        Button btnEdit = new Button("Edit");
        btnEdit.setStyle("-fx-font-size: 12px; -fx-padding: 6 16; -fx-background-color: #818cf8; " +
                        "-fx-text-fill: white; -fx-background-radius: 4; -fx-cursor: hand;");
        btnEdit.setOnAction(e -> handleEditCustomer(customer));

        Button btnContact = new Button("Contact");
        btnContact.setStyle("-fx-font-size: 12px; -fx-padding: 6 16; -fx-background-color: #3b82f6; " +
                          "-fx-text-fill: white; -fx-background-radius: 4; -fx-cursor: hand;");
        btnContact.setOnAction(e -> handleContact(customer));

        buttonBox.getChildren().addAll(btnEdit, btnContact);

        card.getChildren().addAll(
            lblName,
            lblStatus,
            positionBox,
            lblAddress,
            lblPhone,
            new Separator(),
            buttonBox
        );

        return card;
    }

    @FXML private void handleAddCustomer() {
        Dialog<CustomerDAO.Customer> dialog = new Dialog<>();
        dialog.setTitle("Tambah Customer Baru");
        dialog.setHeaderText("Masukkan data customer baru");

        TextField tfName = new TextField();
        tfName.setPromptText("Nama Perusahaan");
        TextField tfPosition = new TextField();
        tfPosition.setPromptText("Posisi/Jabatan");
        TextField tfAddress = new TextField();
        tfAddress.setPromptText("Alamat");
        TextField tfPhone = new TextField();
        tfPhone.setPromptText("No. Telepon");
        TextField tfEmail = new TextField();
        tfEmail.setPromptText("Email");

        VBox content = new VBox(10);
        content.setStyle("-fx-padding: 10;");
        content.getChildren().addAll(
            new Label("Nama Perusahaan:"), tfName,
            new Label("Posisi:"), tfPosition,
            new Label("Alamat:"), tfAddress,
            new Label("No. Telepon:"), tfPhone,
            new Label("Email:"), tfEmail
        );

        dialog.getDialogPane().setContent(content);
        dialog.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);

        dialog.setResultConverter(buttonType -> {
            if (buttonType == ButtonType.OK) {
                String name = tfName.getText().trim();
                if (name.isEmpty()) {
                    showAlert(Alert.AlertType.WARNING, "Peringatan", "Nama perusahaan tidak boleh kosong!");
                    return null;
                }

                if (customerDAO.addCustomer(name, tfPosition.getText(), tfAddress.getText(),
                                           tfPhone.getText(), tfEmail.getText())) {
                    showAlert(Alert.AlertType.INFORMATION, "Sukses", "✅ Customer berhasil ditambahkan!");
                    loadCustomers();
                    return new CustomerDAO.Customer(0, name, tfPosition.getText(), tfAddress.getText(),
                                                    tfPhone.getText(), tfEmail.getText(), "Aktif");
                } else {
                    showAlert(Alert.AlertType.ERROR, "Gagal", "Customer gagal ditambahkan.");
                }
>>>>>>> origin/main
            }
            return null;
        });

<<<<<<< HEAD
        Optional<Customer> result = dialog.showAndWait();
        result.ifPresent(customer -> {
            Task<Customer> saveTask = new Task<>() {
                @Override
                protected Customer call() {
                    return customerService.saveCustomer(customer);
                }
            };
            saveTask.setOnSucceeded(e -> {
                showAlert(Alert.AlertType.INFORMATION, "Sukses", "Data customer berhasil disimpan!");
                loadData();
            });
            saveTask.setOnFailed(e -> {
                log.error("Gagal menyimpan customer", saveTask.getException());
                showAlert(Alert.AlertType.ERROR, "Gagal", "Gagal menyimpan data customer.");
            });
            Thread th = new Thread(saveTask);
            th.setDaemon(true);
            th.start();
        });
    }

    private void showAlert(Alert.AlertType type, String title, String msg) {
        Alert alert = new Alert(type);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(msg);
=======
        dialog.showAndWait();
    }

    @FXML private void handleEditCustomer(CustomerDAO.Customer customer) {
        Dialog<CustomerDAO.Customer> dialog = new Dialog<>();
        dialog.setTitle("Edit Customer");
        dialog.setHeaderText("Edit data customer");

        TextField tfName = new TextField(customer.namaCustomer);
        TextField tfPosition = new TextField(customer.posisi);
        TextField tfAddress = new TextField(customer.alamat);
        TextField tfPhone = new TextField(customer.kontak);
        TextField tfEmail = new TextField(customer.email);
        ComboBox<String> cbStatus = new ComboBox<>();
        cbStatus.getItems().addAll("Aktif", "Nonaktif");
        cbStatus.setValue(customer.status);

        VBox content = new VBox(10);
        content.setStyle("-fx-padding: 10;");
        content.getChildren().addAll(
            new Label("Nama Perusahaan:"), tfName,
            new Label("Posisi:"), tfPosition,
            new Label("Alamat:"), tfAddress,
            new Label("No. Telepon:"), tfPhone,
            new Label("Email:"), tfEmail,
            new Label("Status:"), cbStatus
        );

        dialog.getDialogPane().setContent(content);
        dialog.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);

        dialog.setResultConverter(buttonType -> {
            if (buttonType == ButtonType.OK) {
                if (customerDAO.updateCustomer(customer.id, tfName.getText(), tfPosition.getText(),
                                              tfAddress.getText(), tfPhone.getText(),
                                              tfEmail.getText(), cbStatus.getValue())) {
                    showAlert(Alert.AlertType.INFORMATION, "Sukses", "✅ Customer berhasil diperbarui!");
                    loadCustomers();
                } else {
                    showAlert(Alert.AlertType.ERROR, "Gagal", "Customer gagal diperbarui.");
                }
            }
            return null;
        });

        dialog.showAndWait();
    }

    private void handleContact(CustomerDAO.Customer customer) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("Kontak Customer");
        alert.setHeaderText(customer.namaCustomer);
        alert.setContentText("Telepon: " + customer.kontak + "\nEmail: " + customer.email);
        alert.showAndWait();
    }

    @FXML private void handleFilterChange() {
        applyFilters();
    }

    @FXML private void handleSearch() {
        applyFilters();
    }

    private void showAlert(Alert.AlertType type, String title, String message) {
        Alert alert = new Alert(type);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
>>>>>>> origin/main
        alert.showAndWait();
    }
}
