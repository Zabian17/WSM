package com.mycompany.tugas_akhir;

import com.mycompany.tugas_akhir.model.User;
import com.mycompany.tugas_akhir.model.ApprovalRoutingRule;
import com.mycompany.tugas_akhir.service.ApprovalRoutingService;
import com.mycompany.tugas_akhir.service.UserService;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.VBox;
import javafx.scene.shape.Circle;
import javafx.stage.FileChooser;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Locale;
import java.util.List;
import java.util.ResourceBundle;
import java.util.function.Consumer;

@Component
public class SettingController implements Initializable {

    private static final Logger log = LoggerFactory.getLogger(SettingController.class);
    private final UserService userService;
    private final ApprovalRoutingService approvalRoutingService;

    private User currentUser;
    private Path selectedPhoto;
    private Consumer<User> onUserUpdated;

    @FXML private TextField tfFullName;
    @FXML private TextField tfEmail;
    @FXML private Label lblRole;
    @FXML private ImageView profileImageView;
    @FXML private Label lblAvatarInitials;
    @FXML private Button btnSavePhoto;
    @FXML private Button btnRemovePhoto;
    @FXML private Label lblPhotoStatus;
    @FXML private VBox approvalAdminCard;
    @FXML private TableView<User> userDepartmentTable;
    @FXML private TableColumn<User, String> colDepartmentUser;
    @FXML private TableColumn<User, String> colDepartmentEmail;
    @FXML private TableColumn<User, String> colUserDepartment;
    @FXML private TextField tfUserDepartment;
    @FXML private TableView<ApprovalRoutingRule> approvalRuleTable;
    @FXML private TableColumn<ApprovalRoutingRule, String> colRequesterDepartment;
    @FXML private TableColumn<ApprovalRoutingRule, String> colApproverDepartment;
    @FXML private TextField tfRequesterDepartment;
    @FXML private TextField tfApproverDepartment;
    @FXML private Label lblApprovalAdminStatus;

    @FXML private PasswordField pfOldPassword;
    @FXML private PasswordField pfNewPassword;
    @FXML private PasswordField pfConfirmPassword;

    public SettingController(UserService userService, ApprovalRoutingService approvalRoutingService) {
        this.userService = userService;
        this.approvalRoutingService = approvalRoutingService;
    }

    @Override
    public void initialize(URL url, ResourceBundle rb) {
        if (profileImageView != null) {
            profileImageView.setClip(new Circle(48, 48, 48));
        }
        setupApprovalTables();
        refreshPhotoView();
    }

    public void initUser(User user) {
        this.currentUser = user;
        if (approvalAdminCard != null) {
            boolean admin = user != null && "admin".equalsIgnoreCase(user.getRole());
            approvalAdminCard.setVisible(admin);
            approvalAdminCard.setManaged(admin);
        }
        if (profileImageView != null && profileImageView.getClip() == null) {
            profileImageView.setClip(new Circle(48, 48, 48));
        }
        if (user != null) {
            if (tfFullName != null) tfFullName.setText(user.getFullName() != null ? user.getFullName() : "");
            if (tfEmail != null) tfEmail.setText(user.getEmail() != null ? user.getEmail() : "");
            if (lblRole != null) lblRole.setText(user.getRole() != null ? user.getRole().toUpperCase() : "USER");
            if ("admin".equalsIgnoreCase(user.getRole())) loadApprovalAdminTables();
            refreshPhotoView();
        }
    }

