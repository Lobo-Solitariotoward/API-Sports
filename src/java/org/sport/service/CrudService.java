package org.sport.service;

import org.sport.connection.ConexionMySQL;
import org.sport.util.JdbcUtil;

import java.sql.Connection;
import java.sql.SQLIntegrityConstraintViolationException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;


//Contiene operaciones generales para crear, leer, actualizar y eliminar registros en tablas 
public class CrudService {
    private static final Map<String, TableConfig> TABLES = new LinkedHashMap<>();
    private static final Set<String> READ_ONLY_COLUMNS = Set.of(
            "fecha_registro",
            "fecha_creacion",
            "fecha_actualizacion",
            "fecha_pedido",
            "fecha_solicitud"
    );

    static {
        table("usuarios", "id_usuario", "nombre_completo", "email", "telefono", "contrasena_hash",
                "acepta_terminos", "recibe_promociones", "es_admin", "cuenta_bloqueada", "fecha_registro");
        table("categorias", "id_categoria", "nombre_categoria", "descripcion", "imagen_url", "orden", "activo");
        table("productos", "id_producto", "nombre", "descripcion", "id_categoria", "marca", "precio_normal",
                "precio_oferta", "stock_actual", "stock_minimo", "imagen_url", "activo", "fecha_creacion");
        table("producto_variantes", "id_variante", "id_producto", "color", "talla", "stock_variante", "sku");
        table("carrito", "id_carrito", "id_usuario", "fecha_creacion", "fecha_actualizacion", "cupon_codigo",
                "cupon_descuento");
        table("carrito_items", "id_item", "id_carrito", "id_variante", "cantidad", "precio_unitario");
        table("cupones", "id_cupon", "codigo", "tipo", "valor", "fecha_inicio", "fecha_fin", "max_uso",
                "usos_actuales", "activo");
        table("pedido", "id_pedido", "id_usuario", "fecha_pedido", "total", "estado", "direccion_envio",
                "metodo_pago", "id_cupon_aplicado", "notas");
        table("pedido_items", "id_pedido_item", "id_pedido", "id_variante", "cantidad", "precio_venta");
        table("proveedores", "id_proveedor", "nombre", "contacto_nombre", "contacto_telefono", "contacto_email",
                "direccion", "tiempo_entrega_estimado", "activo");
        table("pedidos_restock", "id_restock", "id_producto", "id_proveedor", "cantidad_solicitada",
                "fecha_solicitud", "fecha_estimada_entrega", "fecha_real_entrega", "estado", "notas");
    }

    private static void table(String name, String idColumn, String... columns) {
        TABLES.put(name, new TableConfig(name, idColumn, new LinkedHashSet<>(Arrays.asList(columns))));
    }

    public List<Map<String, Object>> list(String tableName) throws Exception {
        TableConfig table = table(tableName);
        ConexionMySQL mysql = new ConexionMySQL();
        Connection conn = mysql.open();
        try {
            return sanitize(tableName, JdbcUtil.query(conn, "SELECT * FROM " + table.name + " ORDER BY " + table.idColumn + " DESC"));
        } finally {
            mysql.close();
        }
    }

    public Map<String, Object> get(String tableName, int id) throws Exception {
        TableConfig table = table(tableName);
        ConexionMySQL mysql = new ConexionMySQL();
        Connection conn = mysql.open();
        try {
            Map<String, Object> row = JdbcUtil.queryOne(conn,
                    "SELECT * FROM " + table.name + " WHERE " + table.idColumn + " = ?", id);
            return sanitize(tableName, row);
        } finally {
            mysql.close();
        }
    }

    public Map<String, Object> create(String tableName, Map<String, Object> data) throws Exception {
        TableConfig table = table(tableName);
        List<String> columns = writableColumns(table, data, true);
        if (columns.isEmpty()) {
            throw new IllegalArgumentException("No hay datos para crear el registro");
        }

        String placeholders = String.join(", ", columns.stream().map(c -> "?").toList());
        String sql = "INSERT INTO " + table.name + " (" + String.join(", ", columns) + ") VALUES (" + placeholders + ")";
        List<Object> values = columns.stream().map(data::get).toList();

        ConexionMySQL mysql = new ConexionMySQL();
        Connection conn = mysql.open();
        try {
            int id = JdbcUtil.insert(conn, sql, values.toArray());
            return get(tableName, id);
        } catch (SQLIntegrityConstraintViolationException e) {
            throw new IllegalStateException("Registro duplicado o referencia inexistente", e);
        } finally {
            mysql.close();
        }
    }

    public Map<String, Object> update(String tableName, int id, Map<String, Object> data) throws Exception {
        TableConfig table = table(tableName);
        List<String> columns = writableColumns(table, data, false);
        if (columns.isEmpty()) {
            throw new IllegalArgumentException("No hay datos para actualizar");
        }

        List<String> assignments = columns.stream().map(c -> c + " = ?").toList();
        List<Object> values = new ArrayList<>();
        columns.forEach(c -> values.add(data.get(c)));
        values.add(id);

        ConexionMySQL mysql = new ConexionMySQL();
        Connection conn = mysql.open();
        try {
            int affected = JdbcUtil.update(conn,
                    "UPDATE " + table.name + " SET " + String.join(", ", assignments)
                            + " WHERE " + table.idColumn + " = ?",
                    values.toArray());
            if (affected == 0) {
                return null;
            }
            return get(tableName, id);
        } catch (SQLIntegrityConstraintViolationException e) {
            throw new IllegalStateException("Registro duplicado o referencia inexistente", e);
        } finally {
            mysql.close();
        }
    }

    public boolean delete(String tableName, int id) throws Exception {
        TableConfig table = table(tableName);
        ConexionMySQL mysql = new ConexionMySQL();
        Connection conn = mysql.open();
        try {
            return JdbcUtil.update(conn, "DELETE FROM " + table.name + " WHERE " + table.idColumn + " = ?", id) > 0;
        } finally {
            mysql.close();
        }
    }

    private List<String> writableColumns(TableConfig table, Map<String, Object> data, boolean create) {
        List<String> columns = new ArrayList<>();
        for (String column : table.columns) {
            if (column.equals(table.idColumn) || READ_ONLY_COLUMNS.contains(column)) {
                continue;
            }
            if (create && column.equals("usos_actuales")) {
                continue;
            }
            if (data.containsKey(column)) {
                columns.add(column);
            }
        }
        return columns;
    }

    private TableConfig table(String tableName) {
        TableConfig table = TABLES.get(tableName);
        if (table == null) {
            throw new IllegalArgumentException("Tabla no soportada: " + tableName);
        }
        return table;
    }

    private List<Map<String, Object>> sanitize(String tableName, List<Map<String, Object>> rows) {
        rows.forEach(row -> sanitize(tableName, row));
        return rows;
    }

    private Map<String, Object> sanitize(String tableName, Map<String, Object> row) {
        if (row != null && "usuarios".equals(tableName)) {
            row.remove("contrasena_hash");
        }
        return row;
    }

    private static class TableConfig {
        final String name;
        final String idColumn;
        final Set<String> columns;

        TableConfig(String name, String idColumn, Set<String> columns) {
            this.name = name;
            this.idColumn = idColumn;
            this.columns = columns;
        }
    }
}
