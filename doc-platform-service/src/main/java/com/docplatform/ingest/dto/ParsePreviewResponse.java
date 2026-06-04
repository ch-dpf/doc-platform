package com.docplatform.ingest.dto;

public record ParsePreviewResponse(
        String fileName,
        String mimeType,
        int charCount,
        boolean truncated,
        String text) {}
