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
 * Controlador del sub-módulo de Glucosa.
 *
 * Similar a BloodPressureController pero con un solo campo de entrada
 * (nivel de glucosa en mg/dL). Filtra la tabla para mostrar solo
 * registros de tipo GLUCOSA.
 *
 * Implementa ModuleLimpiable para liberar el listener de Firestore
 * cuando el Shell inyecta otro módulo.
 */
public class GlucoseController implements Initializable, ModuleLimpiable {

    // ── Header ───────────────────────────────────────────
    @FXML private Label lblWelcome;

    // ── Formulario ────────────────────────────────────────
    @FXML private TextField txtGlucosa;
    @FXML private Button    btnGuardarGlucosa;
    @FXML private Label     lblFeedback;

    // ── Tabla ────────────────────────────────────────────
    @FXML private TableView<MetricaRecord>              tablaMetricas;
    @FXML private TableColumn<MetricaRecord, String>    colFecha;
    @FXML private TableColumn<MetricaRecord, Double>    colNivel;
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

        // Nombre del paciente
        String nombre = UserSession.getInstance().getLoggedUser().nombre();
        lblWelcome.setText("Paciente: " + nombre);
    }

    // ── Configuración de columnas ─────────────────────────

    private void configurarColumnas() {
        colFecha.setCellValueFactory(cell ->
                new javafx.beans.property.SimpleStringProperty(cell.getValue().fechaRegistro()));
        colNivel.setCellValueFactory(cell ->
                new javafx.beans.property.SimpleDoubleProperty(cell.getValue().valorPrimario()).asObject());
        colAlerta.setCellValueFactory(cell ->
                new javafx.beans.property.SimpleBooleanProperty(cell.getValue().alerta()).asObject());
        colRecomendacion.setCellValueFactory(cell ->
                new javafx.beans.property.SimpleStringProperty(cell.getValue().recomendacion()));

        // CellFactory visual para la columna Alerta (🟢 / 🔴)
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
        tablaMetricas.setPlaceholder(new Label("No hay registros de glucosa."));
    }

    // ── Listener en tiempo real ────────────────────────────

    private void iniciarListenerTiempoReal() {
        String uid = UserSession.getInstance().getLoggedUser().uid();

        listenerRegistration = metricaService.escucharMetricasPaciente(
                uid,
                metricas -> Platform.runLater(() -> {
                    // Filtrar solo métricas de tipo GLUCOSA
                    var filtradas = metricas.stream()
                            .filter(m -> "GLUCOSA".equals(m.tipo()))
                            .collect(Collectors.toList());
                    metricasList.setAll(filtradas);
                }),
                error -> Platform.runLater(() ->
                        mostrarFeedback("Error al escuchar cambios: " + error.getMessage(), false))
        );
    }

    // ── Handler del formulario ────────────────────────────

    @FXML
    private void onGuardarGlucosaPressed() {
        String glucosaStr = txtGlucosa.getText().trim();

        if (glucosaStr.isEmpty()) {
            mostrarFeedback("Por favor, ingresa el nivel de glucosa.", false);
            return;
        }

        try {
            double nivel = Double.parseDouble(glucosaStr);

            String uid = UserSession.getInstance().getLoggedUser().uid();
            btnGuardarGlucosa.setDisable(true);

            Task<Void> task = new Task<>() {
                @Override
                protected Void call() throws Exception {
                    // valorSecundario = null (glucosa solo usa un valor)
                    metricaService.validarYGuardar(uid, "GLUCOSA", nivel, null);
                    return null;
                }
            };

            task.setOnSucceeded(event -> {
                txtGlucosa.clear();
                mostrarFeedback("Glucosa registrada correctamente.", true);
                btnGuardarGlucosa.setDisable(false);
            });

            task.setOnFailed(event -> {
                mostrarFeedback("Error al guardar: " + task.getException().getMessage(), false);
                btnGuardarGlucosa.setDisable(false);
            });

            new Thread(task).start();

        } catch (NumberFormatException e) {
            mostrarFeedback("El valor debe ser un número válido (ej: 95, 110.5).", false);
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
