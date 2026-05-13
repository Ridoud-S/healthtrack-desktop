package com.itc.healthtrack.controller;

import com.itc.healthtrack.model.Role;
import com.itc.healthtrack.service.AuthService;
import com.itc.healthtrack.session.UserSession;
import com.itc.healthtrack.util.ViewManager;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.stage.Stage;

import java.io.IOException;
import java.util.concurrent.CompletableFuture;

public class LoginController {

    @FXML private TextField txtEmail;
    @FXML private PasswordField txtPassword;
    @FXML private Button btnLogin;
    @FXML private Hyperlink lnkRegister;

    private final AuthService authService = new AuthService();

    @FXML
    private void onLoginPressed() {
        String email    = txtEmail.getText().trim();
        String password = txtPassword.getText();

        if (email.isEmpty() || password.isEmpty()) {
            showAlert(Alert.AlertType.WARNING, "Campos requeridos", "Ingresa tu correo y contraseña.");
            return;
        }

        btnLogin.setDisable(true);

        CompletableFuture.runAsync(() -> {
            try {
                authService.login(email, password);
                Role rol = UserSession.getInstance().getLoggedUser().role();
                Platform.runLater(() -> redirectByRole(rol));
            } catch (Exception e) {
                Platform.runLater(() -> {
                    showAlert(Alert.AlertType.ERROR, "Error de autenticación", e.getMessage());
                    btnLogin.setDisable(false);
                });
            }
        });
    }

    private void redirectByRole(Role rol) {
        switch (rol) {
            case PATIENT -> ViewManager.switchScene("fxml/patient_layout.fxml", "HealthTrack - Dashboard del Paciente", btnLogin);
            case DOCTOR  -> ViewManager.switchScene("fxml/doctor_layout.fxml",     "HealthTrack — Estación de Telemedicina", btnLogin);
            case ADMIN   -> ViewManager.switchScene("fxml/dashboard_admin.fxml",   "HealthTrack - Admin",    btnLogin);
        }
    }

    @FXML
    private void onRegisterPressed() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getClassLoader().getResource("fxml/register.fxml"));
            Parent root = loader.load();
            Stage stage = (Stage) btnLogin.getScene().getWindow();
            stage.setScene(new Scene(root));
        } catch (IOException e) {
            showAlert(Alert.AlertType.ERROR, "Error de navegación", "No se pudo cargar la pantalla de registro.");
        }
    }

    private void showAlert(Alert.AlertType type, String title, String message) {
        Alert alert = new Alert(type);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
}