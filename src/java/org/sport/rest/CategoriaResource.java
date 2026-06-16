package org.sport.rest;

import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.DELETE;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.PUT;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.sport.service.CrudService;
import org.sport.util.ApiResponse;

import java.util.Map;

@Path("categorias")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class CategoriaResource {
    private final CrudService crud = new CrudService();

    @GET
    public Response list() {
        try {
            return ApiResponse.ok(crud.list("categorias"));
        } catch (Exception e) {
            return ApiResponse.serverError(e);
        }
    }

    @GET
    @Path("{id}")
    public Response get(@PathParam("id") int id) {
        try {
            Map<String, Object> item = crud.get("categorias", id);
            return item == null ? ApiResponse.notFound("Categoria no encontrada") : ApiResponse.ok(item);
        } catch (Exception e) {
            return ApiResponse.serverError(e);
        }
    }

    @POST
    public Response create(Map<String, Object> data) {
        try {
            return ApiResponse.created(crud.create("categorias", data));
        } catch (IllegalStateException e) {
            return ApiResponse.conflict("Categoria duplicada");
        } catch (Exception e) {
            return ApiResponse.serverError(e);
        }
    }

    @PUT
    @Path("{id}")
    public Response update(@PathParam("id") int id, Map<String, Object> data) {
        try {
            Map<String, Object> item = crud.update("categorias", id, data);
            return item == null ? ApiResponse.notFound("Categoria no encontrada") : ApiResponse.ok(item);
        } catch (Exception e) {
            return ApiResponse.serverError(e);
        }
    }

    @DELETE
    @Path("{id}")
    public Response delete(@PathParam("id") int id) {
        try {
            return crud.delete("categorias", id) ? ApiResponse.message("Categoria eliminada")
                    : ApiResponse.notFound("Categoria no encontrada");
        } catch (Exception e) {
            return ApiResponse.serverError(e);
        }
    }
}
