package com.itc.healthtrack.controller;

/**
 * Contrato que deben implementar los controladores de sub-módulos
 * que mantengan recursos activos (listeners de Firestore, timers, etc.).
 *
 * El PatientLayoutController invoca {@code detenerListeners()} antes de
 * inyectar un módulo nuevo en el contentArea, evitando fugas de memoria.
 */
public interface ModuleLimpiable {

    /**
     * Libera todos los listeners y recursos activos del módulo.
     * Debe ser idempotente (llamarlo varias veces no causa error).
     */
    void detenerListeners();
}
