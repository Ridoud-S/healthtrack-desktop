package com.itc.healthtrack.service;

import com.google.cloud.firestore.ListenerRegistration;
import com.itc.healthtrack.model.MetricaRecord;
import com.itc.healthtrack.repository.MetricaRepository;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.function.Consumer;

public class MetricaService {

    private final MetricaRepository metricaRepository;

    public MetricaService(MetricaRepository metricaRepository) {
        this.metricaRepository = metricaRepository;
    }

    public void validarYGuardar(String uid, String tipo, Double v1, Double v2) throws Exception {
        boolean alerta = false;
        String recomendacion = null;

        if ("PRESION_ARTERIAL".equals(tipo)) {
            if (v1 > 135 || v2 > 85) {
                alerta = true;
                recomendacion = "Presión elevada. Reducir consumo de sal y monitorear.";
            }
        }

        MetricaRecord metrica = new MetricaRecord(
                null,
                tipo,
                v1,
                v2,
                nowIso(),
                alerta,
                recomendacion
        );

        metricaRepository.save(uid, metrica);
    }

    public List<MetricaRecord> obtenerMetricasPorUid(String uid) throws Exception {
        return metricaRepository.findAllByUid(uid);
    }

    public ListenerRegistration escucharMetricasPaciente(
            String uidPaciente,
            Consumer<List<MetricaRecord>> onUpdate,
            Consumer<Exception> onError) {

        return metricaRepository.listenByUid(uidPaciente, metricas -> {
            try {
                onUpdate.accept(metricas);
            } catch (Exception e) {
                onError.accept(e);
            }
        });
    }


    public static String nowIso() {
        return LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME);
    }
}