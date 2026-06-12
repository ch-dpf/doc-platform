package com.knowbase.pipeline.config;

import com.knowbase.library.config.CleaningRulesSettings;
import com.knowbase.library.config.ParsingRulesSettings;
import com.knowbase.vector.chunk.ChunkingStrategy;
import com.knowbase.vector.config.ChunkingProperties;

/**
 * 平台级解析/清洗基线（代码权威，非库 config_json）。
 * 入库时由 {@link MimeTypePipelineDefaults} 按 MIME 覆盖分块策略与类型相关解析项。
 */
public final class PlatformPipelineDefaults {

    private PlatformPipelineDefaults() {
    }

    public static ParsingRulesSettings baselineParsing() {
        return new ParsingRulesSettings();
    }

    public static CleaningRulesSettings baselineCleaning() {
        return new CleaningRulesSettings();
    }

    public static ParsingRulesSettings copyParsing(ParsingRulesSettings source) {
        ParsingRulesSettings p = new ParsingRulesSettings();
        p.setOcrEnabled(source.isOcrEnabled());
        p.setTableExtraction(source.getTableExtraction());
        p.setImageExtraction(source.getImageExtraction());
        p.setFormulaExtraction(source.getFormulaExtraction());
        p.setAutoDetectEncoding(source.isAutoDetectEncoding());
        p.setDefaultLanguage(source.getDefaultLanguage());
        return p;
    }

    public static CleaningRulesSettings copyCleaning(CleaningRulesSettings source) {
        CleaningRulesSettings c = new CleaningRulesSettings();
        c.setRemoveDuplicateParagraphs(source.isRemoveDuplicateParagraphs());
        c.setRemoveHeaderFooter(source.isRemoveHeaderFooter());
        c.setRemoveWatermark(source.isRemoveWatermark());
        c.setMaskPhone(source.isMaskPhone());
        c.setMaskIdCard(source.isMaskIdCard());
        c.setStopwordFilter(source.isStopwordFilter());
        return c;
    }

    public static ChunkingProperties copyChunking(ChunkingProperties source) {
        ChunkingProperties c = new ChunkingProperties();
        ChunkingStrategy strategy = source.getStrategy() != null
                ? source.getStrategy()
                : ChunkingStrategy.PARAGRAPH_FIRST;
        c.setStrategy(strategy);
        c.setChunkSize(source.getChunkSize());
        c.setOverlap(source.getOverlap());
        c.setMinChunkSize(source.getMinChunkSize());
        c.setMaxChunkSize(source.getMaxChunkSize());
        c.setMinParagraphLength(source.getMinParagraphLength());
        c.setNormalizeBeforeChunk(source.isNormalizeBeforeChunk());
        c.setSemanticSimilarityThreshold(source.getSemanticSimilarityThreshold());
        c.setHierarchicalChunkingEnabled(source.isHierarchicalChunkingEnabled());
        c.setCustomDelimiter(source.getCustomDelimiter());
        return c;
    }
}
