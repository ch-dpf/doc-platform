package com.knowbase.vector.dto;

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
        String chunkProfileId) {

    public SearchHit(
            UUID chunkId,
            UUID docId,
            String tenantId,
            int version,
            int chunkIndex,
            String content,
            double score) {
        this(chunkId, docId, tenantId, version, chunkIndex, content, score, null, null);
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
        this(chunkId, docId, tenantId, version, chunkIndex, content, score, parentContext, null);
    }

    public String contextForPrompt() {
        if (parentContext == null || parentContext.isBlank()) {
            return content != null ? content.strip() : "";
        }
        String child = content != null ? content.strip() : "";
        return "【章节上下文】\n" + parentContext.strip() + "\n【命中片段】\n" + child;
    }
}
