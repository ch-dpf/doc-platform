package com.knowbase.vector.retrieval;

import com.knowbase.ingest.domain.DocMetadata;
import com.knowbase.pipeline.chunk.PipelineChunk;
import com.knowbase.pipeline.config.ChunkProfileFingerprint;
import com.knowbase.platform.JsonSupport;

import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;

/**
 * 从文档元数据构建写入 chunk.metadata 的 JSON 对象。
 */
public final class ChunkMetadataBuilder {

    private ChunkMetadataBuilder() {}

    public static String buildJson(DocMetadata doc) {
        return buildJson(doc, null);
    }

    public static String buildJson(DocMetadata doc, PipelineChunk pipelineChunk) {
        if (doc == null) {
            return null;
        }
        Map<String, String> metadata = new LinkedHashMap<>();
        putIfPresent(metadata, "mimeType", doc.getMimeType());
        if (doc.getSourceType() != null) {
            putIfPresent(metadata, "sourceType", doc.getSourceType().name());
        }
        putIfPresent(metadata, "fileName", doc.getFileName());
        putIfPresent(metadata, "docType", resolveDocType(doc.getFileName(), doc.getMimeType()));
        putIfPresent(metadata, ChunkProfileFingerprint.METADATA_FIELD, doc.getChunkProfileId());
        mergeCustomMetadata(metadata, doc.getCustomMetadataJson());
        if (pipelineChunk != null && pipelineChunk.hasParentContext()) {
            metadata.put("granularity", "child");
            metadata.put("parentIndex", String.valueOf(pipelineChunk.parentIndex()));
            metadata.put("parentContext", pipelineChunk.parentContext());
        }
        return metadata.isEmpty() ? null : JsonSupport.toJson(metadata);
    }

    private static void mergeCustomMetadata(Map<String, String> target, String customMetadataJson) {
        if (customMetadataJson == null || customMetadataJson.isBlank()) {
            return;
        }
        Map<?, ?> raw = JsonSupport.fromJson(customMetadataJson, Map.class);
        for (Map.Entry<?, ?> entry : raw.entrySet()) {
            if (entry.getKey() == null) {
                continue;
            }
            String key = String.valueOf(entry.getKey()).trim();
            if (key.isEmpty() || entry.getValue() == null) {
                continue;
            }
            target.put(key, String.valueOf(entry.getValue()).trim());
        }
    }

    static String resolveDocType(String fileName, String mimeType) {
        String fromName = extensionToken(fileName);
        if (fromName != null) {
            return mapExtensionToDocType(fromName);
        }
        return mapMimeToDocType(mimeType);
    }

    private static String extensionToken(String fileName) {
        if (fileName == null || fileName.isBlank()) {
            return null;
        }
        int dot = fileName.lastIndexOf('.');
        if (dot < 0 || dot == fileName.length() - 1) {
            return null;
        }
        return fileName.substring(dot + 1).trim().toLowerCase(Locale.ROOT);
    }

    private static String mapExtensionToDocType(String ext) {
        return switch (ext) {
            case "pdf" -> "pdf";
            case "doc", "docx" -> "word";
            case "txt" -> "txt";
            case "md", "markdown" -> "markdown";
            case "xls", "xlsx" -> "excel";
            default -> ext;
        };
    }

    private static String mapMimeToDocType(String mimeType) {
        if (mimeType == null || mimeType.isBlank()) {
            return null;
        }
        return switch (mimeType.trim().toLowerCase(Locale.ROOT)) {
            case "application/pdf" -> "pdf";
            case "application/msword",
                    "application/vnd.openxmlformats-officedocument.wordprocessingml.document" -> "word";
            case "text/plain" -> "txt";
            case "text/markdown", "text/x-markdown", "text/x-web-markdown" -> "markdown";
            case "application/vnd.ms-excel",
                    "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet" -> "excel";
            default -> null;
        };
    }

    private static void putIfPresent(Map<String, String> metadata, String key, String value) {
        if (value != null && !value.isBlank()) {
            metadata.put(key, value.trim());
        }
    }
}
