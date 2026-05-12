package com.itc.healthtrack.controller;

import com.google.cloud.firestore.ListenerRegistration;
import com.itc.healthtrack.model.MetricaRecord;
import com.itc.healthtrack.repository.MetricaRepositoryImpl;
import com.itc.healthtrack.service.MetricaService;
import com.itc.healthtrack.session.UserSession;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;

import java.net.URL;
import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.ResourceBundle;
import java.util.stream.Collectors;

/**
 * Controlador del módulo de Resumen General del paciente.
 *
 * Muestra un cockpit de salud con:
 *  - 4 Metric Cards (PA, Glucosa, Peso/IMC, Frec. Cardíaca)
 *  - Panel de Alertas Activas
 *
 * Implementa ModuleLimpiable para que el Shell pueda
 * detener el listener de Firestore al cambiar de módulo.
 */
public class PatientSummaryController implements Initializable, ModuleLimpiable {

    // ── Header ─────────────────────────────────────────────
    @FXML private Label lblSaludo;

    // ── Metric Cards ───────────────────────────────────────
    @FXML private VBox cardPa;
    @FXML private Label lblPaValue;
    @FXML private Label lblPaStatus;

    @FXML private VBox cardGlucosa;
    @FXML private Label lblGlucosaValue;
    @FXML private Label lblGlucosaStatus;

    @FXML private VBox cardPeso;
    @FXML private Label lblPesoValue;
    @FXML private Label lblPesoStatus;

    @FXML private VBox cardFc;
    @FXML private Label lblFcValue;
    @FXML private Label lblFcStatus;

    // ── Alertas ────────────────────────────────────────────
    @FXML private Label lblAlertasCount;
    @FXML private VBox alertasContainer;

    // ── Dependencias ───────────────────────────────────────
    private final MetricaService metricaService =
            new MetricaService(new MetricaRepositoryImpl());
    private ListenerRegistration listenerRegistration;

    // ─────────────────────────────────────────────────────

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        // Saludo contextual según la hora del día
        String nombre = UserSession.getInstance().getLoggedUser().nombre();
        int hora = LocalDateTime.now().getHour();
        String saludo = hora < 12 ? "Buenos días" : hora < 18 ? "Buenas tardes" : "Buenas noches";
        lblSaludo.setText(saludo + ", " + nombre + " \uD83D\uDC4B");

