package com.mycompany.tugas_akhir;

import com.mycompany.tugas_akhir.model.Barang;
import com.mycompany.tugas_akhir.model.InventoryTransfer;
import com.mycompany.tugas_akhir.model.Rak;
import com.mycompany.tugas_akhir.model.User;
import com.mycompany.tugas_akhir.service.InventoryTransferService;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.concurrent.Task;
import javafx.scene.control.*;
import javafx.scene.layout.GridPane;
import javafx.geometry.Insets;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.function.Function;

@Component
public class InventoryTransferWorkflow {

    private static final Logger log = LoggerFactory.getLogger(InventoryTransferWorkflow.class);
    private final InventoryTransferService transferService;

    public InventoryTransferWorkflow(InventoryTransferService transferService) {
        this.transferService = transferService;
    }

    public void openTransfer(List<Barang> barangItems, List<Rak> rakItems, User currentUser, Runnable refresh) {
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
        ComboBox<Barang> item = new ComboBox<>(FXCollections.observableArrayList(barangItems));
        ComboBox<String> source = new ComboBox<>();
        ComboBox<String> destination = new ComboBox<>();
        var rackCodes = FXCollections.observableArrayList(rakItems.stream().map(Rak::getKodeRak).toList());
        var sourceCodes = FXCollections.observableArrayList(rackCodes);
        sourceCodes.add("Stok Belum Dipetakan");
        source.setItems(sourceCodes);
        destination.setItems(rackCodes);
        TextField quantityField = new TextField("1");
        TextArea notesField = new TextArea();
        notesField.setPromptText("Alasan transfer (opsional)");
        notesField.setPrefRowCount(2);
        CheckBox useBarcodes = new CheckBox("Validasi dengan scan QR item dan barcode kedua SLOC");
        TextField itemQr = new TextField();
        itemQr.setPromptText("WMS1 item QR");
        TextField sourceQr = new TextField();
        sourceQr.setPromptText("SLOC1 sumber");
        TextField destinationQr = new TextField();
        destinationQr.setPromptText("SLOC1 tujuan");
        itemQr.setDisable(true);
        sourceQr.setDisable(true);
        destinationQr.setDisable(true);
        useBarcodes.selectedProperty().addListener((observable, oldValue, selected) -> {
            itemQr.setDisable(!selected);
            sourceQr.setDisable(!selected);
            destinationQr.setDisable(!selected);
            item.setDisable(selected);
            source.setDisable(selected);
            destination.setDisable(selected);
        });

        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(10);
        grid.setPadding(new Insets(16));
        grid.add(new Label("Barang"), 0, 0); grid.add(item, 1, 0);
        grid.add(new Label("Dari SLOC"), 0, 1); grid.add(source, 1, 1);
        grid.add(new Label("Ke SLOC"), 0, 2); grid.add(destination, 1, 2);
        grid.add(new Label("Jumlah"), 0, 3); grid.add(quantityField, 1, 3);
        grid.add(new Label("Keterangan"), 0, 4); grid.add(notesField, 1, 4);
        grid.add(useBarcodes, 0, 5, 2, 1);
        grid.add(new Label("QR barang"), 0, 6); grid.add(itemQr, 1, 6);
        grid.add(new Label("Barcode SLOC sumber"), 0, 7); grid.add(sourceQr, 1, 7);
        grid.add(new Label("Barcode SLOC tujuan"), 0, 8); grid.add(destinationQr, 1, 8);
        dialog.getDialogPane().setContent(grid);
        dialog.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);

