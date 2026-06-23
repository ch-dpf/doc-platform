package com.knowbase.retrieval;

import com.knowbase.domain.model.Citation;
import com.knowbase.domain.model.EvidencePack;
import com.knowbase.domain.model.EvidenceSegment;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public final class DefaultEvidenceBuilder implements EvidenceBuilder {

    private static final int DEFAULT_MAX_EVIDENCE = 12;

    @Override
    public EvidencePack build(List<RetrievalCandidate> candidates) {
        List<EvidenceSegment> segments = new ArrayList<>();
        List<Citation> citations = new ArrayList<>();
        List<RetrievalCandidate> selected = candidates.stream()
                .limit(DEFAULT_MAX_EVIDENCE)
                .toList();
        for (RetrievalCandidate candidate : selected) {
            UUID evidenceId = UUID.randomUUID();
            Map<String, Object> metadata = enrichMetadata(candidate);
            segments.add(new EvidenceSegment(
                    evidenceId,
                    candidate.libraryId(),
                    candidate.documentId(),
                    candidate.chunkId(),
                    candidate.indexVersionId(),
                    candidate.content(),
                    candidate.score(),
                    metadata
            ));
            String title = String.valueOf(metadata.getOrDefault("title", "文档片段"));
            String sourceUri = String.valueOf(metadata.getOrDefault("sourceUri", ""));
            citations.add(new Citation(
                    UUID.randomUUID(),
                    candidate.libraryId(),
                    candidate.documentId(),
                    candidate.chunkId(),
                    candidate.indexVersionId(),
                    title,
                    sourceUri,
                    candidate.content(),
                    candidate.score(),
                    locationMetadata(metadata)
            ));
        }
        return new EvidencePack(
                UUID.randomUUID(),
                segments,
                citations,
                0,
                "approx-default",
                "1"
        );
    }

    private static Map<String, Object> enrichMetadata(RetrievalCandidate candidate) {
        java.util.HashMap<String, Object> metadata = new java.util.HashMap<>();
        if (candidate.metadata() != null) {
            metadata.putAll(candidate.metadata());
        }
        metadata.put("libraryId", candidate.libraryId().toString());
        metadata.put("documentId", candidate.documentId().toString());
        metadata.put("chunkId", candidate.chunkId().toString());
        metadata.put("indexVersionId", candidate.indexVersionId().toString());
        metadata.put("score", candidate.score());
        return Map.copyOf(metadata);
    }

    private static Map<String, Object> locationMetadata(Map<String, Object> metadata) {
        java.util.HashMap<String, Object> location = new java.util.HashMap<>();
        copyIfPresent(metadata, location, "pageNumber");
        copyIfPresent(metadata, location, "bbox");
        copyIfPresent(metadata, location, "contentFamily");
        copyIfPresent(metadata, location, "sourceUri");
        copyIfPresent(metadata, location, "title");
        copyIfPresent(metadata, location, "vectorScore");
        copyIfPresent(metadata, location, "keywordScore");
        copyIfPresent(metadata, location, "vectorRank");
        copyIfPresent(metadata, location, "keywordRank");
        copyIfPresent(metadata, location, "retrievalBackend");
        return Map.copyOf(location);
    }

    private static void copyIfPresent(Map<String, Object> source, Map<String, Object> target, String key) {
        if (source != null && source.get(key) != null) {
            target.put(key, source.get(key));
        }
    }
}
