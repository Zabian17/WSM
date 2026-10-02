package com.mycompany.tugas_akhir;

<<<<<<< HEAD
import com.mycompany.tugas_akhir.config.SpringFXMLLoader;
import com.mycompany.tugas_akhir.model.Rak;
import com.mycompany.tugas_akhir.model.Section;
import com.mycompany.tugas_akhir.model.User;
import com.mycompany.tugas_akhir.service.SectionService;
import com.mycompany.tugas_akhir.service.TransaksiService;
import javafx.collections.FXCollections;
import javafx.concurrent.Task;
=======
import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.ResourceBundle;
>>>>>>> origin/main
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
import javafx.scene.shape.Circle;
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

<<<<<<< HEAD
    private static final Logger log = LoggerFactory.getLogger(DashboardController.class);
    private final TransaksiService transaksiService;
    private final SectionService sectionService;
    private final SpringFXMLLoader fxmlLoader;

    private User currentUser;
    private Node defaultDashboardView;

    @FXML private ScrollPane mainScrollPane;
    @FXML private FlowPane sectionsFlowPane;
=======
    private UserDAO.User currentUser;
    private Stage primaryStage;
>>>>>>> origin/main

    @FXML private HBox navDashboard;
    @FXML private HBox navStorage;
    @FXML private HBox navMovements;
    @FXML private HBox navCustomer;
    @FXML private HBox navReport;
    @FXML private HBox navSetting;

    @FXML private Label pageTitleLabel;
    @FXML private TextField searchField;
    @FXML private Label topbarAvatarLabel;
    @FXML private ImageView topbarAvatarImg;
    @FXML private StackPane topbarAvatarFallback;

    @FXML private Label lblBarangMasuk;
    @FXML private Label lblBarangKeluar;

<<<<<<< HEAD
    public DashboardController(TransaksiService transaksiService,
                               SectionService sectionService,
                               SpringFXMLLoader fxmlLoader) {
        this.transaksiService = transaksiService;
        this.sectionService = sectionService;
        this.fxmlLoader = fxmlLoader;
    }
=======
    @FXML private ScrollPane mainScrollPane;
    
    private String currentPage = "dashboard";
