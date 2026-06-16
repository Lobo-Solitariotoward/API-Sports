package org.sport.util;

import java.sql.Connection;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.Statement;
import java.sql.Timestamp;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;


//Transforma consultas de SQL y a convertir sus resultados a JSON
public class JdbcUtil {
    private JdbcUtil() {
    }

    public static List<Map<String, Object>> query(Connection conn, String sql, Object... params) throws Exception {
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            setParams(ps, params);
            try (ResultSet rs = ps.executeQuery()) {
                List<Map<String, Object>> rows = new ArrayList<>();
                while (rs.next()) {
                    rows.add(rowToMap(rs));
                }
                return rows;
            }
        }
    }

    public static Map<String, Object> queryOne(Connection conn, String sql, Object... params) throws Exception {
        List<Map<String, Object>> rows = query(conn, sql, params);
        return rows.isEmpty() ? null : rows.get(0);
    }

    public static int update(Connection conn, String sql, Object... params) throws Exception {
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            setParams(ps, params);
            return ps.executeUpdate();
        }
    }

    public static int insert(Connection conn, String sql, Object... params) throws Exception {
        try (PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            setParams(ps, params);
            ps.executeUpdate();
            try (ResultSet keys = ps.getGeneratedKeys()) {
                return keys.next() ? keys.getInt(1) : 0;
            }
        }
    }

    public static void setParams(PreparedStatement ps, Object... params) throws Exception {
        for (int i = 0; i < params.length; i++) {
            ps.setObject(i + 1, params[i]);
        }
    }

    public static Map<String, Object> rowToMap(ResultSet rs) throws Exception {
        ResultSetMetaData meta = rs.getMetaData();
        Map<String, Object> row = new LinkedHashMap<>();
        for (int i = 1; i <= meta.getColumnCount(); i++) {
            Object value = rs.getObject(i);
            if (value instanceof Timestamp timestamp) {
                value = timestamp.toLocalDateTime().toString();
            } else if (value instanceof Date date) {
                value = date.toLocalDate().toString();
            } else if (value instanceof LocalDateTime dateTime) {
                value = dateTime.toString();
            } else if (value instanceof LocalDate localDate) {
                value = localDate.toString();
            } else if (value instanceof LocalTime localTime) {
                value = localTime.toString();
            }
            row.put(meta.getColumnLabel(i), value);
        }
        return row;
    }
}
