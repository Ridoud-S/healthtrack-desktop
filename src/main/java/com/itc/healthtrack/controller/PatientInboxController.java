package com.itc.healthtrack.controller;

import com.itc.healthtrack.model.NotaMedicaRecord;
import com.itc.healthtrack.repository.NotaMedicaRepository;
import com.itc.healthtrack.repository.NotaMedicaRepositoryImpl;
import com.itc.healthtrack.session.UserSession;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.control.TextArea;
import javafx.scene.layout.VBox;

import java.net.URL;
import java.util.List;
import java.util.ResourceBundle;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

/**
 * Controlador del Buzón del Paciente (Inbox).
 *
 * Muestra las notas médicas / recetas enviadas por los doctores
 * en un layout de bandeja de entrada (master-detail):
 *   - Izquierda: ListView con fecha y nombre del doctor.
 *   - Derecha: TextArea de solo lectura con el contenido de la nota.
 *
 * Implementa {@link ModuleLimpiable} para detener el polling
 * cuando el paciente navega a otro módulo.
 */
public class PatientInboxController implements Initializable, ModuleLimpiable {

    // ── FXML Bindings ─────────────────────────────────────
    @FXML private ListView<NotaMedicaRecord> listNotas;
    @FXML private TextArea txtContenido;
    @FXML private Label lblEmpty;
    @FXML private Label lblNotaHeader;
    @FXML private Label lblTotalNotas;
    @FXML private VBox detailPane;

    // ── Datos ─────────────────────────────────────────────
    private final ObservableList<NotaMedicaRecord> notas = FXCollections.observableArrayList();
    private final NotaMedicaRepository notaMedicaRepository = new NotaMedicaRepositoryImpl();

    // ── Polling para actualización en tiempo real ─────────
    private ScheduledExecutorService scheduler;
    private ScheduledFuture<?> pollingTask;

    // ─────────────────────────────────────────────────────

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        // Configurar el ListView con un CellFactory personalizado
        listNotas.setItems(notas);
        listNotas.setCellFactory(lv -> new NotaListCell());

        // Listener de selección: al elegir una nota, mostrar su contenido
        listNotas.getSelectionModel().selectedItemProperty().addListener(
                (obs, oldVal, newVal) -> mostrarDetalle(newVal)
        );

        // Estado inicial del panel de detalle
        txtContenido.setEditable(false);
        txtContenido.setWrapText(true);
        mostrarDetalle(null);

        // Carga inicial + polling cada 15 segundos
        cargarNotas();
        iniciarPolling();
    }

    // ── Carga de datos desde Firestore ───────────────────

    private void cargarNotas() {
        String uid = UserSession.getInstance().getLoggedUser().uid();

        Thread hilo = new Thread(() -> {
            try {
                List<NotaMedicaRecord> resultado = notaMedicaRepository.obtenerPorPaciente(uid);
                Platform.runLater(() -> {
                    notas.setAll(resultado);
                    lblTotalNotas.setText("(" + resultado.size() + ")");

                    if (resultado.isEmpty()) {
                        lblEmpty.setVisible(true);
                        lblEmpty.setManaged(true);
                    } else {
                        lblEmpty.setVisible(false);
                        lblEmpty.setManaged(false);
                        // Seleccionar la primera nota si no hay selección
                        if (listNotas.getSelectionModel().getSelectedItem() == null) {
                            listNotas.getSelectionModel().selectFirst();
                        }
                    }
                });
            } catch (Exception e) {
                Platform.runLater(() -> {
                    System.err.println("Error al cargar notas médicas: " + e.getMessage());
                    lblEmpty.setText("Error al cargar mensajes");
                    lblEmpty.setVisible(true);
                    lblEmpty.setManaged(true);
                });
            }
        });
        hilo.setDaemon(true);
        hilo.start();
    }

    // ── Polling automático ───────────────────────────────

    private void iniciarPolling() {
        scheduler = Executors.newSingleThreadScheduledExecutor(r -> {
            Thread t = new Thread(r, "inbox-polling");
            t.setDaemon(true);
            return t;
        });

        pollingTask = scheduler.scheduleAtFixedRate(
                this::cargarNotas, 15, 15, TimeUnit.SECONDS
        );
    }

    // ── Mostrar el detalle de la nota seleccionada ───────

    private void mostrarDetalle(NotaMedicaRecord nota) {
        if (nota == null) {
            lblNotaHeader.setText("Selecciona un mensaje");
            txtContenido.setText("");
            txtContenido.setPromptText("Elige una nota de la lista para ver su contenido.");
            return;
        }

        lblNotaHeader.setText("Dr(a). " + nota.doctorNombre() + "  —  " + nota.fecha());
        txtContenido.setText(nota.contenido());
    }

    // ── CellFactory personalizado ─────────────────────────

    /**
     * Renderiza cada item del ListView con fecha y nombre del doctor,
     * usando un diseño limpio sin emojis.
     */
    private static class NotaListCell extends ListCell<NotaMedicaRecord> {
        @Override
        protected void updateItem(NotaMedicaRecord nota, boolean empty) {
            super.updateItem(nota, empty);
            if (empty || nota == null) {
                setText(null);
                setGraphic(null);
                return;
            }

            VBox cellBox = new VBox(4);
            cellBox.getStyleClass().add("nota-cell-box");

            Label lblFecha = new Label(nota.fecha());
            lblFecha.getStyleClass().add("nota-cell-fecha");

            Label lblDoctor = new Label("Dr(a). " + nota.doctorNombre());
            lblDoctor.getStyleClass().add("nota-cell-doctor");

            // Extracto del contenido (máx 60 chars)
            String extracto = nota.contenido() != null && nota.contenido().length() > 60
                    ? nota.contenido().substring(0, 60) + "..."
                    : (nota.contenido() != null ? nota.contenido() : "");
            Label lblExtracto = new Label(extracto);
            lblExtracto.getStyleClass().add("nota-cell-extracto");

            cellBox.getChildren().addAll(lblFecha, lblDoctor, lblExtracto);
            setGraphic(cellBox);
        }
    }

    // ── ModuleLimpiable ──────────────────────────────────

    @Override
    public void detenerListeners() {
        if (pollingTask != null) {
            pollingTask.cancel(true);
            pollingTask = null;
        }
        if (scheduler != null) {
            scheduler.shutdownNow();
            scheduler = null;
        }
    }
}
