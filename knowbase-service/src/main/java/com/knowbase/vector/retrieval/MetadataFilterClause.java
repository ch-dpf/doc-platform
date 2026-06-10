package com.knowbase.vector.retrieval;

/**
 * 单条 chunk.metadata JSONB 等值过滤条件。
 */
public record MetadataFilterClause(String field, String value) {
}
