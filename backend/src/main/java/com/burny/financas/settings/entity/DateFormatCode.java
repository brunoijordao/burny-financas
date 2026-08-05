package com.burny.financas.settings.entity;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

/**
 * Closed set of date display formats (see design.md Decision 2). The wire value is the literal
 * display pattern (e.g. {@code "DD/MM/YYYY"}), which isn't a valid Java identifier, so JSON
 * (de)serialization is customized instead of relying on Jackson's default {@code name()} mapping.
 * Persistence still uses {@code EnumType.STRING} (the constant name), which is unrelated to JSON.
 */
public enum DateFormatCode {
    DD_MM_YYYY("DD/MM/YYYY"),
    MM_DD_YYYY("MM/DD/YYYY"),
    YYYY_MM_DD("YYYY-MM-DD");

    private final String pattern;

    DateFormatCode(String pattern) {
        this.pattern = pattern;
    }

    @JsonValue
    public String getPattern() {
        return pattern;
    }

    @JsonCreator
    public static DateFormatCode fromPattern(String value) {
        for (DateFormatCode code : values()) {
            if (code.pattern.equals(value)) {
                return code;
            }
        }
        throw new IllegalArgumentException("Unsupported date format: " + value);
    }
}
