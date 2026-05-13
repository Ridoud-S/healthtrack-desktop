package com.itc.healthtrack.repository;

import com.google.cloud.firestore.Query;
import com.google.cloud.firestore.QueryDocumentSnapshot;
import com.google.firebase.cloud.FirestoreClient;
import com.itc.healthtrack.model.NotaMedicaRecord;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Implementación Firestore de {@link NotaMedicaRepository}.
 *
 * Ruta: users/{pacienteUid}/notas_medicas/{notaId}
 *
 * Sigue el mismo patrón que {@link MetricaRepositoryImpl}.
 */
public class NotaMedicaRepositoryImpl implements NotaMedicaRepository {

    private static final String COLLECTION_USERS        = "users";
    private static final String COLLECTION_NOTAS_MEDICAS = "notas_medicas";

    @Override
    public void guardar(String pacienteUid, NotaMedicaRecord nota) throws Exception {
        Map<String, Object> data = new HashMap<>();
        data.put("pacienteUid",  nota.pacienteUid());
        data.put("doctorNombre", nota.doctorNombre());
        data.put("contenido",    nota.contenido());
        data.put("fecha",        nota.fecha());

        FirestoreClient.getFirestore()
                .collection(COLLECTION_USERS)
                .document(pacienteUid)
                .collection(COLLECTION_NOTAS_MEDICAS)
                .add(data)
                .get(); // bloquea hasta confirmación
    }

    @Override
    public List<NotaMedicaRecord> obtenerPorPaciente(String pacienteUid) throws Exception {
        List<NotaMedicaRecord> notas = new ArrayList<>();

        for (QueryDocumentSnapshot doc : FirestoreClient.getFirestore()
                .collection(COLLECTION_USERS)
                .document(pacienteUid)
                .collection(COLLECTION_NOTAS_MEDICAS)
                .orderBy("fecha", Query.Direction.DESCENDING)
                .get()
                .get()
                .getDocuments()) {

            notas.add(new NotaMedicaRecord(
                    doc.getId(),
                    doc.getString("pacienteUid"),
                    doc.getString("doctorNombre"),
                    doc.getString("contenido"),
                    doc.getString("fecha")
            ));
        }

        return notas;
    }
}