    private void setupApprovalTables() {
        if (userDepartmentTable != null) {
            colDepartmentUser.setCellValueFactory(cell ->
                    new SimpleStringProperty(cell.getValue().getFullName()));
            colDepartmentEmail.setCellValueFactory(cell ->
                    new SimpleStringProperty(cell.getValue().getEmail()));
            colUserDepartment.setCellValueFactory(cell ->
                    new SimpleStringProperty(cell.getValue().getDepartment() == null
                            ? "Belum ditetapkan" : cell.getValue().getDepartment()));
            userDepartmentTable.getSelectionModel().selectedItemProperty().addListener(
                    (observable, previous, selected) -> {
                        if (selected != null) tfUserDepartment.setText(
                                selected.getDepartment() == null ? "" : selected.getDepartment());
                    });
        }
        if (approvalRuleTable != null) {
            colRequesterDepartment.setCellValueFactory(cell ->
                    new SimpleStringProperty(cell.getValue().getRequesterDepartment()));
            colApproverDepartment.setCellValueFactory(cell ->
                    new SimpleStringProperty(cell.getValue().getApproverDepartment()));
            approvalRuleTable.getSelectionModel().selectedItemProperty().addListener(
                    (observable, previous, selected) -> {
                        if (selected != null) {
                            tfRequesterDepartment.setText(selected.getRequesterDepartment());
                            tfApproverDepartment.setText(selected.getApproverDepartment());
                        }
                    });
        }
    }

    private void loadApprovalAdminTables() {
        Task<List<User>> usersTask = new Task<>() {
            @Override
            protected List<User> call() {
                return userService.getUsersForAssignment();
            }
        };
        usersTask.setOnSucceeded(event ->
                userDepartmentTable.setItems(FXCollections.observableArrayList(usersTask.getValue())));
        usersTask.setOnFailed(event -> {
            log.error("Gagal memuat daftar departemen pengguna", usersTask.getException());
            lblApprovalAdminStatus.setText("Daftar pengguna gagal dimuat.");
        });
        Thread usersThread = new Thread(usersTask, "approval-user-departments");
        usersThread.setDaemon(true);
        usersThread.start();

        Task<List<ApprovalRoutingRule>> rulesTask = new Task<>() {
            @Override
            protected List<ApprovalRoutingRule> call() {
                return approvalRoutingService.getRules();
            }
        };
        rulesTask.setOnSucceeded(event ->
                approvalRuleTable.setItems(FXCollections.observableArrayList(rulesTask.getValue())));
        rulesTask.setOnFailed(event -> {
            log.error("Gagal memuat matriks approval", rulesTask.getException());
            lblApprovalAdminStatus.setText("Matriks approval gagal dimuat.");
        });
        Thread rulesThread = new Thread(rulesTask, "approval-routing-rules");
        rulesThread.setDaemon(true);
        rulesThread.start();
    }

    @FXML
    private void handleSaveUserDepartment() {
        User selected = userDepartmentTable.getSelectionModel().getSelectedItem();
        String department = tfUserDepartment.getText();
        if (currentUser == null || selected == null || department == null || department.isBlank()) {
            lblApprovalAdminStatus.setText("Pilih pengguna dan isi departemennya.");
            return;
        }
        Task<Boolean> task = new Task<>() {
            @Override
            protected Boolean call() {
                return approvalRoutingService.assignDepartment(currentUser.getId(), selected.getId(), department);
            }
        };
        task.setOnSucceeded(event -> {
            if (task.getValue()) {
                lblApprovalAdminStatus.setText("Departemen pengguna berhasil disimpan.");
                loadApprovalAdminTables();
            } else {
                lblApprovalAdminStatus.setText("Departemen gagal disimpan. Pastikan akun admin dan data valid.");
            }
        });
        task.setOnFailed(event -> {
            log.error("Gagal menyimpan departemen pengguna", task.getException());
            lblApprovalAdminStatus.setText("Gagal menyimpan departemen pengguna.");
        });
        Thread thread = new Thread(task, "approval-user-department-save");
        thread.setDaemon(true);
        thread.start();
    }

    @FXML
    private void handleSaveApprovalRule() {
        if (currentUser == null) return;
        String requester = tfRequesterDepartment.getText();
        String approver = tfApproverDepartment.getText();
        Task<Boolean> task = new Task<>() {
            @Override
            protected Boolean call() {
                return approvalRoutingService.saveRule(currentUser.getId(), requester, approver);
            }
        };
        task.setOnSucceeded(event -> {
            if (task.getValue()) {
                lblApprovalAdminStatus.setText("Aturan approval berhasil disimpan.");
                loadApprovalAdminTables();
            } else {
                lblApprovalAdminStatus.setText(
                        "Aturan tidak valid: departemen harus berbeda dan admin harus aktif.");
            }
        });
        task.setOnFailed(event -> {
            log.error("Gagal menyimpan aturan approval", task.getException());
            lblApprovalAdminStatus.setText("Gagal menyimpan aturan approval.");
        });
        Thread thread = new Thread(task, "approval-routing-save");
        thread.setDaemon(true);
        thread.start();
    }

