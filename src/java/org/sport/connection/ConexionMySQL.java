package org.sport.connection;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

public class ConexionMySQL {
    private Connection conn;

    public Connection open() {
        String user = "root";
        String password = "wbUWIzBmDaomKIozaFXtXZAPrhgLjWwO";
        String dbName = "sportzone_db";
        String url = "jdbc:mysql://thomas.proxy.rlwy.net:52573/" + dbName;
        String parametros = "?useSSL=false"
                + "&allowPublicKeyRetrieval=true"
                + "&useUnicode=true"
                + "&characterEncoding=utf-8"
                + "&serverTimezone=America/Mexico_City"
                + "&connectTimeout=5000";

        try {
            Class.forName("com.mysql.cj.jdbc.Driver");
            conn = DriverManager.getConnection(url + parametros, user, password);
            return conn;
        } catch (Exception e) {
            e.printStackTrace();
            throw new RuntimeException("No se pudo conectar a MySQL: " + e.getMessage(), e);
        }
    }

    public void close() {
        if (conn != null) {
            try {
                conn.close();
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }
    }
}
