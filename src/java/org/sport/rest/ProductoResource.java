package org.sport.rest;

import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.DELETE;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.PUT;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.sport.connection.ConexionMySQL;
import org.sport.service.CrudService;
import org.sport.util.ApiResponse;
import org.sport.util.JdbcUtil;
import org.sport.util.ValueUtil;

import java.sql.Connection;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Path("productos")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class ProductoResource {
    private final CrudService crud = new CrudService();

    @GET
    public Response list(@QueryParam("categoria") Integer categoria,
                         @QueryParam("activo") Boolean activo,
                         @QueryParam("buscar") String buscar) {
        ConexionMySQL mysql = new ConexionMySQL();
        Connection conn = mysql.open();
        try {
            StringBuilder sql = new StringBuilder("""
                    SELECT p.*, c.nombre_categoria
                    FROM productos p
                    LEFT JOIN categorias c ON c.id_categoria = p.id_categoria
                    WHERE 1 = 1
                    """);
            List<Object> params = new ArrayList<>();
            if (categoria != null) {
                sql.append(" AND p.id_categoria = ?");
                params.add(categoria);
            }
            if (activo != null) {
                sql.append(" AND p.activo = ?");
                params.add(activo);
            }
            if (buscar != null && !buscar.isBlank()) {
                sql.append(" AND (p.nombre LIKE ? OR p.marca LIKE ? OR p.descripcion LIKE ?)");
                String pattern = "%" + buscar.trim() + "%";
                params.add(pattern);
                params.add(pattern);
                params.add(pattern);
            }
            sql.append(" ORDER BY p.id_producto DESC");
            return ApiResponse.ok(JdbcUtil.query(conn, sql.toString(), params.toArray()));
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
            Map<String, Object> item = crud.get("productos", id);
            return item == null ? ApiResponse.notFound("Producto no encontrado") : ApiResponse.ok(item);
        } catch (Exception e) {
            return ApiResponse.serverError(e);
        }
    }

    @POST
    public Response create(Map<String, Object> data) {
        try {
            return ApiResponse.created(crud.create("productos", data));
        } catch (IllegalStateException e) {
            return ApiResponse.conflict("Categoria no encontrada o datos duplicados");
        } catch (Exception e) {
            return ApiResponse.serverError(e);
        }
    }

    @PUT
    @Path("{id}")
    public Response update(@PathParam("id") int id, Map<String, Object> data) {
        try {
            Map<String, Object> item = crud.update("productos", id, data);
            return item == null ? ApiResponse.notFound("Producto no encontrado") : ApiResponse.ok(item);
        } catch (Exception e) {
            return ApiResponse.serverError(e);
        }
    }

    @DELETE
    @Path("{id}")
    public Response delete(@PathParam("id") int id) {
        try {
            return crud.delete("productos", id) ? ApiResponse.message("Producto eliminado")
                    : ApiResponse.notFound("Producto no encontrado");
        } catch (Exception e) {
            return ApiResponse.serverError(e);
        }
    }

    @GET
    @Path("{id}/variantes")
    public Response variantes(@PathParam("id") int id) {
        ConexionMySQL mysql = new ConexionMySQL();
        Connection conn = mysql.open();
        try {
            return ApiResponse.ok(JdbcUtil.query(conn,
                    "SELECT * FROM producto_variantes WHERE id_producto = ? ORDER BY id_variante DESC", id));
        } catch (Exception e) {
            return ApiResponse.serverError(e);
        } finally {
            mysql.close();
        }
    }

    @POST
    @Path("{id}/variantes")
    public Response createVariante(@PathParam("id") int id, Map<String, Object> data) {
        try {
            data.put("id_producto", id);
            return ApiResponse.created(crud.create("producto_variantes", data));
        } catch (IllegalStateException e) {
            return ApiResponse.conflict("SKU duplicado o producto no encontrado");
        } catch (Exception e) {
            return ApiResponse.serverError(e);
        }
    }

    @PUT
    @Path("variantes/{idVariante}")
    public Response updateVariante(@PathParam("idVariante") int idVariante, Map<String, Object> data) {
        try {
            Map<String, Object> item = crud.update("producto_variantes", idVariante, data);
            return item == null ? ApiResponse.notFound("Variante no encontrada") : ApiResponse.ok(item);
        } catch (Exception e) {
            return ApiResponse.serverError(e);
        }
    }

    @DELETE
    @Path("variantes/{idVariante}")
    public Response deleteVariante(@PathParam("idVariante") int idVariante) {
        try {
            return crud.delete("producto_variantes", idVariante) ? ApiResponse.message("Variante eliminada")
                    : ApiResponse.notFound("Variante no encontrada");
        } catch (Exception e) {
            return ApiResponse.serverError(e);
        }
    }

    @GET
    @Path("{idProducto}/proveedores")
    public Response proveedores(@PathParam("idProducto") int idProducto) {
        ConexionMySQL mysql = new ConexionMySQL();
        Connection conn = mysql.open();
        try {
            return ApiResponse.ok(JdbcUtil.query(conn, """
                    SELECT pp.*, pr.nombre, pr.contacto_nombre, pr.contacto_telefono, pr.contacto_email
                    FROM producto_proveedor pp
                    INNER JOIN proveedores pr ON pr.id_proveedor = pp.id_proveedor
                    WHERE pp.id_producto = ?
                    ORDER BY pp.es_proveedor_principal DESC, pr.nombre
                    """, idProducto));
        } catch (Exception e) {
            return ApiResponse.serverError(e);
        } finally {
            mysql.close();
        }
    }

    @POST
    @Path("{idProducto}/proveedores")
    public Response addProveedor(@PathParam("idProducto") int idProducto, Map<String, Object> data) {
        ConexionMySQL mysql = new ConexionMySQL();
        Connection conn = mysql.open();
        try {
            int idProveedor = ValueUtil.requiredInt(data, "id_proveedor");
            Object costo = data.get("costo_unitario");
            boolean principal = ValueUtil.bool(data, "es_proveedor_principal", false);
            JdbcUtil.update(conn, """
                    INSERT INTO producto_proveedor
                        (id_producto, id_proveedor, costo_unitario, es_proveedor_principal)
                    VALUES (?, ?, ?, ?)
                    ON DUPLICATE KEY UPDATE
                        costo_unitario = VALUES(costo_unitario),
                        es_proveedor_principal = VALUES(es_proveedor_principal)
                    """, idProducto, idProveedor, costo, principal);
            return ApiResponse.ok(JdbcUtil.queryOne(conn,
                    "SELECT * FROM producto_proveedor WHERE id_producto = ? AND id_proveedor = ?",
                    idProducto, idProveedor));
        } catch (IllegalArgumentException e) {
            return ApiResponse.badRequest(e.getMessage());
        } catch (Exception e) {
            return ApiResponse.serverError(e);
        } finally {
            mysql.close();
        }
    }

    @DELETE
    @Path("{idProducto}/proveedores/{idProveedor}")
    public Response deleteProveedor(@PathParam("idProducto") int idProducto, @PathParam("idProveedor") int idProveedor) {
        ConexionMySQL mysql = new ConexionMySQL();
        Connection conn = mysql.open();
        try {
            int affected = JdbcUtil.update(conn,
                    "DELETE FROM producto_proveedor WHERE id_producto = ? AND id_proveedor = ?",
                    idProducto, idProveedor);
            return affected > 0 ? ApiResponse.message("Relacion eliminada")
                    : ApiResponse.notFound("Relacion producto-proveedor no encontrada");
        } catch (Exception e) {
            return ApiResponse.serverError(e);
        } finally {
            mysql.close();
        }
    }
}
