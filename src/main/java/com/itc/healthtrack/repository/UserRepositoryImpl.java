package com.itc.healthtrack.repository;

import com.google.cloud.firestore.ListenerRegistration;
import com.google.cloud.firestore.QueryDocumentSnapshot;
import com.google.firebase.cloud.FirestoreClient;
import com.itc.healthtrack.model.Role;
import com.itc.healthtrack.model.User;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

/**
 * Implementación Firestore de {@link UserRepository}.
 * Sigue el mismo patrón que {@code MetricaRepositoryImpl}.
 */
public class UserRepositoryImpl implements UserRepository {

    private static final String COLLECTION_USERS = "users";

    @Override
    public List<User> obtenerTodosLosUsuarios() throws Exception {
        List<User> usuarios = new ArrayList<>();

        for (QueryDocumentSnapshot doc : FirestoreClient.getFirestore()
                .collection(COLLECTION_USERS)
                .get()
                .get()
                .getDocuments()) {

            usuarios.add(mapDocumentToUser(doc));
        }

        return usuarios;
    }

    @Override
    public ListenerRegistration escucharUsuarios(Consumer<List<User>> onUpdate) {
        return FirestoreClient.getFirestore()
                .collection(COLLECTION_USERS)
                .addSnapshotListener((snapshots, error) -> {
                    if (error != null) {
                        // El error se propagará al controlador si es necesario.
                        return;
                    }
                    if (snapshots == null) return;

                    List<User> usuarios = new ArrayList<>();
                    for (QueryDocumentSnapshot doc : snapshots.getDocuments()) {
                        usuarios.add(mapDocumentToUser(doc));
                    }

                    onUpdate.accept(usuarios);
                });
    }

    @Override
    public void asignarMedicoAPaciente(String pacienteUid, String doctorUid) throws Exception {
        Map<String, Object> update = new HashMap<>();
        update.put("medicoAsignadoUid", doctorUid);

        FirestoreClient.getFirestore()
                .collection(COLLECTION_USERS)
                .document(pacienteUid)
                .update(update)
                .get();   // bloquea hasta confirmación
    }

    // ── Mapping helper ──────────────────────────────────────────────
    private User mapDocumentToUser(QueryDocumentSnapshot doc) {
        String roleRaw = doc.getString("role");
        Role role = (roleRaw != null) ? Role.valueOf(roleRaw.toUpperCase()) : Role.PATIENT;

        return new User(
                doc.getId(),
                doc.getString("nombre"),
                doc.getString("email"),
                role,
                doc.getString("medicoAsignadoUid")
        );
    }
}
