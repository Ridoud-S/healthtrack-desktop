package com.itc.healthtrack.controller;

import com.google.cloud.firestore.ListenerRegistration;
import com.itc.healthtrack.model.MetricaRecord;
import com.itc.healthtrack.repository.MetricaRepository;
import com.itc.healthtrack.repository.MetricaRepositoryImpl;
import com.itc.healthtrack.service.MetricaService;
import com.itc.healthtrack.session.UserSession;
import com.itc.healthtrack.util.ViewManager;
import javafx.application.Platform;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;

import java.net.URL;
import java.util.ResourceBundle;

public class PatientDashboardController implements Initializable {

    @FXML private Label lblWelcome;
    @FXML private Button btnLogout;
    @FXML private TextField txtSistolica;
    @FXML private TextField txtDiastolica;
    @FXML private Button btnGuardarPresion;
    @FXML private TableView<MetricaRecord> tablaMetricas;
    @FXML private TableColumn<MetricaRecord, String> colFecha;
    @FXML private TableColumn<MetricaRecord, String> colTipo;
    @FXML private TableColumn<MetricaRecord, Double> colSistolica;
    @FXML private TableColumn<MetricaRecord, Double> colDiastolica;
    @FXML private TableColumn<MetricaRecord, Boolean> colAlerta;
    @FXML private TableColumn<MetricaRecord, String> colRecomendacion;

    private final ObservableList<MetricaRecord> metricasList = FXCollections.observableArrayList();
    private final MetricaService metricaService = new MetricaService(new MetricaRepositoryImpl());
    private ListenerRegistration listenerRegistration;

    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        String nombre = UserSession.getInstance().getLoggedUser().nombre();
        lblWelcome.setText("Hola, " + nombre + " 👋");

        configurarTabla();
        iniciarListener();
    }

    private void configurarTabla() {
        colFecha.setCellValueFactory(cell ->
                new SimpleStringProperty(cell.getValue().fechaRegistro()));
        colTipo.setCellValueFactory(cell ->
                new SimpleStringProperty(cell.getValue().tipo()));
        colSistolica.setCellValueFactory(cell ->
                new SimpleDoubleProperty(cell.getValue().valorPrimario()).asObject());
        colDiastolica.setCellValueFactory(cell ->
                new SimpleDoubleProperty(cell.getValue().valorSecundario()).asObject());
        colAlerta.setCellValueFactory(cell ->
                new SimpleBooleanProperty(cell.getValue().alerta()).asObject());
        colRecomendacion.setCellValueFactory(cell ->
                new SimpleStringProperty(cell.getValue().recomendacion()));

        tablaMetricas.setItems(metricasList);
        tablaMetricas.setPlaceholder(new Label("No hay métricas registradas."));
    }

    private void iniciarListener() {
        String uid = UserSession.getInstance().getLoggedUser().uid();

        listenerRegistration = metricaService.escucharMetricasPaciente(
                uid,
                metricas -> Platform.runLater(() -> metricasList.setAll(metricas)),
                error    -> Platform.runLater(() ->
                        showAlert(Alert.AlertType.ERROR, "Error en tiempo real", error.getMessage()))
        );
    }

    @FXML
    private void onGuardarPresionPressed() {
        String sistolicaRaw  = txtSistolica.getText().trim();
        String diastolicaRaw = txtDiastolica.getText().trim();

        if (sistolicaRaw.isEmpty() || diastolicaRaw.isEmpty()) {
            showAlert(Alert.AlertType.WARNING, "Campos requeridos",
                    "Ingresa los valores de presión sistólica y diastólica.");
            return;
        }

        double sistolica;
        double diastolica;

        try {
            sistolica  = Double.parseDouble(sistolicaRaw);
            diastolica = Double.parseDouble(diastolicaRaw);
        } catch (NumberFormatException e) {
            showAlert(Alert.AlertType.WARNING, "Formato inválido",
                    "Los valores deben ser números (ej. 120, 80).");
            return;
        }

        String uid = UserSession.getInstance().getLoggedUser().uid();

        btnGuardarPresion.setDisable(true);

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
            btnGuardarPresion.setDisable(false);
        });

        task.setOnFailed(event -> {
            showAlert(Alert.AlertType.ERROR, "Error al guardar",
                    task.getException().getMessage());
            btnGuardarPresion.setDisable(false);
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

    private void showAlert(Alert.AlertType type, String title, String message) {
        Alert alert = new Alert(type);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
}