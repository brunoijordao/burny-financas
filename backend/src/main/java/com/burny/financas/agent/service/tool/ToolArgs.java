package com.burny.financas.agent.service.tool;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Defensive parsing helpers for Gemini {@code functionCall} arguments. Jackson deserializes an
 * untyped JSON object's numeric fields inconsistently (Integer/Long/Double depending on magnitude
 * and JSON type), and a model can legitimately send a numeric id as either a JSON number or a JSON
 * string — every helper here tolerates both rather than assuming one shape.
 */
final class ToolArgs {

    private ToolArgs() {
    }

    static Long asLong(Object value) {
        if (value == null) {
            return null;
        }
        if (value instanceof Number number) {
            return number.longValue();
        }
        String text = String.valueOf(value).trim();
        if (text.isEmpty()) {
            return null;
        }
        try {
            return Long.parseLong(text);
        } catch (NumberFormatException e) {
            return null;
        }
    }

    static BigDecimal asBigDecimal(Object value) {
        if (value == null) {
            return null;
        }
        if (value instanceof Number number) {
            return BigDecimal.valueOf(number.doubleValue());
        }
        String text = String.valueOf(value).trim();
        if (text.isEmpty()) {
            return null;
        }
        try {
            return new BigDecimal(text);
        } catch (NumberFormatException e) {
            return null;
        }
    }

    static String asString(Object value) {
        if (value == null) {
            return null;
        }
        String text = String.valueOf(value).trim();
        return text.isEmpty() ? null : text;
    }

    static LocalDate asLocalDate(Object value) {
        String text = asString(value);
        if (text == null) {
            return null;
        }
        try {
            return LocalDate.parse(text);
        } catch (Exception e) {
            return null;
        }
    }

    /** Builds a {@code LinkedHashMap} (unlike {@code Map.of}, tolerates null values) from alternating key/value pairs. */
    static Map<String, Object> map(Object... keyValuePairs) {
        Map<String, Object> result = new LinkedHashMap<>();
        for (int i = 0; i < keyValuePairs.length; i += 2) {
            result.put((String) keyValuePairs[i], keyValuePairs[i + 1]);
        }
        return result;
    }
}
