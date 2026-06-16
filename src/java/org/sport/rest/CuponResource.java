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
import org.sport.service.CouponService;
import org.sport.service.CrudService;
import org.sport.util.ApiResponse;
import org.sport.util.ValueUtil;

import java.util.Map;

@Path("cupones")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class CuponResource {
    private final CrudService crud = new CrudService();
    private final CouponService coupons = new CouponService();

    @GET
    public Response list() {
        try {
            return ApiResponse.ok(crud.list("cupones"));
        } catch (Exception e) {
            return ApiResponse.serverError(e);
        }
    }

    @GET
    @Path("{id}")
    public Response get(@PathParam("id") int id) {
        try {
            Map<String, Object> item = crud.get("cupones", id);
            return item == null ? ApiResponse.notFound("Cupon no encontrado") : ApiResponse.ok(item);
        } catch (Exception e) {
            return ApiResponse.serverError(e);
        }
    }

    @POST
    public Response create(Map<String, Object> data) {
        try {
            return ApiResponse.created(crud.create("cupones", data));
        } catch (IllegalStateException e) {
            return ApiResponse.conflict("Codigo de cupon duplicado");
        } catch (Exception e) {
            return ApiResponse.serverError(e);
        }
    }

    @PUT
    @Path("{id}")
    public Response update(@PathParam("id") int id, Map<String, Object> data) {
        try {
            Map<String, Object> item = crud.update("cupones", id, data);
            return item == null ? ApiResponse.notFound("Cupon no encontrado") : ApiResponse.ok(item);
        } catch (Exception e) {
            return ApiResponse.serverError(e);
        }
    }

    @DELETE
    @Path("{id}")
    public Response delete(@PathParam("id") int id) {
        try {
            return crud.delete("cupones", id) ? ApiResponse.message("Cupon eliminado")
                    : ApiResponse.notFound("Cupon no encontrado");
        } catch (Exception e) {
            return ApiResponse.serverError(e);
        }
    }

    @POST
    @Path("validar")
    public Response validar(Map<String, Object> data) {
        try {
            Map<String, Object> coupon = coupons.findValidCoupon(ValueUtil.requiredString(data, "codigo"));
            return coupon == null ? ApiResponse.badRequest("Cupon invalido o vencido") : ApiResponse.ok(coupon);
        } catch (Exception e) {
            return ApiResponse.serverError(e);
        }
    }
}
