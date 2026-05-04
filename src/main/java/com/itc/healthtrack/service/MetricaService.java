package com.itc.healthtrack.service;

import com.google.cloud.firestore.ListenerRegistration;
import com.google.cloud.firestore.QueryDocumentSnapshot;
import com.google.firebase.cloud.FirestoreClient;
import com.itc.healthtrack.model.MetricaRecord;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

public class MetricaService {

    private static final String COLLECTION_USERS    = "users";
    private static final String COLLECTION_METRICAS = "metricas";

    public void guardarMetrica(String uidPaciente, MetricaRecord metrica) throws Exception {
        Map<String, Object> data = new HashMap<>();
        data.put("tipo",            metrica.tipo());
        data.put("valorPrimario",   metrica.valorPrimario());
        data.put("valorSecundario", metrica.valorSecundario());
        data.put("fechaRegistro",   metrica.fechaRegistro());

        FirestoreClient.getFirestore()
                .collection(COLLECTION_USERS)
                .document(uidPaciente)
                .collection(COLLECTION_METRICAS)
                .add(data)
                .get();
    }

    public ListenerRegistration escucharMetricasPaciente(
            String uidPaciente,
            Consumer<List<MetricaRecord>> onUpdate,
            Consumer<Exception> onError) {

        return FirestoreClient.getFirestore()
                .collection(COLLECTION_USERS)
                .document(uidPaciente)
                .collection(COLLECTION_METRICAS)
                .addSnapshotListener((snapshots, error) -> {
                    if (error != null) {
                        onError.accept(error);
                        return;
                    }

                    if (snapshots == null) return;

                    List<MetricaRecord> metricas = new ArrayList<>();
                    for (QueryDocumentSnapshot doc : snapshots.getDocuments()) {
                        metricas.add(new MetricaRecord(
                                doc.getId(),
                                doc.getString("tipo"),
                                doc.getDouble("valorPrimario"),
                                doc.getDouble("valorSecundario"),
                                doc.getString("fechaRegistro")
                        ));
                    }

                    onUpdate.accept(metricas);
                });
    }

    public static String nowIso() {
        return LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME);
    }
}