package org.sport.rest;

import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.sport.connection.ConexionMySQL;
import org.sport.service.CrudService;
import org.sport.util.ApiResponse;
import org.sport.util.CryptoUtil;
import org.sport.util.JdbcUtil;
import org.sport.util.ValueUtil;

import java.sql.Connection;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

@Path("auth")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class AuthResource {
    private final CrudService crud = new CrudService();

    // Alias: iOS llama /auth/register
    @POST
    @Path("register")
    public Response register(Map<String, Object> data) {
        return registro(data);
    }

    @POST
    @Path("registro")
    public Response registro(Map<String, Object> data) {
        try {
            ValueUtil.requiredString(data, "nombre_completo");
            ValueUtil.requiredString(data, "email");
            String password = password(data);
            data.put("contrasena_hash", CryptoUtil.sha256(password));
            data.remove("contrasena");
            data.remove("password");
            Map<String, Object> newUser = crud.create("usuarios", data);
            // crud.create ya elimina contrasena_hash via sanitize
            Map<String, Object> result = new LinkedHashMap<>();
            result.put("token", UUID.randomUUID().toString());
            result.put("usuario", newUser);
            return ApiResponse.created(result);
        } catch (IllegalStateException e) {
            return ApiResponse.conflict("El email ya esta registrado");
        } catch (IllegalArgumentException e) {
            return ApiResponse.badRequest(e.getMessage());
        } catch (Exception e) {
            return ApiResponse.serverError(e);
        }
    }

    @POST
    @Path("login")
    public Response login(Map<String, Object> data) {
        ConexionMySQL mysql = new ConexionMySQL();
        Connection conn = mysql.open();
        try {
            String email = ValueUtil.requiredString(data, "email");
            String passwordHash = CryptoUtil.sha256(password(data));
            Map<String, Object> user = JdbcUtil.queryOne(conn, "SELECT * FROM usuarios WHERE email = ?", email);
            if (user == null || !passwordHash.equals(user.get("contrasena_hash"))) {
                return ApiResponse.badRequest("Email o contrasena incorrectos");
            }
            if (isTrue(user.get("cuenta_bloqueada"))) {
                return ApiResponse.forbidden("La cuenta esta bloqueada");
            }
            user.remove("contrasena_hash");
            Map<String, Object> result = new LinkedHashMap<>();
            result.put("token", UUID.randomUUID().toString());
            result.put("usuario", user);
            return ApiResponse.ok(result);
        } catch (IllegalArgumentException e) {
            return ApiResponse.badRequest(e.getMessage());
        } catch (Exception e) {
            return ApiResponse.serverError(e);
        } finally {
            mysql.close();
        }
    }

    private String password(Map<String, Object> data) {
        String password = ValueUtil.string(data, "contrasena");
        if (password == null || password.isEmpty()) {
            password = ValueUtil.string(data, "password");
        }
        if (password == null || password.isEmpty()) {
            throw new IllegalArgumentException("El campo contrasena es obligatorio");
        }
        return password;
    }

    private boolean isTrue(Object value) {
        if (value instanceof Boolean bool) {
            return bool;
        }
        if (value instanceof Number number) {
            return number.intValue() != 0;
        }
        return Boolean.parseBoolean(String.valueOf(value));
    }
}