    @FXML
    private void handleDeleteApprovalRule() {
        ApprovalRoutingRule selected = approvalRuleTable.getSelectionModel().getSelectedItem();
        if (currentUser == null || selected == null) {
            lblApprovalAdminStatus.setText("Pilih aturan approval yang akan dihapus.");
            return;
        }
        Task<Boolean> task = new Task<>() {
            @Override
            protected Boolean call() {
                return approvalRoutingService.deleteRule(currentUser.getId(), selected.getId());
            }
        };
        task.setOnSucceeded(event -> {
            if (task.getValue()) {
                lblApprovalAdminStatus.setText("Aturan approval berhasil dihapus.");
                loadApprovalAdminTables();
            } else {
                lblApprovalAdminStatus.setText("Aturan approval gagal dihapus.");
            }
        });
        task.setOnFailed(event -> {
            log.error("Gagal menghapus aturan approval", task.getException());
            lblApprovalAdminStatus.setText("Gagal menghapus aturan approval.");
        });
        Thread thread = new Thread(task, "approval-routing-delete");
        thread.setDaemon(true);
        thread.start();
    }

    public void setOnUserUpdated(Consumer<User> onUserUpdated) {
        this.onUserUpdated = onUserUpdated;
    }

    @FXML
    private void handleChoosePhoto() {
        FileChooser chooser = new FileChooser();
        chooser.setTitle("Pilih Profile Picture");
        chooser.getExtensionFilters().add(new FileChooser.ExtensionFilter(
                "Image Files (JPG, PNG, WEBP)", "*.jpg", "*.jpeg", "*.png", "*.webp"));

        java.io.File file = chooser.showOpenDialog(tfFullName != null ? tfFullName.getScene().getWindow() : null);
        if (file == null) return;

        Path candidate = file.toPath();
        try {
            String extension = extensionOf(candidate);
            if (!extension.matches("\\.(jpg|jpeg|png|webp)")) {
                throw new IllegalArgumentException("Format foto harus JPG, JPEG, PNG, atau WEBP.");
            }
            if (Files.size(candidate) > 2 * 1024 * 1024) {
                throw new IllegalArgumentException("Ukuran foto maksimal 2 MB.");
            }

            Image preview = loadImage(candidate);
            if (preview == null || preview.isError() || preview.getWidth() <= 0 || preview.getHeight() <= 0) {
                throw new IllegalArgumentException("File bukan gambar yang valid.");
            }

            selectedPhoto = candidate;
            if (profileImageView != null) {
                profileImageView.setImage(preview);
                profileImageView.setVisible(true);
            }
            if (lblAvatarInitials != null) lblAvatarInitials.setVisible(false);
            if (btnSavePhoto != null) btnSavePhoto.setDisable(false);
            if (lblPhotoStatus != null) lblPhotoStatus.setText("Preview foto baru. Tekan Simpan untuk menerapkan.");
        } catch (Exception ex) {
            showAlert(Alert.AlertType.WARNING, "Foto tidak valid", ex.getMessage());
        }
    }

    @FXML
    private void handleSavePhoto() {
        if (currentUser == null || selectedPhoto == null) return;
        setPhotoControlsDisabled(true);
        if (lblPhotoStatus != null) lblPhotoStatus.setText("Menyimpan profile picture...");
        User userToUpdate = currentUser;
        Path photoToSave = selectedPhoto;
        Task<User> saveTask = new Task<>() {
            @Override
            protected User call() throws Exception {
                return userService.updateProfilePicture(userToUpdate, photoToSave);
            }
        };
        saveTask.setOnSucceeded(event -> {
            currentUser = saveTask.getValue();
            selectedPhoto = null;
            refreshPhotoView();
            notifyUserUpdated();
            showAlert(Alert.AlertType.INFORMATION, "Sukses", "Profile picture berhasil diperbarui.");
        });
        saveTask.setOnFailed(event -> {
            Throwable error = saveTask.getException();
            log.error("Gagal menyimpan profile picture", error);
            setPhotoControlsDisabled(false);
            if (lblPhotoStatus != null) lblPhotoStatus.setText("Upload gagal. Foto lama tetap digunakan.");
            showAlert(Alert.AlertType.ERROR, "Upload gagal", photoErrorMessage(error));
        });
        Thread saveThread = new Thread(saveTask, "profile-picture-save");
        saveThread.setDaemon(true);
        saveThread.start();
    }

