package com.knowbase.pipeline.content;

import com.knowbase.ingest.config.OcrProperties;
import com.knowbase.library.config.CleaningRulesSettings;
import com.knowbase.library.config.ParsingRulesSettings;
import org.springframework.stereotype.Component;

/**
 * 按 {@link ContentFamily} 应用解析/清洗默认（原 MimeTypePipelineDefaults 的族群化内核）。
 * 分块策略与数值由库级配置统一；采集级 ingest profile 可显式覆盖并进入非主档。
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
            CleaningRulesSettings cleaning) {
        if (family == null) {
            return;
        }
        switch (family) {
            case TABULAR -> applyTabular(parsing, cleaning);
            case DOCUMENT -> applyDocument(mimeType, parsing, cleaning);
            case PLAIN -> applyPlain(parsing, cleaning);
            case IMAGE -> applyImage(parsing, cleaning);
            case UNKNOWN -> { /* 保留平台/库基线 */ }
        }
    }

    private static void applyTabular(ParsingRulesSettings parsing, CleaningRulesSettings cleaning) {
        parsing.setTableExtraction("text-only");
        parsing.setOcrEnabled(false);
        parsing.setAutoDetectEncoding(true);
        applySpreadsheetCleaning(cleaning);
    }

    private void applyDocument(String mimeType, ParsingRulesSettings parsing, CleaningRulesSettings cleaning) {
        boolean isWord = mimeType != null
                && (mimeType.toLowerCase().contains("word")
                        || mimeType.toLowerCase().contains("msword")
                        || mimeType.toLowerCase().contains("document"));
        if (isWord) {
            parsing.setTableExtraction("structured");
            parsing.setOcrEnabled(false);
        } else {
            boolean ocr = ocrProperties.isEnabled();
            parsing.setOcrEnabled(ocr);
            if (!ocr) {
                parsing.setTableExtraction("text-only");
            }
        }
        parsing.setAutoDetectEncoding(true);
        applyDocumentCleaning(cleaning);
    }

    private static void applyPlain(ParsingRulesSettings parsing, CleaningRulesSettings cleaning) {
        parsing.setTableExtraction("skip");
        parsing.setOcrEnabled(false);
        parsing.setAutoDetectEncoding(true);
        applyPlainTextCleaning(cleaning);
    }

    private void applyImage(ParsingRulesSettings parsing, CleaningRulesSettings cleaning) {
        parsing.setOcrEnabled(ocrProperties.isEnabled());
        parsing.setTableExtraction("skip");
        parsing.setAutoDetectEncoding(true);
        applyPlainTextCleaning(cleaning);
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
