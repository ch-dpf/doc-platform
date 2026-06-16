package com.knowbase.library.support;

import com.knowbase.library.config.VectorLibraryConfig;
import com.knowbase.library.config.VectorLibraryConfigFactory;
import com.knowbase.library.dto.ChunkStrategySummaryRow;
import com.knowbase.library.service.LibraryConfigResolver;
import com.knowbase.pipeline.config.ChunkingStrategyResolver;
import com.knowbase.vector.chunk.ChunkingStrategy;
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
            ChunkingStrategy.AUTO.toWire(), "自动（按文件类型）",
            ChunkingStrategy.PARAGRAPH_FIRST.toWire(), "按段落",
            ChunkingStrategy.HEADING_LEVEL.toWire(), "按标题层级",
            ChunkingStrategy.FIXED_CHAR.toWire(), "固定长度",
            ChunkingStrategy.SEMANTIC.toWire(), "语义分块");

    private final LibraryConfigResolver libraryConfigResolver;

    public LibraryChunkStrategySummaryService(LibraryConfigResolver libraryConfigResolver) {
        this.libraryConfigResolver = libraryConfigResolver;
    }

    public List<ChunkStrategySummaryRow> summarize(UUID libraryId) {
        VectorLibraryConfig cfg = libraryConfigResolver.config(libraryId);
        ChunkingStrategy libraryStrategy = cfg.getChunkingStrategy() != null
                ? cfg.getChunkingStrategy()
                : ChunkingStrategy.AUTO;
        List<String> fileTypes =
                VectorLibraryConfigFactory.systemSupportedFileTypes(cfg.getAllowedMimeTypes());

        List<ChunkStrategySummaryRow> rows = new ArrayList<>();
        for (String fileType : fileTypes) {
            ChunkingStrategy effective =
                    ChunkingStrategyResolver.effectiveForFileType(libraryStrategy, fileType);
            String strategyWire = effective.toWire();
            rows.add(new ChunkStrategySummaryRow(
                    fileType,
                    FILE_TYPE_LABELS.getOrDefault(fileType, fileType),
                    strategyWire,
                    STRATEGY_LABELS.getOrDefault(strategyWire, strategyWire)));
        }
        return rows;
    }
}
