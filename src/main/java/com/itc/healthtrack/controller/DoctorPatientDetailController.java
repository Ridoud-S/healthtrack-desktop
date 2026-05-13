package com.itc.healthtrack.controller;

import com.google.cloud.firestore.ListenerRegistration;
import com.itc.healthtrack.model.MetricaRecord;
import com.itc.healthtrack.model.NotaMedicaRecord;
import com.itc.healthtrack.repository.MetricaRepositoryImpl;
import com.itc.healthtrack.repository.NotaMedicaRepository;
import com.itc.healthtrack.repository.NotaMedicaRepositoryImpl;
import com.itc.healthtrack.service.MetricaService;
import com.itc.healthtrack.session.UserSession;
import javafx.application.Platform;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.scene.chart.CategoryAxis;
import javafx.scene.chart.LineChart;
import javafx.scene.chart.XYChart;
import javafx.scene.control.*;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.CompletableFuture;

/**
 * Controlador del Centro de Mando del Paciente.
 *
 * Se inyecta dinámicamente desde DoctorLayoutController.
 * Recibe el UID y nombre del paciente vía {@link #inicializarPaciente(String, String)}.
 *
 * Secciones:
 *  1. Tendencias (LineChart con pestañas por tipo de métrica)
 *  2. Historial de métricas (TableView)
 *  3. Panel de prescripción (TextArea + botón enviar)
 *
 * Implementa ModuleLimpiable para liberar el listener de métricas
 * cuando el doctor cambia de paciente o cierra sesión.
 */
public class DoctorPatientDetailController implements ModuleLimpiable {

    // ── Header ──────────────────────────────────────────
    @FXML private Label lblPatientName;
    @FXML private Label lblPatientUid;
    @FXML private Label lblMetricCount;

    // ── Tendencias (Charts) ─────────────────────────────
    @FXML private TabPane tabPaneTrends;
    @FXML private LineChart<String, Number> chartPresion;
    @FXML private LineChart<String, Number> chartGlucosa;
    @FXML private LineChart<String, Number> chartFC;
    @FXML private LineChart<String, Number> chartPeso;

    // ── Historial (TableView) ───────────────────────────
    @FXML private TableView<MetricaRecord> tablaMetricas;
    @FXML private TableColumn<MetricaRecord, String> colFecha;
    @FXML private TableColumn<MetricaRecord, String> colTipo;
    @FXML private TableColumn<MetricaRecord, Double> colValPrimario;
    @FXML private TableColumn<MetricaRecord, Double> colValSecundario;
    @FXML private TableColumn<MetricaRecord, String> colAlerta;

    // ── Prescripción ────────────────────────────────────
    @FXML private TextArea txtPrescripcion;
    @FXML private Button btnEnviar;
    @FXML private Label lblFeedback;

    // ── Estado interno ──────────────────────────────────
    private String pacienteUid;
    private String pacienteNombre;

    private final MetricaService metricaService = new MetricaService(new MetricaRepositoryImpl());
    private final NotaMedicaRepository notaMedicaRepo = new NotaMedicaRepositoryImpl();

    private final ObservableList<MetricaRecord> metricasList = FXCollections.observableArrayList();
    private ListenerRegistration metricasListener;

    // Formateador corto para las etiquetas del eje X de los charts
    private static final DateTimeFormatter FMT_CORTO = DateTimeFormatter.ofPattern("dd/MM HH:mm");

    // ─────────────────────────────────────────────────────

    /**
     * Método invocado por DoctorLayoutController al seleccionar un paciente.
     * Configura las vistas y arranca la escucha de métricas en tiempo real.
     */
    public void inicializarPaciente(String uid, String nombre) {
        this.pacienteUid = uid;
        this.pacienteNombre = nombre;

        // Header
        lblPatientName.setText(nombre != null ? nombre : "Paciente");
        lblPatientUid.setText("UID: " + uid.substring(0, Math.min(uid.length(), 12)) + "…");

        // Configurar tabla
        configurarTabla();

        // Iniciar listener en tiempo real de métricas
        iniciarListenerMetricas();

        // Limpiar feedback y prescripción
        lblFeedback.setText("");
        txtPrescripcion.clear();
    }

    // ── Configuración de la tabla ───────────────────────

