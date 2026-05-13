package com.itc.healthtrack.model;

/**
 * Modelo inmutable para las notas médicas / prescripciones
 * que el doctor envía al paciente.
 *
 * Firestore path: users/{pacienteUid}/notas_medicas/{notaId}
 */
public record NotaMedicaRecord(
        String id,
        String pacienteUid,
        String doctorNombre,
        String contenido,
        String fecha
) {}