>>>>>>> origin/main

    @Override
    public void initialize(URL url, ResourceBundle rb) {
        setActiveNav("dashboard");

<<<<<<< HEAD
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
                    + "-fx-border-color: #e2e8f0; -fx-border-radius: 12; -fx-padding: 14; -fx-pref-width: 320px;");

        // 1. HEADER ROW
        HBox header = new HBox(8);
        header.setAlignment(Pos.CENTER_LEFT);

        VBox titleBox = new VBox(2);
        Label lblTitle = new Label(section.getNamaSection());
        lblTitle.setStyle("-fx-font-weight: bold; -fx-font-size: 14px; -fx-text-fill: #1e293b;");

        int activeSlots = (int) section.getRaks().stream().filter(r -> r.getTerisi() > 0).count();
        Label lblSub = new Label(activeSlots + "/" + section.getRaks().size() + " terisi • Target: " + section.getTargetKategori());
        lblSub.setStyle("-fx-font-size: 11px; -fx-text-fill: #64748b;");
        titleBox.getChildren().addAll(lblTitle, lblSub);

        Region spring = new Region();
        HBox.setHgrow(spring, Priority.ALWAYS);

        Button btnConfig = new Button("⚙");
        btnConfig.getStyleClass().add("btn-outline");
        btnConfig.setTooltip(new Tooltip("Konfigurasi Section & Alert"));
        btnConfig.setOnAction(e -> showSectionConfigDialog(section));

        Button btnDelete = new Button("🗑");
        btnDelete.setStyle("-fx-background-color: #fee2e2; -fx-text-fill: #ef4444; -fx-background-radius: 6; -fx-padding: 4 8;");
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
                slotStyle += "-fx-background-color: #fee2e2; -fx-text-fill: #991b1b; -fx-border-color: #fca5a5; -fx-border-radius: 6;";
            } else if (pct >= threshold && threshold > 0) {
                slotStyle += "-fx-background-color: #fef3c7; -fx-text-fill: #92400e; -fx-border-color: #fcd34d; -fx-border-radius: 6;";
            } else if (pct > 0) {
                slotStyle += "-fx-background-color: #dbeafe; -fx-text-fill: #1e40af; -fx-border-color: #93c5fd; -fx-border-radius: 6;";
            } else {
                slotStyle += "-fx-background-color: #f1f5f9; -fx-text-fill: #64748b; -fx-border-color: #e2e8f0; -fx-border-radius: 6;";
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
            alertCard.setStyle(alertCard.getStyle() + "-fx-background-color: #f8fafc; -fx-border-color: #e2e8f0; -fx-border-radius: 8;");
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
                    ? "-fx-background-color: #fff1f2; -fx-border-color: #fecdd3; -fx-border-radius: 8;" 
                    : "-fx-background-color: #fffbeb; -fx-border-color: #fef3c7; -fx-border-radius: 8;"));

                HBox alertHeader = new HBox(6);
                alertHeader.setAlignment(Pos.CENTER_LEFT);
                Label icon = new Label(isDanger ? "✖ DANGER" : "⚠ WARN");
                icon.setStyle(isDanger ? "-fx-text-fill: #e11d48; -fx-font-weight: bold; -fx-font-size: 11px;" 
                                       : "-fx-text-fill: #d97706; -fx-font-weight: bold; -fx-font-size: 11px;");

                Label thresholdLabel = new Label("Batas: " + section.getAlertThreshold() + "%");
                thresholdLabel.setStyle("-fx-font-size: 10px; -fx-text-fill: #64748b;");

                Region rSpring = new Region();
                HBox.setHgrow(rSpring, Priority.ALWAYS);

                alertHeader.getChildren().addAll(icon, rSpring, thresholdLabel);

                Label lblMsg = new Label(String.format("Rak %s terisi %d%% (Kondisi: %s)", triggeredRak.getKodeRak(), maxPct, section.getAlertType()));
                lblMsg.setStyle("-fx-font-size: 11px; -fx-font-weight: bold; -fx-text-fill: " + (isDanger ? "#9f1239;" : "#92400e;"));
                lblMsg.setWrapText(true);

                alertCard.getChildren().addAll(alertHeader, lblMsg);
            } else {
                alertCard.setStyle(alertCard.getStyle() + "-fx-background-color: #f0fdf4; -fx-border-color: #bbf7d0; -fx-border-radius: 8;");
                HBox okHeader = new HBox(6);
                okHeader.setAlignment(Pos.CENTER_LEFT);
                Label icon = new Label("✔ NORMAL");
                icon.setStyle("-fx-text-fill: #16a34a; -fx-font-weight: bold; -fx-font-size: 11px;");
                Label lblMsg = new Label("Seluruh slot terisi di bawah batas " + section.getAlertThreshold() + "%");
                lblMsg.setStyle("-fx-font-size: 11px; -fx-text-fill: #166534;");

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
        grid.add(new Label("Alert Threshold (%):"), 0, 4);
        grid.add(spThreshold, 1, 4);
        grid.add(new Label("Tipe Kondisi Alert:"), 0, 5);
        grid.add(cbAlertType, 1, 5);

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
                            spThreshold.getValue(), cbAlertType.getValue()
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
        grid.add(new Label("Alert Threshold (%):"), 0, 3);
        grid.add(spThreshold, 1, 3);
        grid.add(new Label("Tipe Kondisi Alert:"), 0, 4);
        grid.add(cbAlertType, 1, 4);
        grid.add(cbActive, 1, 5);

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
                            cbActive.isSelected()
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
=======
        lblBarangMasuk.setText("1,234");
        lblBarangKeluar.setText("900");
        
        loadDashboardPage();
        
        if (pageTitleLabel != null && pageTitleLabel.getScene() != null) {
            primaryStage = (Stage) pageTitleLabel.getScene().getWindow();
        }
    }

    public void initUser(UserDAO.User user) {
        this.currentUser = user;
        if (user != null) {
            updateTopbarAvatar(user);
        }
        
        if (primaryStage == null && pageTitleLabel != null && pageTitleLabel.getScene() != null) {
            primaryStage = (Stage) pageTitleLabel.getScene().getWindow();
        }
    }

    public void setCurrentUser(UserDAO.User user) {
        initUser(user);
    }


    public void setPrimaryStage(Stage stage) {
        this.primaryStage = stage;
    }


    public void updateTopbarAvatar(UserDAO.User user) {
        if (user == null) return;

        String picPath = user.profilePicturePath;
        

        if (picPath != null && !picPath.isBlank()) {
            File f = new File(picPath);
            if (f.exists()) {
                try {
                    Image img = new Image(f.toURI().toString(), 34, 34, true, true);
                    topbarAvatarImg.setImage(img);
                    
                    Circle clip = new Circle(17, 17, 17);
                    topbarAvatarImg.setClip(clip);
                    
                    topbarAvatarImg.setVisible(true);
                    topbarAvatarImg.setManaged(true);
                    topbarAvatarFallback.setVisible(false);
                    topbarAvatarFallback.setManaged(false);
                    return;
                } catch (Exception e) {
                    System.err.println("[DashboardController] Gagal load avatar: " + e.getMessage());
                }
            }
        }
        
        topbarAvatarLabel.setText(user.getInitial());
        topbarAvatarImg.setVisible(false);
        topbarAvatarImg.setManaged(false);
        topbarAvatarFallback.setVisible(true);
        topbarAvatarFallback.setManaged(true);
    }


    public String getCurrentPage() {
        return currentPage;
>>>>>>> origin/main
    }

    @FXML
    private void handleNavDashboard() {
        setActiveNav("dashboard");
        pageTitleLabel.setText("Dashboard");
<<<<<<< HEAD
        if (defaultDashboardView != null && mainScrollPane != null) {
            mainScrollPane.setContent(defaultDashboardView);
        }
        loadStatsAsync();
        loadSectionsAsync();
=======
        currentPage = "dashboard";
        loadDashboardPage();
>>>>>>> origin/main
    }

    @FXML
    private void handleNavStorage() {
        setActiveNav("storage");
        pageTitleLabel.setText("Storage");
<<<<<<< HEAD
        loadCustomView("/fxml/storage.fxml", null);
=======
        currentPage = "storage";
        loadStoragePage();
>>>>>>> origin/main
    }

    @FXML
    private void handleNavMovements() {
        setActiveNav("movements");
        pageTitleLabel.setText("Movements");
<<<<<<< HEAD
        loadCustomView("/fxml/movements.fxml", controller -> {
            if (controller instanceof MovementsController mCtrl) {
                mCtrl.initUser(currentUser);
            }
        });
=======
        currentPage = "movements";
        loadMovementsPage();
>>>>>>> origin/main
    }

    @FXML
    private void handleNavCustomer() {
        setActiveNav("customer");
        pageTitleLabel.setText("Customer");
<<<<<<< HEAD
        loadCustomView("/fxml/customer.fxml", null);
=======
        currentPage = "customer";
        loadCustomerPage();
>>>>>>> origin/main
    }

    @FXML
    private void handleNavReport() {
        setActiveNav("report");
        pageTitleLabel.setText("Report");
<<<<<<< HEAD
        loadCustomView("/fxml/report.fxml", null);
=======
        currentPage = "report";
        loadReportPage();
>>>>>>> origin/main
    }

    @FXML
    private void handleNavSetting() {
        setActiveNav("setting");
        pageTitleLabel.setText("Setting");
<<<<<<< HEAD
        loadCustomView("/fxml/setting.fxml", controller -> {
            if (controller instanceof SettingController sCtrl) {
                sCtrl.initUser(currentUser);
                sCtrl.setOnUserUpdated(this::initUser);
            }
        });
    }

    private void updateAvatar(User user, String initials) {
        if (avatarBtn == null) return;
        avatarBtn.setText(initials);
        avatarBtn.setGraphic(null);
        String path = user.getProfilePicturePath();
        if (path != null && !path.isBlank() && Files.isRegularFile(Path.of(path))) {
            Image image = loadImage(Path.of(path));
            if (image != null && !image.isError()) {
                ImageView imageView = new ImageView(image);
                imageView.setFitWidth(34);
                imageView.setFitHeight(34);
                imageView.setPreserveRatio(true);
                imageView.setClip(new Circle(17, 17, 17));
                avatarBtn.setGraphic(imageView);
                avatarBtn.setText(null);
            }
        }
        avatarBtn.setTooltip(new Tooltip(user.getFullName() + " (" + user.getRole() + ")"));
    }

    private Image loadImage(Path path) {
        try {
            return new Image(path.toUri().toString(), false);
        } catch (Exception ex) {
            log.error("Gagal load avatar image: {}", path, ex);
            return null;
        }
=======
        currentPage = "setting";
        loadSettingPage();
>>>>>>> origin/main
    }

    @FXML
    private void handleNotification() {
        showInfo("Notifikasi", "Tidak ada notifikasi baru.");
    }

    @FXML
    private void handleSettingShortcut() {
<<<<<<< HEAD
        handleNavSetting();
=======
        setActiveNav("setting");
        pageTitleLabel.setText("Setting");
        currentPage = "setting";
        loadSettingPage();
>>>>>>> origin/main
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

<<<<<<< HEAD
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

=======
>>>>>>> origin/main
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
<<<<<<< HEAD
=======
                
>>>>>>> origin/main
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
            Scene scene = new Scene(loader.load(), 950, 600);
            Stage stage = (Stage) pageTitleLabel.getScene().getWindow();
            stage.setTitle("GudangKu - Login");
            stage.hide();
            stage.setResizable(false);
            stage.setScene(scene);
            stage.setWidth(950);
            stage.setHeight(600);
            stage.centerOnScreen();
            stage.show();
        } catch (IOException e) {
            log.error("Gagal kembali ke halaman login", e);
            showInfo("Error", "Gagal kembali ke halaman login: " + e.getMessage());
        }
    }

