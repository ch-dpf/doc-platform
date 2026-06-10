package com.knowbase.ingest.support;

import com.knowbase.platform.JsonSupport;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 解析上传时附带的文档级自定义 metadata（JSON 对象，值为字符串）。
 */
public final class DocumentCustomMetadataSupport {

    private DocumentCustomMetadataSupport() {}

    public static String normalizeJson(String raw) {
        if (raw == null || raw.isBlank()) {
            return null;
        }
        Map<?, ?> parsed = JsonSupport.fromJson(raw.trim(), Map.class);
        if (parsed.isEmpty()) {
            return null;
        }
        Map<String, String> normalized = new LinkedHashMap<>();
        for (Map.Entry<?, ?> entry : parsed.entrySet()) {
            if (entry.getKey() == null) {
                continue;
            }
            String key = String.valueOf(entry.getKey()).trim();
            if (key.isEmpty()) {
                continue;
            }
            if (entry.getValue() == null) {
                throw new IllegalArgumentException("documentMetadata 字段 " + key + " 的值不能为空");
            }
            String value = String.valueOf(entry.getValue()).trim();
            if (value.isEmpty()) {
                throw new IllegalArgumentException("documentMetadata 字段 " + key + " 的值不能为空");
            }
            normalized.put(key, value);
        }
        return normalized.isEmpty() ? null : JsonSupport.toJson(normalized);
    }
}
