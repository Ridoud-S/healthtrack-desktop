package com.itc.healthtrack.controller;

import com.itc.healthtrack.util.ViewManager;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.AnchorPane;

import java.net.URL;
import java.util.List;
import java.util.ResourceBundle;

/**
 * Controlador del Shell principal del paciente.
 *
 * Responsabilidades:
 *  - Manejar la navegación del Sidebar sin recargar la ventana.
 *  - Inyectar sub-módulos FXML en el contentArea.
 *  - Resaltar el botón activo del sidebar.
 *  - (Opcional) Pasar datos del paciente a cada sub-módulo via loadViewIntoPaneWithController().
 */
public class PatientLayoutController implements Initializable {

    // ── Sidebar ──────────────────────────────────────────
    @FXML private Button btnSummary;
    @FXML private Button btnBloodPressure;
    @FXML private Button btnGlucose;
    @FXML private Button btnLogout;

    // ── Info del paciente en el sidebar ──────────────────
    @FXML private Label labelPatientName;
    @FXML private Label labelPatientId;

    // ── Contenedor central dinámico ───────────────────────
    @FXML private AnchorPane contentArea;

    // Rutas FXML de cada sub-módulo (ajusta según tu estructura de recursos)
    private static final String VIEW_SUMMARY        = "views/patient_summary.fxml";
    private static final String VIEW_BLOOD_PRESSURE = "views/patient_blood_pressure.fxml";
    private static final String VIEW_GLUCOSE        = "views/patient_glucose.fxml";
    private static final String VIEW_LOGIN          = "views/login.fxml";

    // ─────────────────────────────────────────────────────
    @Override
    public void initialize(URL location, ResourceBundle resources) {
        // Carga el Resumen por defecto al abrir el Shell
        loadModule(VIEW_SUMMARY, btnSummary);

        // TODO: Obtener datos del paciente desde la sesión activa y rellenar el sidebar
        // Patient patient = SessionManager.getCurrentPatient();
        // labelPatientName.setText(patient.getFullName());
        // labelPatientId.setText("ID: " + patient.getId());
    }

    // ── Handlers de los botones del Sidebar ──────────────

    @FXML
    private void handleSummary() {
        loadModule(VIEW_SUMMARY, btnSummary);
    }

    @FXML
    private void handleBloodPressure() {
        loadModule(VIEW_BLOOD_PRESSURE, btnBloodPressure);
    }

    @FXML
    private void handleGlucose() {
        loadModule(VIEW_GLUCOSE, btnGlucose);
    }

    @FXML
    private void handleLogout() {
        // switchScene reemplaza la ventana completa → correcto para salir del Shell
        ViewManager.switchScene(VIEW_LOGIN, "HealthTrack — Iniciar Sesión", btnLogout);
    }

    // ── Lógica de navegación interna ─────────────────────

    /**
     * Carga el FXML del módulo en el contentArea y actualiza
     * el estado visual (active/inactive) de los botones del sidebar.
     */
    private void loadModule(String fxmlPath, Button activeBtn) {
        ViewManager.loadViewIntoPane(fxmlPath, contentArea);
        updateActiveButton(activeBtn);
    }

    /**
     * Quita la clase CSS "nav-btn-active" de todos los botones de navegación
     * y la aplica solo al botón que acaba de ser pulsado.
     */
    private void updateActiveButton(Button activeBtn) {
        List<Button> navButtons = List.of(btnSummary, btnBloodPressure, btnGlucose);

        for (Button btn : navButtons) {
            btn.getStyleClass().remove("nav-btn-active");
        }
        if (!activeBtn.getStyleClass().contains("nav-btn-active")) {
            activeBtn.getStyleClass().add("nav-btn-active");
        }
    }

    /*
     * ──────────────────────────────────────────────────────────────────────────
     * PATRÓN AVANZADO (descomenta si necesitas pasar datos al sub-módulo):
     *
     * Si el sub-módulo necesita recibir el objeto Patient u otros datos,
     * usa loadViewIntoPaneWithController() en lugar de loadViewIntoPane():
     *
     *   @FXML
     *   private void handleBloodPressure() {
     *       BloodPressureController ctrl =
     *           ViewManager.loadViewIntoPaneWithController(VIEW_BLOOD_PRESSURE, contentArea);
     *       ctrl.setPatient(SessionManager.getCurrentPatient());
     *       updateActiveButton(btnBloodPressure);
     *   }
     * ──────────────────────────────────────────────────────────────────────────
     */
}