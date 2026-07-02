package com.knowbase.persistence.support;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.knowbase.domain.model.EvidencePack;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public final class JsonSupport {

    private static final ObjectMapper MAPPER = new ObjectMapper().registerModule(new JavaTimeModule());

    private JsonSupport() {
    }

    public static String write(Object value) {
        if (value == null) {
            return null;
        }
        try {
            return MAPPER.writeValueAsString(value);
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException("JSON 序列化失败", exception);
        }
    }

    public static Map<String, Object> readMap(String json) {
        if (json == null || json.isBlank()) {
            return Map.of();
        }
        try {
            return MAPPER.readValue(json, new TypeReference<>() {
            });
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException("JSON 反序列化失败", exception);
        }
    }

    public static List<String> readStringList(String json) {
        if (json == null || json.isBlank()) {
            return List.of();
        }
        try {
            return MAPPER.readValue(json, new TypeReference<>() {
            });
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException("JSON 反序列化失败", exception);
        }
    }

    public static List<UUID> readUuidList(String json) {
        if (json == null || json.isBlank()) {
            return List.of();
        }
        try {
            return MAPPER.readValue(json, new TypeReference<>() {
            });
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException("JSON 反序列化失败", exception);
        }
    }

    public static Map<String, Double> readDoubleMap(String json) {
        Map<String, Object> raw = readMap(json);
        if (raw.isEmpty()) {
            return Map.of();
        }
        Map<String, Double> result = new HashMap<>();
        raw.forEach((key, value) -> {
            if (value instanceof Number number) {
                result.put(key, number.doubleValue());
            } else if (value != null) {
                try {
                    result.put(key, Double.parseDouble(String.valueOf(value)));
                } catch (NumberFormatException ignored) {
                    // 忽略非法分层召回值
                }
            }
        });
        return Map.copyOf(result);
    }

    public static EvidencePack readEvidencePack(String json) {
        if (json == null || json.isBlank()) {
            return null;
        }
        try {
            return MAPPER.readValue(json, EvidencePack.class);
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException("EvidencePack 反序列化失败", exception);
        }
    }
}
