package com.knowbase.library.support;

import com.fasterxml.jackson.core.type.TypeReference;
import com.knowbase.platform.JsonSupport;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public final class FailedDocIdsJson {

    private static final TypeReference<List<UUID>> UUID_LIST = new TypeReference<>() {};

    private FailedDocIdsJson() {}

    public static List<UUID> parse(String json) {
        if (json == null || json.isBlank()) {
            return List.of();
        }
        try {
            List<UUID> ids = JsonSupport.mapper().readValue(json, UUID_LIST);
            return ids != null ? ids : List.of();
        } catch (Exception e) {
            return List.of();
        }
    }

    public static String append(String existingJson, UUID docId) {
        List<UUID> ids = new ArrayList<>(parse(existingJson));
        if (!ids.contains(docId)) {
            ids.add(docId);
        }
        return JsonSupport.toJson(ids);
    }

    public static String empty() {
        return "[]";
    }
}
