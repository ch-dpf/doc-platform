package com.knowbase.ingestion;

import com.knowbase.ingestion.pdf.PdfLayoutTableDetectionRouter;
import com.knowbase.ingestion.pdf.PdfParseConfidenceAggregator;
import com.knowbase.ingestion.pdf.PdfScannedDocumentRouter;
import com.knowbase.ingestion.pdf.PdfTextExtractabilityAnalyzer;
import com.knowbase.ingestion.pdf.PdfVisionDocumentRouter;
import com.knowbase.ingestion.pdf.VisionDocumentParseSettings;
import com.knowbase.ingestion.parse.EvidenceArtifactGenerator;
import com.knowbase.ingestion.layout.LayoutAnalysisService;
import com.knowbase.domain.status.ContentFamily;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public final class PdfLayoutParser implements DocumentParser {

    private static final Logger log = LoggerFactory.getLogger(PdfLayoutParser.class);

    public static final String PARSER_CODE = "pdf-layout";

    private final VisionDocumentParseSettings visionSettings;
    private final EvidenceArtifactGenerator evidenceArtifactGenerator;
    private final LayoutAnalysisService layoutAnalysisService;
    private final boolean layoutFallbackToHeuristic;

    public PdfLayoutParser() {
        this(VisionDocumentParseSettings.disabled(), EvidenceArtifactGenerator.disabled());
    }

    public PdfLayoutParser(VisionDocumentParseSettings visionSettings) {
        this(visionSettings, EvidenceArtifactGenerator.disabled());
    }

    public PdfLayoutParser(
            VisionDocumentParseSettings visionSettings,
            EvidenceArtifactGenerator evidenceArtifactGenerator
    ) {
        this(visionSettings, evidenceArtifactGenerator, null, true);
    }

    public PdfLayoutParser(
            VisionDocumentParseSettings visionSettings,
            EvidenceArtifactGenerator evidenceArtifactGenerator,
            LayoutAnalysisService layoutAnalysisService,
            boolean layoutFallbackToHeuristic
    ) {
        this.visionSettings = visionSettings == null ? VisionDocumentParseSettings.disabled() : visionSettings;
        this.evidenceArtifactGenerator = evidenceArtifactGenerator == null
                ? EvidenceArtifactGenerator.disabled()
                : evidenceArtifactGenerator;
        this.layoutAnalysisService = layoutAnalysisService;
        this.layoutFallbackToHeuristic = layoutFallbackToHeuristic;
    }

    @Override
    public boolean supports(String sourceUri, String mimeType) {
        if (mimeType != null && mimeType.toLowerCase(Locale.ROOT).contains("pdf")) {
            return true;
        }
        return sourceUri != null && sourceUri.toLowerCase(Locale.ROOT).endsWith(".pdf");
    }

    @Override
    public ParsedDocument parse(DocumentSource source) {
        log.info("PDF 版面解析开始: sourceUri={}", source.sourceUri());
        try (InputStream inputStream = source.inputStream()) {
            byte[] bytes = inputStream.readAllBytes();
            PdfTextExtractabilityAnalyzer.Analysis extractability = PdfTextExtractabilityAnalyzer.analyze(bytes);

            ParsedDocument visionParsed = tryVisionParse(source, bytes, extractability, null, false);
            if (visionParsed != null) {
                return withEvidenceArtifacts(source, bytes, visionParsed);
            }

            if (PdfScannedDocumentRouter.shouldRouteToOcr(extractability, source.metadata(), false)) {
                ParsedDocument ocrParsed = tryOcrParse(source, bytes, extractability);
                if (ocrParsed != null) {
                    return withEvidenceArtifacts(source, bytes, ocrParsed);
                }
            }

            List<StructuralBlock> blocks = PdfLayoutTableDetectionRouter.extractBlocks(
                    source,
                    bytes,
                    layoutAnalysisService,
                    layoutFallbackToHeuristic
            );
            boolean usedMlLayout = blocks.stream()
                    .anyMatch(block -> "ollama-layout".equals(block.metadata().get("layoutProvider")));
            PdfParseConfidenceAggregator.PdfParseConfidence layoutConfidence =
                    PdfParseConfidenceAggregator.aggregate(blocks);

            visionParsed = tryVisionParse(source, bytes, extractability, layoutConfidence, blocks.isEmpty());
            if (visionParsed != null) {
                return withEvidenceArtifacts(source, bytes, visionParsed);
            }

            if (blocks.isEmpty()) {
                if (PdfScannedDocumentRouter.shouldRouteToOcr(extractability, source.metadata(), true)) {
                    ParsedDocument ocrParsed = tryOcrParse(source, bytes, extractability);
                    if (ocrParsed != null) {
                        return withEvidenceArtifacts(source, bytes, ocrParsed);
                    }
                    return fallbackStructureParse(source, bytes);
                }
                return fallbackStructureParse(source, bytes);
            }

            Map<String, Object> metadata = new HashMap<>();
            if (source.metadata() != null) {
                metadata.putAll(source.metadata());
            }
            metadata.put("parserCode", PARSER_CODE);
            metadata.put("structureAware", true);
            metadata.put("layoutParsing", true);
            metadata.put("pdfExtractableChars", extractability.totalChars());
            metadata.put("pdfCharsPerPage", extractability.charsPerPage());
            metadata.put("pdfScannedLikely", extractability.scannedLikely());
            metadata.put("pdfLowTextDensity", extractability.lowTextDensity());
            metadata.put("pdfParseRoute", PdfLayoutTableDetectionRouter.resolveParseRoute(source.metadata(), usedMlLayout));
            if (usedMlLayout) {
                metadata.put("layoutProvider", "ollama-layout");
                metadata.put("tableDetectionSource", "ollama-layout");
            }
            metadata.put("blockCount", blocks.size());
            metadata.put("pageCount", countPages(blocks));
            collectPageDimensions(bytes, blocks, metadata);
            metadata.putAll(PdfParseConfidenceAggregator.toDocumentMetadata(layoutConfidence));
            metadata.putAll(generateEvidenceArtifacts(source, bytes, metadata));
            String flatText = StructureParsingSupport.blocksToText(blocks);
            log.info(
                    "PDF 版面解析完成: sourceUri={}, route={}, blocks={}, mlLayout={}, parseConfidence={}",
                    source.sourceUri(),
                    metadata.get("pdfParseRoute"),
                    blocks.size(),
                    usedMlLayout,
                    metadata.get("parseConfidence")
            );
            return new ParsedDocument(
                    source.sourceUri(),
                    source.filename(),
                    flatText,
                    ContentFamily.RICH_TEXT,
                    Map.copyOf(metadata),
                    blocks
            );
        } catch (IOException exception) {
            log.warn("PDF 版面解析失败: sourceUri={}", source.sourceUri(), exception);
            throw new IllegalStateException("Layout 解析 PDF 失败: " + source.sourceUri(), exception);
        }
    }

    private ParsedDocument tryVisionParse(
            DocumentSource source,
            byte[] bytes,
            PdfTextExtractabilityAnalyzer.Analysis extractability,
            PdfParseConfidenceAggregator.PdfParseConfidence layoutConfidence,
            boolean layoutBlocksEmpty
    ) {
        if (!PdfVisionDocumentRouter.shouldRouteToVision(
                extractability,
                source.metadata(),
                layoutBlocksEmpty,
                layoutConfidence,
                visionSettings
        )) {
            return null;
        }
        try {
            ParsedDocument parsed = PdfVisionDocumentRouter.parseWithVision(
                    source, bytes, extractability, visionSettings);
            if (visionSettings.vlFallbackToHeuristic() && isVisionParseEmpty(parsed)) {
                log.warn(
                        "VLM 解析无文本，回退后续链路: sourceUri={}, blocks={}",
                        source.sourceUri(),
                        parsed.blocks().size()
                );
                return null;
            }
            return parsed;
        } catch (RuntimeException visionFailure) {
            if (!visionSettings.vlFallbackToHeuristic()) {
                throw visionFailure;
            }
            return null;
        }
    }

    private ParsedDocument tryOcrParse(
            DocumentSource source,
            byte[] bytes,
            PdfTextExtractabilityAnalyzer.Analysis extractability
    ) {
        try {
            ParsedDocument parsed = PdfScannedDocumentRouter.parseWithOcr(
                    source, bytes, extractability, layoutAnalysisService);
            if (isVisionParseEmpty(parsed)) {
                log.warn("OCR 回退无文本: sourceUri={}", source.sourceUri());
                return null;
            }
            return parsed;
        } catch (RuntimeException ocrFailure) {
            log.warn("OCR 回退失败: sourceUri={}", source.sourceUri(), ocrFailure);
            return null;
        }
    }

    private static boolean isVisionParseEmpty(ParsedDocument parsed) {
        return parsed.blocks().isEmpty()
                && (parsed.text() == null || parsed.text().isBlank());
    }

    private ParsedDocument withEvidenceArtifacts(DocumentSource source, byte[] bytes, ParsedDocument parsed) {
        Map<String, Object> metadata = new HashMap<>(parsed.metadata());
        metadata.putAll(generateEvidenceArtifacts(source, bytes, metadata));
        return new ParsedDocument(
                parsed.sourceUri(),
                parsed.title(),
                parsed.text(),
                parsed.contentFamily(),
                Map.copyOf(metadata),
                parsed.blocks()
        );
    }

    private Map<String, Object> generateEvidenceArtifacts(
            DocumentSource source,
            byte[] bytes,
            Map<String, Object> metadata
    ) {
        if (!Boolean.TRUE.equals(metadata.get("evidenceArtifactsEnabled")) || !evidenceArtifactGenerator.enabled()) {
            return Map.of();
        }
        return evidenceArtifactGenerator.generateForPdf(bytes, source.sourceUri());
    }

    private static ParsedDocument fallbackStructureParse(DocumentSource source, byte[] bytes) {
        PdfStructureParser fallback = new PdfStructureParser();
        return fallback.parse(new DocumentSource(
                source.sourceUri(),
                source.filename(),
                source.mimeType(),
                new java.io.ByteArrayInputStream(bytes),
                source.metadata()
        ));
    }

    private static int countPages(List<StructuralBlock> blocks) {
        return blocks.stream()
                .map(block -> block.metadata().get("pageNumber"))
                .filter(Number.class::isInstance)
                .mapToInt(value -> ((Number) value).intValue())
                .max()
                .orElse(0);
    }

    private static void collectPageDimensions(byte[] pdfBytes, List<StructuralBlock> blocks, Map<String, Object> metadata) {
        Map<Integer, Double> pageWidths = new HashMap<>();
        Map<Integer, Double> pageHeights = new HashMap<>();
        for (StructuralBlock block : blocks) {
            Map<String, Object> blockMetadata = block.metadata();
            Object pageNumber = blockMetadata.get("pageNumber");
            if (!(pageNumber instanceof Number page)) {
                continue;
            }
            int pageIndex = page.intValue();
            Object width = blockMetadata.get("pageWidth");
            if (width instanceof Number widthNumber) {
                pageWidths.putIfAbsent(pageIndex, widthNumber.doubleValue());
            }
            Object height = blockMetadata.get("pageHeight");
            if (height instanceof Number heightNumber) {
                pageHeights.putIfAbsent(pageIndex, heightNumber.doubleValue());
            }
        }
        if (pageWidths.isEmpty() || pageHeights.isEmpty()) {
            try (PDDocument document = PDDocument.load(pdfBytes)) {
                for (int index = 0; index < document.getNumberOfPages(); index++) {
                    PDPage page = document.getPage(index);
                    int pageNumber = index + 1;
                    pageWidths.putIfAbsent(pageNumber, (double) page.getMediaBox().getWidth());
                    pageHeights.putIfAbsent(pageNumber, (double) page.getMediaBox().getHeight());
                }
            } catch (IOException ignored) {
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
}
