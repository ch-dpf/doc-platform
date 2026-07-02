package com.knowbase.ingestion;

import com.knowbase.domain.status.ContentFamily;
import com.knowbase.ingestion.layout.LayoutAnalysisOptions;
import com.knowbase.ingestion.layout.LayoutAnalysisService;
import com.knowbase.ingestion.layout.OcrRasterLayoutProvider;
import com.knowbase.ingestion.layout.VisionMarkdownLayoutProvider;
import com.knowbase.ingestion.pdf.PdfPageImageRenderer;
import com.knowbase.ingestion.layout.LayoutPageRequest;
import com.knowbase.ingestion.layout.LayoutPageResult;
import com.knowbase.ingestion.ocr.OcrBlockFactory;
import com.knowbase.ingestion.ocr.OcrConfidencePolicy;
import com.knowbase.ingestion.ocr.OcrEngineAdapter;
import com.knowbase.ingestion.ocr.OcrEngineRegistry;
import com.knowbase.ingestion.ocr.OcrEngineResult;
import com.knowbase.ingestion.ocr.OcrRecognizeRequest;
import com.knowbase.ingestion.parse.IngestionParseOptionsSupport;

import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * OCR + 版面解析：对扫描 PDF / 图片执行 OCR，并按行/段落/表格启发式切分为结构块。
 */
public final class OcrLayoutDocumentParser implements DocumentParser {

    public static final String PARSER_CODE = "ocr-layout";

    private final LayoutAnalysisService layoutAnalysisService;
    private final OcrEngineAdapter defaultEngine;

    public OcrLayoutDocumentParser() {
        this(null, OcrEngineRegistry.resolve(Map.of()));
    }

    public OcrLayoutDocumentParser(LayoutAnalysisService layoutAnalysisService) {
        this(layoutAnalysisService, OcrEngineRegistry.resolve(Map.of()));
    }

    OcrLayoutDocumentParser(LayoutAnalysisService layoutAnalysisService, OcrEngineAdapter defaultEngine) {
        this.layoutAnalysisService = layoutAnalysisService;
        this.defaultEngine = defaultEngine;
    }

    @Override
    public boolean supports(String sourceUri, String mimeType) {
        String lowerMime = mimeType == null ? "" : mimeType.toLowerCase(Locale.ROOT);
        if (lowerMime.startsWith("image/")) {
            return true;
        }
        if (lowerMime.contains("pdf")) {
            return true;
        }
        String lowerUri = sourceUri == null ? "" : sourceUri.toLowerCase(Locale.ROOT);
        return lowerUri.endsWith(".png")
                || lowerUri.endsWith(".jpg")
                || lowerUri.endsWith(".jpeg")
                || lowerUri.endsWith(".bmp")
                || lowerUri.endsWith(".webp")
                || lowerUri.endsWith(".tif")
                || lowerUri.endsWith(".tiff")
                || lowerUri.endsWith(".pdf");
    }

    @Override
    public ParsedDocument parse(DocumentSource source) {
        try {
            byte[] content = source.inputStream().readAllBytes();
            Map<String, Object> options = source.metadata() == null ? Map.of() : source.metadata();
            IngestionParseOptionsSupport.IngestionParseOptions parseOptions = IngestionParseOptionsSupport.resolve(options);
            if (isPdf(source) && layoutAnalysisService != null && layoutAnalysisService.hasAvailableProvider()) {
                return parsePdfViaRasterLayout(source, content, options, parseOptions);
            }
            if (layoutAnalysisService != null && layoutAnalysisService.hasAvailableProvider() && !isPdf(source)) {
                return parseViaLayoutService(source, content, options, parseOptions);
            }
            return parseViaOcrEngine(source, content, options, parseOptions);
        } catch (RuntimeException exception) {
            throw exception;
        } catch (Exception exception) {
            throw new IllegalStateException("OCR 版面解析失败: " + source.sourceUri(), exception);
        }
    }

