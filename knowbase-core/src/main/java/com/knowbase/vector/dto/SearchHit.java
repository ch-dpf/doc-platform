package com.knowbase.vector.dto;

import java.util.Map;
import java.util.UUID;

public record SearchHit(
        UUID chunkId,
        UUID docId,
        String tenantId,
        int version,
        int chunkIndex,
        String content,
        double score,
        String parentContext,
        String chunkProfileId,
        TemporalChunkMetadata temporal) {

    /**
     * MyBatis 行映射构造：JDBC 数值列可能为 null，不可直接写入 primitive 字段。
     */
    public SearchHit(
            UUID chunkId,
            UUID docId,
            String tenantId,
            Integer version,
            Integer chunkIndex,
            String content,
            Double score,
            String parentContext,
            String chunkProfileId,
            String periodYear,
            String periodStart,
            String periodEnd,
            String periodMonths,
            String submitter,
            String sectionLabel,
            String hasCompletedWork) {
        this(
                chunkId,
                docId,
                tenantId,
                version != null ? version : 0,
                chunkIndex != null ? chunkIndex : 0,
                content,
                score != null ? score : 0.0,
                parentContext,
                chunkProfileId,
                new TemporalChunkMetadata(
                        periodYear,
                        periodStart,
                        periodEnd,
                        periodMonths,
                        submitter,
                        sectionLabel,
                        hasCompletedWork));
    }

    public SearchHit(
            UUID chunkId,
            UUID docId,
            String tenantId,
            int version,
            int chunkIndex,
            String content,
            double score) {
        this(chunkId, docId, tenantId, version, chunkIndex, content, score, null, null, TemporalChunkMetadata.empty());
    }

    public SearchHit(
            UUID chunkId,
            UUID docId,
            String tenantId,
            int version,
            int chunkIndex,
            String content,
            double score,
            String parentContext) {
        this(chunkId, docId, tenantId, version, chunkIndex, content, score, parentContext, null, TemporalChunkMetadata.empty());
    }

    public SearchHit(
            UUID chunkId,
            UUID docId,
            String tenantId,
            int version,
            int chunkIndex,
            String content,
            double score,
            String parentContext,
            String chunkProfileId) {
        this(
                chunkId,
                docId,
                tenantId,
                version,
                chunkIndex,
                content,
                score,
                parentContext,
                chunkProfileId,
                TemporalChunkMetadata.empty());
    }

    public Map<String, String> temporalMetadataMap() {
        return temporal != null ? temporal.asMap() : Map.of();
    }

    public String contextForPrompt() {
        if (parentContext == null || parentContext.isBlank()) {
            return content != null ? content.strip() : "";
        }
        String child = content != null ? content.strip() : "";
        return "【章节上下文】\n" + parentContext.strip() + "\n【命中片段】\n" + child;
    }
}
