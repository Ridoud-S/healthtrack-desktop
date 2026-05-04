package com.itc.healthtrack.model;

public record User(
        String uid,
        String nombre,
        String email,
        Role role,
        String medicoAsignadoUid
) {}