package com.knowbase.vector.retrieval;

import com.knowbase.library.config.RetrievalRulesSettings;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 校验检索请求中的 metadata 过滤项，仅允许库级 {@code metadataFilterFields} 白名单字段。
 */
public final class MetadataFilterResolver {

    private MetadataFilterResolver() {}

    public static List<MetadataFilterClause> resolve(
            Map<String, String> requested,
            RetrievalRulesSettings retrieval) {
        if (requested == null || requested.isEmpty()) {
            return List.of();
        }
        List<String> allowedFields = retrieval != null ? retrieval.getMetadataFilterFields() : List.of();
        if (allowedFields == null || allowedFields.isEmpty()) {
            throw new InvalidMetadataFilterException("该知识库未配置可过滤的 metadata 字段");
        }
        Set<String> allowed = allowedFields.stream()
                .filter(field -> field != null && !field.isBlank())
                .map(String::trim)
                .collect(Collectors.toSet());
        if (allowed.isEmpty()) {
            throw new InvalidMetadataFilterException("该知识库未配置可过滤的 metadata 字段");
        }

        Map<String, String> normalized = new LinkedHashMap<>();
        for (Map.Entry<String, String> entry : requested.entrySet()) {
            if (entry.getKey() == null || entry.getKey().isBlank()) {
                continue;
            }
            String field = entry.getKey().trim();
            if (!allowed.contains(field)) {
                throw new InvalidMetadataFilterException("不允许的 metadata 过滤字段: " + field);
            }
            if (entry.getValue() == null || entry.getValue().isBlank()) {
                continue;
            }
            normalized.put(field, entry.getValue().trim());
        }
        if (normalized.isEmpty()) {
            return List.of();
        }

        List<MetadataFilterClause> clauses = new ArrayList<>(normalized.size());
        for (Map.Entry<String, String> entry : normalized.entrySet()) {
            clauses.add(new MetadataFilterClause(entry.getKey(), entry.getValue()));
        }
        return clauses;
    }
}
