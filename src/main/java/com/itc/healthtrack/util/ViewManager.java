package com.itc.healthtrack.util;

import com.itc.healthtrack.App;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.layout.Pane;
import javafx.stage.Stage;

import java.io.IOException;

public class ViewManager {

    private ViewManager() {}

    /**
     * Reemplaza la escena completa del Stage actual.
     * Usar para transiciones grandes (ej: Login → Shell del paciente).
     *
     * @param fxmlPath Ruta relativa al FXML dentro del classpath.
     * @param title    Título de la ventana.
     * @param node     Cualquier nodo que pertenezca al Stage actual.
     */
    public static void switchScene(String fxmlPath, String title, Node node) {
        try {
            FXMLLoader loader = new FXMLLoader(App.class.getClassLoader().getResource(fxmlPath));
            Parent root = loader.load();
            Stage stage = (Stage) node.getScene().getWindow();
            stage.setScene(new Scene(root));
            stage.setTitle(title);
        } catch (IOException e) {
            throw new RuntimeException("No se pudo cargar la vista: " + fxmlPath, e);
        }
    }

    /**
     * Inyecta un FXML dentro de un contenedor (Pane) existente,
     * reemplazando su contenido anterior. El Stage y el Sidebar NO se recargan.
     * Usar para navegación interna dentro del Shell (ej: cambiar módulo activo).
     *
     * @param fxmlPath    Ruta relativa al FXML dentro del classpath.
     * @param contentArea El Pane central donde se inyectará la nueva vista.
     *
     * @example ViewManager.loadViewIntoPane("views/patient_summary.fxml", contentArea);
     */
    public static void loadViewIntoPane(String fxmlPath, Pane contentArea) {
        try {
            FXMLLoader loader = new FXMLLoader(App.class.getClassLoader().getResource(fxmlPath));
            Node view = loader.load();

            contentArea.getChildren().setAll(view);

            // Si el contenedor es un AnchorPane, forzamos que el hijo ocupe todo el espacio
            if (contentArea instanceof javafx.scene.layout.AnchorPane anchorPane) {
                javafx.scene.layout.AnchorPane.setTopAnchor(view, 0.0);
                javafx.scene.layout.AnchorPane.setBottomAnchor(view, 0.0);
                javafx.scene.layout.AnchorPane.setLeftAnchor(view, 0.0);
                javafx.scene.layout.AnchorPane.setRightAnchor(view, 0.0);
            }

        } catch (IOException e) {
            throw new RuntimeException("No se pudo inyectar la vista en el panel: " + fxmlPath, e);
        }
    }

    /**
     * @param fxmlPath    Ruta relativa al FXML dentro del classpath.
     * @param contentArea El Pane central donde se inyectará la nueva vista.
     * @param <T>         Tipo del controlador esperado.
     * @return            El controlador del FXML recién cargado.
     *
     * @example BloodPressureController ctrl =
     *              ViewManager.loadViewIntoPane("views/patient_blood_pressure.fxml", contentArea);
     *          ctrl.setPatient(currentPatient);
     */
    public static <T> T loadViewIntoPaneWithController(String fxmlPath, Pane contentArea) {
        try {
            FXMLLoader loader = new FXMLLoader(App.class.getClassLoader().getResource(fxmlPath));
            Node view = loader.load();

            contentArea.getChildren().setAll(view);

            if (contentArea instanceof javafx.scene.layout.AnchorPane anchorPane) {
                javafx.scene.layout.AnchorPane.setTopAnchor(view, 0.0);
                javafx.scene.layout.AnchorPane.setBottomAnchor(view, 0.0);
                javafx.scene.layout.AnchorPane.setLeftAnchor(view, 0.0);
                javafx.scene.layout.AnchorPane.setRightAnchor(view, 0.0);
            }

            return loader.getController();

        } catch (IOException e) {
            throw new RuntimeException("No se pudo inyectar la vista en el panel: " + fxmlPath, e);
        }
    }
}