package com.knowbase.pipeline.content;

import com.knowbase.ingest.config.OcrProperties;
import com.knowbase.library.config.CleaningRulesSettings;
import com.knowbase.library.config.ParsingRulesSettings;
import com.knowbase.vector.chunk.ChunkingStrategy;
import com.knowbase.vector.config.ChunkingProperties;
import org.springframework.stereotype.Component;

/**
 * 按 {@link ContentFamily} 应用解析/清洗/分块策略默认（原 MimeTypePipelineDefaults 的族群化内核）。
 */
@Component
public class ContentFamilyPipelineDefaults {

    private final OcrProperties ocrProperties;

    public ContentFamilyPipelineDefaults(OcrProperties ocrProperties) {
        this.ocrProperties = ocrProperties;
    }

    public void apply(
            ContentFamily family,
            String mimeType,
            ParsingRulesSettings parsing,
            CleaningRulesSettings cleaning,
            ChunkingProperties chunking) {
        if (family == null) {
            return;
        }
        switch (family) {
            case TABULAR -> applyTabular(parsing, cleaning, chunking);
            case DOCUMENT -> applyDocument(mimeType, parsing, cleaning, chunking);
            case PLAIN -> applyPlain(mimeType, parsing, cleaning, chunking);
            case IMAGE -> applyImage(parsing, cleaning, chunking);
            case UNKNOWN -> { /* 保留平台/库基线 */ }
        }
    }

    private static void applyTabular(
            ParsingRulesSettings parsing, CleaningRulesSettings cleaning, ChunkingProperties chunking) {
        parsing.setTableExtraction("text-only");
        parsing.setOcrEnabled(false);
        parsing.setAutoDetectEncoding(true);
        applySpreadsheetCleaning(cleaning);
        chunking.setStrategy(ChunkingStrategy.PARAGRAPH_FIRST);
    }

    private void applyDocument(
            String mimeType,
            ParsingRulesSettings parsing,
            CleaningRulesSettings cleaning,
            ChunkingProperties chunking) {
        boolean isWord = mimeType != null
                && (mimeType.toLowerCase().contains("word")
                        || mimeType.toLowerCase().contains("msword")
                        || mimeType.toLowerCase().contains("document"));
        if (isWord) {
            parsing.setTableExtraction("structured");
            parsing.setOcrEnabled(false);
            chunking.setStrategy(ChunkingStrategy.HEADING_LEVEL);
        } else {
            boolean ocr = ocrProperties.isEnabled();
            parsing.setOcrEnabled(ocr);
            if (!ocr) {
                parsing.setTableExtraction("text-only");
            }
            chunking.setStrategy(ChunkingStrategy.PARAGRAPH_FIRST);
        }
        parsing.setAutoDetectEncoding(true);
        applyDocumentCleaning(cleaning);
    }

    private static void applyPlain(
            String mimeType,
            ParsingRulesSettings parsing,
            CleaningRulesSettings cleaning,
            ChunkingProperties chunking) {
        parsing.setTableExtraction("skip");
        parsing.setOcrEnabled(false);
        parsing.setAutoDetectEncoding(true);
        applyPlainTextCleaning(cleaning);
        boolean markdown = mimeType != null
                && (mimeType.toLowerCase().contains("markdown") || "text/x-markdown".equalsIgnoreCase(mimeType));
        chunking.setStrategy(markdown ? ChunkingStrategy.HEADING_LEVEL : ChunkingStrategy.PARAGRAPH_FIRST);
    }

    private void applyImage(
            ParsingRulesSettings parsing, CleaningRulesSettings cleaning, ChunkingProperties chunking) {
        parsing.setOcrEnabled(ocrProperties.isEnabled());
        parsing.setTableExtraction("skip");
        parsing.setAutoDetectEncoding(true);
        applyPlainTextCleaning(cleaning);
        chunking.setStrategy(ChunkingStrategy.PARAGRAPH_FIRST);
    }

    private static void applySpreadsheetCleaning(CleaningRulesSettings cleaning) {
        cleaning.setRemoveHeaderFooter(false);
        cleaning.setRemoveWatermark(true);
        cleaning.setRemoveDuplicateParagraphs(true);
    }

    private static void applyDocumentCleaning(CleaningRulesSettings cleaning) {
        cleaning.setRemoveHeaderFooter(true);
        cleaning.setRemoveWatermark(true);
        cleaning.setRemoveDuplicateParagraphs(true);
    }

    private static void applyPlainTextCleaning(CleaningRulesSettings cleaning) {
        cleaning.setRemoveHeaderFooter(false);
        cleaning.setRemoveWatermark(false);
        cleaning.setRemoveDuplicateParagraphs(true);
    }
}