        // Un solo listener para TODAS las métricas del paciente
        String uid = UserSession.getInstance().getLoggedUser().uid();
        listenerRegistration = metricaService.escucharMetricasPaciente(
                uid,
                metricas -> Platform.runLater(() -> {
                    actualizarCards(metricas);
                    actualizarAlertas(metricas);
                }),
                error -> Platform.runLater(() ->
                        System.err.println("Error en listener de resumen: " + error.getMessage()))
        );
    }

    // ── Actualización de Metric Cards ──────────────────────

    private void actualizarCards(List<MetricaRecord> metricas) {
        actualizarCard("PRESION_ARTERIAL",    metricas, lblPaValue,      lblPaStatus,      cardPa,      true);
        actualizarCard("GLUCOSA",             metricas, lblGlucosaValue, lblGlucosaStatus, cardGlucosa, false);
        actualizarCard("PESO_IMC",            metricas, lblPesoValue,    lblPesoStatus,    cardPeso,    false);
        actualizarCard("FRECUENCIA_CARDIACA", metricas, lblFcValue,      lblFcStatus,      cardFc,      false);
    }

    /**
     * Actualiza una Metric Card individual con el último registro
     * del tipo indicado.
     *
     * @param tipo       Tipo de métrica (ej. "PRESION_ARTERIAL")
     * @param metricas   Lista completa de métricas del paciente
     * @param lblValue   Label que muestra el valor numérico
     * @param lblStatus  Label que muestra el estado (Normal / Alerta)
     * @param card       VBox de la card (para aplicar estilo condicional)
     * @param dualValue  true si la métrica usa valorPrimario + valorSecundario
     */
    private void actualizarCard(String tipo, List<MetricaRecord> metricas,
                                Label lblValue, Label lblStatus, VBox card,
                                boolean dualValue) {

        Optional<MetricaRecord> ultimo = metricas.stream()
                .filter(m -> tipo.equals(m.tipo()))
                .max(Comparator.comparing(MetricaRecord::fechaRegistro));

        if (ultimo.isPresent()) {
            MetricaRecord m = ultimo.get();

            // Valor numérico
            if (dualValue) {
                lblValue.setText(fmt(m.valorPrimario()) + " / " + fmt(m.valorSecundario()));
            } else {
                lblValue.setText(fmt(m.valorPrimario()));
            }

            // Estado visual
            card.getStyleClass().removeAll("metric-card-normal", "metric-card-alert");
            lblStatus.getStyleClass().removeAll("status-normal", "status-alert");

            if (m.alerta()) {
                lblStatus.setText("⚠ Alerta");
                lblStatus.getStyleClass().add("status-alert");
                card.getStyleClass().add("metric-card-alert");
            } else {
                lblStatus.setText("✅ Normal");
                lblStatus.getStyleClass().add("status-normal");
                card.getStyleClass().add("metric-card-normal");
            }
        } else {
            lblValue.setText("--");
            lblStatus.setText("Sin datos");
            card.getStyleClass().removeAll("metric-card-normal", "metric-card-alert");
            lblStatus.getStyleClass().removeAll("status-normal", "status-alert");
        }
    }

    // ── Actualización del Panel de Alertas ─────────────────

    private void actualizarAlertas(List<MetricaRecord> metricas) {
        List<MetricaRecord> alertas = metricas.stream()
                .filter(MetricaRecord::alerta)
                .sorted(Comparator.comparing(MetricaRecord::fechaRegistro).reversed())
                .collect(Collectors.toList());

        lblAlertasCount.setText("(" + alertas.size() + ")");
        alertasContainer.getChildren().clear();

        if (alertas.isEmpty()) {
            Label sinAlertas = new Label("\uD83C\uDF89 No hay alertas activas. ¡Todo en orden!");
            sinAlertas.getStyleClass().add("no-alertas-label");
            alertasContainer.getChildren().add(sinAlertas);
            return;
        }

        for (MetricaRecord alerta : alertas) {
            alertasContainer.getChildren().add(crearAlertaItem(alerta));
        }
    }

    private HBox crearAlertaItem(MetricaRecord metrica) {
        HBox item = new HBox(12);
        item.getStyleClass().add("alert-item");
        item.setAlignment(Pos.CENTER_LEFT);
        item.setPadding(new Insets(12, 16, 12, 16));

        // Icono
        Label icon = new Label("⚠");
        icon.getStyleClass().add("alert-item-icon");

        // Contenido
        VBox info = new VBox(4);

        String tipoLegible = switch (metrica.tipo()) {
            case "PRESION_ARTERIAL"    -> "Presión Arterial";
            case "GLUCOSA"             -> "Glucosa";
            case "PESO_IMC"            -> "Peso / IMC";
            case "FRECUENCIA_CARDIACA" -> "Frecuencia Cardíaca";
            default                    -> metrica.tipo();
        };

        String valorTexto = metrica.valorSecundario() != null
                ? fmt(metrica.valorPrimario()) + "/" + fmt(metrica.valorSecundario())
                : fmt(metrica.valorPrimario());

        Label titulo = new Label(metrica.fechaRegistro() + " — " + tipoLegible + " (" + valorTexto + ")");
        titulo.getStyleClass().add("alert-item-title");

        Label recomendacion = new Label("→ " +
                (metrica.recomendacion() != null ? metrica.recomendacion() : "Sin recomendación"));
        recomendacion.getStyleClass().add("alert-item-recommendation");
        recomendacion.setWrapText(true);

        info.getChildren().addAll(titulo, recomendacion);
        item.getChildren().addAll(icon, info);

        return item;
    }

    // ── Utilidades ─────────────────────────────────────────

    /**
     * Formatea un Double eliminando el .0 si es entero.
     */
    private String fmt(Double value) {
        if (value == null) return "--";
        if (value == Math.floor(value)) return String.valueOf(value.intValue());
        return String.format("%.1f", value);
    }

    // ── ModuleLimpiable ────────────────────────────────────

    @Override
    public void detenerListeners() {
        if (listenerRegistration != null) {
            listenerRegistration.remove();
            listenerRegistration = null;
        }
    }
}
