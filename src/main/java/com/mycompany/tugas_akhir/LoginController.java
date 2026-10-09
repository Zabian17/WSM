package com.mycompany.tugas_akhir;

import com.mycompany.tugas_akhir.config.SpringFXMLLoader;
import com.mycompany.tugas_akhir.model.User;
import com.mycompany.tugas_akhir.service.UserService;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.stage.Stage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.net.URL;
import java.util.ResourceBundle;

@Component
public class LoginController implements Initializable {

    private static final Logger log = LoggerFactory.getLogger(LoginController.class);
    private final UserService userService;
    private final SpringFXMLLoader fxmlLoader;

    public LoginController(UserService userService, SpringFXMLLoader fxmlLoader) {
        this.userService = userService;
        this.fxmlLoader = fxmlLoader;
    }

    @FXML private VBox signUpPane;
    @FXML private VBox signInPane;
    @FXML private StackPane tabSignUp;
    @FXML private StackPane tabSignIn;
    @FXML private Label lblSignUp;
    @FXML private Label lblSignIn;

    @FXML private TextField tfFullName;
    @FXML private TextField tfEmail;
    @FXML private PasswordField pfPassword;

    @FXML private TextField tfLoginEmail;
    @FXML private PasswordField pfLoginPassword;
    @FXML private CheckBox cbRemember;

    @Override
    public void initialize(URL url, ResourceBundle rb) {
        showSignUp();
    }

    @FXML
    private void showSignUp() {
        signUpPane.setVisible(true);
        signUpPane.setManaged(true);
        signInPane.setVisible(false);
        signInPane.setManaged(false);

        tabSignUp.getStyleClass().setAll("tab-btn-active");
        tabSignIn.getStyleClass().setAll("tab-btn");
        lblSignUp.getStyleClass().setAll("tab-label-active");
        lblSignIn.getStyleClass().setAll("tab-label");
    }

    @FXML
    private void showSignIn() {
        signInPane.setVisible(true);
        signInPane.setManaged(true);
        signUpPane.setVisible(false);
        signUpPane.setManaged(false);

        tabSignIn.getStyleClass().setAll("tab-btn-active");
        tabSignUp.getStyleClass().setAll("tab-btn");
        lblSignIn.getStyleClass().setAll("tab-label-active");
        lblSignUp.getStyleClass().setAll("tab-label");
    }

    @FXML
    private void handleSignUp() {
        String name     = tfFullName.getText().trim();
        String email    = tfEmail.getText().trim();
        String password = pfPassword.getText();

        if (name.isEmpty() || email.isEmpty() || password.isEmpty()) {
            showAlert(Alert.AlertType.WARNING, "Peringatan", "Semua field harus diisi!");
            return;
        }

        if (password.length() < 6) {
            showAlert(Alert.AlertType.WARNING, "Peringatan", "Password minimal 6 karakter!");
            return;
        }

        if (!email.contains("@") || !email.contains(".")) {
            showAlert(Alert.AlertType.WARNING, "Peringatan", "Format email tidak valid!");
            return;
        }

        boolean success = userService.register(name, email, password);

        if (success) {
            showAlert(Alert.AlertType.INFORMATION, "Berhasil",
                    "Akun berhasil dibuat!\nSilakan sign in dengan email Anda.");
            tfFullName.clear();
            tfEmail.clear();
            pfPassword.clear();
            showSignIn();
        } else {
            showAlert(Alert.AlertType.ERROR, "Gagal",
                    "Email sudah terdaftar atau terjadi kesalahan.\nCoba gunakan email lain.");
        }
    }

    @FXML
    private void handleSignIn() {
        String email    = tfLoginEmail.getText().trim();
        String password = pfLoginPassword.getText();

        if (email.isEmpty() || password.isEmpty()) {
            showAlert(Alert.AlertType.WARNING, "Peringatan", "Email dan password harus diisi!");
            return;
        }

        User user = userService.login(email, password);

        if (user != null) {
            navigateToDashboard(user);
        } else {
            showAlert(Alert.AlertType.ERROR, "Login Gagal",
                    "Email atau password salah.\nSilakan coba lagi.");
            pfLoginPassword.clear();
        }
    }

    private void navigateToDashboard(User user) {
        try {
            FXMLLoader loader = fxmlLoader.getLoader("/fxml/dashboard.fxml");
            Scene scene = new Scene(loader.load(), 1200, 760);

            DashboardController dashCtrl = loader.getController();
            dashCtrl.initUser(user);
            Stage stage = (Stage) tfLoginEmail.getScene().getWindow();
            stage.setTitle("GudangKu - Dashboard");
            stage.hide();
            stage.setResizable(true);
            stage.setMinWidth(1050);
            stage.setMinHeight(680);
            stage.setScene(scene);
            stage.setWidth(1200);
            stage.setHeight(760);
            stage.centerOnScreen();
            stage.show();
        } catch (IOException e) {
            log.error("Gagal membuka dashboard: {}", e.getMessage(), e);
            showAlert(Alert.AlertType.ERROR, "Error", "Gagal membuka dashboard: " + e.getMessage());
        }
    }

    @FXML
    private void handleForgotPassword() {
        showAlert(Alert.AlertType.INFORMATION, "Lupa Password", "Fitur reset password akan segera tersedia.");
    }

    private void showAlert(Alert.AlertType type, String title, String message) {
        Alert alert = new Alert(type);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
}
