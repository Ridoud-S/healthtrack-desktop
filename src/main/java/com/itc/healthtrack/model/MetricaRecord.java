package com.itc.healthtrack.model;

public record MetricaRecord(
        String id,
        String tipo,
        Double valorPrimario,
        Double valorSecundario,
        String fechaRegistro
) {}