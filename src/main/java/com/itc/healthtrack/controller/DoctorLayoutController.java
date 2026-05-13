package com.itc.healthtrack.controller;

import com.itc.healthtrack.model.User;
import com.itc.healthtrack.service.UserService;
import com.itc.healthtrack.session.UserSession;
import com.itc.healthtrack.util.ViewManager;
import javafx.application.Platform;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;

import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.ResourceBundle;

/**
 * Controlador del Shell principal del médico — "Estación de Telemedicina".
 *
 * Responsabilidades:
 *  - Cargar en el sidebar los pacientes asignados al médico logueado.
 *  - Filtrar pacientes en tiempo real con la barra de búsqueda.
 *  - Al hacer clic en un paciente, inyectar doctor_patient_detail.fxml
 *    en el contentArea, pasándole el UID y nombre del paciente.
 *  - Limpiar listeners del módulo anterior antes de cargar uno nuevo.
 *  - Gestionar cierre de sesión.
 */
public class DoctorLayoutController implements Initializable {

    // ── Sidebar ──────────────────────────────────────────
    @FXML private Label lblDoctorName;
    @FXML private Label lblDoctorId;
    @FXML private Label lblPatientCount;
    @FXML private TextField txtBuscarPaciente;
    @FXML private VBox pacientesContainer;
    @FXML private Button btnLogout;

    // ── Centro ───────────────────────────────────────────
    @FXML private AnchorPane contentArea;
    @FXML private VBox emptyState;

    // ── Estado interno ───────────────────────────────────
    private final UserService userService = new UserService();
    private final List<User> allPacientes = new ArrayList<>();
    private Object currentModuleController;
    private String selectedPatientUid;

    private static final String VIEW_PATIENT_DETAIL = "fxml/doctor_patient_detail.fxml";
    private static final String VIEW_LOGIN          = "fxml/login.fxml";

    // ─────────────────────────────────────────────────────
    @Override
    public void initialize(URL location, ResourceBundle resources) {
        User doctor = UserSession.getInstance().getLoggedUser();
        lblDoctorName.setText("Dr. " + doctor.nombre());
        String uid = doctor.uid();
        lblDoctorId.setText("ID: " + uid.substring(0, Math.min(uid.length(), 8)) + "…");

        // Configurar búsqueda en tiempo real
        txtBuscarPaciente.textProperty().addListener((obs, oldVal, newVal) -> filtrarPacientes(newVal));

        // Cargar pacientes desde Firestore en background
        cargarPacientes();
    }

    // ── Carga de pacientes asignados ────────────────────

    private void cargarPacientes() {
        String medicoUid = UserSession.getInstance().getLoggedUser().uid();

        Task<List<User>> task = new Task<>() {
            @Override
            protected List<User> call() throws Exception {
                return userService.obtenerPacientesPorMedico(medicoUid);
            }
        };

        task.setOnSucceeded(event -> {
            allPacientes.clear();
            allPacientes.addAll(task.getValue());
            lblPatientCount.setText("(" + allPacientes.size() + ")");
            renderPacientes(allPacientes);
        });

        task.setOnFailed(event -> {
            System.err.println("Error al cargar pacientes: " + task.getException().getMessage());
            lblPatientCount.setText("(error)");
        });

        new Thread(task, "doctor-load-patients").start();
    }

    // ── Filtrado de pacientes ───────────────────────────

    private void filtrarPacientes(String query) {
        if (query == null || query.isBlank()) {
            renderPacientes(allPacientes);
            return;
        }

        String lowerQuery = query.toLowerCase();
        List<User> filtrados = allPacientes.stream()
                .filter(p -> (p.nombre() != null && p.nombre().toLowerCase().contains(lowerQuery))
                          || (p.email() != null && p.email().toLowerCase().contains(lowerQuery)))
                .toList();

        renderPacientes(filtrados);
    }

    // ── Renderizado de la lista de pacientes ────────────

    private void renderPacientes(List<User> pacientes) {
        pacientesContainer.getChildren().clear();

        if (pacientes.isEmpty()) {
            Label emptyLabel = new Label("No hay pacientes asignados.");
            emptyLabel.getStyleClass().add("empty-patient-label");
            pacientesContainer.getChildren().add(emptyLabel);
            return;
        }

        for (User paciente : pacientes) {
            VBox card = crearPacienteCard(paciente);
            pacientesContainer.getChildren().add(card);
        }
    }

    /**
     * Crea una "tarjeta" visual para cada paciente en el sidebar.
     */
    private VBox crearPacienteCard(User paciente) {
        // Nombre
        Label nombre = new Label(paciente.nombre() != null ? paciente.nombre() : "Sin nombre");
        nombre.getStyleClass().add("patient-card-name");

        // Email
        Label email = new Label(paciente.email() != null ? paciente.email() : "");
        email.getStyleClass().add("patient-card-email");

        // Indicador de estado
        Label statusDot = new Label("●");
        statusDot.getStyleClass().add("patient-card-status-dot");

        HBox header = new HBox(8, statusDot, nombre);
        header.setAlignment(Pos.CENTER_LEFT);

        VBox card = new VBox(2, header, email);
        card.getStyleClass().add("patient-card");
        card.setPadding(new Insets(10, 12, 10, 12));

        // Marcar el seleccionado
        if (paciente.uid().equals(selectedPatientUid)) {
            card.getStyleClass().add("patient-card-selected");
        }

        // Handler de clic
        card.setOnMouseClicked(event -> onPacienteSeleccionado(paciente));

        return card;
    }

    // ── Selección de paciente ───────────────────────────

    private void onPacienteSeleccionado(User paciente) {
        selectedPatientUid = paciente.uid();

        // 1. Limpiar el módulo anterior (listeners, etc.)
        limpiarModuloActual();

        // 2. Ocultar el empty state
        emptyState.setVisible(false);
        emptyState.setManaged(false);

        // 3. Cargar la vista de detalle y obtener su controlador
        DoctorPatientDetailController detailCtrl =
                ViewManager.loadViewIntoPaneWithController(VIEW_PATIENT_DETAIL, contentArea);

        // 4. Pasar datos del paciente al controlador de detalle
        detailCtrl.inicializarPaciente(paciente.uid(), paciente.nombre());

        currentModuleController = detailCtrl;

        // 5. Actualizar visual del sidebar
        renderPacientes(
                txtBuscarPaciente.getText() != null && !txtBuscarPaciente.getText().isBlank()
                        ? allPacientes.stream()
                            .filter(p -> p.nombre() != null && p.nombre().toLowerCase()
                                    .contains(txtBuscarPaciente.getText().toLowerCase()))
                            .toList()
                        : allPacientes
        );
    }

    // ── Limpieza de módulo activo ───────────────────────

    private void limpiarModuloActual() {
        if (currentModuleController instanceof ModuleLimpiable moduloAnterior) {
            moduloAnterior.detenerListeners();
        }
        currentModuleController = null;
    }

    // ── Logout ──────────────────────────────────────────

    @FXML
    private void handleLogout() {
        limpiarModuloActual();
        UserSession.getInstance().cleanSession();
        ViewManager.switchScene(VIEW_LOGIN, "HealthTrack — Iniciar Sesión", btnLogout);
    }
}
