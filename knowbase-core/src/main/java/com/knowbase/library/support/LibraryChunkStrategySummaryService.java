package com.knowbase.library.support;

import com.knowbase.ingest.parse.ParserEngineRegistry;
import com.knowbase.ingest.parse.ParserRuleResolver;
import com.knowbase.library.config.CleaningRulesSettings;
import com.knowbase.library.config.ParsingRulesSettings;
import com.knowbase.library.config.VectorLibraryConfig;
import com.knowbase.library.config.VectorLibraryConfigFactory;
import com.knowbase.library.dto.ChunkStrategySummaryRow;
import com.knowbase.library.service.LibraryConfigResolver;
import com.knowbase.pipeline.config.MimeTypePipelineDefaults;
import com.knowbase.pipeline.config.PlatformPipelineDefaults;
import com.knowbase.vector.chunk.ChunkingStrategy;
import com.knowbase.vector.config.ChunkingProperties;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
public class LibraryChunkStrategySummaryService {

    private static final Map<String, String> FILE_TYPE_LABELS = Map.of(
            "pdf", "PDF",
            "word", "Word",
            "txt", "TXT",
            "markdown", "Markdown",
            "excel", "Excel");

    private static final Map<String, String> STRATEGY_LABELS = Map.of(
            ChunkingStrategy.PARAGRAPH_FIRST.toWire(), "按段落",
            ChunkingStrategy.HEADING_LEVEL.toWire(), "按标题层级",
            ChunkingStrategy.FIXED_CHAR.toWire(), "固定长度",
            ChunkingStrategy.SEMANTIC.toWire(), "语义分块");

    private final LibraryConfigResolver libraryConfigResolver;
    private final MimeTypePipelineDefaults mimeDefaults;
    private final ParserEngineRegistry parserEngineRegistry;

    public LibraryChunkStrategySummaryService(
            LibraryConfigResolver libraryConfigResolver,
            MimeTypePipelineDefaults mimeDefaults,
            ParserEngineRegistry parserEngineRegistry) {
        this.libraryConfigResolver = libraryConfigResolver;
        this.mimeDefaults = mimeDefaults;
        this.parserEngineRegistry = parserEngineRegistry;
    }

    public List<ChunkStrategySummaryRow> summarize(UUID libraryId) {
        VectorLibraryConfig cfg = libraryConfigResolver.config(libraryId);
        ChunkingProperties libraryBase = libraryConfigResolver.chunkingFor(libraryId);
        List<String> fileTypes =
                VectorLibraryConfigFactory.systemSupportedFileTypes(cfg.getAllowedMimeTypes());
        List<ChunkStrategySummaryRow> rows = new ArrayList<>();
        for (String fileType : fileTypes) {
            rows.add(rowForFileType(fileType, libraryBase, cfg));
        }
        return rows;
    }

    private ChunkStrategySummaryRow rowForFileType(
            String fileType, ChunkingProperties libraryBase, VectorLibraryConfig cfg) {
        String mime = VectorLibraryConfigFactory.resolveMimeTypes(List.of(fileType), List.of()).stream()
                .findFirst()
                .orElse("application/octet-stream");
        ParsingRulesSettings parsing =
                PlatformPipelineDefaults.copyParsing(PlatformPipelineDefaults.baselineParsing());
        CleaningRulesSettings cleaning =
                PlatformPipelineDefaults.copyCleaning(PlatformPipelineDefaults.baselineCleaning());
        ChunkingProperties chunking = PlatformPipelineDefaults.copyChunking(libraryBase);
        mimeDefaults.apply(mime, parsing, cleaning);
        String parserId = ParserRuleResolver.resolveParserId(cfg.getParserRules(), fileType);
        parserEngineRegistry.apply(parserId, parsing);
        if (cfg.getParsing() != null) {
            if (cfg.getParsing().getDefaultLanguage() != null && !cfg.getParsing().getDefaultLanguage().isBlank()) {
                parsing.setDefaultLanguage(cfg.getParsing().getDefaultLanguage().trim());
            }
            parsing.setAutoDetectEncoding(cfg.getParsing().isAutoDetectEncoding());
        }

        String strategyWire = chunking.getStrategy() != null
                ? chunking.getStrategy().toWire()
                : ChunkingStrategy.PARAGRAPH_FIRST.toWire();
        boolean hierarchical = cfg.isHierarchicalChunkingEnabled()
                && chunking.getStrategy() == ChunkingStrategy.HEADING_LEVEL;
        String delimiterNote = cfg.getChunkDelimiter() != null && !cfg.getChunkDelimiter().isBlank()
                ? "；库级自定义分隔符优先"
                : "";
        return new ChunkStrategySummaryRow(
                fileType,
                FILE_TYPE_LABELS.getOrDefault(fileType, fileType),
                strategyWire,
                STRATEGY_LABELS.getOrDefault(strategyWire, strategyWire),
                hierarchical,
                buildParsingNote(parsing, parserId) + delimiterNote);
    }

    private String buildParsingNote(ParsingRulesSettings parsing, String parserId) {
        List<String> parts = new ArrayList<>();
        parts.add("解析器:" + parserEngineRegistry.labelFor(parserId));
        if (parsing.isOcrEnabled()) {
            parts.add("OCR 开启");
        }
        if (parsing.getTableExtraction() != null && !"skip".equals(parsing.getTableExtraction())) {
            parts.add("表格:" + parsing.getTableExtraction());
        }
        return parts.isEmpty() ? "解析随 MIME 默认；分块随库配置" : String.join("；", parts) + "；分块随库配置";
    }
}