        dialog.showAndWait().ifPresent(result -> {
            if (result != ButtonType.OK) return;
            boolean scanned = useBarcodes.isSelected();
            Barang selectedItem = item.getValue();
            String sourceCode = source.getValue();
            String destinationCode = destination.getValue();
            int quantity;
            try {
                quantity = Integer.parseInt(quantityField.getText().trim());
                if (quantity <= 0) throw new NumberFormatException();
            } catch (NumberFormatException exception) {
                showAlert(Alert.AlertType.WARNING, "Jumlah Tidak Valid", "Jumlah harus berupa angka positif.");
                return;
            }
            if (!scanned && (selectedItem == null || sourceCode == null || destinationCode == null)) {
                showAlert(Alert.AlertType.WARNING, "Data Belum Lengkap", "Pilih barang, lokasi sumber, dan lokasi tujuan.");
                return;
            }
            if (scanned && (itemQr.getText().isBlank() || sourceQr.getText().isBlank() || destinationQr.getText().isBlank())) {
                showAlert(Alert.AlertType.WARNING, "Barcode Belum Lengkap", "Scan QR barang, SLOC sumber, dan SLOC tujuan.");
                return;
            }
            Integer sourceId = scanned || "Stok Belum Dipetakan".equals(sourceCode) ? null : rackId(rakItems, sourceCode);
            Integer destinationId = scanned ? null : rackId(rakItems, destinationCode);
            if (!scanned && (destinationId == null || (!"Stok Belum Dipetakan".equals(sourceCode) && sourceId == null))) {
                showAlert(Alert.AlertType.ERROR, "Lokasi Tidak Valid", "Lokasi yang dipilih tidak lagi tersedia.");
                return;
            }

            Integer userId = currentUser.getId();
            String notes = notesField.getText().trim();
            String itemCode = itemQr.getText().trim();
            String sourceBarcode = sourceQr.getText().trim();
            String destinationBarcode = destinationQr.getText().trim();
            Task<Boolean> task = new Task<>() {
                @Override protected Boolean call() {
                    return scanned
                            ? transferService.transferStockWithBarcodes(itemCode, sourceBarcode, destinationBarcode,
                                    quantity, notes, userId)
                            : transferService.transferStock(selectedItem.getId(), sourceId, destinationId,
                                    quantity, notes, userId);
                }
            };
            task.setOnSucceeded(event -> {
                if (task.getValue()) {
                    showAlert(Alert.AlertType.INFORMATION, "Transfer Berhasil", scanned
                            ? quantity + " unit lot tervalidasi berhasil dipindahkan."
                            : quantity + " " + selectedItem.getSatuan() + " dipindahkan dari "
                                    + sourceCode + " ke " + destinationCode + ".");
                    refresh.run();
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

    public void openHistory() {
        Dialog<ButtonType> dialog = new Dialog<>();
        dialog.setTitle("Riwayat Transfer SLOC");
        dialog.getDialogPane().getButtonTypes().add(ButtonType.CLOSE);
        TableView<InventoryTransfer> table = new TableView<>();
        table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_FLEX_LAST_COLUMN);
        table.getColumns().add(column("Kode", transfer -> transfer.getTransferCode()));
        table.getColumns().add(column("Barang", transfer -> transfer.getBarang().getNamaBarang()));
        table.getColumns().add(column("Dari", transfer -> transfer.getSourceRak() == null
                ? "Belum dipetakan" : transfer.getSourceRak().getKodeRak()));
        table.getColumns().add(column("Ke", transfer -> transfer.getDestinationRak().getKodeRak()));
        table.getColumns().add(column("Jumlah", transfer -> String.valueOf(transfer.getQuantity())));
        table.getColumns().add(column("Petugas", transfer -> transfer.getUser().getFullName()));
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");
        table.getColumns().add(column("Waktu", transfer -> transfer.getCreatedAt().format(formatter)));
        table.setPlaceholder(new Label("Memuat riwayat transfer..."));
        dialog.getDialogPane().setContent(table);
        dialog.getDialogPane().setPrefSize(850, 420);
        dialog.show();

        Task<List<InventoryTransfer>> task = new Task<>() {
            @Override protected List<InventoryTransfer> call() { return transferService.getRecentTransfers(); }
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

    private Integer rackId(List<Rak> rackItems, String code) {
        return rackItems.stream().filter(rak -> rak.getKodeRak().equals(code))
                .map(Rak::getId).findFirst().orElse(null);
    }

    private TableColumn<InventoryTransfer, String> column(
            String title, Function<InventoryTransfer, String> value) {
        TableColumn<InventoryTransfer, String> column = new TableColumn<>(title);
        column.setCellValueFactory(cell -> new SimpleStringProperty(value.apply(cell.getValue())));
        return column;
    }

    private void showAlert(Alert.AlertType type, String title, String message) {
        Alert alert = new Alert(type);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
}
