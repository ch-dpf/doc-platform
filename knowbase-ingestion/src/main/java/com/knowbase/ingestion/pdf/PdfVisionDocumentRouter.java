package com.knowbase.ingestion.pdf;



import com.knowbase.domain.status.ContentFamily;

import com.knowbase.ingestion.DocumentSource;

import com.knowbase.ingestion.ParsedDocument;

import com.knowbase.ingestion.PdfLayoutParser;

import com.knowbase.ingestion.StructuralBlock;

import com.knowbase.ingestion.StructureParsingSupport;

import com.knowbase.ingestion.layout.LayoutAnalysisService;

import java.util.HashMap;

import java.util.List;

import java.util.Locale;

import java.util.Map;



/**

 * Routes complex or scanned PDFs to {@link LayoutAnalysisService}.

 */

public final class PdfVisionDocumentRouter {



    private PdfVisionDocumentRouter() {

    }



    public static boolean shouldRouteToVision(

            PdfTextExtractabilityAnalyzer.Analysis analysis,

            Map<String, Object> metadata,

            boolean layoutBlocksEmpty,

            PdfParseConfidenceAggregator.PdfParseConfidence layoutConfidence,

            VisionDocumentParseSettings settings

    ) {

        if (settings == null || !settings.available()) {

            return false;

        }

        if (metadata != null && Boolean.FALSE.equals(metadata.get("pdfVlFallback"))) {

            return false;

        }

        if (explicitVisionMode(metadata)) {

            return true;

        }

        if (analysis != null && analysis.scannedLikely() && settings.vlOnScanned()) {

            return true;

        }

        if (layoutBlocksEmpty && analysis != null && analysis.scannedLikely() && settings.vlOnScanned()) {

            return true;

        }

        if (layoutConfidence != null

                && settings.vlOnLowConfidence()

                && layoutConfidence.score() < settings.vlLowConfidenceThreshold()) {

            return true;

        }

        return false;

    }



    public static ParsedDocument parseWithVision(

            DocumentSource source,

            byte[] bytes,

            PdfTextExtractabilityAnalyzer.Analysis analysis,

            VisionDocumentParseSettings settings

    ) {

        if (settings == null || !settings.available()) {

            throw new IllegalStateException("layout analysis 未配置");

        }

        LayoutAnalysisService layoutAnalysisService = settings.layoutAnalysisService();

        Map<String, Object> options = new HashMap<>();
        if (source.metadata() != null) {
            options.putAll(source.metadata());
        }
        options.putIfAbsent("layoutProvider", com.knowbase.ingestion.layout.VisionMarkdownLayoutProvider.PROVIDER_CODE);

        List<StructuralBlock> blocks = layoutAnalysisService.analyzePdfPages(source, bytes, settings.vlMaxPages(), options);

        blocks = StructureParsingSupport.enrichHeadingPathsPublic(blocks);



        Map<String, Object> metadata = new HashMap<>();

        if (source.metadata() != null) {

            metadata.putAll(source.metadata());

        }

        String layoutProvider = stringValue(options.get("layoutProvider"));

        String layoutModel = firstBlockValue(blocks, "layoutModel");

        metadata.putAll(layoutAnalysisService.buildDocumentMetadata(

                blocks,

                countPages(blocks),

                layoutProvider == null ? stringValue(options.get("layoutProvider")) : layoutProvider,

                layoutModel

        ));

        metadata.put("parserCode", PdfLayoutParser.PARSER_CODE);

        metadata.put("parser", PdfLayoutParser.PARSER_CODE);

        metadata.put("pdfParseRoute", "vision-vl");

        metadata.put("vlApplied", true);

        metadata.put("layoutAnalysisApplied", true);

        if (layoutModel != null) {

            metadata.put("visionLanguageModel", layoutModel);

        }

        metadata.put("structureAware", !blocks.isEmpty());

        metadata.put("parseConfidence", blocks.isEmpty() ? 0.5d : 0.85d);

        metadata.put("parseConfidenceSource", "vision-vl");

        if (analysis != null) {

            metadata.put("pdfExtractableChars", analysis.totalChars());

            metadata.put("pdfCharsPerPage", analysis.charsPerPage());

            metadata.put("pdfScannedLikely", analysis.scannedLikely());

            metadata.put("pdfLowTextDensity", analysis.lowTextDensity());

        }

        collectPageDimensions(bytes, blocks, metadata);



        String flatText = blocks.isEmpty() ? "" : StructureParsingSupport.blocksToTextPublic(blocks);

        ContentFamily family = analysis != null && analysis.scannedLikely()

                ? ContentFamily.SCANNED_DOCUMENT

                : ContentFamily.RICH_TEXT;

        return new ParsedDocument(

                source.sourceUri(),

                firstNonBlank(source.filename(), source.sourceUri()),

                flatText,

                family,

                Map.copyOf(metadata),

                blocks

        );

    }



