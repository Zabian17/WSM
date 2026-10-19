package com.mycompany.tugas_akhir;

import com.mycompany.tugas_akhir.config.SpringFXMLLoader;
import com.mycompany.tugas_akhir.model.Rak;
import com.mycompany.tugas_akhir.model.Section;
import com.mycompany.tugas_akhir.model.User;
import com.mycompany.tugas_akhir.service.SectionService;
import com.mycompany.tugas_akhir.service.TransaksiService;
import javafx.collections.FXCollections;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.*;
import javafx.stage.Stage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.ResourceBundle;

@Component
public class DashboardController implements Initializable {

    private static final Logger log = LoggerFactory.getLogger(DashboardController.class);
    private final TransaksiService transaksiService;
    private final SectionService sectionService;
    private final SpringFXMLLoader fxmlLoader;

    private User currentUser;
    private Node defaultDashboardView;

    @FXML private ScrollPane mainScrollPane;
    @FXML private FlowPane sectionsFlowPane;

    @FXML private HBox navDashboard;
    @FXML private HBox navStorage;
    @FXML private HBox navMovements;
    @FXML private HBox navCustomer;
    @FXML private HBox navReport;
    @FXML private HBox navSetting;

    @FXML private Label pageTitleLabel;
    @FXML private Label topbarAvatarLabel;
    @FXML private StackPane topbarAvatarStack;
    @FXML private ImageView topbarAvatarImg;
    @FXML private StackPane topbarAvatarFallback;

    @FXML private Label lblBarangMasuk;
    @FXML private Label lblBarangKeluar;

    public DashboardController(TransaksiService transaksiService,
                               SectionService sectionService,
                               SpringFXMLLoader fxmlLoader) {
        this.transaksiService = transaksiService;
        this.sectionService = sectionService;
        this.fxmlLoader = fxmlLoader;
    }

    @Override
    public void initialize(URL url, ResourceBundle rb) {
        setActiveNav("dashboard");

        if (mainScrollPane != null) {
            defaultDashboardView = mainScrollPane.getContent();
        }

        loadStatsAsync();
        loadSectionsAsync();
    }

    public void initUser(User user) {
        this.currentUser = user;
        if (user != null) {
            String inisial = (user.getFullName() != null && !user.getFullName().isEmpty()) 
                    ? user.getFullName().substring(0, 1).toUpperCase() : "U";
            updateAvatar(user, inisial);
        }
    }

    private void loadStatsAsync() {
        Task<int[]> statsTask = new Task<>() {
            @Override
            protected int[] call() {
                return transaksiService.getStatistik24H();
            }
        };

        statsTask.setOnSucceeded(e -> {
            int[] stats = statsTask.getValue();
            if (lblBarangMasuk != null) lblBarangMasuk.setText(String.format("%,d", stats[0]));
            if (lblBarangKeluar != null) lblBarangKeluar.setText(String.format("%,d", stats[1]));
        });

        statsTask.setOnFailed(e -> log.error("Gagal load statistik 24H", statsTask.getException()));

        Thread thread = new Thread(statsTask);
        thread.setDaemon(true);
        thread.start();
    }

    @FXML
    private void handleRefreshSections() {
        loadSectionsAsync();
        loadStatsAsync();
    }

    private void loadSectionsAsync() {
        Task<List<Section>> sectionTask = new Task<>() {
            @Override
            protected List<Section> call() {
                return sectionService.getAllSections();
            }
        };

        sectionTask.setOnSucceeded(e -> {
            List<Section> sections = sectionTask.getValue();
            renderDynamicSections(sections);
        });

        sectionTask.setOnFailed(e -> log.error("Gagal load dynamic sections", sectionTask.getException()));

        Thread th = new Thread(sectionTask);
        th.setDaemon(true);
        th.start();
    }

    private void renderDynamicSections(List<Section> sections) {
        if (sectionsFlowPane == null) return;
        sectionsFlowPane.getChildren().clear();

        for (Section section : sections) {
            VBox card = createSectionCard(section);
            sectionsFlowPane.getChildren().add(card);
        }
    }

