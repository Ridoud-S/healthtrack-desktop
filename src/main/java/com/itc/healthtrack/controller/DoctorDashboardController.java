package com.itc.healthtrack.controller;

import com.google.cloud.firestore.ListenerRegistration;
import com.itc.healthtrack.model.MetricaRecord;
import com.itc.healthtrack.model.User;
import com.itc.healthtrack.repository.MetricaRepository;
import com.itc.healthtrack.repository.MetricaRepositoryImpl;
import com.itc.healthtrack.service.MetricaService;
import com.itc.healthtrack.service.UserService;
import com.itc.healthtrack.session.UserSession;
import com.itc.healthtrack.util.ViewManager;
import javafx.application.Platform;
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

public class DoctorDashboardController implements Initializable {

    @FXML private Label lblWelcome;
    @FXML private Button btnLogout;

    @FXML private TableView<User> tablaPacientes;
    @FXML private TableColumn<User, String> colNombre;
    @FXML private TableColumn<User, String> colEmail;

    @FXML private Label lblPacienteSeleccionado;
    @FXML private TableView<MetricaRecord> tablaMetricas;
    @FXML private TableColumn<MetricaRecord, String> colFecha;
    @FXML private TableColumn<MetricaRecord, String> colTipo;
    @FXML private TableColumn<MetricaRecord, Double> colSistolica;
    @FXML private TableColumn<MetricaRecord, Double> colDiastolica;
    @FXML private TableColumn<MetricaRecord, String> colAlerta;
    @FXML private TableColumn<MetricaRecord, String> colRecomendacion;

    private final ObservableList<User> pacientesList = FXCollections.observableArrayList();
    private final ObservableList<MetricaRecord> metricasList = FXCollections.observableArrayList();

    private final UserService userService = new UserService();
    private final MetricaService metricaService = new MetricaService(new MetricaRepositoryImpl());

    private ListenerRegistration listenerRegistration;

    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        String nombre = UserSession.getInstance().getLoggedUser().nombre();
        lblWelcome.setText("Hola, Dr. " + nombre + " 👋");

        configurarTablaPacientes();
        configurarTablaMetricas();
        cargarPacientes();
    }

    private void configurarTablaPacientes() {
        colNombre.setCellValueFactory(cell ->
                new SimpleStringProperty(cell.getValue().nombre()));
        colEmail.setCellValueFactory(cell ->
                new SimpleStringProperty(cell.getValue().email()));

        tablaPacientes.setItems(pacientesList);
        tablaPacientes.setPlaceholder(new Label("No tienes pacientes asignados."));

        tablaPacientes.getSelectionModel().selectedItemProperty().addListener(
                (obs, anterior, seleccionado) -> {
                    if (seleccionado != null) {
                        onPacienteSeleccionado(seleccionado);
                    }
                }
        );
    }

    private void configurarTablaMetricas() {
        colFecha.setCellValueFactory(cell ->
                new SimpleStringProperty(cell.getValue().fechaRegistro()));
        colTipo.setCellValueFactory(cell ->
                new SimpleStringProperty(cell.getValue().tipo()));
        colSistolica.setCellValueFactory(cell ->
                new SimpleDoubleProperty(cell.getValue().valorPrimario()).asObject());
        colDiastolica.setCellValueFactory(cell ->
                new SimpleDoubleProperty(cell.getValue().valorSecundario()).asObject());
        colAlerta.setCellValueFactory(cell ->
                new SimpleStringProperty(cell.getValue().alerta() ? "Sí" : "No"));
        colRecomendacion.setCellValueFactory(cell ->
                new SimpleStringProperty(cell.getValue().recomendacion()));

        tablaMetricas.setItems(metricasList);
        tablaMetricas.setPlaceholder(new Label("Selecciona un paciente para ver su historial."));
    }

    private void cargarPacientes() {
        String medicoUid = UserSession.getInstance().getLoggedUser().uid();

        Task<java.util.List<User>> task = new Task<>() {
            @Override
            protected java.util.List<User> call() throws Exception {
                return userService.obtenerPacientesPorMedico(medicoUid);
            }
        };

        task.setOnSucceeded(event -> pacientesList.setAll(task.getValue()));
        task.setOnFailed(event -> showAlert(Alert.AlertType.ERROR,
                "Error al cargar pacientes", task.getException().getMessage()));

        new Thread(task).start();
    }

    private void onPacienteSeleccionado(User paciente) {
        if (listenerRegistration != null) {
            listenerRegistration.remove();
        }

        lblPacienteSeleccionado.setText("Métricas de: " + paciente.nombre());
        metricasList.clear();

        listenerRegistration = metricaService.escucharMetricasPaciente(
                paciente.uid(),
                metricas -> Platform.runLater(() -> metricasList.setAll(metricas)),
                error    -> Platform.runLater(() -> showAlert(Alert.AlertType.ERROR,
                        "Error en tiempo real", error.getMessage()))
        );
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
