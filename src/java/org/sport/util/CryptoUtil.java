package org.sport.util;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;

public class CryptoUtil {
    private CryptoUtil() {
    }

    //Convierte contraseñas a formato sha256
    public static String sha256(String value) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(value.getBytes(StandardCharsets.UTF_8));
            StringBuilder hex = new StringBuilder();
            for (byte b : hash) {
                hex.append(String.format("%02x", b));
            }
            return hex.toString();
        } catch (Exception e) {
            throw new IllegalStateException("No fue posible generar el hash", e);
        }
    }
}