    private void configurarTabla() {
        colFecha.setCellValueFactory(cell ->
                new SimpleStringProperty(formatearFechaCorta(cell.getValue().fechaRegistro())));
        colTipo.setCellValueFactory(cell ->
                new SimpleStringProperty(tipoLegible(cell.getValue().tipo())));
        colValPrimario.setCellValueFactory(cell -> {
            Double val = cell.getValue().valorPrimario();
            return new SimpleDoubleProperty(val != null ? val : 0.0).asObject();
        });
        colValSecundario.setCellValueFactory(cell -> {
            Double val = cell.getValue().valorSecundario();
            return new SimpleDoubleProperty(val != null ? val : 0.0).asObject();
        });
        colAlerta.setCellValueFactory(cell ->
                new SimpleStringProperty(cell.getValue().alerta() ? "⚠ Sí" : "✓ No"));

        tablaMetricas.setItems(metricasList);
        tablaMetricas.setPlaceholder(new Label("Cargando métricas del paciente…"));
    }

    // ── Listener en tiempo real ─────────────────────────

    private void iniciarListenerMetricas() {
        metricasListener = metricaService.escucharMetricasPaciente(
                pacienteUid,
                metricas -> Platform.runLater(() -> {
                    metricasList.setAll(metricas);
                    lblMetricCount.setText(metricas.size() + " registros");
                    actualizarCharts(metricas);
                }),
                error -> Platform.runLater(() ->
                        lblFeedback.setText("Error al cargar métricas: " + error.getMessage()))
        );
    }

    // ── Actualización de Charts ─────────────────────────

    private void actualizarCharts(List<MetricaRecord> metricas) {
        // Limpiar todos los charts
        chartPresion.getData().clear();
        chartGlucosa.getData().clear();
        chartFC.getData().clear();
        chartPeso.getData().clear();

        // Separar métricas por tipo y ordenar por fecha
        List<MetricaRecord> presion = metricas.stream()
                .filter(m -> "PRESION_ARTERIAL".equals(m.tipo()))
                .sorted(Comparator.comparing(MetricaRecord::fechaRegistro, Comparator.nullsLast(Comparator.naturalOrder())))
                .toList();

        List<MetricaRecord> glucosa = metricas.stream()
                .filter(m -> "GLUCOSA".equals(m.tipo()))
                .sorted(Comparator.comparing(MetricaRecord::fechaRegistro, Comparator.nullsLast(Comparator.naturalOrder())))
                .toList();

        List<MetricaRecord> fc = metricas.stream()
                .filter(m -> "FRECUENCIA_CARDIACA".equals(m.tipo()))
                .sorted(Comparator.comparing(MetricaRecord::fechaRegistro, Comparator.nullsLast(Comparator.naturalOrder())))
                .toList();

        List<MetricaRecord> peso = metricas.stream()
                .filter(m -> "PESO_IMC".equals(m.tipo()))
                .sorted(Comparator.comparing(MetricaRecord::fechaRegistro, Comparator.nullsLast(Comparator.naturalOrder())))
                .toList();

        // ── Chart: Presión Arterial (Sistólica + Diastólica)
        if (!presion.isEmpty()) {
            XYChart.Series<String, Number> serSistolica = new XYChart.Series<>();
            serSistolica.setName("Sistólica");
            XYChart.Series<String, Number> serDiastolica = new XYChart.Series<>();
            serDiastolica.setName("Diastólica");

            for (MetricaRecord m : presion) {
                String label = formatearFechaCorta(m.fechaRegistro());
                serSistolica.getData().add(new XYChart.Data<>(label, m.valorPrimario() != null ? m.valorPrimario() : 0));
                serDiastolica.getData().add(new XYChart.Data<>(label, m.valorSecundario() != null ? m.valorSecundario() : 0));
            }

            chartPresion.getData().addAll(serSistolica, serDiastolica);
        }

        // ── Chart: Glucosa
        if (!glucosa.isEmpty()) {
            XYChart.Series<String, Number> serGlucosa = new XYChart.Series<>();
            serGlucosa.setName("Glucosa (mg/dL)");

            for (MetricaRecord m : glucosa) {
                String label = formatearFechaCorta(m.fechaRegistro());
                serGlucosa.getData().add(new XYChart.Data<>(label, m.valorPrimario() != null ? m.valorPrimario() : 0));
            }

            chartGlucosa.getData().add(serGlucosa);
        }

        // ── Chart: Frecuencia Cardíaca
        if (!fc.isEmpty()) {
            XYChart.Series<String, Number> serFC = new XYChart.Series<>();
            serFC.setName("BPM");

            for (MetricaRecord m : fc) {
                String label = formatearFechaCorta(m.fechaRegistro());
                serFC.getData().add(new XYChart.Data<>(label, m.valorPrimario() != null ? m.valorPrimario() : 0));
            }

            chartFC.getData().add(serFC);
        }

        // ── Chart: Peso / IMC
        if (!peso.isEmpty()) {
            XYChart.Series<String, Number> serPeso = new XYChart.Series<>();
            serPeso.setName("IMC");

            for (MetricaRecord m : peso) {
                String label = formatearFechaCorta(m.fechaRegistro());
                // valorPrimario = IMC para PESO_IMC según MetricaService
                serPeso.getData().add(new XYChart.Data<>(label, m.valorPrimario() != null ? m.valorPrimario() : 0));
            }

            chartPeso.getData().add(serPeso);
        }
    }

