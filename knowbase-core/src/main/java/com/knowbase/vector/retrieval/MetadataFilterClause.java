package com.knowbase.vector.retrieval;

/**
 * 单条 chunk.metadata JSONB 等值过滤条件。
 */
public record MetadataFilterClause(String field, String value, FilterOperator operator) {

    public enum FilterOperator {
        EQ,
        GTE,
        LTE
    }

    public MetadataFilterClause(String field, String value) {
        this(field, value, FilterOperator.EQ);
    }
}
