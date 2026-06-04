package com.docplatform.library.config;

/**
 * 合并向量库配置时仅覆盖低风险字段，保留存储与数据源等创建时设定。
 */
public final class VectorLibraryConfigMerger {

    private VectorLibraryConfigMerger() {
    }

    public static void mergeSafeFields(VectorLibraryConfig target, VectorLibraryConfig incoming) {
        if (incoming == null) {
            return;
        }
        if (incoming.getEmbeddingProvider() != null) {
            target.setEmbeddingProvider(incoming.getEmbeddingProvider());
        }
        if (incoming.getEmbeddingModel() != null) {
            target.setEmbeddingModel(incoming.getEmbeddingModel());
        }
        if (incoming.getEmbeddingDimension() > 0) {
            target.setEmbeddingDimension(incoming.getEmbeddingDimension());
        }
        if (incoming.getChunkingStrategy() != null) {
            target.setChunkingStrategy(incoming.getChunkingStrategy());
        }
        target.setChunkSize(incoming.getChunkSize());
        target.setChunkOverlap(incoming.getChunkOverlap());
        target.setMinChunkSize(incoming.getMinChunkSize());
        target.setMaxChunkSize(incoming.getMaxChunkSize());
        target.setMinParagraphLength(incoming.getMinParagraphLength());
        target.setNormalizeBeforeChunk(incoming.isNormalizeBeforeChunk());
        target.setTextNormalizationEnabled(incoming.isTextNormalizationEnabled());
        if (incoming.getTextNormalization() != null) {
            target.setTextNormalization(incoming.getTextNormalization());
        }
    }
}