<<<<<<< HEAD
=======

    private void loadStoragePage() {
        try {
            FXMLLoader loader = new FXMLLoader(
                    getClass().getResource("/fxml/storage.fxml")
            );
            VBox storagePage = loader.load();
            mainScrollPane.setContent(storagePage);
        } catch (IOException e) {
            e.printStackTrace();
            showInfo("Error", "Gagal load halaman Storage: " + e.getMessage());
        }
    }

    private void loadMovementsPage() {
        try {
            FXMLLoader loader = new FXMLLoader(
                    getClass().getResource("/fxml/movements.fxml")
            );
            VBox movementsPage = loader.load();
            
            MovementsController controller = loader.getController();
            if (controller != null && currentUser != null) {
                controller.initUser(currentUser);
            }
            
            mainScrollPane.setContent(movementsPage);
        } catch (IOException e) {
            e.printStackTrace();
            showInfo("Error", "Gagal load halaman Movements: " + e.getMessage());
        }
    }

    private void loadCustomerPage() {
        try {
            FXMLLoader loader = new FXMLLoader(
                    getClass().getResource("/fxml/customer.fxml")
            );
            VBox customerPage = loader.load();

            CustomerController controller = loader.getController();
            if (controller != null && currentUser != null) {
                controller.initUser(currentUser);
            }

            mainScrollPane.setContent(customerPage);
        } catch (IOException e) {
            e.printStackTrace();
            showInfo("Error", "Gagal load halaman Customer: " + e.getMessage());
        }
    }

    private void loadReportPage() {
        try {
            FXMLLoader loader = new FXMLLoader(
                    getClass().getResource("/fxml/report.fxml")
            );
            VBox reportPage = loader.load();

            ReportController controller = loader.getController();
            if (controller != null && currentUser != null) {
                controller.initUser(currentUser);
            }

            mainScrollPane.setContent(reportPage);
        } catch (IOException e) {
            e.printStackTrace();
            showInfo("Error", "Gagal load halaman Report: " + e.getMessage());
        }
    }

    private void loadSettingPage() {
        try {
            FXMLLoader loader = new FXMLLoader(
                    getClass().getResource("/fxml/setting.fxml")
            );
            VBox settingPage = loader.load();

            SettingController controller = loader.getController();
            if (controller != null && currentUser != null) {
                controller.initUser(currentUser);
                controller.setDashboardController(this);
            }

            mainScrollPane.setContent(settingPage);
        } catch (IOException e) {
            e.printStackTrace();
            showInfo("Error", "Gagal load halaman Setting: " + e.getMessage());
        }
    }

    private void loadDashboardPage() {
        try {
            FXMLLoader loader = new FXMLLoader(
                    getClass().getResource("/fxml/dashboard-content.fxml")
            );
            VBox dashboardContent = loader.load();
            mainScrollPane.setContent(dashboardContent);
        } catch (IOException e) {
            e.printStackTrace();
            showInfo("Error", "Gagal load halaman Dashboard: " + e.getMessage());
        }
    }
        


>>>>>>> origin/main
    private void showInfo(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
}