    private VBox createSectionCard(Section section) {
        VBox card = new VBox(12);
        card.setStyle("-fx-background-color: #ffffff; -fx-background-radius: 12; "
                    + "-fx-border-color: #e7ebf2; -fx-border-radius: 12; -fx-padding: 16; -fx-pref-width: 320px;");

        // 1. HEADER ROW
        HBox header = new HBox(8);
        header.setAlignment(Pos.CENTER_LEFT);

        VBox titleBox = new VBox(2);
        Label lblTitle = new Label(section.getNamaSection());
        lblTitle.setStyle("-fx-font-weight: bold; -fx-font-size: 14px; -fx-text-fill: #202c41;");

        int activeSlots = (int) section.getRaks().stream().filter(r -> r.getTerisi() > 0).count();
        Label lblSub = new Label(activeSlots + "/" + section.getRaks().size() + " terisi • Target: " + section.getTargetKategori());
        lblSub.setStyle("-fx-font-size: 11px; -fx-text-fill: #748198;");
        titleBox.getChildren().addAll(lblTitle, lblSub);

        Region spring = new Region();
        HBox.setHgrow(spring, Priority.ALWAYS);

        Button btnConfig = new Button("⚙");
        btnConfig.getStyleClass().add("btn-outline");
        btnConfig.setTooltip(new Tooltip("Konfigurasi Section & Alert"));
        btnConfig.setOnAction(e -> showSectionConfigDialog(section));

        Button btnDelete = new Button("×");
        btnDelete.getStyleClass().add("btn-action-delete");
        btnDelete.setTooltip(new Tooltip("Hapus Section"));
        btnDelete.setOnAction(e -> handleDeleteSection(section));

        header.getChildren().addAll(titleBox, spring, btnConfig, btnDelete);

        // 2. SLOTS GRID PANE
        GridPane grid = new GridPane();
        grid.setHgap(6);
        grid.setVgap(6);
        grid.setAlignment(Pos.CENTER);

        int cols = 2;
        int row = 0;
        int col = 0;

        for (Rak rak : section.getRaks()) {
            Button btnSlot = new Button(rak.getKodeRak());
            btnSlot.setPrefWidth(135);
            btnSlot.setPrefHeight(28);

            int pct = rak.getPersentase();
            int threshold = section.getAlertThreshold();

            String slotStyle = "-fx-font-size: 11px; -fx-font-weight: bold; -fx-background-radius: 6; ";
            if (pct >= 100) {
                slotStyle += "-fx-background-color: #fff0ef; -fx-text-fill: #b74545; -fx-border-color: #f3d0cc; -fx-border-radius: 6;";
            } else if (pct >= threshold && threshold > 0) {
                slotStyle += "-fx-background-color: #fff7e8; -fx-text-fill: #9b681d; -fx-border-color: #f1ddb8; -fx-border-radius: 6;";
            } else if (pct > 0) {
                slotStyle += "-fx-background-color: #edf3ff; -fx-text-fill: #2458c6; -fx-border-color: #cfdbf3; -fx-border-radius: 6;";
            } else {
                slotStyle += "-fx-background-color: #f4f6fa; -fx-text-fill: #748198; -fx-border-color: #e3e8f0; -fx-border-radius: 6;";
            }

            btnSlot.setStyle(slotStyle);
            btnSlot.setTooltip(new Tooltip("Rak: " + rak.getKodeRak() + "\nTerisi: " + rak.getTerisi() + "/" + rak.getKapasitas() + " unit (" + pct + "%)\nKlik untuk detail kapasitas"));

            btnSlot.setOnAction(e -> showUpdateSlotDialog(rak));

            grid.add(btnSlot, col, row);
            col++;
            if (col >= cols) {
                col = 0;
                row++;
            }
        }

        // 3. INDEPENDENT STORAGE ALERT BOX PER SECTION
        VBox alertCard = new VBox(6);
        alertCard.setStyle("-fx-padding: 10; -fx-background-radius: 8; ");

        if (!section.getAlertActive()) {
            alertCard.setStyle(alertCard.getStyle() + "-fx-background-color: #f7f8fb; -fx-border-color: #e7ebf2; -fx-border-radius: 8;");
            Label lblInactive = new Label("⚪ Alert Nonaktif untuk section ini");
            lblInactive.setStyle("-fx-font-size: 11px; -fx-text-fill: #94a3b8;");
            alertCard.getChildren().add(lblInactive);
        } else {
            // Find if any rak triggers alert for this section
            Rak triggeredRak = null;
            int maxPct = -1;

            for (Rak r : section.getRaks()) {
                if (r.getPersentase() >= section.getAlertThreshold() && r.getPersentase() > maxPct) {
                    triggeredRak = r;
                    maxPct = r.getPersentase();
                }
            }

            if (triggeredRak != null) {
                boolean isDanger = maxPct >= 100;
                alertCard.setStyle(alertCard.getStyle() + (isDanger 
                    ? "-fx-background-color: #fff1ef; -fx-border-color: #f3d0cc; -fx-border-radius: 8;"
                    : "-fx-background-color: #fff8eb; -fx-border-color: #f1ddb8; -fx-border-radius: 8;"));

                HBox alertHeader = new HBox(6);
                alertHeader.setAlignment(Pos.CENTER_LEFT);
                Label icon = new Label(isDanger ? "DANGER" : "PERINGATAN");
                icon.setStyle(isDanger ? "-fx-text-fill: #b74545; -fx-font-weight: bold; -fx-font-size: 11px;"
                                       : "-fx-text-fill: #9b681d; -fx-font-weight: bold; -fx-font-size: 11px;");

                Label thresholdLabel = new Label("Batas: " + section.getAlertThreshold() + "%");
                thresholdLabel.setStyle("-fx-font-size: 10px; -fx-text-fill: #748198;");

                Region rSpring = new Region();
                HBox.setHgrow(rSpring, Priority.ALWAYS);

                alertHeader.getChildren().addAll(icon, rSpring, thresholdLabel);

                Label lblMsg = new Label(String.format("Rak %s terisi %d%% (Kondisi: %s)", triggeredRak.getKodeRak(), maxPct, section.getAlertType()));
                lblMsg.setStyle("-fx-font-size: 11px; -fx-font-weight: bold; -fx-text-fill: " + (isDanger ? "#b74545;" : "#9b681d;"));
                lblMsg.setWrapText(true);

                alertCard.getChildren().addAll(alertHeader, lblMsg);
            } else {
                alertCard.setStyle(alertCard.getStyle() + "-fx-background-color: #eef7f1; -fx-border-color: #d3e8d9; -fx-border-radius: 8;");
                HBox okHeader = new HBox(6);
                okHeader.setAlignment(Pos.CENTER_LEFT);
                Label icon = new Label("✔ NORMAL");
                icon.setStyle("-fx-text-fill: #357a50; -fx-font-weight: bold; -fx-font-size: 11px;");
                Label lblMsg = new Label("Seluruh slot terisi di bawah batas " + section.getAlertThreshold() + "%");
                lblMsg.setStyle("-fx-font-size: 11px; -fx-text-fill: #426b50;");

                okHeader.getChildren().addAll(icon, lblMsg);
                alertCard.getChildren().add(okHeader);
            }
        }

        card.getChildren().addAll(header, grid, alertCard);
        return card;
    }

