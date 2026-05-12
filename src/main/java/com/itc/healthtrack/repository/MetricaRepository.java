package com.itc.healthtrack.repository;

import com.google.cloud.firestore.ListenerRegistration;
import com.itc.healthtrack.model.MetricaRecord;

import java.util.List;
import java.util.function.Consumer;

public interface MetricaRepository {

    void save(String uid, MetricaRecord metrica) throws Exception;

    List<MetricaRecord> findAllByUid(String uid) throws Exception;

    ListenerRegistration listenByUid(String uid, Consumer<List<MetricaRecord>> onUpdate);
}
