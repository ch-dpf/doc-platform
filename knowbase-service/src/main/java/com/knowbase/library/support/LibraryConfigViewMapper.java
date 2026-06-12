package com.knowbase.library.support;

import com.knowbase.library.config.RetrievalRulesSettings;
import com.knowbase.library.config.VectorLibraryConfig;
import com.knowbase.library.dto.config.LibraryConfigView;
import com.knowbase.library.dto.config.LibraryIndexPipelineDto;

import java.util.ArrayList;
import java.util.List;

/** config_json ↔ 分节库配置 API 视图。 */
public final class LibraryConfigViewMapper {

    private LibraryConfigViewMapper() {
    }

    public static LibraryConfigView toView(VectorLibraryConfig cfg) {
        if (cfg == null) {
            cfg = new VectorLibraryConfig();
        }
        return new LibraryConfigView(
                Math.max(1, cfg.getConfigVersion()),
                cfg.getMetadataDbType(),
                copyTags(cfg.getTags()),
                toIndexPipeline(cfg),
                copyRetrieval(cfg.getRetrieval()),
                cfg.getPrimaryChunkProfileId(),
                cfg.isAllowCustomChunkProfiles(),
                cfg.getMaxActiveChunkProfiles());
    }

    public static LibraryIndexPipelineDto toIndexPipeline(VectorLibraryConfig cfg) {
        return new LibraryIndexPipelineDto(
                cfg.getChunkSize(),
                cfg.getChunkOverlap(),
                cfg.getEmbeddingModel(),
                cfg.getEmbeddingDimension(),
                cfg.isHierarchicalChunkingEnabled(),
                cfg.getChunkDelimiter());
    }

    private static RetrievalRulesSettings copyRetrieval(RetrievalRulesSettings src) {
        if (src == null) {
            return new RetrievalRulesSettings();
        }
        RetrievalRulesSettings copy = new RetrievalRulesSettings();
        copy.setHybridSearchEnabled(src.isHybridSearchEnabled());
        copy.setRerankEnabled(src.isRerankEnabled());
        copy.setRerankModel(src.getRerankModel());
        copy.setSimilarityThreshold(src.getSimilarityThreshold());
        copy.setDefaultTopK(src.getDefaultTopK() > 0 ? src.getDefaultTopK() : 12);
        copy.setMetadataFilterFields(src.getMetadataFilterFields() != null
                ? new ArrayList<>(src.getMetadataFilterFields())
                : new ArrayList<>());
        return copy;
    }

    private static List<String> copyTags(List<String> tags) {
        return tags != null ? new ArrayList<>(tags) : new ArrayList<>();
    }
}