    @FXML
    private void handleTambahSection() {
        Dialog<ButtonType> dlg = new Dialog<>();
        dlg.setTitle("Tambah Section Baru");
        dlg.setHeaderText("Buat area penyimpanan baru untuk dashboard");

        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(12);
        grid.setPadding(new Insets(20, 30, 10, 10));

        TextField tfKode = new TextField("D");
        tfKode.setPromptText("Kode Section (A, B, C...)");
        TextField tfNama = new TextField("Section D - Cold Storage");
        tfNama.setPromptText("Nama Section...");
        Spinner<Integer> spKapasitas = new Spinner<>(2, 24, 12);
        TextField tfKategori = new TextField("Semua");
        tfKategori.setPromptText("Kategori barang...");
        TextField tfZone = new TextField();
        tfZone.setPromptText("Contoh: ZONE-A");
        TextField tfStorageType = new TextField("GENERAL");
        tfStorageType.setPromptText("GENERAL, COLD, HAZARDOUS");
        Spinner<Integer> spPutawayPriority = new Spinner<>(0, 1000, 50);
        Spinner<Integer> spThreshold = new Spinner<>(10, 100, 80);
        ComboBox<String> cbAlertType = new ComboBox<>(FXCollections.observableArrayList(
            "Kapasitas Penuh (>= %)", "Stok Rendah (< Unit)", "Stok Habis (= 0)"
        ));
        cbAlertType.setValue("Kapasitas Penuh (>= %)");

        grid.add(new Label("Kode Section:"), 0, 0);
        grid.add(tfKode, 1, 0);
        grid.add(new Label("Nama Section:"), 0, 1);
        grid.add(tfNama, 1, 1);
        grid.add(new Label("Jumlah Slot:"), 0, 2);
        grid.add(spKapasitas, 1, 2);
        grid.add(new Label("Target Kategori:"), 0, 3);
        grid.add(tfKategori, 1, 3);
        grid.add(new Label("Zona:"), 0, 4);
        grid.add(tfZone, 1, 4);
        grid.add(new Label("Tipe Storage:"), 0, 5);
        grid.add(tfStorageType, 1, 5);
        grid.add(new Label("Prioritas Putaway:"), 0, 6);
        grid.add(spPutawayPriority, 1, 6);
        grid.add(new Label("Alert Threshold (%):"), 0, 7);
        grid.add(spThreshold, 1, 7);
        grid.add(new Label("Tipe Kondisi Alert:"), 0, 8);
        grid.add(cbAlertType, 1, 8);

        dlg.getDialogPane().setContent(grid);
        dlg.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);

