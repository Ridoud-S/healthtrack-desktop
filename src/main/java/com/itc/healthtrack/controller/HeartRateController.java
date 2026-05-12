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

import java.net.URL;
import java.util.ResourceBundle;
import java.util.stream.Collectors;

/**
 * Controlador del sub-módulo de Frecuencia Cardíaca.
 *
 * Un solo campo de entrada para los latidos por minuto (BPM).
 * Filtra la tabla para mostrar solo registros de tipo FRECUENCIA_CARDIACA.
 *
 * Implementa ModuleLimpiable para liberar el listener de Firestore
 * cuando el Shell inyecta otro módulo.
 */
public class HeartRateController implements Initializable, ModuleLimpiable {

    // ── Header ───────────────────────────────────────────
    @FXML private Label lblWelcome;

    // ── Formulario ────────────────────────────────────────
    @FXML private TextField txtBpm;
    @FXML private Button    btnGuardarFc;
    @FXML private Label     lblFeedback;

    // ── Tabla ────────────────────────────────────────────
    @FXML private TableView<MetricaRecord>              tablaMetricas;
    @FXML private TableColumn<MetricaRecord, String>    colFecha;
    @FXML private TableColumn<MetricaRecord, Double>    colBpm;
    @FXML private TableColumn<MetricaRecord, Boolean>   colAlerta;
    @FXML private TableColumn<MetricaRecord, String>    colRecomendacion;

    // ── Dependencias ─────────────────────────────────────
    private final MetricaService metricaService =
            new MetricaService(new MetricaRepositoryImpl());
    private final ObservableList<MetricaRecord> metricasList =
            FXCollections.observableArrayList();
    private ListenerRegistration listenerRegistration;

    // ─────────────────────────────────────────────────────

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        configurarColumnas();
        iniciarListenerTiempoReal();

        String nombre = UserSession.getInstance().getLoggedUser().nombre();
        lblWelcome.setText("Paciente: " + nombre);
    }

    // ── Configuración de columnas ─────────────────────────

    private void configurarColumnas() {
        colFecha.setCellValueFactory(cell ->
                new javafx.beans.property.SimpleStringProperty(cell.getValue().fechaRegistro()));
        colBpm.setCellValueFactory(cell ->
                new javafx.beans.property.SimpleDoubleProperty(cell.getValue().valorPrimario()).asObject());
        colAlerta.setCellValueFactory(cell ->
                new javafx.beans.property.SimpleBooleanProperty(cell.getValue().alerta()).asObject());
        colRecomendacion.setCellValueFactory(cell ->
                new javafx.beans.property.SimpleStringProperty(cell.getValue().recomendacion()));

        // CellFactory visual para la columna Alerta
        colAlerta.setCellFactory(col -> new TableCell<>() {
            @Override
            protected void updateItem(Boolean alerta, boolean empty) {
                super.updateItem(alerta, empty);
                if (empty || alerta == null) {
                    setText(null);
                    setGraphic(null);
                } else {
                    Label indicator = new Label(alerta ? "⚠ Alerta" : "✅ Normal");
                    indicator.getStyleClass().add(alerta ? "alert-badge-danger" : "alert-badge-ok");
                    setGraphic(indicator);
                }
            }
        });

        tablaMetricas.setItems(metricasList);
        tablaMetricas.setPlaceholder(new Label("No hay registros de frecuencia cardíaca."));
    }

    // ── Listener en tiempo real ────────────────────────────

    private void iniciarListenerTiempoReal() {
        String uid = UserSession.getInstance().getLoggedUser().uid();

        listenerRegistration = metricaService.escucharMetricasPaciente(
                uid,
                metricas -> Platform.runLater(() -> {
                    var filtradas = metricas.stream()
                            .filter(m -> "FRECUENCIA_CARDIACA".equals(m.tipo()))
                            .collect(Collectors.toList());
                    metricasList.setAll(filtradas);
                }),
                error -> Platform.runLater(() ->
                        mostrarFeedback("Error al escuchar cambios: " + error.getMessage(), false))
        );
    }

    // ── Handler del formulario ────────────────────────────

    @FXML
    private void onGuardarFcPressed() {
        String bpmStr = txtBpm.getText().trim();

        if (bpmStr.isEmpty()) {
            mostrarFeedback("Por favor, ingresa los latidos por minuto.", false);
            return;
        }

        try {
            double bpm = Double.parseDouble(bpmStr);

            // Validación de rango razonable (1-300 BPM)
            if (bpm < 1 || bpm > 300) {
                mostrarFeedback("Valor fuera de rango. Ingresa un BPM entre 1 y 300.", false);
                return;
            }

            String uid = UserSession.getInstance().getLoggedUser().uid();
            btnGuardarFc.setDisable(true);

            Task<Void> task = new Task<>() {
                @Override
                protected Void call() throws Exception {
                    metricaService.validarYGuardar(uid, "FRECUENCIA_CARDIACA", bpm, null);
                    return null;
                }
            };

            task.setOnSucceeded(event -> {
                txtBpm.clear();
                mostrarFeedback("Frecuencia cardíaca registrada correctamente.", true);
                btnGuardarFc.setDisable(false);
            });

            task.setOnFailed(event -> {
                mostrarFeedback("Error al guardar: " + task.getException().getMessage(), false);
                btnGuardarFc.setDisable(false);
            });

            new Thread(task).start();

        } catch (NumberFormatException e) {
            mostrarFeedback("El valor debe ser un número válido (ej: 72, 85).", false);
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

    // ── ModuleLimpiable ───────────────────────────────────

    @Override
    public void detenerListeners() {
        if (listenerRegistration != null) {
            listenerRegistration.remove();
            listenerRegistration = null;
        }
    }
}
