package com.knowbase.library.config;

import com.knowbase.vector.chunk.ChunkingStrategy;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * 知识库默认配置工厂（快速创建 / 高级配置共用）。
 */
public final class VectorLibraryConfigFactory {

    private static final Map<String, List<String>> FILE_TYPE_MIMES = Map.of(
            "pdf", List.of("application/pdf"),
            "word", List.of(
                    "application/msword",
                    "application/vnd.openxmlformats-officedocument.wordprocessingml.document"),
            "txt", List.of("text/plain"),
            "markdown", List.of("text/markdown", "text/x-markdown"),
            "excel", List.of(
                    "application/vnd.ms-excel",
                    "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"));

    private VectorLibraryConfigFactory() {
    }

    public static VectorLibraryConfig quickDefaults(List<String> globalMimeTypes) {
        return applyPhase2Defaults(new VectorLibraryConfig(), globalMimeTypes, "quick");
    }

    public static VectorLibraryConfig advancedDefaults(List<String> globalMimeTypes) {
        return applyPhase2Defaults(new VectorLibraryConfig(), globalMimeTypes, "advanced");
    }

    public static VectorLibraryConfig applyPhase2Defaults(
            VectorLibraryConfig cfg, List<String> globalMimeTypes, String wizardMode) {
        if (cfg.getConfigVersion() <= 0) {
            cfg.setConfigVersion(1);
        }
        if (cfg.getWizardMode() == null || cfg.getWizardMode().isBlank()) {
            cfg.setWizardMode(wizardMode != null ? wizardMode : "quick");
        }
        if (cfg.getTags() == null) {
            cfg.setTags(new ArrayList<>());
        }
        if (cfg.getIngestAccess() == null) {
            cfg.setIngestAccess(new IngestAccessSettings());
        }
        if (cfg.getParsing() == null) {
            cfg.setParsing(new ParsingRulesSettings());
        }
        if (cfg.getCleaning() == null) {
            cfg.setCleaning(new CleaningRulesSettings());
        }
        if (cfg.getRetrieval() == null) {
            cfg.setRetrieval(new RetrievalRulesSettings());
        }
        if (cfg.getGovernance() == null) {
            cfg.setGovernance(new GovernanceRulesSettings());
        }
        if (cfg.getTextNormalization() == null) {
            cfg.setTextNormalization(new TextNormalizationSettings());
        }

        cfg.setIngestSourceMode("upload");
        cfg.getIngestAccess().setAccessMode(IngestAccessSettings.FIXED_ACCESS_MODE);
        if (cfg.getIngestAccess().getSupportedFileTypes() == null
                || cfg.getIngestAccess().getSupportedFileTypes().isEmpty()) {
            cfg.getIngestAccess().setSupportedFileTypes(IngestAccessSettings.defaultFileTypes());
        }
        if (cfg.getAllowedMimeTypes() == null || cfg.getAllowedMimeTypes().isEmpty()) {
            cfg.setAllowedMimeTypes(resolveMimeTypes(cfg.getIngestAccess().getSupportedFileTypes(), globalMimeTypes));
        }
        if (cfg.getChunkingStrategy() == null) {
            cfg.setChunkingStrategy(ChunkingStrategy.PARAGRAPH_FIRST);
        }
        if (cfg.getChunkSize() <= 0) {
            cfg.setChunkSize(600);
        }
        if (cfg.getChunkOverlap() <= 0) {
            cfg.setChunkOverlap(100);
        }
        return cfg;
    }

    public static List<String> resolveMimeTypes(List<String> fileTypes, List<String> globalFallback) {
        if (fileTypes == null || fileTypes.isEmpty()) {
            return globalFallback != null ? globalFallback : List.of();
        }
        List<String> mimes = new ArrayList<>();
        for (String type : fileTypes) {
            List<String> mapped = FILE_TYPE_MIMES.get(type != null ? type.trim().toLowerCase() : "");
            if (mapped != null) {
                mimes.addAll(mapped);
            }
        }
        return mimes.isEmpty() ? (globalFallback != null ? globalFallback : List.of()) : mimes;
    }
}
