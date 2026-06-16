package com.knowbase.library.config;

import com.knowbase.library.dto.config.LibraryIndexPipelineDto;
import com.knowbase.library.dto.config.LibraryParsingDto;

import java.util.ArrayList;
import java.util.List;

/** 按配置分节合并向量库 config_json。 */
public final class VectorLibraryConfigMerger {

    private VectorLibraryConfigMerger() {
    }

    public static void mergeBasic(VectorLibraryConfig target, List<String> tags) {
        if (tags != null) {
            target.setTags(new ArrayList<>(tags));
        }
    }

    public static void mergeIndexPipeline(VectorLibraryConfig target, LibraryIndexPipelineDto dto) {
        if (dto == null) {
            return;
        }
        if (dto.embeddingModel() != null) {
            target.setEmbeddingProvider("ollama");
            target.setEmbeddingModel(dto.embeddingModel());
        }
        if (dto.embeddingDimension() > 0) {
            target.setEmbeddingDimension(dto.embeddingDimension());
        }
        target.setChunkSize(dto.chunkSize());
        target.setChunkOverlap(dto.chunkOverlap());
        target.setHierarchicalChunkingEnabled(dto.hierarchicalChunkingEnabled());
        target.setChunkDelimiter(dto.chunkDelimiter() != null ? dto.chunkDelimiter().strip() : "");
    }

    public static void mergeRetrieval(VectorLibraryConfig target, RetrievalRulesSettings retrieval) {
        if (retrieval != null) {
            target.setRetrieval(retrieval);
        }
    }

    public static void mergeParsing(VectorLibraryConfig target, LibraryParsingDto dto) {
        if (dto == null) {
            return;
        }
        if (dto.parserRules() != null) {
            target.setParserRules(new ArrayList<>(dto.parserRules()));
        }
        if (target.getParsing() == null) {
            target.setParsing(new ParsingRulesSettings());
        }
        if (dto.defaultLanguage() != null && !dto.defaultLanguage().isBlank()) {
            target.getParsing().setDefaultLanguage(dto.defaultLanguage().trim());
        }
        if (dto.autoDetectEncoding() != null) {
            target.getParsing().setAutoDetectEncoding(dto.autoDetectEncoding());
        }
    }

    /** @deprecated 单体更新已废弃，保留供测试对照 */
    @Deprecated
    public static void mergeSafeFields(VectorLibraryConfig target, VectorLibraryConfig incoming, boolean lockPipelineConfig) {
        if (incoming == null) {
            return;
        }
        mergeBasic(target, incoming.getTags());
        if (incoming.getRetrieval() != null) {
            mergeRetrieval(target, incoming.getRetrieval());
        }
        if (lockPipelineConfig) {
            return;
        }
        mergeIndexPipeline(target, LibraryIndexPipelineDtoFromConfig.from(incoming));
    }

    private static final class LibraryIndexPipelineDtoFromConfig {
        private LibraryIndexPipelineDtoFromConfig() {
        }

        static LibraryIndexPipelineDto from(VectorLibraryConfig cfg) {
            return new LibraryIndexPipelineDto(
                    cfg.getChunkSize(),
                    cfg.getChunkOverlap(),
                    cfg.getEmbeddingModel(),
                    cfg.getEmbeddingDimension(),
                    cfg.isHierarchicalChunkingEnabled(),
                    cfg.getChunkDelimiter());
        }
    }
}
