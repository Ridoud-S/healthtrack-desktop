package com.itc.healthtrack.model;

public record Usuario(
        String uid,
        String nombre,
        String email,
        Role role,
        String medicoAsignadoUid
) {}