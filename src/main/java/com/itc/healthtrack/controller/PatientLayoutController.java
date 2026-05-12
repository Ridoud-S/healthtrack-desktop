package com.itc.healthtrack.controller;

import com.itc.healthtrack.session.UserSession;
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
 *  - Detener listeners del módulo anterior antes de cargar uno nuevo (evita memory leaks).
 */
public class PatientLayoutController implements Initializable {

    // ── Sidebar ──────────────────────────────────────────
    @FXML private Button btnSummary;
    @FXML private Button btnBloodPressure;
    @FXML private Button btnGlucose;
    @FXML private Button btnHeartRate;
    @FXML private Button btnWeightBmi;
    @FXML private Button btnLogout;

    // ── Info del paciente en el sidebar ──────────────────
    @FXML private Label labelPatientName;
    @FXML private Label labelPatientId;

    // ── Contenedor central dinámico ───────────────────────
    @FXML private AnchorPane contentArea;

    // Rutas FXML de cada sub-módulo
    private static final String VIEW_SUMMARY        = "fxml/patient_summary.fxml";
    private static final String VIEW_BLOOD_PRESSURE = "fxml/patient_blood_pressure.fxml";
    private static final String VIEW_GLUCOSE        = "fxml/patient_glucose.fxml";
    private static final String VIEW_HEART_RATE     = "fxml/patient_heart_rate.fxml";
    private static final String VIEW_WEIGHT_BMI     = "fxml/patient_weight_bmi.fxml";
    private static final String VIEW_LOGIN          = "fxml/login.fxml";

    // ── Referencia al controlador del módulo activo ──────
    // Se usa para invocar detenerListeners() antes de inyectar uno nuevo.
    private Object currentModuleController;

    // ─────────────────────────────────────────────────────
    @Override
    public void initialize(URL location, ResourceBundle resources) {
        // Rellenar info del paciente en el sidebar
        String nombre = UserSession.getInstance().getLoggedUser().nombre();
        String uid    = UserSession.getInstance().getLoggedUser().uid();
        labelPatientName.setText(nombre);
        labelPatientId.setText("ID: " + uid.substring(0, Math.min(uid.length(), 8)) + "…");

        // Carga el Resumen por defecto al abrir el Shell
        loadModule(VIEW_SUMMARY, btnSummary);
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
    private void handleHeartRate() {
        loadModule(VIEW_HEART_RATE, btnHeartRate);
    }

    @FXML
    private void handleWeightBmi() {
        loadModule(VIEW_WEIGHT_BMI, btnWeightBmi);
    }

    @FXML
    private void handleLogout() {
        // Detener listeners del módulo activo antes de salir del Shell
        limpiarModuloActual();
        UserSession.getInstance().cleanSession();
        ViewManager.switchScene(VIEW_LOGIN, "HealthTrack — Iniciar Sesión", btnLogout);
    }

    // ── Lógica de navegación interna ─────────────────────

    /**
     * Carga el FXML del módulo en el contentArea y actualiza
     * el estado visual (active/inactive) de los botones del sidebar.
     *
     * IMPORTANTE: Antes de inyectar la nueva vista, se invoca
     * detenerListeners() en el módulo anterior si implementa
     * ModuleLimpiable, evitando fugas de memoria por listeners
     * de Firestore acumulados.
     */
    private void loadModule(String fxmlPath, Button activeBtn) {
        // 1. Limpiar el módulo anterior
        limpiarModuloActual();

        // 2. Cargar la nueva vista y obtener su controlador
        currentModuleController = ViewManager.loadViewIntoPaneWithController(fxmlPath, contentArea);

        // 3. Actualizar el estado visual del sidebar
        updateActiveButton(activeBtn);
    }

    /**
     * Si el controlador del módulo activo implementa ModuleLimpiable,
     * invoca detenerListeners() para liberar recursos.
     */
    private void limpiarModuloActual() {
        if (currentModuleController instanceof ModuleLimpiable moduloAnterior) {
            moduloAnterior.detenerListeners();
        }
        currentModuleController = null;
    }

    /**
     * Quita la clase CSS "nav-btn-active" de todos los botones de navegación
     * y la aplica solo al botón que acaba de ser pulsado.
     */
    private void updateActiveButton(Button activeBtn) {
        List<Button> navButtons = List.of(btnSummary, btnBloodPressure, btnGlucose, btnHeartRate, btnWeightBmi);

        for (Button btn : navButtons) {
            btn.getStyleClass().remove("nav-btn-active");
        }
        if (!activeBtn.getStyleClass().contains("nav-btn-active")) {
            activeBtn.getStyleClass().add("nav-btn-active");
        }
    }
}