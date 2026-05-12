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
 * Controlador del sub-módulo de Peso / IMC.
 *
 * Dos campos de entrada: Peso (kg) y Estatura (m).
 * Calcula el IMC (Peso / Estatura²) al guardar y envía:
 *   valorPrimario  = IMC calculado  (para la regla: alerta si > 30)
 *   valorSecundario = Peso en kg    (dato complementario)
 *
 * La estatura no se persiste — se usa solo para el cálculo.
 * Se puede recalcular si es necesario: estatura = √(peso / IMC).
 *
 * Implementa ModuleLimpiable para liberar el listener de Firestore.
 */
public class WeightBmiController implements Initializable, ModuleLimpiable {

    // ── Header ───────────────────────────────────────────
    @FXML private Label lblWelcome;

    // ── Formulario ────────────────────────────────────────
    @FXML private TextField txtPeso;
    @FXML private TextField txtEstatura;
    @FXML private Label     lblImcPreview;
    @FXML private Button    btnGuardarPeso;
    @FXML private Label     lblFeedback;

    // ── Tabla ────────────────────────────────────────────
    @FXML private TableView<MetricaRecord>              tablaMetricas;
    @FXML private TableColumn<MetricaRecord, String>    colFecha;
    @FXML private TableColumn<MetricaRecord, Double>    colImc;
    @FXML private TableColumn<MetricaRecord, Double>    colPeso;
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
        configurarPreviewImc();
        iniciarListenerTiempoReal();

        String nombre = UserSession.getInstance().getLoggedUser().nombre();
        lblWelcome.setText("Paciente: " + nombre);
    }

    // ── Preview en tiempo real del IMC ────────────────────

    /**
     * Agrega listeners a los campos de Peso y Estatura para calcular
     * el IMC en tiempo real mientras el usuario escribe.
     * Esto le da feedback instantáneo antes de presionar "Guardar".
     */
    private void configurarPreviewImc() {
        javafx.beans.value.ChangeListener<String> listener = (obs, oldVal, newVal) -> actualizarPreview();
        txtPeso.textProperty().addListener(listener);
        txtEstatura.textProperty().addListener(listener);
    }

    private void actualizarPreview() {
        try {
            double peso = Double.parseDouble(txtPeso.getText().trim());
            double estatura = Double.parseDouble(txtEstatura.getText().trim());

            if (estatura > 0 && peso > 0) {
                double imc = peso / (estatura * estatura);
                String categoria = clasificarImc(imc);
                lblImcPreview.setText(String.format("IMC: %.1f — %s", imc, categoria));
                lblImcPreview.setVisible(true);
                lblImcPreview.setManaged(true);

                // Estilo condicional del preview
                lblImcPreview.getStyleClass().removeAll("imc-normal", "imc-warning", "imc-danger");
                if (imc > 30) {
                    lblImcPreview.getStyleClass().add("imc-danger");
                } else if (imc > 25) {
                    lblImcPreview.getStyleClass().add("imc-warning");
                } else {
                    lblImcPreview.getStyleClass().add("imc-normal");
                }
            } else {
                lblImcPreview.setVisible(false);
                lblImcPreview.setManaged(false);
            }
        } catch (NumberFormatException e) {
            lblImcPreview.setVisible(false);
            lblImcPreview.setManaged(false);
        }
    }

    /**
     * Clasifica el IMC según la OMS.
     */
    private String clasificarImc(double imc) {
        if (imc < 18.5) return "Bajo peso";
        if (imc < 25.0) return "Peso normal";
        if (imc < 30.0) return "Sobrepeso";
        if (imc < 35.0) return "Obesidad grado I";
        if (imc < 40.0) return "Obesidad grado II";
        return "Obesidad grado III";
    }

    // ── Configuración de columnas ─────────────────────────

    private void configurarColumnas() {
        colFecha.setCellValueFactory(cell ->
                new javafx.beans.property.SimpleStringProperty(cell.getValue().fechaRegistro()));
        colImc.setCellValueFactory(cell ->
                new javafx.beans.property.SimpleDoubleProperty(cell.getValue().valorPrimario()).asObject());
        colPeso.setCellValueFactory(cell -> {
            Double peso = cell.getValue().valorSecundario();
            return new javafx.beans.property.SimpleDoubleProperty(peso != null ? peso : 0.0).asObject();
        });
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

        // Formatear IMC a 1 decimal en la tabla
        colImc.setCellFactory(col -> new TableCell<>() {
            @Override
            protected void updateItem(Double imc, boolean empty) {
                super.updateItem(imc, empty);
                setText(empty || imc == null ? null : String.format("%.1f", imc));
            }
        });

        tablaMetricas.setItems(metricasList);
        tablaMetricas.setPlaceholder(new Label("No hay registros de peso / IMC."));
    }

    // ── Listener en tiempo real ────────────────────────────

    private void iniciarListenerTiempoReal() {
        String uid = UserSession.getInstance().getLoggedUser().uid();

        listenerRegistration = metricaService.escucharMetricasPaciente(
                uid,
                metricas -> Platform.runLater(() -> {
                    var filtradas = metricas.stream()
                            .filter(m -> "PESO_IMC".equals(m.tipo()))
                            .collect(Collectors.toList());
                    metricasList.setAll(filtradas);
                }),
                error -> Platform.runLater(() ->
                        mostrarFeedback("Error al escuchar cambios: " + error.getMessage(), false))
        );
    }

    // ── Handler del formulario ────────────────────────────

    @FXML
    private void onGuardarPesoPressed() {
        String pesoStr     = txtPeso.getText().trim();
        String estaturaStr = txtEstatura.getText().trim();

        if (pesoStr.isEmpty() || estaturaStr.isEmpty()) {
            mostrarFeedback("Por favor, completa ambos campos (peso y estatura).", false);
            return;
        }

        try {
            double peso     = Double.parseDouble(pesoStr);
            double estatura = Double.parseDouble(estaturaStr);

            // Validaciones de rango razonable
            if (peso < 1 || peso > 500) {
                mostrarFeedback("Peso fuera de rango. Ingresa un valor entre 1 y 500 kg.", false);
                return;
            }
            if (estatura < 0.3 || estatura > 2.8) {
                mostrarFeedback("Estatura fuera de rango. Ingresa un valor entre 0.3 y 2.8 m.", false);
                return;
            }

            double imc = peso / (estatura * estatura);

            String uid = UserSession.getInstance().getLoggedUser().uid();
            btnGuardarPeso.setDisable(true);

            Task<Void> task = new Task<>() {
                @Override
                protected Void call() throws Exception {
                    // v1 = IMC (para la regla de alerta > 30)
                    // v2 = Peso en kg (dato complementario)
                    metricaService.validarYGuardar(uid, "PESO_IMC", imc, peso);
                    return null;
                }
            };

            task.setOnSucceeded(event -> {
                txtPeso.clear();
                txtEstatura.clear();
                mostrarFeedback(String.format("Registrado — IMC: %.1f (%s)", imc, clasificarImc(imc)), true);
                btnGuardarPeso.setDisable(false);
            });

            task.setOnFailed(event -> {
                mostrarFeedback("Error al guardar: " + task.getException().getMessage(), false);
                btnGuardarPeso.setDisable(false);
            });

            new Thread(task).start();

        } catch (NumberFormatException e) {
            mostrarFeedback("Los valores deben ser números válidos (ej: 70.5 kg, 1.75 m).", false);
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
