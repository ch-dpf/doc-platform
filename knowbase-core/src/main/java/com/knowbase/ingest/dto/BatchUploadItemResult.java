package com.knowbase.ingest.dto;

import java.util.UUID;

public record BatchUploadItemResult(
        String fileName,
        boolean success,
        DocumentResponse document,
        String errorCode,
        String message,
        UUID asyncTaskId) {

    public BatchUploadItemResult(
            String fileName,
            boolean success,
            DocumentResponse document,
            String errorCode,
            String message) {
        this(fileName, success, document, errorCode, message, null);
    }
}
