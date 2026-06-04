package com.docplatform.ingest.dto;

public record BatchUploadItemResult(
        String fileName,
        boolean success,
        DocumentResponse document,
        String errorCode,
        String message) {}
