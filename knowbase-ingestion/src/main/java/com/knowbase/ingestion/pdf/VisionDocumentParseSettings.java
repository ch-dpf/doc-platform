package com.knowbase.ingestion.pdf;

import com.knowbase.ingestion.layout.LayoutAnalysisService;

/**
 * Runtime settings for raster layout / vision PDF parsing.
 */
public record VisionDocumentParseSettings(
        LayoutAnalysisService layoutAnalysisService,
        boolean vlOnScanned,
        boolean vlOnLowConfidence,
        double vlLowConfidenceThreshold,
        boolean vlFallbackToHeuristic,
        int vlMaxPages
) {

    public boolean available() {
        return layoutAnalysisService != null && layoutAnalysisService.hasAvailableProvider();
    }

    public static VisionDocumentParseSettings disabled() {
        return new VisionDocumentParseSettings(null, true, true, 0.55d, true, 0);
    }
}
