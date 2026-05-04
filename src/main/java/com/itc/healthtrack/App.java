package com.itc.healthtrack;

import com.itc.healthtrack.config.FirebaseConfig;
import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

import java.io.IOException;

public class App extends Application {

    @Override
    public void start(Stage stage) {
        FirebaseConfig.getInstance().initFirebase();

        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/login.fxml"));
            Parent root = loader.load();
            Scene scene = new Scene(root);
            stage.setScene(scene);
            stage.show();
        } catch (IOException e) {
            throw new RuntimeException("No se pudo cargar login.fxml", e);
        }
    }

    public static void main(String[] args) {
        launch(args);
    }
}