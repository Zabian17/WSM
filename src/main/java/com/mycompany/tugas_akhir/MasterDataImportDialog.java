package com.mycompany.tugas_akhir;

import com.mycompany.tugas_akhir.service.MasterDataImportService;
import com.mycompany.tugas_akhir.service.MasterDataImportService.Dataset;
import com.mycompany.tugas_akhir.service.MasterDataImportService.ImportPreview;
import com.mycompany.tugas_akhir.service.MasterDataImportService.PreviewRow;
import javafx.beans.property.ReadOnlyStringWrapper;
import javafx.concurrent.Task;
import javafx.geometry.Insets;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Dialog;
import javafx.scene.control.Label;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.stage.FileChooser;
import javafx.stage.Window;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.util.List;

final class MasterDataImportDialog {

    private static final Logger log = LoggerFactory.getLogger(MasterDataImportDialog.class);
    private final MasterDataImportService importService;

    MasterDataImportDialog(MasterDataImportService importService) {
        this.importService = importService;
    }

    void show(Window owner) {
        Dialog<Void> dialog = new Dialog<>();
        dialog.setTitle("Impor Master Data");
        dialog.setHeaderText("Impor supplier, customer, atau barang dari file Excel");
        dialog.initOwner(owner);
        dialog.getDialogPane().getButtonTypes().add(ButtonType.CLOSE);

        ComboBox<Dataset> datasetSelector = new ComboBox<>();
        datasetSelector.getItems().setAll(Dataset.values());
        datasetSelector.getSelectionModel().select(Dataset.ITEM);

        Button templateButton = new Button("Unduh Template");
        Button chooseFileButton = new Button("Pilih File Excel");
        Button importButton = new Button("Impor Data");
        importButton.setDisable(true);
        Label selectedFileLabel = new Label("Belum ada file dipilih");
        Label previewStatus = new Label("Pilih jenis data dan file .xlsx untuk melihat pratinjau.");
        TableView<PreviewRow> previewTable = new TableView<>();
        previewTable.setPlaceholder(new Label("Pratinjau data akan tampil di sini."));
        previewTable.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_FLEX_LAST_COLUMN);

        final ImportPreview[] currentPreview = {null};

        datasetSelector.valueProperty().addListener((observable, oldValue, newValue) -> {
            currentPreview[0] = null;
            previewTable.getColumns().clear();
            previewTable.getItems().clear();
            selectedFileLabel.setText("Belum ada file dipilih");
            importButton.setDisable(true);
            previewStatus.setText("Pilih file Excel untuk melihat pratinjau.");
        });

