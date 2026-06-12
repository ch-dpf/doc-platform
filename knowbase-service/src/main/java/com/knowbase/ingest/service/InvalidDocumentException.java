package com.knowbase.ingest.service;

import java.util.List;

public class InvalidDocumentException extends RuntimeException {

    public static final String CODE_MIME_NOT_ALLOWED = "MIME_TYPE_NOT_ALLOWED";
    public static final String CODE_FILE_TOO_LARGE = "FILE_TOO_LARGE";
    public static final String CODE_BATCH_LIMIT = "BATCH_FILE_LIMIT_EXCEEDED";
    public static final String CODE_LIBRARY_DOCUMENT_LIMIT = "LIBRARY_DOCUMENT_LIMIT_EXCEEDED";
    public static final String CODE_LIBRARY_SIZE_LIMIT = "LIBRARY_SIZE_LIMIT_EXCEEDED";

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
    public static final String CODE_INGEST_PROFILE_NOT_ALLOWED = "INGEST_PROFILE_NOT_ALLOWED";
    public static final String CODE_INGEST_PROFILE_INVALID = "INGEST_PROFILE_INVALID";
    public static final String CODE_DUPLICATE_DIFFERENT_CHUNK_PROFILE = "DUPLICATE_DIFFERENT_CHUNK_PROFILE";

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

    public static InvalidDocumentException libraryDocumentLimit(String fileName, int current, int max) {
        String message = String.format(
                "知识库文档数已达上限（当前 %d，上限 %d），无法继续上传「%s」。",
                current,
                max,
                fileName);
        return new InvalidDocumentException(CODE_LIBRARY_DOCUMENT_LIMIT, message, fileName, null, List.of());
    }

    public static InvalidDocumentException duplicateDifferentChunkProfile(
            String fileName, String existingChunkProfileId) {
        String profile = existingChunkProfileId != null && !existingChunkProfileId.isBlank()
                ? existingChunkProfileId
                : "（库默认档）";
        String message = String.format(
                "文件「%s」内容与库内已有文档相同，但本次分块参数不同（已有分块档 %s）。"
                        + "不会覆盖原文档；请关闭分块覆盖使用库默认，或修改文件内容后重试。",
                fileName,
                profile);
        return new InvalidDocumentException(
                CODE_DUPLICATE_DIFFERENT_CHUNK_PROFILE, message, fileName, null, List.of());
    }

    public static InvalidDocumentException librarySizeLimit(String fileName, long projectedBytes, long maxBytes) {
        String message = String.format(
                "上传「%s」后将超出知识库总大小上限（预计 %d 字节，上限 %d 字节）。",
                fileName,
                projectedBytes,
                maxBytes);
        return new InvalidDocumentException(CODE_LIBRARY_SIZE_LIMIT, message, fileName, null, List.of());
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