        dlg.showAndWait().ifPresent(btn -> {
            if (btn == ButtonType.OK) {
                Task<Section> task = new Task<>() {
                    @Override
                    protected Section call() {
                        return sectionService.addSection(
                            tfKode.getText(), tfNama.getText(),
                            spKapasitas.getValue(), tfKategori.getText(),
                            spThreshold.getValue(), cbAlertType.getValue(), tfZone.getText(),
                            tfStorageType.getText(), spPutawayPriority.getValue()
                        );
                    }
                };
                task.setOnSucceeded(e -> {
                    showInfo("Berhasil", "Section baru berhasil ditambahkan!");
                    loadSectionsAsync();
                });
                Thread th = new Thread(task);
                th.setDaemon(true);
                th.start();
            }
        });
    }

    private void showSectionConfigDialog(Section section) {
        Dialog<ButtonType> dlg = new Dialog<>();
        dlg.setTitle("Konfigurasi Section & Alert");
        dlg.setHeaderText("Pengaturan independen untuk " + section.getNamaSection());

        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(12);
        grid.setPadding(new Insets(20, 30, 10, 10));

        TextField tfNama = new TextField(section.getNamaSection());
        Spinner<Integer> spKapasitas = new Spinner<>(section.getRaks().size(), 36, section.getKapasitas());
        TextField tfKategori = new TextField(section.getTargetKategori());
        TextField tfZone = new TextField(section.getZoneCode());
        TextField tfStorageType = new TextField(section.getStorageType());
        Spinner<Integer> spPutawayPriority = new Spinner<>(0, 1000, section.getPutawayPriority());
        Spinner<Integer> spThreshold = new Spinner<>(10, 100, section.getAlertThreshold());
        ComboBox<String> cbAlertType = new ComboBox<>(FXCollections.observableArrayList(
            "Kapasitas Penuh (>= %)", "Stok Rendah (< Unit)", "Stok Habis (= 0)"
        ));
        cbAlertType.setValue(section.getAlertType());
        CheckBox cbActive = new CheckBox("Aktifkan Storage Alert pada Section ini");
        cbActive.setSelected(section.getAlertActive());

        grid.add(new Label("Nama Section:"), 0, 0);
        grid.add(tfNama, 1, 0);
        grid.add(new Label("Jumlah Slot:"), 0, 1);
        grid.add(spKapasitas, 1, 1);
        grid.add(new Label("Target Kategori:"), 0, 2);
        grid.add(tfKategori, 1, 2);
        grid.add(new Label("Zona:"), 0, 3);
        grid.add(tfZone, 1, 3);
        grid.add(new Label("Tipe Storage:"), 0, 4);
        grid.add(tfStorageType, 1, 4);
        grid.add(new Label("Prioritas Putaway:"), 0, 5);
        grid.add(spPutawayPriority, 1, 5);
        grid.add(new Label("Alert Threshold (%):"), 0, 6);
        grid.add(spThreshold, 1, 6);
        grid.add(new Label("Tipe Kondisi Alert:"), 0, 7);
        grid.add(cbAlertType, 1, 7);
        grid.add(cbActive, 1, 8);

        dlg.getDialogPane().setContent(grid);
        dlg.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);

        dlg.showAndWait().ifPresent(btn -> {
            if (btn == ButtonType.OK) {
                Task<Section> task = new Task<>() {
                    @Override
                    protected Section call() {
                        return sectionService.updateSectionConfig(
                            section.getId(), tfNama.getText(),
                            spKapasitas.getValue(), tfKategori.getText(),
                            spThreshold.getValue(), cbAlertType.getValue(),
                            cbActive.isSelected(), tfZone.getText(), tfStorageType.getText(),
                            spPutawayPriority.getValue()
                        );
                    }
                };
                task.setOnSucceeded(e -> {
                    showInfo("Berhasil", "Konfigurasi section berhasil diperbarui!");
                    loadSectionsAsync();
                });
                Thread th = new Thread(task);
                th.setDaemon(true);
                th.start();
            }
        });
    }

    private void handleDeleteSection(Section section) {
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setTitle("Hapus Section");
        confirm.setHeaderText(null);
        confirm.setContentText("Apakah Anda yakin ingin menghapus " + section.getNamaSection() + "?");

        confirm.showAndWait().ifPresent(btn -> {
            if (btn == ButtonType.OK) {
                Task<Boolean> task = new Task<>() {
                    @Override
                    protected Boolean call() {
                        return sectionService.deleteSection(section.getId());
                    }
                };
                task.setOnSucceeded(e -> {
                    showInfo("Berhasil", "Section berhasil dihapus.");
                    loadSectionsAsync();
                });
                Thread th = new Thread(task);
                th.setDaemon(true);
                th.start();
            }
        });
    }

    private void showUpdateSlotDialog(Rak rak) {
        showInfo("Kapasitas " + rak.getKodeRak(),
                "Terisi: " + rak.getTerisi() + " dari " + rak.getKapasitas() + " unit ("
                        + rak.getPersentase() + "%). Perubahan occupancy dicatat melalui transaksi atau transfer stok.");
    }

    @FXML
    private void handleNavDashboard() {
        setActiveNav("dashboard");
        pageTitleLabel.setText("Dashboard");
        if (defaultDashboardView != null && mainScrollPane != null) {
            mainScrollPane.setContent(defaultDashboardView);
        }
        loadStatsAsync();
        loadSectionsAsync();
    }

    @FXML
    private void handleNavStorage() {
        setActiveNav("storage");
        pageTitleLabel.setText("Storage");
        loadCustomView("/fxml/storage.fxml", controller -> {
            if (controller instanceof StorageController storageController) {
                storageController.initUser(currentUser);
            }
        });
    }

    @FXML
    private void handleNavMovements() {
        setActiveNav("movements");
        pageTitleLabel.setText("Pergerakan");
        loadCustomView("/fxml/movements.fxml", controller -> {
            if (controller instanceof MovementsController mCtrl) {
                mCtrl.initUser(currentUser);
            }
        });
    }

    @FXML
    private void handleNavCustomer() {
        setActiveNav("customer");
        pageTitleLabel.setText("Pelanggan");
        loadCustomView("/fxml/customer.fxml", null);
    }

    @FXML
    private void handleNavReport() {
        setActiveNav("report");
        pageTitleLabel.setText("Laporan");
        loadCustomView("/fxml/report.fxml", null);
    }

    @FXML
    private void handleNavSetting() {
        setActiveNav("setting");
        pageTitleLabel.setText("Pengaturan");
        loadCustomView("/fxml/setting.fxml", controller -> {
            if (controller instanceof SettingController sCtrl) {
                sCtrl.initUser(currentUser);
                sCtrl.setOnUserUpdated(this::initUser);
            }
        });
    }

    private void updateAvatar(User user, String initials) {
        if (topbarAvatarLabel == null || topbarAvatarStack == null
                || topbarAvatarImg == null || topbarAvatarFallback == null) return;
        topbarAvatarLabel.setText(initials);
        topbarAvatarImg.setImage(null);
        topbarAvatarImg.setVisible(false);
        topbarAvatarImg.setManaged(false);
        topbarAvatarFallback.setVisible(true);
        topbarAvatarFallback.setManaged(true);
        String path = user.getProfilePicturePath();
        if (path != null && !path.isBlank() && Files.isRegularFile(Path.of(path))) {
            Image image = loadImage(Path.of(path));
            if (image != null && !image.isError()) {
                topbarAvatarImg.setImage(image);
                topbarAvatarImg.setVisible(true);
                topbarAvatarImg.setManaged(true);
                topbarAvatarFallback.setVisible(false);
                topbarAvatarFallback.setManaged(false);
            }
        }
        Tooltip.install(topbarAvatarStack, new Tooltip(user.getFullName() + " (" + user.getRole() + ")"));
    }

    private Image loadImage(Path path) {
        try {
            return new Image(path.toUri().toString(), false);
        } catch (Exception ex) {
            log.error("Gagal load avatar image: {}", path, ex);
            return null;
        }
    }

    @FXML
    private void handleNotification() {
        showInfo("Notifikasi", "Tidak ada notifikasi baru.");
    }

    @FXML
    private void handleSettingShortcut() {
        handleNavSetting();
    }

    @FXML
    private void handleLogout() {
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setTitle("Konfirmasi Logout");
        confirm.setHeaderText(null);
        confirm.setContentText("Apakah Anda yakin ingin logout?");
        confirm.showAndWait().ifPresent(response -> {
            if (response == ButtonType.OK) {
                goToLogin();
            }
        });
    }

    private interface ControllerInitializer {
        void init(Object controller);
    }

    private void loadCustomView(String fxmlPath, ControllerInitializer initializer) {
        try {
            FXMLLoader loader = fxmlLoader.getLoader(fxmlPath);
            Node node = loader.load();
            if (initializer != null) {
                initializer.init(loader.getController());
            }
            if (mainScrollPane != null) {
                mainScrollPane.setContent(node);
            }
        } catch (IOException e) {
            log.error("Gagal memuat view FXML: {}", fxmlPath, e);
            showInfo("Error", "Gagal memuat halaman: " + e.getMessage());
        }
    }

    private void setActiveNav(String active) {
        resetAllNav();
        switch (active) {
            case "dashboard" -> activateNav(navDashboard);
            case "storage"   -> activateNav(navStorage);
            case "movements" -> activateNav(navMovements);
            case "customer"  -> activateNav(navCustomer);
            case "report"    -> activateNav(navReport);
            case "setting"   -> activateNav(navSetting);
        }
    }

    private void resetAllNav() {
        HBox[] navItems = {navDashboard, navStorage, navMovements, navCustomer, navReport, navSetting};
        for (HBox nav : navItems) {
            if (nav != null) {
                nav.getStyleClass().setAll("nav-item");
                nav.getChildren().forEach(node -> {
                    if (node instanceof Label lbl) {
                        String sc = lbl.getStyleClass().stream().findFirst().orElse("");
                        if (sc.equals("nav-icon-active")) {
                            lbl.getStyleClass().setAll("nav-icon");
                        } else if (sc.equals("nav-label-active")) {
                            lbl.getStyleClass().setAll("nav-label");
                        }
                    }
                });
            }
        }
    }

    private void activateNav(HBox nav) {
        if (nav == null) return;
        nav.getStyleClass().setAll("nav-item-active");
        nav.getChildren().forEach(node -> {
            if (node instanceof Label lbl) {
                String sc = lbl.getStyleClass().stream().findFirst().orElse("");
                if (sc.equals("nav-icon")) {
                    lbl.getStyleClass().setAll("nav-icon-active");
                } else if (sc.equals("nav-label")) {
                    lbl.getStyleClass().setAll("nav-label-active");
                }
            }
        });
    }



    private void goToLogin() {
        try {
            FXMLLoader loader = fxmlLoader.getLoader("/fxml/login.fxml");
            Scene scene = new Scene(loader.load(), 1024, 680);
            Stage stage = (Stage) pageTitleLabel.getScene().getWindow();
            stage.setTitle("GudangKu | Sistem Manajemen Gudang");
            stage.hide();
            stage.setResizable(true);
            stage.setMinWidth(900);
            stage.setMinHeight(560);
            stage.setScene(scene);
            stage.setWidth(1024);
            stage.setHeight(680);
            stage.centerOnScreen();
            stage.show();
        } catch (IOException e) {
            log.error("Gagal kembali ke halaman login", e);
            showInfo("Error", "Gagal kembali ke halaman login: " + e.getMessage());
        }
    }

    private void showInfo(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
}
