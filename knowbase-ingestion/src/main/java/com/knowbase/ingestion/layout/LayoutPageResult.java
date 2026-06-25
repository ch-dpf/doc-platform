package com.knowbase.ingestion.layout;

import com.knowbase.ingestion.StructuralBlock;

import java.util.List;
import java.util.Map;

public record LayoutPageResult(
        String providerCode,
        String modelName,
        int pageNumber,
        List<StructuralBlock> blocks,
        List<LayoutTableRegion> tableRegions,
        String detectedLanguage,
        Double rotationDegrees,
        Map<String, Object> metadata
) {
    public static LayoutPageResult empty(String providerCode, int pageNumber) {
        return new LayoutPageResult(
                providerCode,
                providerCode,
                pageNumber,
                List.of(),
                List.of(),
                null,
                null,
                Map.of()
        );
    }
}
