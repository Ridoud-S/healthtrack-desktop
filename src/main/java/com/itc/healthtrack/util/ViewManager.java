package com.itc.healthtrack.util;

import com.itc.healthtrack.App;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

import java.io.IOException;

public class ViewManager {

    private ViewManager() {}

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
}