    private ParsedDocument parsePdfViaRasterLayout(
            DocumentSource source,
            byte[] content,
            Map<String, Object> options,
            IngestionParseOptionsSupport.IngestionParseOptions parseOptions
    ) {
        Map<String, Object> mergedOptions = new HashMap<>(options);
        mergedOptions.put(LayoutAnalysisOptions.PDF_BYTES, content);
        mergedOptions.put("layoutProvider", OcrRasterLayoutProvider.PROVIDER_CODE);
        List<StructuralBlock> blocks;
        try {
            blocks = layoutAnalysisService.analyzePdfPages(
                    source,
                    content,
                    0,
                    Map.copyOf(mergedOptions)
            );
        } catch (RuntimeException rasterFailure) {
            blocks = List.of();
        }
        if (blocks.isEmpty()) {
            mergedOptions.remove("layoutProvider");
            mergedOptions.putIfAbsent("layoutProvider", VisionMarkdownLayoutProvider.PROVIDER_CODE);
            try {
                blocks = layoutAnalysisService.analyzePdfPages(
                        source,
                        content,
                        0,
                        Map.copyOf(mergedOptions)
                );
            } catch (RuntimeException visionFailure) {
                blocks = List.of();
            }
        }
        blocks = OcrConfidencePolicy.apply(
                blocks,
                parseOptions.ocrConfidenceThreshold(),
                parseOptions.ocrDownweightMode()
        );
        Map<String, Object> parsedMetadata = new HashMap<>();
        if (source.metadata() != null) {
            parsedMetadata.putAll(source.metadata());
        }
        parsedMetadata.put("parserCode", PARSER_CODE);
        parsedMetadata.put("parser", PARSER_CODE);
        parsedMetadata.put("layoutAnalysisApplied", true);
        String layoutProvider = resolveLayoutProvider(blocks, OcrRasterLayoutProvider.PROVIDER_CODE);
        parsedMetadata.put("layoutProvider", layoutProvider);
        parsedMetadata.put("ocrApplied", true);
        parsedMetadata.put("layoutParsing", true);
        parsedMetadata.put("pdfParseRoute", VisionMarkdownLayoutProvider.PROVIDER_CODE.equals(layoutProvider)
                ? "vision-vl"
                : "ocr-raster");
        parsedMetadata.put("parseConfidence", OcrConfidencePolicy.aggregateDocumentScore(blocks));
        parsedMetadata.put("parseConfidenceSource", "ocr-layout");
        parsedMetadata.put("structureAware", !blocks.isEmpty());
        parsedMetadata.put("blockCount", blocks.size());
        parsedMetadata.put("pageCount", countPdfPages(content));
        String flatText = blocks.isEmpty() ? "" : StructureParsingSupport.blocksToText(blocks);
        return new ParsedDocument(
                source.sourceUri(),
                firstNonBlank(source.filename(), source.sourceUri()),
                flatText,
                ContentFamily.SCANNED_DOCUMENT,
                Map.copyOf(parsedMetadata),
                blocks
        );
    }

    private static int countPdfPages(byte[] content) {
        try {
            return Math.max(1, PdfPageImageRenderer.render(content, 0).size());
        } catch (RuntimeException exception) {
            return 0;
        }
    }

    private static String resolveLayoutProvider(List<StructuralBlock> blocks, String defaultProvider) {
        for (StructuralBlock block : blocks) {
            Object provider = block.metadata().get("layoutProvider");
            if (provider != null && !String.valueOf(provider).isBlank()) {
                return String.valueOf(provider);
            }
        }
        return defaultProvider;
    }

