package com.itc.healthtrack.controller;

import com.itc.healthtrack.model.Role;
import com.itc.healthtrack.service.AuthService;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.*;

import java.util.concurrent.CompletableFuture;

public class RegisterController {

    @FXML private TextField txtNombre;
    @FXML private TextField txtEmail;
    @FXML private PasswordField txtPassword;
    @FXML private PasswordField txtConfirmPassword;
    @FXML private ComboBox<Role> cmbRol;
    @FXML private Button btnRegister;

    private final AuthService authService = new AuthService();

    @FXML
    public void initialize() {
        cmbRol.getItems().addAll(Role.values());
        cmbRol.setValue(Role.PATIENT);
    }

    @FXML
    private void onRegisterPressed() {
        String nombre           = txtNombre.getText().trim();
        String email            = txtEmail.getText().trim();
        String password         = txtPassword.getText();
        String confirmPassword  = txtConfirmPassword.getText();
        Role role = cmbRol.getValue();

        if (nombre.isEmpty() || email.isEmpty() || password.isEmpty()) {
            showAlert(Alert.AlertType.WARNING, "Campos requeridos", "Todos los campos son obligatorios.");
            return;
        }

        if (!password.equals(confirmPassword)) {
            showAlert(Alert.AlertType.WARNING, "Contraseñas distintas", "Las contraseñas no coinciden.");
            return;
        }

        if (password.length() < 6) {
            showAlert(Alert.AlertType.WARNING, "Contraseña débil", "La contraseña debe tener al menos 6 caracteres.");
            return;
        }

        btnRegister.setDisable(true);

        CompletableFuture.runAsync(() -> {
            try {
                authService.register(email, password, nombre, role);
                Platform.runLater(() -> {
                    showAlert(Alert.AlertType.INFORMATION, "Registro exitoso",
                            "Usuario creado correctamente. Ya puedes iniciar sesión.");
                    btnRegister.setDisable(false);
                });
            } catch (Exception e) {
                Platform.runLater(() -> {
                    showAlert(Alert.AlertType.ERROR, "Error al registrar", e.getMessage());
                    btnRegister.setDisable(false);
                });
            }
        });
    }

    private void showAlert(Alert.AlertType type, String title, String message) {
        Alert alert = new Alert(type);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
}