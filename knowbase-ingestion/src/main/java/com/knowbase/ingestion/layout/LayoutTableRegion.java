package com.knowbase.ingestion.layout;

import java.util.List;
import java.util.Map;

public record LayoutTableRegion(
        int tableRegionId,
        String label,
        List<Double> bbox,
        String detectionSource
) {
    public Map<String, Object> toBlockMetadata(int pageNumber) {
        java.util.HashMap<String, Object> metadata = new java.util.HashMap<>();
        metadata.put("tableRegionId", tableRegionId);
        metadata.put("tableRegionLabel", label);
        metadata.put("tableDetectionSource", detectionSource);
        metadata.put("pageNumber", pageNumber);
        if (bbox != null && bbox.size() == 4) {
            metadata.put("tableRegionBbox", bbox);
        }
        return Map.copyOf(metadata);
    }
}
