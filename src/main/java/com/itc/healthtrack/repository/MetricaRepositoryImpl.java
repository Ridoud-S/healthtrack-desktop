package com.itc.healthtrack.repository;

import com.google.cloud.firestore.ListenerRegistration;
import com.google.cloud.firestore.QueryDocumentSnapshot;
import com.google.firebase.cloud.FirestoreClient;
import com.itc.healthtrack.model.MetricaRecord;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

public class MetricaRepositoryImpl implements MetricaRepository {

    private static final String COLLECTION_USERS = "users";
    private static final String COLLECTION_METRICAS = "metricas";

    @Override
    public void save(String uid, MetricaRecord metrica) throws Exception {
        Map<String, Object> data = new HashMap<>();
        data.put("tipo", metrica.tipo());
        data.put("valorPrimario", metrica.valorPrimario());
        data.put("valorSecundario", metrica.valorSecundario());
        data.put("fechaRegistro", metrica.fechaRegistro());
        data.put("alerta", metrica.alerta());
        data.put("recomendacion", metrica.recomendacion());

        FirestoreClient.getFirestore()
                .collection(COLLECTION_USERS)
                .document(uid)
                .collection(COLLECTION_METRICAS)
                .add(data)
                .get();
    }

    @Override
    public List<MetricaRecord> findAllByUid(String uid) throws Exception {
        List<MetricaRecord> metricas = new ArrayList<>();
        FirestoreClient.getFirestore()
                .collection(COLLECTION_USERS)
                .document(uid)
                .collection(COLLECTION_METRICAS)
                .get()
                .get()
                .getDocuments()
                .forEach(doc -> {
                    boolean alerta = Boolean.TRUE.equals(doc.getBoolean("alerta"));
                    String recomendacion = doc.getString("recomendacion");
                    metricas.add(new MetricaRecord(
                            doc.getId(),
                            doc.getString("tipo"),
                            doc.getDouble("valorPrimario"),
                            doc.getDouble("valorSecundario"),
                            doc.getString("fechaRegistro"),
                            alerta,
                            recomendacion
                    ));
                });
        return metricas;
    }

    @Override
    public ListenerRegistration listenByUid(String uid, Consumer<List<MetricaRecord>> onUpdate) {
        return FirestoreClient.getFirestore()
                .collection(COLLECTION_USERS)
                .document(uid)
                .collection(COLLECTION_METRICAS)
                .addSnapshotListener((snapshots, error) -> {
                    if (error != null) {
                        // En este caso, el error se maneja en el servicio
                        return;
                    }

                    if (snapshots == null) return;

                    List<MetricaRecord> metricas = new ArrayList<>();
                    for (QueryDocumentSnapshot doc : snapshots.getDocuments()) {
                        boolean alerta = Boolean.TRUE.equals(doc.getBoolean("alerta"));
                        String recomendacion = doc.getString("recomendacion");
                        metricas.add(new MetricaRecord(
                                doc.getId(),
                                doc.getString("tipo"),
                                doc.getDouble("valorPrimario"),
                                doc.getDouble("valorSecundario"),
                                doc.getString("fechaRegistro"),
                                alerta,
                                recomendacion
                        ));
                    }

                    onUpdate.accept(metricas);
                });
    }
}
