package com.knowbase.ingestion.layout;

import com.knowbase.ingestion.LayoutPdfTextExtractor;
import com.knowbase.ingestion.StructuralBlock;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Default local layout model: PDFBox TextPosition extraction per page (no HTTP / ONNX).
 * Always available when {@link LayoutAnalysisOptions#PDF_BYTES} is present in options.
 */
public final class LocalPdfTextLayoutProvider implements LayoutAnalysisProvider {

    public static final String PROVIDER_CODE = LayoutAnalysisOptions.DEFAULT_LOCAL_PROVIDER;
    public static final String MODEL_NAME = "pdfbox-textposition-v1";

    @Override
    public String providerCode() {
        return PROVIDER_CODE;
    }

    @Override
    public boolean available() {
        return true;
    }

    @Override
    public boolean supports(LayoutPageRequest request) {
        return readPdfBytes(request) != null && request.pageNumber() > 0;
    }

    @Override
    public LayoutPageResult analyze(LayoutPageRequest request) {
        byte[] pdfBytes = readPdfBytes(request);
        if (pdfBytes == null || pdfBytes.length == 0) {
            return emptyResult(request);
        }
        try {
            List<StructuralBlock> blocks = LayoutPdfTextExtractor.extractPage(pdfBytes, request.pageNumber());
            List<StructuralBlock> stamped = LayoutResultMapper.stampProvider(
                    blocks,
                    PROVIDER_CODE,
                    MODEL_NAME,
                    "local-pdf-textposition"
            );
            Map<String, Object> metadata = new HashMap<>(request.effectiveOptions());
            metadata.put("layoutProvider", PROVIDER_CODE);
            metadata.put("layoutModel", MODEL_NAME);
            metadata.put("layoutAnalysisRoute", "local-pdf-textposition");
            metadata.put("pageNumber", request.pageNumber());
            return new LayoutPageResult(
                    PROVIDER_CODE,
                    MODEL_NAME,
                    request.pageNumber(),
                    stamped,
                    detectTableRegions(stamped),
                    null,
                    null,
                    Map.copyOf(metadata)
            );
        } catch (IOException exception) {
            throw new IllegalStateException(
                    "本地 PDF 版面解析失败: page=" + request.pageNumber() + " uri=" + request.sourceUri(),
                    exception
            );
        }
    }

    static byte[] readPdfBytes(LayoutPageRequest request) {
        if (request == null) {
            return null;
        }
        Object raw = request.effectiveOptions().get(LayoutAnalysisOptions.PDF_BYTES);
        if (raw instanceof byte[] bytes && bytes.length > 0) {
            return bytes;
        }
        return null;
    }

    private static LayoutPageResult emptyResult(LayoutPageRequest request) {
        return new LayoutPageResult(
                PROVIDER_CODE,
                MODEL_NAME,
                request.pageNumber(),
                List.of(),
                List.of(),
                null,
                null,
                Map.of("layoutProvider", PROVIDER_CODE, "layoutModel", MODEL_NAME)
        );
    }

    private static List<LayoutTableRegion> detectTableRegions(List<StructuralBlock> blocks) {
        List<LayoutTableRegion> regions = new ArrayList<>();
        for (StructuralBlock block : blocks) {
            if (!"table_row".equals(block.blockType())) {
                continue;
            }
            Object regionId = block.metadata().get("tableRegionId");
            int id = regionId instanceof Number number ? number.intValue() : regions.size();
            Object bbox = block.metadata().get("tableRegionBbox");
            if (bbox == null) {
                bbox = block.metadata().get("bbox");
            }
            @SuppressWarnings("unchecked")
            List<Double> bboxList = bbox instanceof List<?> list ? (List<Double>) list : null;
            String label = String.valueOf(block.metadata().getOrDefault("tableRegionLabel", "pdf-table-" + id));
            String detection = String.valueOf(block.metadata().getOrDefault("tableDetection", "local-pdf"));
            regions.add(new LayoutTableRegion(id, label, bboxList, detection));
        }
        return List.copyOf(regions);
    }
}
