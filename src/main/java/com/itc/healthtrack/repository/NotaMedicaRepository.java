package com.itc.healthtrack.repository;

import com.itc.healthtrack.model.NotaMedicaRecord;

import java.util.List;

/**
 * Contrato DAO para operaciones sobre la subcolección
 * users/{pacienteUid}/notas_medicas.
 */
public interface NotaMedicaRepository {

    /**
     * Guarda una nueva nota médica en la subcolección del paciente.
     *
     * @param pacienteUid UID del paciente destino.
     * @param nota        Datos de la nota a guardar.
     */
    void guardar(String pacienteUid, NotaMedicaRecord nota) throws Exception;

    /**
     * Obtiene todas las notas médicas del paciente (snapshot único).
     *
     * @param pacienteUid UID del paciente.
     * @return Lista de notas ordenadas por fecha.
     */
    List<NotaMedicaRecord> obtenerPorPaciente(String pacienteUid) throws Exception;
}
