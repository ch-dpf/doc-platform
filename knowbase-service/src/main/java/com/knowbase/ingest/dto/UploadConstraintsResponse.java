package com.knowbase.ingest.dto;

import java.util.List;

public record UploadConstraintsResponse(
        List<String> allowedMimeTypes,
        long maxFileSizeBytes,
        String maxFileSizeDisplay,
        int maxBatchFiles,
        String storageType,
        /** upload | crawl | both */
        String ingestSourceMode,
        boolean uploadAllowed,
        boolean collectAllowed,
        boolean ocrEnabled,
        boolean ocrEngineAvailable,
        boolean manualReviewRequired,
        String versionUpdateStrategy) {}