        templateButton.setOnAction(event -> saveTemplate(owner, datasetSelector.getValue()));
        chooseFileButton.setOnAction(event -> {
            FileChooser chooser = new FileChooser();
            chooser.setTitle("Pilih File Excel");
            chooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("Excel Workbook (*.xlsx)", "*.xlsx"));
            File file = chooser.showOpenDialog(owner);
            if (file == null) return;
            selectedFileLabel.setText(file.getName());
            loadPreview(file, datasetSelector, previewTable, previewStatus, chooseFileButton,
                    importButton, currentPreview);
        });
        importButton.setOnAction(event -> {
            ImportPreview preview = currentPreview[0];
            if (preview == null || !preview.isValid()) return;
            List<List<String>> rows = preview.rows().stream().map(PreviewRow::values).toList();
            runImport(datasetSelector.getValue(), rows, importButton, dialog);
        });

        HBox actions = new HBox(8, datasetSelector, templateButton, chooseFileButton, importButton);
        HBox fileInfo = new HBox(8, new Label("File:"), selectedFileLabel);
        VBox content = new VBox(12, actions, fileInfo, previewStatus, previewTable);
        content.setPadding(new Insets(8, 0, 0, 0));
        VBox.setVgrow(previewTable, Priority.ALWAYS);
        dialog.getDialogPane().setContent(content);
        dialog.getDialogPane().setPrefSize(900, 560);
        dialog.showAndWait();
    }

    private void saveTemplate(Window owner, Dataset dataset) {
        FileChooser chooser = new FileChooser();
        chooser.setTitle("Simpan Template " + dataset.getLabel());
        chooser.setInitialFileName("template-" + dataset.name().toLowerCase() + ".xlsx");
        chooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("Excel Workbook (*.xlsx)", "*.xlsx"));
        File target = chooser.showSaveDialog(owner);
        if (target == null) return;
        try {
            Files.write(target.toPath(), importService.createTemplate(dataset));
        } catch (IOException exception) {
            log.error("Gagal menyimpan template Excel {}", target, exception);
            showError("Template gagal disimpan: " + exception.getMessage());
        }
    }

    private void loadPreview(File file, ComboBox<Dataset> datasetSelector, TableView<PreviewRow> table,
                             Label status, Button chooseFileButton, Button importButton,
                             ImportPreview[] currentPreview) {
        Dataset dataset = datasetSelector.getValue();
        Task<ImportPreview> task = new Task<>() {
            @Override
            protected ImportPreview call() throws Exception {
                try (FileInputStream input = new FileInputStream(file)) {
                    return importService.preview(dataset, input);
                }
            }
        };
        task.setOnRunning(event -> {
            datasetSelector.setDisable(true);
            chooseFileButton.setDisable(true);
            importButton.setDisable(true);
            status.setText("Memeriksa file dan data...");
        });
        task.setOnSucceeded(event -> {
            datasetSelector.setDisable(false);
            chooseFileButton.setDisable(false);
            ImportPreview preview = task.getValue();
            currentPreview[0] = preview;
            populateTable(table, preview);
            if (preview.isValid()) {
                status.setText(preview.rows().size() + " baris valid. Tidak ada duplikat.");
                importButton.setDisable(false);
            } else {
                status.setText("Impor diblokir: " + preview.getInvalidRowCount()
                        + " baris tidak valid atau file kosong. Perbaiki file lalu pratinjau kembali.");
                importButton.setDisable(true);
            }
        });
        task.setOnFailed(event -> {
            datasetSelector.setDisable(false);
            chooseFileButton.setDisable(false);
            currentPreview[0] = null;
            importButton.setDisable(true);
            status.setText("File gagal diperiksa.");
            Throwable cause = task.getException();
            log.error("Gagal memvalidasi file impor Excel {}", file, cause);
            showError(cause.getMessage() == null ? "File tidak dapat dibaca." : cause.getMessage());
        });
        Thread thread = new Thread(task, "master-data-import-preview");
        thread.setDaemon(true);
        thread.start();
    }

    private void populateTable(TableView<PreviewRow> table, ImportPreview preview) {
        table.getColumns().clear();
        TableColumn<PreviewRow, String> rowColumn = new TableColumn<>("Baris");
        rowColumn.setCellValueFactory(cell -> new ReadOnlyStringWrapper(
                cell.getValue().rowNumber() == 0 ? "-" : Integer.toString(cell.getValue().rowNumber())));
        rowColumn.setPrefWidth(55);
        table.getColumns().add(rowColumn);
        for (int columnIndex = 0; columnIndex < preview.headers().size(); columnIndex++) {
            final int valueIndex = columnIndex;
            TableColumn<PreviewRow, String> column = new TableColumn<>(preview.headers().get(columnIndex));
            column.setCellValueFactory(cell -> new ReadOnlyStringWrapper(
                    cell.getValue().values().size() > valueIndex ? cell.getValue().values().get(valueIndex) : ""));
            table.getColumns().add(column);
        }
        TableColumn<PreviewRow, String> validationColumn = new TableColumn<>("Validasi");
        validationColumn.setCellValueFactory(cell -> new ReadOnlyStringWrapper(
                cell.getValue().isValid() ? "Valid" : cell.getValue().error()));
        validationColumn.setPrefWidth(210);
        table.getColumns().add(validationColumn);
        table.getItems().setAll(preview.rows());
    }

    private void runImport(Dataset dataset, List<List<String>> rows, Button importButton, Dialog<Void> dialog) {
        importButton.setDisable(true);
        Task<Integer> task = new Task<>() {
            @Override
            protected Integer call() {
                return importService.importRows(dataset, rows);
            }
        };
        task.setOnSucceeded(event -> {
            showInfo(task.getValue() + " data " + dataset.getLabel().toLowerCase() + " berhasil diimpor.");
            dialog.close();
        });
        task.setOnFailed(event -> {
            Throwable cause = task.getException();
            log.error("Gagal mengimpor master data {}", dataset, cause);
            showError(cause.getMessage() == null ? "Data gagal diimpor." : cause.getMessage());
            importButton.setDisable(false);
        });
        Thread thread = new Thread(task, "master-data-import");
        thread.setDaemon(true);
        thread.start();
    }

    private void showError(String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR, message, ButtonType.OK);
        alert.setTitle("Impor Gagal");
        alert.setHeaderText("File atau data tidak dapat diproses");
        alert.showAndWait();
    }

    private void showInfo(String message) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION, message, ButtonType.OK);
        alert.setTitle("Impor Berhasil");
        alert.setHeaderText(null);
        alert.showAndWait();
    }
}