    @FXML
    private void handleRemovePhoto() {
        if (currentUser == null || currentUser.getProfilePicturePath() == null) return;
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setTitle("Hapus Profile Picture");
        confirm.setHeaderText(null);
        confirm.setContentText("Hapus profile picture dan kembali ke avatar default?");
        confirm.showAndWait().ifPresent(response -> {
            if (response != ButtonType.OK) return;
            setPhotoControlsDisabled(true);
            if (lblPhotoStatus != null) lblPhotoStatus.setText("Menghapus profile picture...");
            User userToUpdate = currentUser;
            Task<User> removeTask = new Task<>() {
                @Override
                protected User call() throws Exception {
                    return userService.removeProfilePicture(userToUpdate);
                }
            };
            removeTask.setOnSucceeded(event -> {
                currentUser = removeTask.getValue();
                selectedPhoto = null;
                refreshPhotoView();
                notifyUserUpdated();
                showAlert(Alert.AlertType.INFORMATION, "Sukses", "Profile picture berhasil dihapus.");
            });
            removeTask.setOnFailed(event -> {
                log.error("Gagal menghapus profile picture", removeTask.getException());
                setPhotoControlsDisabled(false);
                if (lblPhotoStatus != null) lblPhotoStatus.setText("Penghapusan gagal. Foto lama tetap digunakan.");
                showAlert(Alert.AlertType.ERROR, "Penghapusan gagal", "Profile picture gagal dihapus. Coba lagi.");
            });
            Thread removeThread = new Thread(removeTask, "profile-picture-remove");
            removeThread.setDaemon(true);
            removeThread.start();
        });
    }

    @FXML
    private void handleCancelPhoto() {
        selectedPhoto = null;
        refreshPhotoView();
    }

    private void refreshPhotoView() {
        if (currentUser == null || profileImageView == null) return;
        String path = currentUser.getProfilePicturePath();
        boolean hasPhoto = path != null && !path.isBlank() && Files.isRegularFile(Path.of(path));
        Image img = hasPhoto ? loadImage(Path.of(path)) : null;

        if (img != null && !img.isError()) {
            profileImageView.setImage(img);
            profileImageView.setVisible(true);
            if (lblAvatarInitials != null) lblAvatarInitials.setVisible(false);
            if (btnRemovePhoto != null) {
                btnRemovePhoto.setVisible(true);
                btnRemovePhoto.setManaged(true);
            }
        } else {
            profileImageView.setImage(null);
            profileImageView.setVisible(false);
            if (lblAvatarInitials != null) {
                lblAvatarInitials.setText(initialsFor(currentUser));
                lblAvatarInitials.setVisible(true);
            }
            if (btnRemovePhoto != null) {
                btnRemovePhoto.setVisible(false);
                btnRemovePhoto.setManaged(false);
            }
        }

        if (btnSavePhoto != null) btnSavePhoto.setDisable(selectedPhoto == null);
        if (lblPhotoStatus != null) lblPhotoStatus.setText(hasPhoto ? "Foto tersimpan." : "Belum ada profile picture.");
    }

    private void setPhotoControlsDisabled(boolean disabled) {
        if (btnSavePhoto != null) btnSavePhoto.setDisable(disabled || selectedPhoto == null);
        if (btnRemovePhoto != null) btnRemovePhoto.setDisable(disabled);
    }

    private void notifyUserUpdated() {
        if (onUserUpdated != null) onUserUpdated.accept(currentUser);
    }

    private String initialsFor(User user) {
        return user != null && user.getFullName() != null && !user.getFullName().isBlank()
                ? user.getFullName().substring(0, 1).toUpperCase(Locale.ROOT) : "U";
    }

