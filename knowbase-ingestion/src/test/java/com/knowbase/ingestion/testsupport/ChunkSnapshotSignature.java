package com.knowbase.ingestion.testsupport;

import com.knowbase.domain.model.DocumentChunk;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.function.Predicate;

/**
 * Stable chunk-boundary signature for snapshot regression tests.
 */
public final class ChunkSnapshotSignature {

    private ChunkSnapshotSignature() {
    }

    public record Signature(
            int totalChunks,
            int indexableChunks,
            int maxIndexableTokens,
            List<String> indexableFingerprints
    ) {
    }

    public static Signature capture(List<DocumentChunk> chunks, Predicate<DocumentChunk> indexableFilter) {
        List<DocumentChunk> indexable = chunks.stream().filter(indexableFilter).toList();
        int maxTokens = indexable.stream().mapToInt(DocumentChunk::tokenCount).max().orElse(0);
        List<String> fingerprints = new ArrayList<>();
        for (DocumentChunk chunk : indexable) {
            String content = chunk.content() == null ? "" : chunk.content().trim();
            if (content.length() > 48) {
                content = content.substring(0, 48);
            }
            fingerprints.add(content);
        }
        return new Signature(chunks.size(), indexable.size(), maxTokens, List.copyOf(fingerprints));
    }

    public static Map<String, Object> asMap(Signature signature) {
        Map<String, Object> map = new TreeMap<>();
        map.put("totalChunks", signature.totalChunks());
        map.put("indexableChunks", signature.indexableChunks());
        map.put("maxIndexableTokens", signature.maxIndexableTokens());
        map.put("indexableFingerprints", signature.indexableFingerprints());
        return map;
    }
}
