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
import org.sport.connection.ConexionMySQL;
import org.sport.service.CouponService;
import org.sport.util.ApiResponse;
import org.sport.util.JdbcUtil;
import org.sport.util.ValueUtil;

import java.math.BigDecimal;
import java.sql.Connection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Path("carrito")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class CarritoResource {
    private final CouponService coupons = new CouponService();

    @GET
    @Path("usuario/{idUsuario}")
    public Response getByUsuario(@PathParam("idUsuario") int idUsuario) {
        ConexionMySQL mysql = new ConexionMySQL();
        Connection conn = mysql.open();
        try {
            Map<String, Object> cart = getOrCreateCart(conn, idUsuario);
            return ApiResponse.ok(cartDetails(conn, cart));
        } catch (Exception e) {
            return ApiResponse.serverError(e);
        } finally {
            mysql.close();
        }
    }

    @POST
    @Path("usuario/{idUsuario}/items")
    public Response addItem(@PathParam("idUsuario") int idUsuario, Map<String, Object> data) {
        ConexionMySQL mysql = new ConexionMySQL();
        Connection conn = mysql.open();
        try {
            int idVariante = ValueUtil.requiredInt(data, "id_variante");
            int cantidad = Math.max(1, ValueUtil.integer(data, "cantidad") == null ? 1 : ValueUtil.integer(data, "cantidad"));
            Map<String, Object> variant = productForVariant(conn, idVariante);
            if (variant == null) {
                return ApiResponse.notFound("Variante no encontrada");
            }
            if (((Number) variant.get("stock_variante")).intValue() < cantidad) {
                return ApiResponse.badRequest("Stock insuficiente");
            }

            BigDecimal price = priceFromProduct(variant);
            Map<String, Object> cart = getOrCreateCart(conn, idUsuario);
            Map<String, Object> existing = JdbcUtil.queryOne(conn,
                    "SELECT * FROM carrito_items WHERE id_carrito = ? AND id_variante = ?",
                    cart.get("id_carrito"), idVariante);
            if (existing == null) {
                JdbcUtil.insert(conn,
                        "INSERT INTO carrito_items (id_carrito, id_variante, cantidad, precio_unitario) VALUES (?, ?, ?, ?)",
                        cart.get("id_carrito"), idVariante, cantidad, price);
            } else {
                JdbcUtil.update(conn,
                        "UPDATE carrito_items SET cantidad = cantidad + ?, precio_unitario = ? WHERE id_item = ?",
                        cantidad, price, existing.get("id_item"));
            }
            touchCart(conn, cart.get("id_carrito"));
            return ApiResponse.ok(cartDetails(conn, getOrCreateCart(conn, idUsuario)));
        } catch (IllegalArgumentException e) {
            return ApiResponse.badRequest(e.getMessage());
        } catch (Exception e) {
            return ApiResponse.serverError(e);
        } finally {
            mysql.close();
        }
    }

    @PUT
    @Path("items/{idItem}")
    public Response updateItem(@PathParam("idItem") int idItem, Map<String, Object> data) {
        ConexionMySQL mysql = new ConexionMySQL();
        Connection conn = mysql.open();
        try {
            int cantidad = ValueUtil.requiredInt(data, "cantidad");
            if (cantidad <= 0) {
                JdbcUtil.update(conn, "DELETE FROM carrito_items WHERE id_item = ?", idItem);
                return ApiResponse.message("Item eliminado");
            }
            int affected = JdbcUtil.update(conn, "UPDATE carrito_items SET cantidad = ? WHERE id_item = ?", cantidad, idItem);
            return affected > 0 ? ApiResponse.message("Item actualizado") : ApiResponse.notFound("Item no encontrado");
        } catch (IllegalArgumentException e) {
            return ApiResponse.badRequest(e.getMessage());
        } catch (Exception e) {
            return ApiResponse.serverError(e);
        } finally {
            mysql.close();
        }
    }

    @DELETE
    @Path("items/{idItem}")
    public Response deleteItem(@PathParam("idItem") int idItem) {
        ConexionMySQL mysql = new ConexionMySQL();
        Connection conn = mysql.open();
        try {
            return JdbcUtil.update(conn, "DELETE FROM carrito_items WHERE id_item = ?", idItem) > 0
                    ? ApiResponse.message("Item eliminado")
                    : ApiResponse.notFound("Item no encontrado");
        } catch (Exception e) {
            return ApiResponse.serverError(e);
        } finally {
            mysql.close();
        }
    }

    @POST
    @Path("usuario/{idUsuario}/cupon")
    public Response applyCoupon(@PathParam("idUsuario") int idUsuario, Map<String, Object> data) {
        ConexionMySQL mysql = new ConexionMySQL();
        Connection conn = mysql.open();
        try {
            String code = ValueUtil.requiredString(data, "codigo");
            Map<String, Object> coupon = coupons.findValidCoupon(conn, code);
            if (coupon == null) {
                return ApiResponse.badRequest("Cupon invalido o vencido");
            }
            Map<String, Object> cart = getOrCreateCart(conn, idUsuario);
            BigDecimal subtotal = subtotal(conn, cart.get("id_carrito"));
            BigDecimal discount = coupons.calculateDiscount(coupon, subtotal);
            JdbcUtil.update(conn,
                    "UPDATE carrito SET cupon_codigo = ?, cupon_descuento = ?, fecha_actualizacion = CURRENT_TIMESTAMP WHERE id_carrito = ?",
                    code, discount, cart.get("id_carrito"));
            return ApiResponse.ok(cartDetails(conn, getOrCreateCart(conn, idUsuario)));
        } catch (IllegalArgumentException e) {
            return ApiResponse.badRequest(e.getMessage());
        } catch (Exception e) {
            return ApiResponse.serverError(e);
        } finally {
            mysql.close();
        }
    }

    @DELETE
    @Path("usuario/{idUsuario}/cupon")
    public Response removeCoupon(@PathParam("idUsuario") int idUsuario) {
        ConexionMySQL mysql = new ConexionMySQL();
        Connection conn = mysql.open();
        try {
            Map<String, Object> cart = getOrCreateCart(conn, idUsuario);
            JdbcUtil.update(conn,
                    "UPDATE carrito SET cupon_codigo = NULL, cupon_descuento = 0, fecha_actualizacion = CURRENT_TIMESTAMP WHERE id_carrito = ?",
                    cart.get("id_carrito"));
            return ApiResponse.ok(cartDetails(conn, getOrCreateCart(conn, idUsuario)));
        } catch (Exception e) {
            return ApiResponse.serverError(e);
        } finally {
            mysql.close();
        }
    }

    static Map<String, Object> getOrCreateCart(Connection conn, int idUsuario) throws Exception {
        Map<String, Object> cart = JdbcUtil.queryOne(conn,
                "SELECT * FROM carrito WHERE id_usuario = ? ORDER BY id_carrito DESC LIMIT 1", idUsuario);
        if (cart == null) {
            int id = JdbcUtil.insert(conn,
                    "INSERT INTO carrito (id_usuario, cupon_descuento) VALUES (?, 0)", idUsuario);
            cart = JdbcUtil.queryOne(conn, "SELECT * FROM carrito WHERE id_carrito = ?", id);
        }
        return cart;
    }

    static Map<String, Object> cartDetails(Connection conn, Map<String, Object> cart) throws Exception {
        List<Map<String, Object>> items = JdbcUtil.query(conn, """
                SELECT ci.*, pv.color, pv.talla, pv.sku, pv.stock_variante,
                       p.id_producto, p.nombre, p.marca, p.imagen_url
                FROM carrito_items ci
                INNER JOIN producto_variantes pv ON pv.id_variante = ci.id_variante
                INNER JOIN productos p ON p.id_producto = pv.id_producto
                WHERE ci.id_carrito = ?
                ORDER BY ci.id_item DESC
                """, cart.get("id_carrito"));

        BigDecimal subtotal = BigDecimal.ZERO;
        for (Map<String, Object> item : items) {
            BigDecimal line = new BigDecimal(String.valueOf(item.get("precio_unitario")))
                    .multiply(new BigDecimal(String.valueOf(item.get("cantidad"))));
            item.put("subtotal", line);
            subtotal = subtotal.add(line);
        }

        BigDecimal discount = cart.get("cupon_descuento") == null
                ? BigDecimal.ZERO
                : new BigDecimal(String.valueOf(cart.get("cupon_descuento")));
        Map<String, Object> details = new LinkedHashMap<>(cart);
        details.put("items", items);
        details.put("subtotal", subtotal);
        details.put("total", subtotal.subtract(discount).max(BigDecimal.ZERO));
        return details;
    }

    static BigDecimal subtotal(Connection conn, Object idCarrito) throws Exception {
        Map<String, Object> row = JdbcUtil.queryOne(conn,
                "SELECT COALESCE(SUM(cantidad * precio_unitario), 0) subtotal FROM carrito_items WHERE id_carrito = ?",
                idCarrito);
        return new BigDecimal(String.valueOf(row.get("subtotal")));
    }

    static Map<String, Object> productForVariant(Connection conn, int idVariante) throws Exception {
        return JdbcUtil.queryOne(conn, """
                SELECT pv.*, p.precio_normal, p.precio_oferta, p.activo
                FROM producto_variantes pv
                INNER JOIN productos p ON p.id_producto = pv.id_producto
                WHERE pv.id_variante = ? AND p.activo = TRUE
                """, idVariante);
    }

    static BigDecimal priceFromProduct(Map<String, Object> product) {
        Object offer = product.get("precio_oferta");
        if (offer != null) {
            return new BigDecimal(String.valueOf(offer));
        }
        return new BigDecimal(String.valueOf(product.get("precio_normal")));
    }

    private void touchCart(Connection conn, Object idCarrito) throws Exception {
        JdbcUtil.update(conn, "UPDATE carrito SET fecha_actualizacion = CURRENT_TIMESTAMP WHERE id_carrito = ?", idCarrito);
    }
}