    // ── Enviar prescripción / nota médica ───────────────

    @FXML
    private void handleEnviarNota() {
        String contenido = txtPrescripcion.getText();

        if (contenido == null || contenido.isBlank()) {
            lblFeedback.setText("⚠ Escribe una nota antes de enviar.");
            lblFeedback.getStyleClass().removeAll("feedback-success");
            lblFeedback.getStyleClass().add("feedback-warning");
            return;
        }

        btnEnviar.setDisable(true);
        lblFeedback.setText("Enviando…");
        lblFeedback.getStyleClass().removeAll("feedback-success", "feedback-warning");

        String doctorNombre = UserSession.getInstance().getLoggedUser().nombre();
        String fecha = LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME);

        NotaMedicaRecord nota = new NotaMedicaRecord(
                null,
                pacienteUid,
                doctorNombre,
                contenido,
                fecha
        );

        CompletableFuture.runAsync(() -> {
            try {
                notaMedicaRepo.guardar(pacienteUid, nota);
                Platform.runLater(() -> {
                    lblFeedback.setText("✅ Nota enviada exitosamente al paciente.");
                    lblFeedback.getStyleClass().removeAll("feedback-warning");
                    lblFeedback.getStyleClass().add("feedback-success");
                    txtPrescripcion.clear();
                    btnEnviar.setDisable(false);
                });
            } catch (Exception e) {
                Platform.runLater(() -> {
                    lblFeedback.setText("❌ Error al enviar: " + e.getMessage());
                    lblFeedback.getStyleClass().removeAll("feedback-success");
                    lblFeedback.getStyleClass().add("feedback-warning");
                    btnEnviar.setDisable(false);
                });
            }
        });
    }

    // ── Limpieza de listeners (ModuleLimpiable) ─────────

    @Override
    public void detenerListeners() {
        if (metricasListener != null) {
            metricasListener.remove();
            metricasListener = null;
        }
    }

    // ── Utilidades ──────────────────────────────────────

    /**
     * Convierte un ISO date-time a un formato corto para etiquetas.
     */
    private String formatearFechaCorta(String isoDate) {
        if (isoDate == null || isoDate.isEmpty()) return "N/A";
        try {
            LocalDateTime dt = LocalDateTime.parse(isoDate);
            return dt.format(FMT_CORTO);
        } catch (DateTimeParseException e) {
            // Si no es parseable, devolver los primeros 16 caracteres
            return isoDate.length() > 16 ? isoDate.substring(0, 16) : isoDate;
        }
    }

    /**
     * Convierte el tipo interno de métrica a un nombre legible.
     */
    private String tipoLegible(String tipo) {
        if (tipo == null) return "—";
        return switch (tipo) {
            case "PRESION_ARTERIAL"    -> "Presión Arterial";
            case "GLUCOSA"             -> "Glucosa";
            case "FRECUENCIA_CARDIACA" -> "Frec. Cardíaca";
            case "PESO_IMC"            -> "Peso / IMC";
            default                    -> tipo;
        };
    }
}
