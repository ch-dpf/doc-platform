package com.knowbase.library.config;



/**

 * 合并向量库配置：按是否已有入库内容区分可写字段。

 */

public final class VectorLibraryConfigMerger {



    private VectorLibraryConfigMerger() {

    }



    public static void mergeSafeFields(VectorLibraryConfig target, VectorLibraryConfig incoming) {

        mergeSafeFields(target, incoming, false);

    }



    /**

     * @param lockPipelineConfig true 时锁定解析/清洗/分块/向量化等影响已入库文档的字段

     */

    public static void mergeSafeFields(

            VectorLibraryConfig target, VectorLibraryConfig incoming, boolean lockPipelineConfig) {

        if (incoming == null) {

            return;

        }

        if (incoming.getTags() != null) {

            target.setTags(incoming.getTags());

        }

        if (incoming.getLibraryPresetId() != null) {

            target.setLibraryPresetId(incoming.getLibraryPresetId());

        }

        if (incoming.getRetrieval() != null) {

            target.setRetrieval(incoming.getRetrieval());

        }

        if (incoming.getGovernance() != null) {

            target.setGovernance(incoming.getGovernance());

        }

        mergeIngestAccess(target, incoming);



        if (lockPipelineConfig) {

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

        if (incoming.getParsing() != null) {

            target.setParsing(incoming.getParsing());

        }

        if (incoming.getCleaning() != null) {

            target.setCleaning(incoming.getCleaning());

        }

    }



    private static void mergeIngestAccess(VectorLibraryConfig target, VectorLibraryConfig incoming) {

        if (incoming.getIngestAccess() == null) {

            return;

        }

        if (target.getIngestAccess() == null) {

            target.setIngestAccess(incoming.getIngestAccess());

            return;

        }

        if (incoming.getIngestAccess().getSupportedFileTypes() != null) {

            target.getIngestAccess().setSupportedFileTypes(incoming.getIngestAccess().getSupportedFileTypes());

        }

        if (incoming.getIngestAccess().getCapacityLimits() != null) {

            target.getIngestAccess().setCapacityLimits(incoming.getIngestAccess().getCapacityLimits());

        }

        if (incoming.getIngestAccess().getVersionPolicy() != null) {

            target.getIngestAccess().setVersionPolicy(incoming.getIngestAccess().getVersionPolicy());

        }

    }

}


