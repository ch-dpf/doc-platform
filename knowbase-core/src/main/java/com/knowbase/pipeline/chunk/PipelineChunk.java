package com.knowbase.pipeline.chunk;

/**
 * 单条待索引分块：子块用于嵌入检索，parentContext 用于 RAG 上下文扩展（不单独嵌入）。
 */
public record PipelineChunk(String content, String parentContext, int parentIndex) {

    public static PipelineChunk leaf(String content) {
        return new PipelineChunk(content, null, -1);
    }

    public boolean hasParentContext() {
        return parentContext != null && !parentContext.isBlank();
    }
}
