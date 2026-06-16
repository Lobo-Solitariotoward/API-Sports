package org.sport.rest;

import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.PATCH;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.sport.connection.ConexionMySQL;
import org.sport.util.ApiResponse;
import org.sport.util.JdbcUtil;
import org.sport.util.ValueUtil;

import java.math.BigDecimal;
import java.sql.Connection;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Path("pedidos")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class PedidoResource {
    private static final Set<String> ESTADOS = Set.of("pendiente", "pagado", "enviado", "entregado", "cancelado");

    @GET
    public Response list() {
        ConexionMySQL mysql = new ConexionMySQL();
        Connection conn = mysql.open();
        try {
            return ApiResponse.ok(JdbcUtil.query(conn,
                    "SELECT p.*, u.nombre_completo, u.email FROM pedido p INNER JOIN usuarios u ON u.id_usuario = p.id_usuario ORDER BY p.id_pedido DESC"));
        } catch (Exception e) {
            return ApiResponse.serverError(e);
        } finally {
            mysql.close();
        }
    }

    @GET
    @Path("{id}")
    public Response get(@PathParam("id") int id) {
        ConexionMySQL mysql = new ConexionMySQL();
        Connection conn = mysql.open();
        try {
            Map<String, Object> order = orderDetails(conn, id);
            return order == null ? ApiResponse.notFound("Pedido no encontrado") : ApiResponse.ok(order);
        } catch (Exception e) {
            return ApiResponse.serverError(e);
        } finally {
            mysql.close();
        }
    }

    @GET
    @Path("usuario/{idUsuario}")
    public Response byUsuario(@PathParam("idUsuario") int idUsuario) {
        ConexionMySQL mysql = new ConexionMySQL();
        Connection conn = mysql.open();
        try {
            return ApiResponse.ok(JdbcUtil.query(conn,
                    "SELECT * FROM pedido WHERE id_usuario = ? ORDER BY id_pedido DESC", idUsuario));
        } catch (Exception e) {
            return ApiResponse.serverError(e);
        } finally {
            mysql.close();
        }
    }

    @POST
    @Path("desde-carrito")
    public Response fromCart(Map<String, Object> data) {
        ConexionMySQL mysql = new ConexionMySQL();
        Connection conn = mysql.open();
        try {
            int idUsuario = ValueUtil.requiredInt(data, "id_usuario");
            String direccion = ValueUtil.requiredString(data, "direccion_envio");
            String metodoPago = ValueUtil.requiredString(data, "metodo_pago");
            String notas = ValueUtil.string(data, "notas");

            conn.setAutoCommit(false);
            Map<String, Object> cart = CarritoResource.getOrCreateCart(conn, idUsuario);
            List<Map<String, Object>> items = JdbcUtil.query(conn, """
                    SELECT ci.*, pv.stock_variante, pv.id_producto
                    FROM carrito_items ci
                    INNER JOIN producto_variantes pv ON pv.id_variante = ci.id_variante
                    WHERE ci.id_carrito = ?
                    """, cart.get("id_carrito"));
            if (items.isEmpty()) {
                conn.rollback();
                return ApiResponse.badRequest("El carrito esta vacio");
            }

            for (Map<String, Object> item : items) {
                int stock = ((Number) item.get("stock_variante")).intValue();
                int cantidad = ((Number) item.get("cantidad")).intValue();
                if (stock < cantidad) {
                    conn.rollback();
                    return ApiResponse.badRequest("Stock insuficiente para una variante del carrito");
                }
            }

            BigDecimal subtotal = CarritoResource.subtotal(conn, cart.get("id_carrito"));
            BigDecimal discount = cart.get("cupon_descuento") == null
                    ? BigDecimal.ZERO
                    : new BigDecimal(String.valueOf(cart.get("cupon_descuento")));
            BigDecimal total = subtotal.subtract(discount).max(BigDecimal.ZERO);

            Integer idCupon = null;
            if (cart.get("cupon_codigo") != null) {
                Map<String, Object> coupon = JdbcUtil.queryOne(conn,
                        "SELECT id_cupon FROM cupones WHERE codigo = ?", cart.get("cupon_codigo"));
                if (coupon != null) {
                    idCupon = ((Number) coupon.get("id_cupon")).intValue();
                }
            }

            int idPedido = JdbcUtil.insert(conn, """
                    INSERT INTO pedido
                        (id_usuario, total, estado, direccion_envio, metodo_pago, id_cupon_aplicado, notas)
                    VALUES (?, ?, 'pendiente', ?, ?, ?, ?)
                    """, idUsuario, total, direccion, metodoPago, idCupon, notas);

            for (Map<String, Object> item : items) {
                int cantidad = ((Number) item.get("cantidad")).intValue();
                JdbcUtil.insert(conn, """
                        INSERT INTO pedido_items (id_pedido, id_variante, cantidad, precio_venta)
                        VALUES (?, ?, ?, ?)
                        """, idPedido, item.get("id_variante"), cantidad, item.get("precio_unitario"));
                JdbcUtil.update(conn,
                        "UPDATE producto_variantes SET stock_variante = stock_variante - ? WHERE id_variante = ?",
                        cantidad, item.get("id_variante"));
                JdbcUtil.update(conn,
                        "UPDATE productos SET stock_actual = GREATEST(stock_actual - ?, 0) WHERE id_producto = ?",
                        cantidad, item.get("id_producto"));
            }

            if (idCupon != null) {
                JdbcUtil.update(conn, "UPDATE cupones SET usos_actuales = usos_actuales + 1 WHERE id_cupon = ?", idCupon);
            }
            JdbcUtil.update(conn, "DELETE FROM carrito_items WHERE id_carrito = ?", cart.get("id_carrito"));
            JdbcUtil.update(conn,
                    "UPDATE carrito SET cupon_codigo = NULL, cupon_descuento = 0, fecha_actualizacion = CURRENT_TIMESTAMP WHERE id_carrito = ?",
                    cart.get("id_carrito"));

            conn.commit();
            return ApiResponse.created(orderDetails(conn, idPedido));
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

    @PATCH
    @Path("{id}/estado")
    public Response updateEstado(@PathParam("id") int id, Map<String, Object> data) {
        ConexionMySQL mysql = new ConexionMySQL();
        Connection conn = mysql.open();
        try {
            String estado = ValueUtil.requiredString(data, "estado");
            if (!ESTADOS.contains(estado)) {
                return ApiResponse.badRequest("Estado invalido");
            }
            int affected = JdbcUtil.update(conn, "UPDATE pedido SET estado = ? WHERE id_pedido = ?", estado, id);
            return affected > 0 ? ApiResponse.ok(orderDetails(conn, id)) : ApiResponse.notFound("Pedido no encontrado");
        } catch (IllegalArgumentException e) {
            return ApiResponse.badRequest(e.getMessage());
        } catch (Exception e) {
            return ApiResponse.serverError(e);
        } finally {
            mysql.close();
        }
    }

    private Map<String, Object> orderDetails(Connection conn, int idPedido) throws Exception {
        Map<String, Object> order = JdbcUtil.queryOne(conn, "SELECT * FROM pedido WHERE id_pedido = ?", idPedido);
        if (order == null) {
            return null;
        }
        order.put("items", JdbcUtil.query(conn, """
                SELECT pi.*, pv.color, pv.talla, pv.sku, p.nombre, p.marca, p.imagen_url
                FROM pedido_items pi
                INNER JOIN producto_variantes pv ON pv.id_variante = pi.id_variante
                INNER JOIN productos p ON p.id_producto = pv.id_producto
                WHERE pi.id_pedido = ?
                ORDER BY pi.id_pedido_item
                """, idPedido));
        return order;
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
