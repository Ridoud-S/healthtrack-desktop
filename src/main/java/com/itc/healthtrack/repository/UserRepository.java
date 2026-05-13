package com.itc.healthtrack.repository;

import com.google.cloud.firestore.ListenerRegistration;
import com.itc.healthtrack.model.User;

import java.util.List;
import java.util.function.Consumer;

/**
 * Contrato DAO para operaciones administrativas sobre la colección /users.
 */
public interface UserRepository {

    /**
     * Obtiene todos los documentos de la colección "users" (snapshot único).
     */
    List<User> obtenerTodosLosUsuarios() throws Exception;

    /**
     * Registra un listener en tiempo real sobre la colección "users".
     * Cada vez que se agregue, modifique o elimine un usuario, el callback
     * {@code onUpdate} recibirá la lista actualizada completa.
     *
     * @return Un {@link ListenerRegistration} que debe liberarse al salir.
     */
    ListenerRegistration escucharUsuarios(Consumer<List<User>> onUpdate);

    /**
     * Actualiza el campo {@code medicoAsignadoUid} del paciente indicado.
     *
     * @param pacienteUid UID del paciente al que se le asigna un médico.
     * @param doctorUid   UID del doctor que se asigna.
     */
    void asignarMedicoAPaciente(String pacienteUid, String doctorUid) throws Exception;
}
