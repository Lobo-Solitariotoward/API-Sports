package org.sport.service;

import org.sport.connection.ConexionMySQL;
import org.sport.util.JdbcUtil;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.sql.Connection;
import java.time.LocalDate;
import java.util.Map;


//Valida cupones: revisa si están activos, vigentes, si no superaron el máximo de usos y calcula el descuento.


public class CouponService {
    public Map<String, Object> findValidCoupon(String code) throws Exception {
        ConexionMySQL mysql = new ConexionMySQL();
        Connection conn = mysql.open();
        try {
            return findValidCoupon(conn, code);
        } finally {
            mysql.close();
        }
    }

    public Map<String, Object> findValidCoupon(Connection conn, String code) throws Exception {
        Map<String, Object> coupon = JdbcUtil.queryOne(conn,
                "SELECT * FROM cupones WHERE codigo = ? AND activo = TRUE", code);
        if (coupon == null) {
            return null;
        }

        LocalDate today = LocalDate.now();
        LocalDate start = LocalDate.parse(String.valueOf(coupon.get("fecha_inicio")));
        LocalDate end = LocalDate.parse(String.valueOf(coupon.get("fecha_fin")));
        if (today.isBefore(start) || today.isAfter(end)) {
            return null;
        }

        Object maxUsoValue = coupon.get("max_uso");
        if (maxUsoValue != null) {
            int maxUso = ((Number) maxUsoValue).intValue();
            int usosActuales = ((Number) coupon.get("usos_actuales")).intValue();
            if (usosActuales >= maxUso) {
                return null;
            }
        }
        return coupon;
    }

    public BigDecimal calculateDiscount(Map<String, Object> coupon, BigDecimal subtotal) {
        if (coupon == null || subtotal == null) {
            return BigDecimal.ZERO;
        }

        String tipo = String.valueOf(coupon.get("tipo"));
        BigDecimal valor = new BigDecimal(String.valueOf(coupon.get("valor")));
        BigDecimal discount = "PORCENTAJE".equalsIgnoreCase(tipo)
                ? subtotal.multiply(valor).divide(new BigDecimal("100"), 2, RoundingMode.HALF_UP)
                : valor;
        return discount.min(subtotal).max(BigDecimal.ZERO);
    }
}
