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

@Path("proveedores")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class ProveedorResource {
    private final CrudService crud = new CrudService();

    @GET
    public Response list() {
        try {
            return ApiResponse.ok(crud.list("proveedores"));
        } catch (Exception e) {
            return ApiResponse.serverError(e);
        }
    }

    @GET
    @Path("{id}")
    public Response get(@PathParam("id") int id) {
        try {
            Map<String, Object> item = crud.get("proveedores", id);
            return item == null ? ApiResponse.notFound("Proveedor no encontrado") : ApiResponse.ok(item);
        } catch (Exception e) {
            return ApiResponse.serverError(e);
        }
    }

    @POST
    public Response create(Map<String, Object> data) {
        try {
            return ApiResponse.created(crud.create("proveedores", data));
        } catch (Exception e) {
            return ApiResponse.serverError(e);
        }
    }

    @PUT
    @Path("{id}")
    public Response update(@PathParam("id") int id, Map<String, Object> data) {
        try {
            Map<String, Object> item = crud.update("proveedores", id, data);
            return item == null ? ApiResponse.notFound("Proveedor no encontrado") : ApiResponse.ok(item);
        } catch (Exception e) {
            return ApiResponse.serverError(e);
        }
    }

    @DELETE
    @Path("{id}")
    public Response delete(@PathParam("id") int id) {
        try {
            return crud.delete("proveedores", id) ? ApiResponse.message("Proveedor eliminado")
                    : ApiResponse.notFound("Proveedor no encontrado");
        } catch (Exception e) {
            return ApiResponse.serverError(e);
        }
    }
}
