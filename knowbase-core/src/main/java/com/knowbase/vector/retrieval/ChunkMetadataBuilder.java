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
        if (pipelineChunk != null) {
            mergeTemporalMetadata(metadata, doc, pipelineChunk.content());
        }
        return metadata.isEmpty() ? null : JsonSupport.toJson(metadata);
    }

    /** 将时间元数据合并进已有 chunk.metadata JSON（用于存量回填）。 */
    public static String mergeTemporalIntoExisting(String existingMetadataJson, DocMetadata doc, String content) {
        if (doc == null) {
            return existingMetadataJson;
        }
        Map<String, String> metadata = parseMetadataMap(existingMetadataJson);
        mergeTemporalMetadata(metadata, doc, content);
        return metadata.isEmpty() ? existingMetadataJson : JsonSupport.toJson(metadata);
    }

    public static boolean hasCompleteTemporalFields(String metadataJson) {
        Map<String, String> metadata = parseMetadataMap(metadataJson);
        return metadata.containsKey(TemporalMetadataFields.PERIOD_YEAR)
                && metadata.containsKey(TemporalMetadataFields.PERIOD_START)
                && metadata.containsKey(TemporalMetadataFields.SUBMITTER);
    }

    private static Map<String, String> parseMetadataMap(String metadataJson) {
        Map<String, String> metadata = new LinkedHashMap<>();
        if (metadataJson == null || metadataJson.isBlank()) {
            return metadata;
        }
        Map<?, ?> raw = JsonSupport.fromJson(metadataJson, Map.class);
        for (Map.Entry<?, ?> entry : raw.entrySet()) {
            if (entry.getKey() == null || entry.getValue() == null) {
                continue;
            }
            String key = String.valueOf(entry.getKey()).trim();
            if (!key.isEmpty()) {
                metadata.put(key, String.valueOf(entry.getValue()).trim());
            }
        }
        return metadata;
    }

    private static void mergeTemporalMetadata(Map<String, String> metadata, DocMetadata doc, String content) {
        ChunkTemporalMetadataExtractor.TemporalMetadata temporal =
                ChunkTemporalMetadataExtractor.extract(doc, content);
        putIfPresent(metadata, TemporalMetadataFields.PERIOD_YEAR, temporal.periodYear());
        putIfPresent(metadata, TemporalMetadataFields.PERIOD_START, temporal.periodStart());
        putIfPresent(metadata, TemporalMetadataFields.PERIOD_END, temporal.periodEnd());
        putIfPresent(metadata, TemporalMetadataFields.PERIOD_MONTHS, temporal.periodMonths());
        putIfPresent(metadata, TemporalMetadataFields.SUBMITTER, temporal.submitter());
        putIfPresent(metadata, TemporalMetadataFields.SECTION_LABEL, temporal.sectionLabel());
        putIfPresent(metadata, TemporalMetadataFields.HAS_COMPLETED_WORK, temporal.hasCompletedWork());
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
