package com.cloudbrain.application.ai;

import java.util.LinkedHashMap;
import java.util.Map;

public final class AITextParser {

    private AITextParser() {
    }

    public static Map<String, String> parseKeyValueBlock(String text) {
        Map<String, String> values = new LinkedHashMap<>();
        if (text == null || text.isBlank()) {
            return values;
        }
        String[] lines = text.split("\\R");
        for (String line : lines) {
            int index = line.indexOf(':');
            if (index <= 0) {
                continue;
            }
            String key = line.substring(0, index).trim();
            String value = line.substring(index + 1).trim();
            if (!key.isBlank()) {
                values.put(key, value);
            }
        }
        return values;
    }

    public static String firstNonBlank(Map<String, String> values, String key, String fallback) {
        if (values == null) {
            return fallback;
        }
        String value = values.get(key);
        return value == null || value.isBlank() ? fallback : value.trim();
    }
}
