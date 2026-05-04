package com.itc.healthtrack.service;

import com.google.cloud.firestore.QueryDocumentSnapshot;
import com.google.firebase.cloud.FirestoreClient;
import com.itc.healthtrack.model.Role;
import com.itc.healthtrack.model.User;

import java.util.ArrayList;
import java.util.List;

public class UserService {

    private static final String COLLECTION_USERS = "users";

    public List<User> obtenerPacientesPorMedico(String medicoUid) throws Exception {
        List<User> pacientes = new ArrayList<>();

        for (QueryDocumentSnapshot doc : FirestoreClient.getFirestore()
                .collection(COLLECTION_USERS)
                .whereEqualTo("role", "PATIENT")
                .whereEqualTo("medicoAsignadoUid", medicoUid)
                .get()
                .get()
                .getDocuments()) {

            pacientes.add(new User(
                    doc.getId(),
                    doc.getString("nombre"),
                    doc.getString("email"),
                    Role.valueOf(doc.getString("role")),
                    doc.getString("medicoAsignadoUid")
            ));
        }

        return pacientes;
    }
}