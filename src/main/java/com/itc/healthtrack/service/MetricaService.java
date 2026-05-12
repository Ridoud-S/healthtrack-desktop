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
        } else if ("GLUCOSA".equals(tipo)) {
            if (v1 > 126) {
                alerta = true;
                recomendacion = "Glucosa elevada en ayunas. Consulta con endocrinólogo.";
            }
        } else if ("PESO_IMC".equals(tipo)) {
            if (v1 > 30) {
                alerta = true;
                recomendacion = "IMC elevado (obesidad). Considera cambios en dieta y ejercicio.";
            }
        } else if ("FRECUENCIA_CARDIACA".equals(tipo)) {
            if (v1 > 100) {
                alerta = true;
                recomendacion = "Taquicardia detectada. Mantente en reposo y monitorea.";
            } else if (v1 < 60) {
                alerta = true;
                recomendacion = "Bradicardia detectada. Consulta con cardiólogo si hay síntomas.";
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