package com.mycompany.tugas_akhir;

import com.mycompany.tugas_akhir.model.Rak;
import com.mycompany.tugas_akhir.model.StockCount;
import com.mycompany.tugas_akhir.model.StockCountStatus;
import com.mycompany.tugas_akhir.model.User;
import com.mycompany.tugas_akhir.service.StockCountService;
import com.mycompany.tugas_akhir.service.StockCountService.CycleCountRecommendation;
import com.mycompany.tugas_akhir.service.StockCountService.CountLineView;
import javafx.beans.property.ReadOnlyStringWrapper;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.concurrent.Task;
import javafx.event.ActionEvent;
import javafx.geometry.Insets;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonBar;
import javafx.scene.control.ButtonType;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Dialog;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.control.cell.TextFieldTableCell;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.stage.Window;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.format.DateTimeFormatter;
import java.time.YearMonth;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

final class StockCountDialog {

    private static final Logger log = LoggerFactory.getLogger(StockCountDialog.class);
    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    private final StockCountService stockCountService;
    private final User currentUser;

    StockCountDialog(StockCountService stockCountService, User currentUser) {
        this.stockCountService = stockCountService;
        this.currentUser = currentUser;
    }

    void show(Window owner, Runnable onPosted) {
        Dialog<ButtonType> dialog = new Dialog<>();
        dialog.setTitle("Stock Opname / Cycle Count");
        dialog.setHeaderText("Hitung satu rak per sesi; stok disesuaikan hanya setelah posting.");
        dialog.initOwner(owner);
        dialog.getDialogPane().getButtonTypes().add(ButtonType.CLOSE);

        ComboBox<CycleCountRecommendation> rackSelector = new ComboBox<>();
        rackSelector.setPromptText("Pilih rak");
        rackSelector.setPrefWidth(340);
        rackSelector.setCellFactory(list -> recommendationCell());
        rackSelector.setButtonCell(recommendationCell());
        Button createButton = new Button("Buat Sesi Opname");
        createButton.setDisable(true);
        Button periodicButton = new Button("Buat Opname Periodik");
        Button dailyButton = new Button("Opname Rak Berubah Hari Ini");
        TextField monthField = new TextField(YearMonth.now().toString());
        monthField.setPromptText("yyyy-MM");
        monthField.setPrefColumnCount(8);
        Button monthlyButton = new Button("Jadwalkan Bulanan");
        TableView<StockCount> sessions = createSessionTable();
        Button countButton = new Button("Isi / Tinjau Hitungan");
        Button postButton = new Button("Posting Penyesuaian");
        Button cancelButton = new Button("Batalkan Sesi");
        Button refreshButton = new Button("Muat Ulang");
        countButton.setDisable(true);
        postButton.setDisable(true);
        cancelButton.setDisable(true);

        sessions.getSelectionModel().selectedItemProperty().addListener((observable, oldValue, selected) -> {
            boolean counting = selected != null && selected.getStatus() == StockCountStatus.COUNTING;
            boolean counted = selected != null && selected.getStatus() == StockCountStatus.COUNTED;
            boolean scheduled = selected != null && selected.getStatus() == StockCountStatus.SCHEDULED;
            countButton.setDisable(selected == null);
            countButton.setText(selected != null && selected.getStatus() == StockCountStatus.SCHEDULED
                    ? "Mulai Opname" : counting ? "Isi Hitungan" : "Tinjau Hasil");
            postButton.setDisable(!counted);
            cancelButton.setDisable(!scheduled && !counting && !counted);
        });

        createButton.setOnAction(event -> createSession(rackSelector, createButton, sessions));
        dailyButton.setOnAction(event -> createDailySession(dailyButton, sessions));
        periodicButton.setOnAction(event -> createPeriodicSession(periodicButton, sessions));
        monthlyButton.setOnAction(event -> createMonthlySchedule(monthField, monthlyButton, sessions));
        countButton.setOnAction(event -> {
            StockCount selected = sessions.getSelectionModel().getSelectedItem();
            if (selected != null && selected.getStatus() == StockCountStatus.SCHEDULED)
                startScheduledSession(owner, selected, sessions);
            else if (selected != null) openCountEditor(owner, selected, sessions,
                    selected.getStatus() == StockCountStatus.COUNTING);
        });
        postButton.setOnAction(event -> {
            StockCount selected = sessions.getSelectionModel().getSelectedItem();
            if (selected != null) postSession(owner, selected, sessions, onPosted);
        });
        cancelButton.setOnAction(event -> {
            StockCount selected = sessions.getSelectionModel().getSelectedItem();
            if (selected != null) cancelSession(owner, selected, sessions);
        });
        refreshButton.setOnAction(event -> loadSessions(sessions));

        HBox createControls = new HBox(8, rackSelector, createButton, dailyButton);
        HBox cycleControls = new HBox(8, periodicButton, refreshButton);
        HBox monthlyControls = new HBox(8, monthField, monthlyButton);
        HBox actionControls = new HBox(8, countButton, postButton, cancelButton);
        VBox content = new VBox(12,
                new Label("Cycle count dilakukan per rak. Semua SKU pada rak dimasukkan, termasuk saldo nol."),
                createControls, cycleControls, monthlyControls, sessions, actionControls);
        content.setPadding(new Insets(8, 0, 0, 0));
        VBox.setVgrow(sessions, Priority.ALWAYS);
        dialog.getDialogPane().setContent(content);
        dialog.getDialogPane().setPrefSize(880, 570);
        dialog.show();

        Task<List<CycleCountRecommendation>> racksTask = new Task<>() {
            @Override
            protected List<CycleCountRecommendation> call() {
                return stockCountService.getCycleCountRecommendations();
            }
        };
        racksTask.setOnSucceeded(event -> {
            rackSelector.setItems(FXCollections.observableArrayList(racksTask.getValue()));
            createButton.setDisable(rackSelector.getItems().isEmpty());
            if (!rackSelector.getItems().isEmpty()) rackSelector.getSelectionModel().selectFirst();
        });
        racksTask.setOnFailed(event -> {
            log.error("Gagal memuat daftar rak untuk stock opname", racksTask.getException());
            showError(owner, "Daftar rak gagal dimuat.");
        });
        startTask(racksTask, "stock-count-racks");
        loadSessions(sessions);
    }

