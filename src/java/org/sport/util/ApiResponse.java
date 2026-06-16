package org.sport.util;

import jakarta.ws.rs.core.Response;
import java.util.LinkedHashMap;
import java.util.Map;


//Crea respuestas en JSON uniformes
public class ApiResponse {
    private ApiResponse() {
    }

    public static Response ok(Object data) {
        return build(Response.Status.OK, true, "Operacion exitosa", data);
    }

    public static Response created(Object data) {
        return build(Response.Status.CREATED, true, "Registro creado", data);
    }

    public static Response message(String message) {
        return build(Response.Status.OK, true, message, null);
    }

    public static Response badRequest(String message) {
        return build(Response.Status.BAD_REQUEST, false, message, null);
    }

    public static Response notFound(String message) {
        return build(Response.Status.NOT_FOUND, false, message, null);
    }

    public static Response conflict(String message) {
        return build(Response.Status.CONFLICT, false, message, null);
    }

    public static Response forbidden(String message) {
        return build(Response.Status.FORBIDDEN, false, message, null);
    }

    public static Response serverError(Exception e) {
        return build(Response.Status.INTERNAL_SERVER_ERROR, false, e.getMessage(), null);
    }

    private static Response build(Response.Status status, boolean success, String message, Object data) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("success", success);
        body.put("message", message);
        if (data != null) {
            body.put("data", data);
        }
        return Response.status(status).entity(body).build();
    }
}