    private String extensionOf(Path file) {
        String name = file.getFileName().toString().toLowerCase(Locale.ROOT);
        int dot = name.lastIndexOf('.');
        return dot >= 0 ? name.substring(dot) : "";
    }

    private String photoErrorMessage(Throwable error) {
        return error != null && error.getMessage() != null
                ? error.getMessage() : "Foto gagal disimpan. Coba lagi.";
    }

    private Image loadImage(Path path) {
        try {
            return new Image(path.toUri().toString(), false);
        } catch (Exception ex) {
            log.error("Gagal load image: {}", path, ex);
            return null;
        }
    }

    @FXML
    private void handleSaveProfile() {
        if (currentUser == null) {
            showAlert(Alert.AlertType.WARNING, "Peringatan", "Sesi pengguna tidak ditemukan.");
            return;
        }

        String name = tfFullName.getText().trim();
        String email = tfEmail.getText().trim();

        if (name.isEmpty() || email.isEmpty()) {
            showAlert(Alert.AlertType.WARNING, "Peringatan", "Nama dan Email tidak boleh kosong!");
            return;
        }
        if (!email.matches("^[^\\s@]+@[^\\s@]+\\.[^\\s@]+$")) {
            showAlert(Alert.AlertType.WARNING, "Peringatan", "Format email tidak valid.");
            return;
        }

        Integer userId = currentUser.getId();
        Task<User> task = new Task<>() {
            @Override
            protected User call() {
                return userService.updateProfile(userId, name, email);
            }
        };
        task.setOnSucceeded(event -> {
            if (task.getValue() == null) {
                showAlert(Alert.AlertType.ERROR, "Gagal", "Email sudah digunakan atau profil tidak valid.");
                return;
            }
            currentUser = task.getValue();
            notifyUserUpdated();
            showAlert(Alert.AlertType.INFORMATION, "Sukses", "Profil berhasil diperbarui.");
        });
        task.setOnFailed(event -> {
            log.error("Gagal memperbarui profil", task.getException());
            showAlert(Alert.AlertType.ERROR, "Gagal", "Profil gagal disimpan.");
        });
        Thread thread = new Thread(task, "profile-update");
        thread.setDaemon(true);
        thread.start();
    }

    @FXML
    private void handleChangePassword() {
        String oldPass = pfOldPassword.getText();
        String newPass = pfNewPassword.getText();
        String confirmPass = pfConfirmPassword.getText();

        if (oldPass.isEmpty() || newPass.isEmpty() || confirmPass.isEmpty()) {
            showAlert(Alert.AlertType.WARNING, "Peringatan", "Semua kolom password harus diisi!");
            return;
        }

        if (newPass.length() < 6) {
            showAlert(Alert.AlertType.WARNING, "Peringatan", "Password baru minimal 6 karakter!");
            return;
        }

        if (!newPass.equals(confirmPass)) {
            showAlert(Alert.AlertType.WARNING, "Peringatan", "Konfirmasi password baru tidak cocok!");
            return;
        }

        if (currentUser == null || currentUser.getId() == null) {
            showAlert(Alert.AlertType.WARNING, "Peringatan", "Sesi pengguna tidak ditemukan.");
            return;
        }

        Integer userId = currentUser.getId();
        Task<Boolean> task = new Task<>() {
            @Override
            protected Boolean call() {
                return userService.changePassword(userId, oldPass, newPass);
            }
        };
        task.setOnSucceeded(event -> {
            if (!task.getValue()) {
                showAlert(Alert.AlertType.ERROR, "Gagal", "Password saat ini salah atau perubahan tidak valid.");
                return;
            }
            pfOldPassword.clear();
            pfNewPassword.clear();
            pfConfirmPassword.clear();
            showAlert(Alert.AlertType.INFORMATION, "Sukses", "Password berhasil diubah.");
        });
        task.setOnFailed(event -> {
            log.error("Gagal mengubah password", task.getException());
            showAlert(Alert.AlertType.ERROR, "Gagal", "Password gagal diubah.");
        });
        Thread thread = new Thread(task, "password-update");
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
