package com.knowbase.pipeline.config;

import com.knowbase.ingest.dto.IngestProfileSummary;
import com.knowbase.ingest.service.InvalidDocumentException;
import com.knowbase.platform.JsonSupport;

public final class IngestProfileSupport {

    private static final int MIN_CHUNK_SIZE = 100;
    private static final int MAX_CHUNK_SIZE = 8000;
    private static final int MAX_CHUNK_OVERLAP = 2000;

    private IngestProfileSupport() {
    }

    public static IngestProfile parse(String json) {
        if (json == null || json.isBlank()) {
            return null;
        }
        try {
            IngestProfile profile = JsonSupport.fromJson(json, IngestProfile.class);
            return profile != null && !profile.isEmpty() ? profile : null;
        } catch (IllegalStateException ex) {
            return null;
        }
    }

    /**
     * Validates ingest profile for upload: only chunk numeric overrides allowed.
     *
     * @return normalized JSON or null when absent / empty
     */
    public static String prepareForUpload(String json) {
        if (json == null || json.isBlank()) {
            return null;
        }
        IngestProfile raw;
        try {
            raw = JsonSupport.fromJson(json, IngestProfile.class);
        } catch (IllegalStateException ex) {
            throw InvalidDocumentException.of(
                    InvalidDocumentException.CODE_INGEST_PROFILE_INVALID,
                    "ingestProfile 不是合法的 JSON 对象");
        }
        if (raw == null) {
            return null;
        }
        if (raw.getParsing() != null || raw.getCleaning() != null) {
            throw InvalidDocumentException.of(
                    InvalidDocumentException.CODE_INGEST_PROFILE_INVALID,
                    "ingestProfile 仅允许覆盖 chunkSize、chunkOverlap、minParagraphLength；解析由 MIME 决定，分块策略由库配置决定");
        }
        IngestProfile normalized = new IngestProfile();
        if (raw.getChunkSize() != null) {
            normalized.setChunkSize(validateChunkSize(raw.getChunkSize()));
        }
        if (raw.getChunkOverlap() != null) {
            normalized.setChunkOverlap(validateChunkOverlap(raw.getChunkOverlap()));
        }
        if (raw.getMinParagraphLength() != null) {
            normalized.setMinParagraphLength(validateMinParagraphLength(raw.getMinParagraphLength()));
        }
        if (normalized.isEmpty()) {
            return null;
        }
        return JsonSupport.toJson(normalized);
    }

    public static String normalizeJson(String json) {
        IngestProfile profile = parse(json);
        if (profile == null) {
            return null;
        }
        return JsonSupport.toJson(profile);
    }

    public static boolean hasChunkingOverride(String json) {
        IngestProfile profile = parse(json);
        return profile != null && profile.hasChunkingOverride();
    }

    public static IngestProfileSummary toSummary(String json) {
        IngestProfile profile = parse(json);
        if (profile == null) {
            return null;
        }
        if (profile.getChunkSize() == null && profile.getChunkOverlap() == null) {
            return null;
        }
        return new IngestProfileSummary(profile.getChunkSize(), profile.getChunkOverlap());
    }

    private static int validateChunkSize(int value) {
        if (value < MIN_CHUNK_SIZE || value > MAX_CHUNK_SIZE) {
            throw InvalidDocumentException.of(
                    InvalidDocumentException.CODE_INGEST_PROFILE_INVALID,
                    "chunkSize 须在 " + MIN_CHUNK_SIZE + "–" + MAX_CHUNK_SIZE + " 之间");
        }
        return value;
    }

    private static int validateChunkOverlap(int value) {
        if (value < 0 || value > MAX_CHUNK_OVERLAP) {
            throw InvalidDocumentException.of(
                    InvalidDocumentException.CODE_INGEST_PROFILE_INVALID,
                    "chunkOverlap 须在 0–" + MAX_CHUNK_OVERLAP + " 之间");
        }
        return value;
    }

    private static int validateMinParagraphLength(int value) {
        if (value < 0 || value > 500) {
            throw InvalidDocumentException.of(
                    InvalidDocumentException.CODE_INGEST_PROFILE_INVALID,
                    "minParagraphLength 须在 0–500 之间");
        }
        return value;
    }
}
