package com.itc.healthtrack.service;

import com.google.cloud.firestore.DocumentReference;
import com.google.firebase.cloud.FirestoreClient;
import com.itc.healthtrack.model.MetricaRecord;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;

public class MetricaService {

    private static final String COLLECTION_USERS = "users";
    private static final String COLLECTION_METRICAS = "metricas";

    public void guardarMetrica(String uidPaciente, MetricaRecord metrica) throws Exception {
        Map<String, Object> data = new HashMap<>();
        data.put("tipo", metrica.tipo());
        data.put("valorPrimario", metrica.valorPrimario());
        data.put("valorSecundario", metrica.valorSecundario());
        data.put("fechaRegistro", metrica.fechaRegistro());

        FirestoreClient.getFirestore()
                .collection(COLLECTION_USERS)
                .document(uidPaciente)
                .collection(COLLECTION_METRICAS)
                .add(data)
                .get();
    }

    public static String nowIso() {
        return LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME);
    }
}