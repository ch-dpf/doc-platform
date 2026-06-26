package com.knowbase.ingestion.pdf;

import com.knowbase.ingestion.DocumentSource;
import com.knowbase.ingestion.LayoutPdfTextExtractor;
import com.knowbase.ingestion.PdfLayoutParser;
import com.knowbase.ingestion.StructuralBlock;
import com.knowbase.ingestion.layout.LayoutAnalysisOptions;
import com.knowbase.ingestion.layout.LayoutAnalysisService;
import com.knowbase.ingestion.layout.OllamaLayoutTableProvider;
import com.knowbase.ingestion.parse.IngestionParseOptionsSupport;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Routes electronic PDF parsing to Ollama ML layout (ruled/borderless/nested tables) with heuristic fallback.
 */
public final class PdfLayoutTableDetectionRouter {

    private PdfLayoutTableDetectionRouter() {
    }

    public static List<StructuralBlock> extractBlocks(
            DocumentSource source,
            byte[] pdfBytes,
            LayoutAnalysisService layoutAnalysisService,
            boolean fallbackToHeuristic
    ) throws IOException {
        if (shouldUseOllamaLayout(source.metadata()) && layoutAnalysisService != null) {
            try {
                Map<String, Object> options = buildLayoutOptions(source, pdfBytes);
                List<StructuralBlock> mlBlocks = layoutAnalysisService.analyzePdfPages(
                        source,
                        pdfBytes,
                        0,
                        options
                );
                if (!mlBlocks.isEmpty()) {
                    return mlBlocks;
                }
            } catch (RuntimeException exception) {
                if (!fallbackToHeuristic) {
                    throw exception;
                }
            }
        }
        return LayoutPdfTextExtractor.extract(pdfBytes);
    }

    public static String resolveParseRoute(Map<String, Object> metadata, boolean usedMl) {
        if (usedMl) {
            return "ollama-layout";
        }
        return "layout-heuristic";
    }

    private static boolean shouldUseOllamaLayout(Map<String, Object> metadata) {
        if (metadata == null || metadata.isEmpty()) {
            return false;
        }
        IngestionParseOptionsSupport.IngestionParseOptions options = IngestionParseOptionsSupport.resolve(metadata);
        if (OllamaLayoutTableProvider.PROVIDER_CODE.equalsIgnoreCase(options.layoutProvider())) {
            return true;
        }
        Object explicit = metadata.get("tableMlDetection");
        if (explicit instanceof Boolean enabled && enabled) {
            return true;
        }
        Object provider = metadata.get("tableDetectionProvider");
        return provider != null
                && "ollama".equalsIgnoreCase(String.valueOf(provider).trim());
    }

    private static Map<String, Object> buildLayoutOptions(DocumentSource source, byte[] pdfBytes) {
        Map<String, Object> options = new HashMap<>();
        if (source.metadata() != null) {
            options.putAll(source.metadata());
        }
        options.put(LayoutAnalysisOptions.PDF_BYTES, pdfBytes);
        options.put("layoutProvider", OllamaLayoutTableProvider.PROVIDER_CODE);
        options.put("parserCode", PdfLayoutParser.PARSER_CODE);
        options.putIfAbsent("pdfParseRoute", "ollama-layout");
        return Map.copyOf(options);
    }
}
