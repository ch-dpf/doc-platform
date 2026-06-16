package com.knowbase.library.config;

import com.knowbase.vector.retrieval.TemporalMetadataFields;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/** 知识库默认配置工厂（建库短表单与后端缺省共用）。 */
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

    public static VectorLibraryConfig defaults(List<String> globalMimeTypes) {
        return applyDefaults(new VectorLibraryConfig(), globalMimeTypes);
    }

    public static VectorLibraryConfig applyDefaults(VectorLibraryConfig cfg, List<String> globalMimeTypes) {
        if (cfg.getConfigVersion() <= 0) {
            cfg.setConfigVersion(1);
        }
        if (cfg.getTags() == null) {
            cfg.setTags(new ArrayList<>());
        }
        if (cfg.getIngestAccess() == null) {
            cfg.setIngestAccess(new IngestAccessSettings());
        }
        if (cfg.getRetrieval() == null) {
            cfg.setRetrieval(new RetrievalRulesSettings());
        }
        applyRetrievalDefaults(cfg.getRetrieval());
        applyParsingDefaults(cfg, globalMimeTypes);
        if (cfg.getGovernance() == null) {
            cfg.setGovernance(new GovernanceRulesSettings());
        }
        cfg.setIngestSourceMode("upload");
        cfg.getIngestAccess().setAccessMode(IngestAccessSettings.FIXED_ACCESS_MODE);
        cfg.setAllowedMimeTypes(globalMimeTypes != null ? globalMimeTypes : List.of());
        if (cfg.getChunkSize() <= 0) {
            cfg.setChunkSize(500);
        }
        if (cfg.getChunkOverlap() <= 0) {
            cfg.setChunkOverlap(120);
        }
        if (cfg.getChunkingStrategy() == null) {
            cfg.setChunkingStrategy(com.knowbase.vector.chunk.ChunkingStrategy.AUTO);
        }
        if (cfg.getEmbeddingProvider() == null || cfg.getEmbeddingProvider().isBlank()) {
            cfg.setEmbeddingProvider("ollama");
        }
        if (cfg.getEmbeddingModel() == null || cfg.getEmbeddingModel().isBlank()) {
            cfg.setEmbeddingModel("nomic-embed-text");
        }
        if (cfg.getEmbeddingDimension() <= 0) {
            cfg.setEmbeddingDimension(768);
        }
        return cfg;
    }

    private static void applyRetrievalDefaults(RetrievalRulesSettings retrieval) {
        if (retrieval == null) {
            return;
        }
        retrieval.setHybridSearchEnabled(true);
        retrieval.setRerankEnabled(true);
        if (retrieval.getSimilarityThreshold() <= 0.0) {
            retrieval.setSimilarityThreshold(0.4);
        }
        if (retrieval.getDefaultTopK() <= 0) {
            retrieval.setDefaultTopK(12);
        }
        if (retrieval.getMetadataFilterFields() == null
                || retrieval.getMetadataFilterFields().isEmpty()) {
            retrieval.setMetadataFilterFields(
                    new ArrayList<>(TemporalMetadataFields.defaultFilterWhitelist()));
        }
    }

    private static void applyParsingDefaults(VectorLibraryConfig cfg, List<String> globalMimeTypes) {
        if (cfg.getParsing() == null) {
            cfg.setParsing(new ParsingRulesSettings());
        }
        ParsingRulesSettings parsing = cfg.getParsing();
        if (parsing.getDefaultLanguage() == null || parsing.getDefaultLanguage().isBlank()) {
            parsing.setDefaultLanguage("zh-CN");
        }
        if (cfg.getParserRules() == null || cfg.getParserRules().isEmpty()) {
            List<ParserEngineRule> rules = new ArrayList<>();
            for (String fileType : systemSupportedFileTypes(globalMimeTypes)) {
                ParserEngineRule rule = new ParserEngineRule();
                rule.setFileType(fileType);
                rule.setParserId("auto");
                rules.add(rule);
            }
            cfg.setParserRules(rules);
        }
    }

    /** 由系统 MIME 白名单推导对外展示的文件类型标识（一期只读）。 */
    public static List<String> systemSupportedFileTypes(List<String> globalMimeTypes) {
        if (globalMimeTypes == null || globalMimeTypes.isEmpty()) {
            return List.of();
        }
        List<String> types = new ArrayList<>();
        for (Map.Entry<String, List<String>> entry : FILE_TYPE_MIMES.entrySet()) {
            boolean matched = entry.getValue().stream().anyMatch(globalMimeTypes::contains);
            if (matched) {
                types.add(entry.getKey());
            }
        }
        return types;
    }

    /** 由 MIME 或文件名扩展名解析文件类型标识（pdf/word/excel/txt/markdown）。 */
    public static String resolveFileType(String mimeType, String fileName) {
        if (mimeType != null && !mimeType.isBlank()) {
            String m = mimeType.trim().toLowerCase();
            for (Map.Entry<String, List<String>> entry : FILE_TYPE_MIMES.entrySet()) {
                for (String mapped : entry.getValue()) {
                    if (mapped.equalsIgnoreCase(m)) {
                        return entry.getKey();
                    }
                }
            }
        }
        if (fileName != null) {
            String lower = fileName.trim().toLowerCase();
            if (lower.endsWith(".pdf")) {
                return "pdf";
            }
            if (lower.endsWith(".doc") || lower.endsWith(".docx")) {
                return "word";
            }
            if (lower.endsWith(".xls") || lower.endsWith(".xlsx")) {
                return "excel";
            }
            if (lower.endsWith(".txt")) {
                return "txt";
            }
            if (lower.endsWith(".md") || lower.endsWith(".markdown")) {
                return "markdown";
            }
        }
        return "";
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
