package com.knowbase.api.result;

import java.util.UUID;

public record DocumentIngestResult(
        UUID docId,
        UUID libraryId,
        String tenantId,
        String fileName,
        String parseStatus,
        String indexStatus,
        int version) {}
