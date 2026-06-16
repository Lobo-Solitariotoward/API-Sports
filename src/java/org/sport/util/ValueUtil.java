package org.sport.util;

import java.math.BigDecimal;
import java.util.Map;


//lee valores que vienen en JSON, como strings, booleanos, y decimales
public class ValueUtil {
    private ValueUtil() {
    }

    public static String string(Map<String, Object> data, String key) {
        Object value = data.get(key);
        return value == null ? null : String.valueOf(value).trim();
    }

    public static String requiredString(Map<String, Object> data, String key) {
        String value = string(data, key);
        if (value == null || value.isEmpty()) {
            throw new IllegalArgumentException("El campo " + key + " es obligatorio");
        }
        return value;
    }

    public static Integer integer(Map<String, Object> data, String key) {
        Object value = data.get(key);
        if (value == null || String.valueOf(value).isBlank()) {
            return null;
        }
        if (value instanceof Number number) {
            return number.intValue();
        }
        return Integer.valueOf(String.valueOf(value));
    }

    public static int requiredInt(Map<String, Object> data, String key) {
        Integer value = integer(data, key);
        if (value == null) {
            throw new IllegalArgumentException("El campo " + key + " es obligatorio");
        }
        return value;
    }

    public static BigDecimal decimal(Map<String, Object> data, String key) {
        Object value = data.get(key);
        if (value == null || String.valueOf(value).isBlank()) {
            return null;
        }
        if (value instanceof BigDecimal decimal) {
            return decimal;
        }
        return new BigDecimal(String.valueOf(value));
    }

    public static boolean bool(Map<String, Object> data, String key, boolean defaultValue) {
        Object value = data.get(key);
        if (value == null) {
            return defaultValue;
        }
        if (value instanceof Boolean bool) {
            return bool;
        }
        if (value instanceof Number number) {
            return number.intValue() != 0;
        }
        return Boolean.parseBoolean(String.valueOf(value));
    }
}
