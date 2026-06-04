package com.docplatform.ingest.service;

import java.util.List;

public class InvalidDocumentException extends RuntimeException {

    public static final String CODE_MIME_NOT_ALLOWED = "MIME_TYPE_NOT_ALLOWED";
    public static final String CODE_FILE_TOO_LARGE = "FILE_TOO_LARGE";
    public static final String CODE_BATCH_LIMIT = "BATCH_FILE_LIMIT_EXCEEDED";

    private final String errorCode;
    private final String fileName;
    private final String detectedMimeType;
    private final List<String> allowedMimeTypes;

    public InvalidDocumentException(
            String errorCode,
            String message,
            String fileName,
            String detectedMimeType,
            List<String> allowedMimeTypes) {
        super(message);
        this.errorCode = errorCode;
        this.fileName = fileName;
        this.detectedMimeType = detectedMimeType;
        this.allowedMimeTypes = allowedMimeTypes;
    }

    public static InvalidDocumentException mimeNotAllowed(
            String fileName, String mimeType, List<String> allowedMimeTypes) {
        String displayMime = mimeType != null ? mimeType : "未知";
        String message = String.format(
                "文件「%s」的类型（%s）不在允许范围内，仅支持 PDF、Word、Excel、纯文本与 Markdown 等文档格式。",
                fileName,
                displayMime);
        return new InvalidDocumentException(CODE_MIME_NOT_ALLOWED, message, fileName, mimeType, allowedMimeTypes);
    }

    public static final String CODE_COLLECTION_FAILED = "COLLECTION_FAILED";
    public static final String CODE_FETCH_FAILED = "URL_FETCH_FAILED";
    public static final String CODE_INGEST_SOURCE_NOT_ALLOWED = "INGEST_SOURCE_NOT_ALLOWED";

    public static InvalidDocumentException of(String errorCode, String message) {
        return new InvalidDocumentException(errorCode, message, null, null, List.of());
    }

    public static InvalidDocumentException fileTooLarge(String fileName, long sizeBytes, long maxBytes) {
        String message = String.format(
                "文件「%s」大小为 %d 字节，超过上限 %d 字节。",
                fileName,
                sizeBytes,
                maxBytes);
        return new InvalidDocumentException(CODE_FILE_TOO_LARGE, message, fileName, null, List.of());
    }

    public String getErrorCode() {
        return errorCode;
    }

    public String getFileName() {
        return fileName;
    }

    public String getDetectedMimeType() {
        return detectedMimeType;
    }

    public List<String> getAllowedMimeTypes() {
        return allowedMimeTypes;
    }
}