    private static void collectPageDimensions(byte[] pdfBytes, List<StructuralBlock> blocks, Map<String, Object> metadata) {

        Map<Integer, Double> pageWidths = new HashMap<>();

        Map<Integer, Double> pageHeights = new HashMap<>();

        for (StructuralBlock block : blocks) {

            Object pageNumber = block.metadata().get("pageNumber");

            if (!(pageNumber instanceof Number page)) {

                continue;

            }

            int pageIndex = page.intValue();

            Object width = block.metadata().get("pageWidth");

            if (width instanceof Number widthNumber) {

                pageWidths.putIfAbsent(pageIndex, widthNumber.doubleValue());

            }

            Object height = block.metadata().get("pageHeight");

            if (height instanceof Number heightNumber) {

                pageHeights.putIfAbsent(pageIndex, heightNumber.doubleValue());

            }

        }

        if (pageWidths.isEmpty() || pageHeights.isEmpty()) {

            try (org.apache.pdfbox.pdmodel.PDDocument document = org.apache.pdfbox.pdmodel.PDDocument.load(pdfBytes)) {

                for (int index = 0; index < document.getNumberOfPages(); index++) {

                    org.apache.pdfbox.pdmodel.PDPage page = document.getPage(index);

                    int pageNumber = index + 1;

                    pageWidths.putIfAbsent(pageNumber, (double) page.getMediaBox().getWidth());

                    pageHeights.putIfAbsent(pageNumber, (double) page.getMediaBox().getHeight());

                }

            } catch (java.io.IOException ignored) {

                // keep block-derived dimensions only

            }

        }

        if (!pageWidths.isEmpty()) {

            metadata.put("pageWidths", Map.copyOf(pageWidths));

        }

        if (!pageHeights.isEmpty()) {

            metadata.put("pageHeights", Map.copyOf(pageHeights));

        }

    }



    private static int countPages(List<StructuralBlock> blocks) {

        return blocks.stream()

                .map(block -> block.metadata().get("pageNumber"))

                .filter(Number.class::isInstance)

                .mapToInt(value -> ((Number) value).intValue())

                .max()

                .orElse(0);

    }



    private static boolean explicitVisionMode(Map<String, Object> metadata) {

        if (metadata == null) {

            return false;

        }

        Object mode = metadata.get("pdfParseMode");

        if (mode == null) {

            mode = metadata.get("parseMode");

        }

        if (mode == null) {

            return false;

        }

        String normalized = String.valueOf(mode).trim().toLowerCase(Locale.ROOT);

        return "vl".equals(normalized)

                || "vision".equals(normalized)

                || "vision-vl".equals(normalized)

                || "paddleocr-vl".equals(normalized);

    }



    private static String firstBlockValue(List<StructuralBlock> blocks, String key) {

        for (StructuralBlock block : blocks) {

            Object value = block.metadata().get(key);

            if (value != null && !String.valueOf(value).isBlank()) {

                return String.valueOf(value);

            }

        }

        return null;

    }



    private static String stringValue(Object value) {

        return value == null ? null : String.valueOf(value);

    }



    private static String firstNonBlank(String... values) {

        for (String value : values) {

            if (value != null && !value.isBlank()) {

                return value;

            }

        }

        return "untitled";

    }

}


