package com.mycompany.tugas_akhir;

import com.mycompany.tugas_akhir.model.User;
import com.mycompany.tugas_akhir.service.UserService;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.shape.Circle;
import javafx.stage.FileChooser;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Locale;
import java.util.ResourceBundle;
import java.util.function.Consumer;

@Component
public class SettingController implements Initializable {

    private static final Logger log = LoggerFactory.getLogger(SettingController.class);
    private final UserService userService;

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

    @FXML private PasswordField pfOldPassword;
    @FXML private PasswordField pfNewPassword;
    @FXML private PasswordField pfConfirmPassword;

    public SettingController(UserService userService) {
        this.userService = userService;
    }

    @Override
    public void initialize(URL url, ResourceBundle rb) {
        if (profileImageView != null) {
            profileImageView.setClip(new Circle(48, 48, 48));
        }
        refreshPhotoView();
    }

    public void initUser(User user) {
        this.currentUser = user;
        if (profileImageView != null && profileImageView.getClip() == null) {
            profileImageView.setClip(new Circle(48, 48, 48));
        }
        if (user != null) {
            if (tfFullName != null) tfFullName.setText(user.getFullName() != null ? user.getFullName() : "");
            if (tfEmail != null) tfEmail.setText(user.getEmail() != null ? user.getEmail() : "");
            if (lblRole != null) lblRole.setText(user.getRole() != null ? user.getRole().toUpperCase() : "USER");
            refreshPhotoView();
        }
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
