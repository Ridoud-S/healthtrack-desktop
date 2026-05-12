package com.itc.healthtrack.controller;

import com.google.cloud.firestore.ListenerRegistration;
import com.itc.healthtrack.model.MetricaRecord;
import com.itc.healthtrack.repository.MetricaRepositoryImpl;
import com.itc.healthtrack.service.MetricaService;
import com.itc.healthtrack.session.UserSession;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;

import java.net.URL;
import java.util.ResourceBundle;

/**
 * Controlador del sub-módulo de Presión Arterial.
 *
 * Renombrado desde PatientDashboardController.
 * Ya NO maneja navegación ni cierre de sesión — esa responsabilidad
 * pertenece al Shell (PatientLayoutController).
 */
public class BloodPressureController implements Initializable {

    // ── Header ───────────────────────────────────────────
    @FXML private Label lblWelcome;

    // ── Formulario ────────────────────────────────────────
    @FXML private TextField txtSistolica;
    @FXML private TextField txtDiastolica;
    @FXML private Button    btnGuardarPresion;
    @FXML private Label     lblFeedback;

    // ── Tabla ────────────────────────────────────────────
    @FXML private TableView<MetricaRecord>        tablaMetricas;
    @FXML private TableColumn<MetricaRecord, String>   colFecha;
    @FXML private TableColumn<MetricaRecord, String>   colTipo;
    @FXML private TableColumn<MetricaRecord, Double>   colSistolica;
    @FXML private TableColumn<MetricaRecord, Double>   colDiastolica;
    @FXML private TableColumn<MetricaRecord, Boolean>  colAlerta;
    @FXML private TableColumn<MetricaRecord, String>   colRecomendacion;

    // ── Dependencias ─────────────────────────────────────
    private final MetricaService metricaService = new MetricaService(new MetricaRepositoryImpl());
    private final ObservableList<MetricaRecord> metricasList = FXCollections.observableArrayList();
    private ListenerRegistration listenerRegistration;

    // ─────────────────────────────────────────────────────

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        configurarColumnas();
        iniciarListenerTiempoReal();

        // Obtener nombre del paciente desde sesión
        String nombrePaciente = UserSession.getInstance().getLoggedUser().nombre();
        lblWelcome.setText("Paciente: " + nombrePaciente);
    }

    // ── Configuración de columnas ─────────────────────────

    private void configurarColumnas() {
        colFecha.setCellValueFactory(cell ->
                new javafx.beans.property.SimpleStringProperty(cell.getValue().fechaRegistro()));
        colTipo.setCellValueFactory(cell ->
                new javafx.beans.property.SimpleStringProperty(cell.getValue().tipo()));
        colSistolica.setCellValueFactory(cell ->
                new javafx.beans.property.SimpleDoubleProperty(cell.getValue().valorPrimario()).asObject());
        colDiastolica.setCellValueFactory(cell ->
                new javafx.beans.property.SimpleDoubleProperty(cell.getValue().valorSecundario()).asObject());
        colAlerta.setCellValueFactory(cell ->
                new javafx.beans.property.SimpleBooleanProperty(cell.getValue().alerta()).asObject());
        colRecomendacion.setCellValueFactory(cell ->
                new javafx.beans.property.SimpleStringProperty(cell.getValue().recomendacion()));

        tablaMetricas.setItems(metricasList);
        tablaMetricas.setPlaceholder(new Label("No hay métricas registradas."));
    }

    // ── Listener en tiempo real ────────────────────────────

    private void iniciarListenerTiempoReal() {
        String uid = UserSession.getInstance().getLoggedUser().uid();

        listenerRegistration = metricaService.escucharMetricasPaciente(
                uid,
                metricas -> Platform.runLater(() -> metricasList.setAll(metricas)),
                error -> Platform.runLater(() ->
                        mostrarFeedback("Error al escuchar cambios: " + error.getMessage(), false))
        );
    }

    // ── Handler del formulario ────────────────────────────

    @FXML
    private void onGuardarPresionPressed() {
        String sistolicaStr  = txtSistolica.getText().trim();
        String diastolicaStr = txtDiastolica.getText().trim();

        if (sistolicaStr.isEmpty() || diastolicaStr.isEmpty()) {
            mostrarFeedback("Por favor, completa ambos campos.", false);
            return;
        }

        try {
            double sistolica  = Double.parseDouble(sistolicaStr);
            double diastolica = Double.parseDouble(diastolicaStr);

            String uid = UserSession.getInstance().getLoggedUser().uid();
            btnGuardarPresion.setDisable(true);

            // Ejecutar en background thread
            Task<Void> task = new Task<>() {
                @Override
                protected Void call() throws Exception {
                    metricaService.validarYGuardar(uid, "PRESION_ARTERIAL", sistolica, diastolica);
                    return null;
                }
            };

            task.setOnSucceeded(event -> {
                txtSistolica.clear();
                txtDiastolica.clear();
                mostrarFeedback("Métrica guardada correctamente.", true);
                btnGuardarPresion.setDisable(false);
            });

            task.setOnFailed(event -> {
                mostrarFeedback("Error al guardar: " + task.getException().getMessage(), false);
                btnGuardarPresion.setDisable(false);
            });

            new Thread(task).start();

        } catch (NumberFormatException e) {
            mostrarFeedback("Los valores deben ser números válidos (ej: 120.5, 80.0).", false);
        }
    }

    // ── Utilidades de UI ──────────────────────────────────

    private void mostrarFeedback(String mensaje, boolean esExito) {
        lblFeedback.setText(mensaje);
        lblFeedback.getStyleClass().removeAll("lbl-feedback-success", "lbl-feedback-error");
        lblFeedback.getStyleClass().add(esExito ? "lbl-feedback-success" : "lbl-feedback-error");
        lblFeedback.setVisible(true);
        lblFeedback.setManaged(true);
    }

    // ── Cleanup (llamado por Shell al cerrar) ───────────

    public void detenerListeners() {
        if (listenerRegistration != null) {
            listenerRegistration.remove();
        }
    }
}

