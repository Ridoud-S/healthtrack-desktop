package com.itc.healthtrack.service;

import com.google.cloud.firestore.DocumentSnapshot;
import com.google.firebase.cloud.FirestoreClient;
import com.itc.healthtrack.model.Role;
import com.itc.healthtrack.model.User;
import com.itc.healthtrack.session.UserSession;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.HashMap;
import java.util.Map;

public class AuthService {

    private static final String API_KEY = "AIzaSyC416vySXBRm1zOp-Xu1gz__Hm159I1AGI";
    private static final String SIGN_IN_URL =
            "https://identitytoolkit.googleapis.com/v1/accounts:signInWithPassword?key=" + API_KEY;
    private static final String SIGN_UP_URL =
            "https://identitytoolkit.googleapis.com/v1/accounts:signUp?key=" + API_KEY;

    private final HttpClient httpClient = HttpClient.newHttpClient();

    public void login(String email, String password) throws Exception {
        String uid = authenticateWithRestApi(email, password);
        User user = fetchUserFromFirestore(uid);
        UserSession.getInstance().setLoggedUser(user);
    }

    public void register(String email, String password, String nombre, Role role) throws Exception {
        String uid = registerWithRestApi(email, password);
        saveUserInFirestore(uid, nombre, email, role);
    }

    private String authenticateWithRestApi(String email, String password) throws Exception {
        String body = String.format(
                "{\"email\":\"%s\",\"password\":\"%s\",\"returnSecureToken\":true}",
                email, password
        );

        HttpResponse<String> response = sendPost(SIGN_IN_URL, body);

        if (response.statusCode() == 200) {
            return extractField(response.body(), "localId");
        }

        throw new Exception("Credenciales inválidas: " + extractNestedField(response.body(), "message"));
    }

    private String registerWithRestApi(String email, String password) throws Exception {
        String body = String.format(
                "{\"email\":\"%s\",\"password\":\"%s\",\"returnSecureToken\":true}",
                email, password
        );

        HttpResponse<String> response = sendPost(SIGN_UP_URL, body);

        if (response.statusCode() == 200) {
            return extractField(response.body(), "localId");
        }

        throw new Exception("Error al registrar: " + extractNestedField(response.body(), "message"));
    }

    private User fetchUserFromFirestore(String uid) throws Exception {
        DocumentSnapshot doc = FirestoreClient.getFirestore()
                .collection("users")
                .document(uid)
                .get()
                .get();

        if (!doc.exists()) {
            throw new Exception("Usuario autenticado pero sin perfil en Firestore. Contacta al administrador.");
        }

        return mapDocumentToUser(uid, doc);
    }

    private void saveUserInFirestore(String uid, String nombre, String email, Role role) throws Exception {
        Map<String, Object> data = new HashMap<>();
        data.put("nombre", nombre);
        data.put("email", email);
        data.put("role", role.name());
        data.put("medicoAsignadoUid", null);

        FirestoreClient.getFirestore()
                .collection("users")
                .document(uid)
                .set(data)
                .get();
    }

    private User mapDocumentToUser(String uid, DocumentSnapshot doc) throws Exception {
        String nombre            = doc.getString("nombre");
        String email             = doc.getString("email");
        String roleRaw           = doc.getString("role");
        String medicoAsignadoUid = doc.getString("medicoAsignadoUid");

        if (roleRaw == null) throw new Exception("El perfil del usuario no tiene un rol asignado.");

        Role role = Role.valueOf(roleRaw.toUpperCase());
        return new User(uid, nombre, email, role, medicoAsignadoUid);
    }

    private HttpResponse<String> sendPost(String url, String body) throws Exception {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(body))
                .build();

        return httpClient.send(request, HttpResponse.BodyHandlers.ofString());
    }

    private String extractField(String json, String field) throws Exception {
        String key = "\"" + field + "\"";
        int keyIndex = json.indexOf(key);
        if (keyIndex == -1) throw new Exception("Campo '" + field + "' no encontrado en la respuesta.");

        int colonIndex = json.indexOf(":", keyIndex);
        int quoteStart = json.indexOf("\"", colonIndex);
        if (quoteStart == -1) throw new Exception("Valor de '" + field + "' no encontrado.");

        quoteStart += 1;
        int quoteEnd = json.indexOf("\"", quoteStart);
        return json.substring(quoteStart, quoteEnd);
    }

    private String extractNestedField(String json, String field) {
        try {
            return extractField(json, field);
        } catch (Exception e) {
            return "Error desconocido.";
        }
    }
}