    private TableView<StockCount> createSessionTable() {
        TableView<StockCount> table = new TableView<>();
        table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_FLEX_LAST_COLUMN);
        table.getColumns().add(column("Kode Sesi", count -> count.getCountCode()));
        table.getColumns().add(column("Siklus", count -> count.getCycleCode() == null
                ? "Ad hoc" : count.getCycleCode()));
        table.getColumns().add(column("Rak", count -> count.getRak().getKodeRak()));
        table.getColumns().add(column("Dibuat", count -> count.getCreatedAt().format(DATE_FORMAT)));
        table.getColumns().add(column("Jadwal", count -> count.getDueDate() == null
                ? "-" : count.getDueDate().format(DateTimeFormatter.ofPattern("dd/MM/yyyy"))));
        table.getColumns().add(column("Status", count -> count.getStatus().name()));
        table.getColumns().add(column("Baris / Selisih", count -> {
            long varianceLines = count.getLines().stream()
                    .filter(line -> line.getCountedQuantity() != null
                            && !line.getCountedQuantity().equals(line.getExpectedQuantity())).count();
            long completed = count.getLines().stream().filter(line -> line.getCountedQuantity() != null).count();
            String progress = count.getCycleType() == com.mycompany.tugas_akhir.model.StockCountCycleType.PERIODIC
                    ? completed + "/" + count.getLines().size() + " dihitung" : count.getLines().size() + " SKU";
            return progress + (count.getStatus() == StockCountStatus.COUNTING ? ""
                    : " / " + varianceLines + " selisih");
        }));
        table.setPlaceholder(new Label("Belum ada sesi stock opname."));
        return table;
    }

    private TableColumn<StockCount, String> column(String title,
                                                   java.util.function.Function<StockCount, String> value) {
        TableColumn<StockCount, String> column = new TableColumn<>(title);
        column.setCellValueFactory(cell -> new ReadOnlyStringWrapper(value.apply(cell.getValue())));
        return column;
    }

    private ListCell<CycleCountRecommendation> recommendationCell() {
        return new ListCell<>() {
            @Override
            protected void updateItem(CycleCountRecommendation recommendation, boolean empty) {
                super.updateItem(recommendation, empty);
                if (empty || recommendation == null) {
                    setText(null);
                    setTooltip(null);
                    return;
                }
                Rak rack = recommendation.rack();
                setText(recommendation.priority() + " | " + rack.getKodeRak() + " | "
                        + recommendation.reason());
                setTooltip(new javafx.scene.control.Tooltip(recommendation.reason()));
            }
        };
    }

    private void loadSessions(TableView<StockCount> table) {
        Task<List<StockCount>> task = new Task<>() {
            @Override
            protected List<StockCount> call() { return stockCountService.getRecentCounts(); }
        };
        task.setOnSucceeded(event -> table.setItems(FXCollections.observableArrayList(task.getValue())));
        task.setOnFailed(event -> {
            log.error("Gagal memuat sesi stock opname", task.getException());
            showError(table.getScene().getWindow(), "Riwayat stock opname gagal dimuat.");
        });
        startTask(task, "stock-count-list");
    }

    private void createSession(ComboBox<CycleCountRecommendation> rackSelector, Button createButton,
                               TableView<StockCount> table) {
        CycleCountRecommendation recommendation = rackSelector.getValue();
        if (recommendation == null) return;
        Rak rack = recommendation.rack();
        createButton.setDisable(true);
        Task<StockCount> task = new Task<>() {
            @Override
            protected StockCount call() {
                return stockCountService.createCycleCount(rack.getId(), currentUser.getId());
            }
        };
        task.setOnSucceeded(event -> {
            createButton.setDisable(false);
            loadSessions(table);
            showInfo(table.getScene().getWindow(), "Sesi " + task.getValue().getCountCode()
                    + " dibuat. Saldo buku disimpan sebagai snapshot.");
        });
        task.setOnFailed(event -> {
            createButton.setDisable(false);
            log.error("Gagal membuat sesi stock opname", task.getException());
            showError(table.getScene().getWindow(), messageFor(task.getException()));
        });
        startTask(task, "stock-count-create");
    }

    private void openCountEditor(Window owner, StockCount count, TableView<StockCount> sessions, boolean editable) {
        Task<List<CountLineView>> task = new Task<>() {
            @Override
            protected List<CountLineView> call() {
                return stockCountService.getCountLines(count.getId(), currentUser.getId(), editable);
            }
        };
        task.setOnSucceeded(event -> showCountEditor(owner, count, task.getValue(), sessions, editable));
        task.setOnFailed(event -> {
            log.error("Gagal memuat detail hitungan {}", count.getCountCode(), task.getException());
            showError(owner, messageFor(task.getException()));
        });
        startTask(task, "stock-count-detail");
    }

    private void showCountEditor(Window owner, StockCount count, List<CountLineView> lines,
                                 TableView<StockCount> sessions, boolean editable) {
        Dialog<Boolean> dialog = new Dialog<>();
        dialog.setTitle((editable ? "Isi Hitungan - " : "Tinjau Hasil - ") + count.getCountCode());
        dialog.setHeaderText(editable
                ? "Masukkan jumlah fisik untuk setiap SKU. Semua baris wajib diisi."
                : "Bandingkan saldo buku dan hasil hitung fisik.");
        dialog.initOwner(owner);
        ButtonType saveType = new ButtonType("Simpan Hitungan", ButtonBar.ButtonData.OK_DONE);
        if (editable) dialog.getDialogPane().getButtonTypes().addAll(saveType, ButtonType.CANCEL);
        else dialog.getDialogPane().getButtonTypes().add(ButtonType.CLOSE);

        if (editable && lines.isEmpty()) {
            showInfo(owner, "Tidak ada SKU yang belum dihitung dan ditugaskan kepada Anda pada sesi ini.");
            return;
        }

        var rows = FXCollections.observableArrayList(lines.stream().map(CountEditRow::new).toList());
        TableView<CountEditRow> table = new TableView<>(rows);
        table.setEditable(editable);
        table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_FLEX_LAST_COLUMN);
        table.getColumns().add(editColumn("Kode", row -> row.itemCode));
        table.getColumns().add(editColumn("Nama Barang", row -> row.itemName));
        table.getColumns().add(editColumn("Saldo Buku", row -> row.expectedQuantity + " " + safeUnit(row.unit)));
        if (!editable && lines.stream().anyMatch(line -> line.assignedUserName() != null)) {
            table.getColumns().add(editColumn("Petugas", row -> row.assignedUserName));
            table.getColumns().add(editColumn("Dihitung Oleh", row -> row.countedByName));
        }

        TableColumn<CountEditRow, String> countedColumn = new TableColumn<>("Hitung Fisik");
        countedColumn.setCellValueFactory(cell -> new SimpleStringProperty(
                cell.getValue().countedText == null || cell.getValue().countedText.isBlank()
                        ? "Belum dihitung" : cell.getValue().countedText));
        if (editable) {
            countedColumn.setCellFactory(TextFieldTableCell.forTableColumn());
            countedColumn.setOnEditCommit(event -> event.getRowValue().countedText = event.getNewValue());
            countedColumn.setPrefWidth(140);
        }
        table.getColumns().add(countedColumn);
        if (!editable) {
            TableColumn<CountEditRow, String> varianceColumn = new TableColumn<>("Selisih");
            varianceColumn.setCellValueFactory(cell -> new ReadOnlyStringWrapper(cell.getValue().varianceText()));
            table.getColumns().add(varianceColumn);
        }

        VBox content = new VBox(8, new Label(editable
                ? "Nilai nol harus dimasukkan secara eksplisit." : "Selisih = hitung fisik - saldo buku."), table);
        content.setPadding(new Insets(8, 0, 0, 0));
        VBox.setVgrow(table, Priority.ALWAYS);
        dialog.getDialogPane().setContent(content);
        dialog.getDialogPane().setPrefSize(760, 520);
        if (editable) {
            Button saveButton = (Button) dialog.getDialogPane().lookupButton(saveType);
            saveButton.addEventFilter(ActionEvent.ACTION, event -> {
                try {
                    for (CountEditRow row : rows) row.toQuantity();
                } catch (IllegalArgumentException exception) {
                    event.consume();
                    showError(owner, exception.getMessage());
                }
            });
        }
        dialog.setResultConverter(button -> editable && button == saveType);
        dialog.showAndWait().filter(Boolean::booleanValue).ifPresent(ignored -> {
            Map<Integer, Integer> quantities = new HashMap<>();
            for (CountEditRow row : rows) quantities.put(row.lineId, row.toQuantity());
            Task<Void> task = new Task<>() {
                @Override
                protected Void call() {
                    stockCountService.submitCounts(count.getId(), quantities, currentUser.getId());
                    return null;
                }
            };
            task.setOnSucceeded(event -> {
                loadSessions(sessions);
                showInfo(owner, "Hitungan tersimpan. Sesi siap ditinjau dan diposting setelah seluruh tugas selesai.");
            });
            task.setOnFailed(event -> {
                log.error("Gagal menyimpan hitungan {}", count.getCountCode(), task.getException());
                showError(owner, messageFor(task.getException()));
            });
            startTask(task, "stock-count-submit");
        });
    }

    private void createPeriodicSession(Button button, TableView<StockCount> table) {
        button.setDisable(true);
        Task<List<StockCount>> task = new Task<>() {
            @Override
            protected List<StockCount> call() {
                return stockCountService.createPeriodicCycleCount(currentUser.getId());
            }
        };
        task.setOnSucceeded(event -> {
            button.setDisable(false);
            loadSessions(table);
            showInfo(table.getScene().getWindow(), "Opname periodik "
                    + task.getValue().getFirst().getCycleCode() + " dibuat untuk "
                    + task.getValue().size() + " rak dan dibagi rata ke pengguna.");
        });
        task.setOnFailed(event -> {
            button.setDisable(false);
            log.error("Gagal membuat opname periodik", task.getException());
            showError(table.getScene().getWindow(), messageFor(task.getException()));
        });
        startTask(task, "stock-count-periodic-create");
    }

    private void createDailySession(Button button, TableView<StockCount> table) {
        button.setDisable(true);
        Task<List<StockCount>> task = new Task<>() {
            @Override
            protected List<StockCount> call() {
                return stockCountService.createDailyCycleCount(currentUser.getId());
            }
        };
        task.setOnSucceeded(event -> {
            button.setDisable(false);
            loadSessions(table);
            List<StockCount> counts = task.getValue();
            if (counts.isEmpty()) {
                showInfo(table.getScene().getWindow(), "Tidak ada mutasi stok pada rak hari ini.");
                return;
            }
            showInfo(table.getScene().getWindow(), "Opname harian " + counts.getFirst().getCycleCode()
                    + " dibuat untuk " + counts.size() + " rak yang mengalami mutasi hari ini.");
        });
        task.setOnFailed(event -> {
            button.setDisable(false);
            log.error("Gagal membuat opname harian", task.getException());
            showError(table.getScene().getWindow(), messageFor(task.getException()));
        });
        startTask(task, "stock-count-daily-create");
    }

    private void createMonthlySchedule(TextField monthField, Button button, TableView<StockCount> table) {
        final YearMonth month;
        try {
            month = YearMonth.parse(monthField.getText().trim());
        } catch (RuntimeException exception) {
            showError(table.getScene().getWindow(), "Bulan harus ditulis dalam format yyyy-MM.");
            return;
        }
        button.setDisable(true);
        Task<List<StockCount>> task = new Task<>() {
            @Override
            protected List<StockCount> call() {
                return stockCountService.createMonthlySchedule(month, currentUser.getId());
            }
        };
        task.setOnSucceeded(event -> {
            button.setDisable(false);
            loadSessions(table);
            showInfo(table.getScene().getWindow(), "Jadwal opname " + month
                    + " dibuat untuk " + task.getValue().size() + " rak pada hari kerja Senin–Jumat.");
        });
        task.setOnFailed(event -> {
            button.setDisable(false);
            log.error("Gagal membuat jadwal opname bulanan", task.getException());
            showError(table.getScene().getWindow(), messageFor(task.getException()));
        });
        startTask(task, "stock-count-monthly-create");
    }

    private void startScheduledSession(Window owner, StockCount count, TableView<StockCount> sessions) {
        Task<StockCount> task = new Task<>() {
            @Override
            protected StockCount call() { return stockCountService.startScheduledCount(count.getId()); }
        };
        task.setOnSucceeded(event -> {
            loadSessions(sessions);
            openCountEditor(owner, task.getValue(), sessions, true);
        });
        task.setOnFailed(event -> {
            log.error("Gagal memulai jadwal opname {}", count.getCountCode(), task.getException());
            showError(owner, messageFor(task.getException()));
        });
        startTask(task, "stock-count-monthly-start");
    }

    private TableColumn<CountEditRow, String> editColumn(String title,
                                                         java.util.function.Function<CountEditRow, String> value) {
        TableColumn<CountEditRow, String> column = new TableColumn<>(title);
        column.setCellValueFactory(cell -> new ReadOnlyStringWrapper(value.apply(cell.getValue())));
        return column;
    }

    private String safeUnit(String unit) {
        return unit == null || unit.isBlank() ? "pcs" : unit;
    }

    private void postSession(Window owner, StockCount count, TableView<StockCount> sessions,
                             Runnable onPosted) {
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION,
                "Posting akan mengubah saldo barang, lot, dan keterisian rak sesuai hasil hitung. "
                        + "Pastikan seluruh selisih sudah ditinjau.",
                ButtonType.CANCEL, ButtonType.OK);
        confirm.initOwner(owner);
        confirm.setTitle("Konfirmasi Posting Opname");
        confirm.setHeaderText(count.getCountCode() + " - " + count.getRak().getKodeRak());
        if (confirm.showAndWait().orElse(ButtonType.CANCEL) != ButtonType.OK) return;

        Task<Void> task = new Task<>() {
            @Override
            protected Void call() {
                stockCountService.postCount(count.getId(), currentUser.getId());
                return null;
            }
        };
        task.setOnSucceeded(event -> {
            loadSessions(sessions);
            onPosted.run();
            showInfo(owner, "Penyesuaian stock opname berhasil diposting.");
        });
        task.setOnFailed(event -> {
            log.error("Gagal memposting sesi stock opname {}", count.getCountCode(), task.getException());
            showError(owner, messageFor(task.getException()));
        });
        startTask(task, "stock-count-post");
    }

    private void cancelSession(Window owner, StockCount count, TableView<StockCount> sessions) {
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION,
                "Sesi akan ditandai batal dan tidak akan mengubah stok.",
                ButtonType.CANCEL, ButtonType.OK);
        confirm.initOwner(owner);
        confirm.setTitle("Batalkan Sesi Opname");
        confirm.setHeaderText(count.getCountCode());
        if (confirm.showAndWait().orElse(ButtonType.CANCEL) != ButtonType.OK) return;
        Task<Void> task = new Task<>() {
            @Override
            protected Void call() {
                stockCountService.cancelCount(count.getId());
                return null;
            }
        };
        task.setOnSucceeded(event -> loadSessions(sessions));
        task.setOnFailed(event -> {
            log.error("Gagal membatalkan sesi opname {}", count.getCountCode(), task.getException());
            showError(owner, messageFor(task.getException()));
        });
        startTask(task, "stock-count-cancel");
    }

    private void startTask(Task<?> task, String threadName) {
        Thread thread = new Thread(task, threadName);
        thread.setDaemon(true);
        thread.start();
    }

    private String messageFor(Throwable error) {
        String message = error == null ? null : error.getMessage();
        return message == null || message.isBlank() ? "Proses stock opname gagal." : message;
    }

    private void showError(Window owner, String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR, message, ButtonType.OK);
        alert.initOwner(owner);
        alert.setTitle("Stock Opname Gagal");
        alert.setHeaderText(null);
        alert.showAndWait();
    }

    private void showInfo(Window owner, String message) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION, message, ButtonType.OK);
        alert.initOwner(owner);
        alert.setTitle("Stock Opname");
        alert.setHeaderText(null);
        alert.showAndWait();
    }

    private static final class CountEditRow {
        private final Integer lineId;
        private final String itemCode;
        private final String itemName;
        private final Integer expectedQuantity;
        private final String unit;
        private final String assignedUserName;
        private final String countedByName;
        private String countedText;

        private CountEditRow(CountLineView line) {
            lineId = line.lineId();
            itemCode = line.itemCode();
            itemName = line.itemName();
            expectedQuantity = line.expectedQuantity();
            unit = line.unit();
            assignedUserName = line.assignedUserName() == null ? "-" : line.assignedUserName();
            countedByName = line.countedByName() == null ? "-" : line.countedByName();
            countedText = line.countedQuantity() == null ? "" : line.countedQuantity().toString();
        }

        private int toQuantity() {
            try {
                int quantity = Integer.parseInt(countedText == null ? "" : countedText.trim());
                if (quantity < 0) throw new NumberFormatException();
                return quantity;
            } catch (NumberFormatException exception) {
                throw new IllegalArgumentException("Masukkan bilangan bulat nol atau lebih untuk setiap SKU.");
            }
        }

        private String varianceText() {
            Integer counted = countedText == null || countedText.isBlank()
                    ? null : Integer.valueOf(countedText);
            return counted == null ? "-" : Integer.toString(counted - expectedQuantity);
        }
    }
}
