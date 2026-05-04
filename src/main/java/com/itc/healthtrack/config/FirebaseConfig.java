package com.itc.healthtrack.config;

import com.google.auth.oauth2.GoogleCredentials;
import com.google.firebase.FirebaseApp;
import com.google.firebase.FirebaseOptions;

import com.itc.healthtrack.App;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;

public class FirebaseConfig {

    private static final Logger logger = LoggerFactory.getLogger(FirebaseConfig.class);
    private static FirebaseConfig instance;

    private FirebaseConfig() {}

    public static FirebaseConfig getInstance() {
        if (instance == null) {
            instance = new FirebaseConfig();
        }
        return instance;
    }

    public void initFirebase() {
        if (!FirebaseApp.getApps().isEmpty()) {
            return;
        }

        try (InputStream serviceAccount = App.class.getResourceAsStream("/serviceAccountKey.json")) {
            if (serviceAccount == null) {
                logger.error("No se encontró serviceAccountKey.json en el classpath.");
                return;
            }

            FirebaseOptions options = FirebaseOptions.builder()
                    .setCredentials(GoogleCredentials.fromStream(serviceAccount))
                    .build();

            FirebaseApp.initializeApp(options);
            logger.info("Firebase conectado exitosamente.");

        } catch (IOException e) {
            logger.error("Error al inicializar Firebase: {}", e.getMessage(), e);
        }
    }
}