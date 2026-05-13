package com.itc.healthtrack.controller;

import com.google.cloud.firestore.ListenerRegistration;
import com.itc.healthtrack.model.Role;
import com.itc.healthtrack.model.User;
import com.itc.healthtrack.repository.UserRepository;
import com.itc.healthtrack.repository.UserRepositoryImpl;
import com.itc.healthtrack.session.UserSession;
import com.itc.healthtrack.util.ViewManager;
import javafx.application.Platform;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import javafx.util.StringConverter;

import java.net.URL;
import java.util.List;
import java.util.ResourceBundle;
import java.util.stream.Collectors;

/**
 * Controlador del Panel de Administración.
 * Permite al Admin visualizar todos los pacientes y asignarles un médico.
 */
public class AdminDashboardController implements Initializable {

    // ── FXML Bindings ──────────────────────────────────────────────
    @FXML private Label lblWelcome;
    @FXML private Label lblStatusBar;
    @FXML private Button btnLogout;
    @FXML private Button btnAsignar;

    @FXML private TableView<User> tablaPacientes;
    @FXML private TableColumn<User, String> colNombre;
    @FXML private TableColumn<User, String> colEmail;
    @FXML private TableColumn<User, String> colMedicoAsignado;

    @FXML private ComboBox<User> cbDoctores;

    // ── Estado interno ─────────────────────────────────────────────
    private final ObservableList<User> pacientesList = FXCollections.observableArrayList();
    private final ObservableList<User> doctoresList = FXCollections.observableArrayList();

    private final UserRepository userRepository = new UserRepositoryImpl();
    private ListenerRegistration listenerRegistration;

    /** Cache plana de TODOS los usuarios (para resolver nombres). */
    private List<User> todosLosUsuarios = List.of();

    // ── Inicialización ─────────────────────────────────────────────
    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        User admin = UserSession.getInstance().getLoggedUser();
        lblWelcome.setText("Panel de Administración — " + admin.nombre());

        configurarTablaPacientes();
        configurarComboBoxDoctores();
        cargarUsuarios();
    }

    // ══════════════════════════════════════════════════════════════
    //  CONFIGURACIÓN DE COMPONENTES
    // ══════════════════════════════════════════════════════════════

    private void configurarTablaPacientes() {
        colNombre.setCellValueFactory(cell ->
                new SimpleStringProperty(cell.getValue().nombre()));

        colEmail.setCellValueFactory(cell ->
                new SimpleStringProperty(cell.getValue().email()));

        colMedicoAsignado.setCellValueFactory(cell -> {
            String medicoUid = cell.getValue().medicoAsignadoUid();
            String nombreMedico = resolverNombreMedico(medicoUid);
            return new SimpleStringProperty(nombreMedico);
        });

        tablaPacientes.setItems(pacientesList);
        tablaPacientes.setPlaceholder(new Label("No se encontraron pacientes registrados."));
    }

    private void configurarComboBoxDoctores() {
        cbDoctores.setItems(doctoresList);
        cbDoctores.setPromptText("Seleccionar doctor…");

        cbDoctores.setConverter(new StringConverter<>() {
            @Override
            public String toString(User doctor) {
                if (doctor == null) return "";
                return doctor.nombre() + "  (" + doctor.email() + ")";
            }

            @Override
            public User fromString(String s) {
                return null; // No se usa
            }
        });

        // Cell factory para que los items del dropdown también muestren nombre + email
        cbDoctores.setCellFactory(listView -> new ListCell<>() {
            @Override
            protected void updateItem(User doctor, boolean empty) {
                super.updateItem(doctor, empty);
                if (empty || doctor == null) {
                    setText(null);
                } else {
                    setText(doctor.nombre() + "  (" + doctor.email() + ")");
                }
            }
        });
    }

    // ══════════════════════════════════════════════════════════════
    //  CARGA DE DATOS
    // ══════════════════════════════════════════════════════════════

    private void cargarUsuarios() {
        lblStatusBar.setText("Cargando usuarios…");
        btnAsignar.setDisable(true);

        // Usamos el listener en tiempo real para mantener la vista actualizada
        listenerRegistration = userRepository.escucharUsuarios(usuarios -> {
            Platform.runLater(() -> {
                todosLosUsuarios = usuarios;

                List<User> pacientes = usuarios.stream()
                        .filter(u -> u.role() == Role.PATIENT)
                        .collect(Collectors.toList());

                List<User> doctores = usuarios.stream()
                        .filter(u -> u.role() == Role.DOCTOR)
                        .collect(Collectors.toList());

                pacientesList.setAll(pacientes);
                doctoresList.setAll(doctores);

                btnAsignar.setDisable(false);
                lblStatusBar.setText(
                        pacientes.size() + " pacientes  ·  " + doctores.size() + " doctores cargados"
                );
            });
        });
    }

    // ══════════════════════════════════════════════════════════════
    //  ACCIONES DEL USUARIO
    // ══════════════════════════════════════════════════════════════

    @FXML
    private void onAsignarPressed() {
        User paciente = tablaPacientes.getSelectionModel().getSelectedItem();
        User doctor = cbDoctores.getSelectionModel().getSelectedItem();

        if (paciente == null) {
            showAlert(Alert.AlertType.WARNING,
                    "Selección requerida",
                    "Selecciona un paciente de la tabla antes de asignar.");
            return;
        }

        if (doctor == null) {
            showAlert(Alert.AlertType.WARNING,
                    "Selección requerida",
                    "Selecciona un doctor del listado antes de asignar.");
            return;
        }

        btnAsignar.setDisable(true);
        lblStatusBar.setText("Asignando a " + doctor.nombre() + " → " + paciente.nombre() + "…");

        Task<Void> task = new Task<>() {
            @Override
            protected Void call() throws Exception {
                userRepository.asignarMedicoAPaciente(paciente.uid(), doctor.uid());
                return null;
            }
        };

        task.setOnSucceeded(event -> {
            btnAsignar.setDisable(false);
            lblStatusBar.setText("✓ Médico asignado correctamente.");
            showAlert(Alert.AlertType.INFORMATION,
                    "Asignación exitosa",
                    "El Dr. " + doctor.nombre() + " ha sido asignado al paciente "
                            + paciente.nombre() + ".");
            // La tabla se actualiza automáticamente vía el listener de Firestore.
        });

        task.setOnFailed(event -> {
            btnAsignar.setDisable(false);
            lblStatusBar.setText("Error al asignar médico.");
            showAlert(Alert.AlertType.ERROR,
                    "Error al asignar médico",
                    task.getException().getMessage());
        });

        new Thread(task).start();
    }

    @FXML
    private void onLogoutPressed() {
        if (listenerRegistration != null) {
            listenerRegistration.remove();
        }
        UserSession.getInstance().cleanSession();
        ViewManager.switchScene("fxml/login.fxml", "HealthTrack - Login", btnLogout);
    }

    // ══════════════════════════════════════════════════════════════
    //  HELPERS
    // ══════════════════════════════════════════════════════════════

    /**
     * Resuelve el nombre legible de un médico a partir de su UID.
     * Si no hay asignación, devuelve "Sin asignar".
     */
    private String resolverNombreMedico(String medicoUid) {
        if (medicoUid == null || medicoUid.isBlank()) {
            return "— Sin asignar —";
        }
        return todosLosUsuarios.stream()
                .filter(u -> u.uid().equals(medicoUid))
                .map(User::nombre)
                .findFirst()
                .orElse("UID: " + medicoUid);
    }

    private void showAlert(Alert.AlertType type, String title, String message) {
        Alert alert = new Alert(type);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
}
