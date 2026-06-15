package com.knowbase.platform;

import com.knowbase.library.config.VectorLibraryConfig;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

public final class JsonSupport {

    private static final ObjectMapper MAPPER = new ObjectMapper()
            .registerModule(new JavaTimeModule())
            .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);

    private JsonSupport() {
    }

    public static ObjectMapper mapper() {
        return MAPPER;
    }

    public static String toJson(Object value) {
        try {
            return MAPPER.writeValueAsString(value);
        } catch (Exception e) {
            throw new IllegalStateException("JSON serialize failed", e);
        }
    }

    public static <T> T fromJson(String json, Class<T> type) {
        try {
            return MAPPER.readValue(json, type);
        } catch (Exception e) {
            throw new IllegalStateException("JSON parse failed", e);
        }
    }

    public static VectorLibraryConfig parseLibraryConfig(String json) {
        if (json == null || json.isBlank()) {
            return new VectorLibraryConfig();
        }
        try {
            return fromJson(json, VectorLibraryConfig.class);
        } catch (IllegalStateException ex) {
            return new VectorLibraryConfig();
        }
    }
}
