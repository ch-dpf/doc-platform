package com.knowbase.ingestion.pdf;

import com.knowbase.ingestion.DocumentSource;
import com.knowbase.ingestion.OcrLayoutDocumentParser;
import com.knowbase.ingestion.ParsedDocument;
import com.knowbase.ingestion.layout.LayoutAnalysisService;

import java.io.ByteArrayInputStream;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

/**
 * Routes scanned or low-text PDFs to the OCR layout parser.
 */
public final class PdfScannedDocumentRouter {

    private PdfScannedDocumentRouter() {
    }

    public static boolean shouldRouteToOcr(
            PdfTextExtractabilityAnalyzer.Analysis analysis,
            Map<String, Object> metadata,
            boolean layoutBlocksEmpty
    ) {
        if (layoutBlocksEmpty) {
            return true;
        }
        if (analysis != null && analysis.scannedLikely()) {
            return !Boolean.FALSE.equals(metadata == null ? null : metadata.get("pdfOcrFallback"));
        }
        if (analysis != null && analysis.lowTextDensity()) {
            return Boolean.TRUE.equals(metadata == null ? null : metadata.get("pdfOcrOnLowDensity"));
        }
        if (metadata == null) {
            return false;
        }
        Object mode = metadata.get("parseMode");
        if (mode == null) {
            mode = metadata.get("pdfParseMode");
        }
        if (mode == null) {
            return false;
        }
        String normalized = String.valueOf(mode).trim().toLowerCase(Locale.ROOT);
        return "ocr".equals(normalized) || "ocr-layout".equals(normalized) || "scanned".equals(normalized);
    }

    public static ParsedDocument parseWithOcr(
            DocumentSource source,
            byte[] bytes,
            PdfTextExtractabilityAnalyzer.Analysis analysis
    ) {
        return parseWithOcr(source, bytes, analysis, null);
    }

    public static ParsedDocument parseWithOcr(
            DocumentSource source,
            byte[] bytes,
            PdfTextExtractabilityAnalyzer.Analysis analysis,
            LayoutAnalysisService layoutAnalysisService
    ) {
        Map<String, Object> metadata = new HashMap<>();
        if (source.metadata() != null) {
            metadata.putAll(source.metadata());
        }
        metadata.put("pdfParseRoute", "ocr-fallback");
        metadata.putIfAbsent("ocrApplied", true);
        if (analysis != null) {
            metadata.put("pdfExtractableChars", analysis.totalChars());
            metadata.put("pdfCharsPerPage", analysis.charsPerPage());
            metadata.put("pdfScannedLikely", analysis.scannedLikely());
        }
        ParsedDocument parsed = new OcrLayoutDocumentParser(layoutAnalysisService).parse(new DocumentSource(
                source.sourceUri(),
                source.filename(),
                source.mimeType() == null ? "application/pdf" : source.mimeType(),
                new ByteArrayInputStream(bytes),
                Map.copyOf(metadata)
        ));
        Map<String, Object> merged = new HashMap<>();
        if (parsed.metadata() != null) {
            merged.putAll(parsed.metadata());
        }
        merged.put("parserCode", OcrLayoutDocumentParser.PARSER_CODE);
        merged.put("pdfLayoutFallback", true);
        merged.put("pdfParseRoute", "ocr-fallback");
        return new ParsedDocument(
                parsed.sourceUri(),
                parsed.title(),
                parsed.text(),
                parsed.contentFamily(),
                Map.copyOf(merged),
                parsed.blocks()
        );
    }
}
