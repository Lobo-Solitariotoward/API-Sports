package org.sport.rest;

import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.DELETE;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.PATCH;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.PUT;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.sport.connection.ConexionMySQL;
import org.sport.service.CrudService;
import org.sport.util.ApiResponse;
import org.sport.util.JdbcUtil;
import org.sport.util.ValueUtil;

import java.sql.Connection;
import java.time.LocalDate;
import java.util.Map;
import java.util.Set;

@Path("restock")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class RestockResource {
    private static final Set<String> ESTADOS = Set.of("solicitado", "enviado", "recibido", "cancelado");
    private final CrudService crud = new CrudService();

    @GET
    public Response list() {
        ConexionMySQL mysql = new ConexionMySQL();
        Connection conn = mysql.open();
        try {
            return ApiResponse.ok(JdbcUtil.query(conn, """
                    SELECT r.*, p.nombre producto_nombre, pr.nombre proveedor_nombre
                    FROM pedidos_restock r
                    INNER JOIN productos p ON p.id_producto = r.id_producto
                    INNER JOIN proveedores pr ON pr.id_proveedor = r.id_proveedor
                    ORDER BY r.id_restock DESC
                    """));
        } catch (Exception e) {
            return ApiResponse.serverError(e);
        } finally {
            mysql.close();
        }
    }

    @GET
    @Path("{id}")
    public Response get(@PathParam("id") int id) {
        try {
            Map<String, Object> item = crud.get("pedidos_restock", id);
            return item == null ? ApiResponse.notFound("Pedido de restock no encontrado") : ApiResponse.ok(item);
        } catch (Exception e) {
            return ApiResponse.serverError(e);
        }
    }

    @POST
    public Response create(Map<String, Object> data) {
        try {
            return ApiResponse.created(crud.create("pedidos_restock", data));
        } catch (IllegalStateException e) {
            return ApiResponse.conflict("Producto o proveedor no encontrado");
        } catch (Exception e) {
            return ApiResponse.serverError(e);
        }
    }

    @PUT
    @Path("{id}")
    public Response update(@PathParam("id") int id, Map<String, Object> data) {
        try {
            Map<String, Object> item = crud.update("pedidos_restock", id, data);
            return item == null ? ApiResponse.notFound("Pedido de restock no encontrado") : ApiResponse.ok(item);
        } catch (Exception e) {
            return ApiResponse.serverError(e);
        }
    }

    @DELETE
    @Path("{id}")
    public Response delete(@PathParam("id") int id) {
        try {
            return crud.delete("pedidos_restock", id) ? ApiResponse.message("Pedido de restock eliminado")
                    : ApiResponse.notFound("Pedido de restock no encontrado");
        } catch (Exception e) {
            return ApiResponse.serverError(e);
        }
    }

    @PATCH
    @Path("{id}/estado")
    public Response updateEstado(@PathParam("id") int id, Map<String, Object> data) {
        ConexionMySQL mysql = new ConexionMySQL();
        Connection conn = mysql.open();
        try {
            String estado = ValueUtil.requiredString(data, "estado");
            if (!ESTADOS.contains(estado)) {
                return ApiResponse.badRequest("Estado de restock invalido");
            }

            conn.setAutoCommit(false);
            Map<String, Object> restock = JdbcUtil.queryOne(conn,
                    "SELECT * FROM pedidos_restock WHERE id_restock = ?", id);
            if (restock == null) {
                conn.rollback();
                return ApiResponse.notFound("Pedido de restock no encontrado");
            }

            String oldStatus = String.valueOf(restock.get("estado"));
            String fechaReal = ValueUtil.string(data, "fecha_real_entrega");
            if ("recibido".equals(estado) && (fechaReal == null || fechaReal.isBlank())) {
                fechaReal = LocalDate.now().toString();
            }
            JdbcUtil.update(conn,
                    "UPDATE pedidos_restock SET estado = ?, fecha_real_entrega = COALESCE(?, fecha_real_entrega) WHERE id_restock = ?",
                    estado, fechaReal, id);

            if ("recibido".equals(estado) && !"recibido".equals(oldStatus)) {
                int cantidad = ((Number) restock.get("cantidad_solicitada")).intValue();
                JdbcUtil.update(conn, "UPDATE productos SET stock_actual = stock_actual + ? WHERE id_producto = ?",
                        cantidad, restock.get("id_producto"));
                Integer idVariante = ValueUtil.integer(data, "id_variante");
                if (idVariante != null) {
                    JdbcUtil.update(conn,
                            "UPDATE producto_variantes SET stock_variante = stock_variante + ? WHERE id_variante = ? AND id_producto = ?",
                            cantidad, idVariante, restock.get("id_producto"));
                }
            }

            conn.commit();
            return ApiResponse.ok(JdbcUtil.queryOne(conn, "SELECT * FROM pedidos_restock WHERE id_restock = ?", id));
        } catch (IllegalArgumentException e) {
            rollbackQuietly(conn);
            return ApiResponse.badRequest(e.getMessage());
        } catch (Exception e) {
            rollbackQuietly(conn);
            return ApiResponse.serverError(e);
        } finally {
            mysql.close();
        }
    }

    private void rollbackQuietly(Connection conn) {
        try {
            if (conn != null) {
                conn.rollback();
            }
        } catch (Exception ignored) {
        }
    }
}