    private ParsedDocument parseViaLayoutService(
            DocumentSource source,
            byte[] content,
            Map<String, Object> options,
            IngestionParseOptionsSupport.IngestionParseOptions parseOptions
    ) {
        Map<String, Object> mergedOptions = new HashMap<>(options);
        mergedOptions.putIfAbsent("layoutProvider", "ocr-raster");
        LayoutPageResult pageResult = layoutAnalysisService.analyzePage(new LayoutPageRequest(
                content,
                source.mimeType(),
                1,
                0d,
                0d,
                source.sourceUri(),
                Map.copyOf(mergedOptions)
        ));
        List<StructuralBlock> blocks = OcrConfidencePolicy.apply(
                pageResult.blocks(),
                parseOptions.ocrConfidenceThreshold(),
                parseOptions.ocrDownweightMode()
        );
        Map<String, Object> parsedMetadata = new HashMap<>(options);
        parsedMetadata.putAll(pageResult.metadata());
        parsedMetadata.put("parserCode", PARSER_CODE);
        parsedMetadata.put("parser", PARSER_CODE);
        parsedMetadata.put("layoutAnalysisApplied", true);
        parsedMetadata.put("layoutProvider", pageResult.providerCode());
        parsedMetadata.put("layoutModel", pageResult.modelName());
        parsedMetadata.put("ocrApplied", true);
        parsedMetadata.put("layoutParsing", true);
        if (pageResult.detectedLanguage() != null) {
            parsedMetadata.put("detectedLanguage", pageResult.detectedLanguage());
            parsedMetadata.put("ocrLanguage", pageResult.detectedLanguage());
        }
        if (pageResult.rotationDegrees() != null) {
            parsedMetadata.put("pageRotation", pageResult.rotationDegrees());
        }
        parsedMetadata.put("parseConfidence", OcrConfidencePolicy.aggregateDocumentScore(blocks));
        parsedMetadata.put("parseConfidenceSource", "ocr-layout");
        parsedMetadata.put("structureAware", !blocks.isEmpty());
        parsedMetadata.put("blockCount", blocks.size());
        String flatText = blocks.isEmpty() ? "" : StructureParsingSupport.blocksToText(blocks);
        return new ParsedDocument(
                source.sourceUri(),
                firstNonBlank(source.filename(), source.sourceUri()),
                flatText,
                ContentFamily.IMAGE_TEXT,
                Map.copyOf(parsedMetadata),
                blocks
        );
    }

    private ParsedDocument parseViaOcrEngine(
            DocumentSource source,
            byte[] content,
            Map<String, Object> options,
            IngestionParseOptionsSupport.IngestionParseOptions parseOptions
    ) {
        String language = parseOptions.ocrLanguage();
        if ("auto".equalsIgnoreCase(language)) {
            language = null;
        }
        OcrEngineAdapter engine = resolveEngine(options);
        OcrRecognizeRequest request = new OcrRecognizeRequest(
                source.sourceUri(),
                source.mimeType(),
                language,
                options
        );
        OcrEngineResult engineResult = engine.recognize(content, request);
        Map<String, Object> parsedMetadata = new HashMap<>();
        if (source.metadata() != null) {
            parsedMetadata.putAll(source.metadata());
        }
        parsedMetadata.putAll(engineResult.engineMetadata());
        List<StructuralBlock> blocks = OcrBlockFactory.fromEngineResult(engineResult, parsedMetadata);
        blocks = OcrConfidencePolicy.apply(
                blocks,
                parseOptions.ocrConfidenceThreshold(),
                parseOptions.ocrDownweightMode()
        );
        parsedMetadata.put("parserCode", PARSER_CODE);
        parsedMetadata.put("parser", PARSER_CODE);
        parsedMetadata.put("ocrEngine", engine.engineCode());
        parsedMetadata.put("ocrApplied", true);
        parsedMetadata.put("layoutParsing", true);
        parsedMetadata.put("ocrLanguage", language == null ? "auto" : language);
        parsedMetadata.put("parseConfidence", OcrConfidencePolicy.aggregateDocumentScore(blocks));
        parsedMetadata.put("parseConfidenceSource", "ocr-layout");
        parsedMetadata.put("structureAware", !blocks.isEmpty());
        parsedMetadata.put("blockCount", blocks.size());
        String flatText = blocks.isEmpty()
                ? engineResult.rawPayload() == null ? "" : engineResult.rawPayload()
                : StructureParsingSupport.blocksToText(blocks);
        ContentFamily family = isPdf(source) ? ContentFamily.SCANNED_DOCUMENT : ContentFamily.IMAGE_TEXT;
        return new ParsedDocument(
                source.sourceUri(),
                firstNonBlank(source.filename(), source.sourceUri()),
                flatText,
                family,
                Map.copyOf(parsedMetadata),
                blocks
        );
    }

    private OcrEngineAdapter resolveEngine(Map<String, Object> options) {
        if (options == null || options.isEmpty()) {
            return defaultEngine;
        }
        Object requested = options.get("ocrEngine");
        if (requested == null) {
            requested = options.get("ocrEngineCode");
        }
        if (requested == null || String.valueOf(requested).isBlank()) {
            return defaultEngine;
        }
        return OcrEngineRegistry.resolve(options);
    }

    private static boolean isPdf(DocumentSource source) {
        if (source.mimeType() != null && source.mimeType().toLowerCase(Locale.ROOT).contains("pdf")) {
            return true;
        }
        return source.sourceUri() != null && source.sourceUri().toLowerCase(Locale.ROOT).endsWith(".pdf");
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
