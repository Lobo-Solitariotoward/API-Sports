package org.sport.rest;

import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.PATCH;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.PUT;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.sport.service.CrudService;
import org.sport.util.ApiResponse;
import org.sport.util.CryptoUtil;
import org.sport.util.ValueUtil;

import java.util.Map;

@Path("usuarios")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class UsuarioResource {
    private final CrudService crud = new CrudService();

    @GET
    public Response list() {
        try {
            return ApiResponse.ok(crud.list("usuarios"));
        } catch (Exception e) {
            return ApiResponse.serverError(e);
        }
    }

    @GET
    @Path("{id}")
    public Response get(@PathParam("id") int id) {
        try {
            Map<String, Object> user = crud.get("usuarios", id);
            return user == null ? ApiResponse.notFound("Usuario no encontrado") : ApiResponse.ok(user);
        } catch (Exception e) {
            return ApiResponse.serverError(e);
        }
    }

    @POST
    public Response create(Map<String, Object> data) {
        try {
            preparePassword(data);
            return ApiResponse.created(crud.create("usuarios", data));
        } catch (IllegalStateException e) {
            return ApiResponse.conflict("Email duplicado o datos relacionados no encontrados");
        } catch (Exception e) {
            return ApiResponse.serverError(e);
        }
    }

    @PUT
    @Path("{id}")
    public Response update(@PathParam("id") int id, Map<String, Object> data) {
        try {
            preparePassword(data);
            Map<String, Object> user = crud.update("usuarios", id, data);
            return user == null ? ApiResponse.notFound("Usuario no encontrado") : ApiResponse.ok(user);
        } catch (IllegalStateException e) {
            return ApiResponse.conflict("Email duplicado o datos relacionados no encontrados");
        } catch (Exception e) {
            return ApiResponse.serverError(e);
        }
    }

    @PATCH
    @Path("{id}/bloqueo")
    public Response bloqueo(@PathParam("id") int id, Map<String, Object> data) {
        data.put("cuenta_bloqueada", ValueUtil.bool(data, "cuenta_bloqueada", true));
        return update(id, data);
    }

    @PATCH
    @Path("{id}/admin")
    public Response admin(@PathParam("id") int id, Map<String, Object> data) {
        data.put("es_admin", ValueUtil.bool(data, "es_admin", true));
        return update(id, data);
    }

    private void preparePassword(Map<String, Object> data) {
        String password = ValueUtil.string(data, "contrasena");
        if (password == null || password.isEmpty()) {
            password = ValueUtil.string(data, "password");
        }
        if (password != null && !password.isEmpty()) {
            data.put("contrasena_hash", CryptoUtil.sha256(password));
        }
        data.remove("contrasena");
        data.remove("password");
    }
}
