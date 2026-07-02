package com.knowbase.ingestion.layout;

import com.knowbase.ingestion.StructuralBlock;
import com.knowbase.ingestion.ocr.OcrBlockFactory;
import com.knowbase.ingestion.ocr.OcrEngineAdapter;
import com.knowbase.ingestion.ocr.OcrEngineRegistry;
import com.knowbase.ingestion.ocr.OcrEngineResult;
import com.knowbase.ingestion.ocr.OcrRecognizeRequest;
import com.knowbase.ingestion.parse.IngestionParseOptionsSupport;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * OCR + heuristic layout for raster pages (images / rendered PDF pages).
 */
public final class OcrRasterLayoutProvider implements LayoutAnalysisProvider {

    public static final String PROVIDER_CODE = "ocr-raster";

    private final OcrEngineAdapter defaultEngine;

    public OcrRasterLayoutProvider() {
        this(OcrEngineRegistry.resolve(Map.of()));
    }

    OcrRasterLayoutProvider(OcrEngineAdapter defaultEngine) {
        this.defaultEngine = defaultEngine;
    }

    @Override
    public String providerCode() {
        return PROVIDER_CODE;
    }

    @Override
    public boolean available() {
        return defaultEngine != null;
    }

    @Override
    public boolean supports(LayoutPageRequest request) {
        return available() && request.imageBytes() != null && request.imageBytes().length > 0;
    }

    @Override
    public LayoutPageResult analyze(LayoutPageRequest request) {
        Map<String, Object> options = request.effectiveOptions();
        OcrEngineAdapter engine = resolveEngine(options);
        Map<String, Object> baseMetadata = new HashMap<>(options);
        baseMetadata.put("pageNumber", request.pageNumber());
        baseMetadata.put("pageWidth", request.pageWidth());
        baseMetadata.put("pageHeight", request.pageHeight());
        baseMetadata.put("layoutProvider", PROVIDER_CODE);
        baseMetadata.put("layoutAnalysisRoute", "ocr-raster");
        String language = IngestionParseOptionsSupport.resolve(options).ocrLanguage();
        if (language != null && !language.isBlank() && !"auto".equalsIgnoreCase(language)) {
            baseMetadata.putIfAbsent("ocrLanguage", language);
        }
        OcrRecognizeRequest recognizeRequest = new OcrRecognizeRequest(
                request.sourceUri(),
                request.mimeType(),
                language,
                options
        );
        OcrEngineResult engineResult = engine.recognize(request.imageBytes(), recognizeRequest);
        Map<String, Object> parsedMetadata = new HashMap<>(baseMetadata);
        parsedMetadata.putAll(engineResult.engineMetadata());
        parsedMetadata.put("ocrEngine", engine.engineCode());
        parsedMetadata.put("ocrApplied", true);
        List<StructuralBlock> blocks = LayoutResultMapper.stampProvider(
                OcrBlockFactory.fromEngineResult(engineResult, parsedMetadata),
                PROVIDER_CODE,
                engine.engineCode(),
                "ocr-raster"
        );
        String detectedLanguage = readString(parsedMetadata, "ocrLanguage", "Content-Language", "language");
        Double rotation = readRotation(parsedMetadata);
        return new LayoutPageResult(
                PROVIDER_CODE,
                engine.engineCode(),
                request.pageNumber(),
                blocks,
                detectTableRegions(blocks, request.pageNumber()),
                detectedLanguage,
                rotation,
                Map.copyOf(parsedMetadata)
        );
    }

    private static OcrEngineAdapter resolveEngine(Map<String, Object> options) {
        if (options == null || options.isEmpty()) {
            return OcrEngineRegistry.resolve(Map.of());
        }
        return OcrEngineRegistry.resolve(options);
    }

    private static List<LayoutTableRegion> detectTableRegions(List<StructuralBlock> blocks, int pageNumber) {
        int regionId = 0;
        List<LayoutTableRegion> regions = new java.util.ArrayList<>();
        for (StructuralBlock block : blocks) {
            if (!"table_row".equals(block.blockType())) {
                continue;
            }
            Object existing = block.metadata().get("tableRegionId");
            int id = existing instanceof Number number ? number.intValue() : regionId++;
            Object bbox = block.metadata().get("bbox");
            @SuppressWarnings("unchecked")
            List<Double> bboxList = bbox instanceof List<?> list ? (List<Double>) list : null;
            regions.add(new LayoutTableRegion(
                    id,
                    "ocr-table-" + id,
                    bboxList,
                    "ocr-heuristic"
            ));
        }
        return List.copyOf(regions);
    }

    private static String readString(Map<String, Object> metadata, String... keys) {
        for (String key : keys) {
            Object value = metadata.get(key);
            if (value != null && !String.valueOf(value).isBlank()) {
                return String.valueOf(value).trim();
            }
        }
        return null;
    }

    private static Double readRotation(Map<String, Object> metadata) {
        for (String key : List.of("rotation", "pageRotation", "tesseract-ocr:orientation", "Orientation")) {
            Object value = metadata.get(key);
            if (value instanceof Number number) {
                return number.doubleValue();
            }
            if (value != null) {
                try {
                    return Double.parseDouble(String.valueOf(value).trim());
                } catch (NumberFormatException ignored) {
                    // continue
                }
            }
        }
        return null;
    }
}
