package com.knowbase.retrieval;

import com.knowbase.domain.model.DocumentChunk;
import com.knowbase.domain.repository.KnowbaseRepository;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

import java.util.function.Function;

/**
 * WeKnora-style parent-child retrieval: when a child chunk hits, enrich evidence with its parent summary.
 */
public final class ParentChildRetrievalExpander {

    private final KnowbaseRepository repository;
    private final Function<UUID, Optional<DocumentChunk>> chunkLookup;

    public ParentChildRetrievalExpander(KnowbaseRepository repository) {
        this.repository = Objects.requireNonNull(repository, "repository");
        this.chunkLookup = null;
    }

    ParentChildRetrievalExpander(Function<UUID, Optional<DocumentChunk>> chunkLookup) {
        this.repository = null;
        this.chunkLookup = Objects.requireNonNull(chunkLookup, "chunkLookup");
    }

    public List<RetrievalCandidate> expand(List<RetrievalCandidate> candidates, Map<String, Object> retrievalPolicy) {
        if (!readBoolean(retrievalPolicy, "expandParentChunks", true)) {
            return candidates == null ? List.of() : candidates;
        }
        if (candidates == null || candidates.isEmpty()) {
            return List.of();
        }
        Map<UUID, RetrievalCandidate> byChunkId = new LinkedHashMap<>();
        List<RetrievalCandidate> expanded = new ArrayList<>(candidates.size());
        for (RetrievalCandidate candidate : candidates) {
            RetrievalCandidate enriched = enrichCandidate(candidate);
            expanded.add(enriched);
            byChunkId.putIfAbsent(enriched.chunkId(), enriched);
            injectParentCandidate(enriched, byChunkId).ifPresent(parent -> {
                expanded.add(parent);
                byChunkId.put(parent.chunkId(), parent);
            });
        }
        return List.copyOf(expanded);
    }

    private Optional<DocumentChunk> findChunk(UUID chunkId) {
        if (repository != null) {
            return repository.findChunk(chunkId);
        }
        return chunkLookup.apply(chunkId);
    }

    private RetrievalCandidate enrichCandidate(RetrievalCandidate candidate) {
        Optional<DocumentChunk> child = findChunk(candidate.chunkId());
        if (child.isEmpty() || child.get().parentChunkId() == null) {
            return candidate;
        }
        Optional<DocumentChunk> parent = findChunk(child.get().parentChunkId());
        if (parent.isEmpty()) {
            return candidate;
        }
        String parentText = normalize(parent.get().content());
        String childText = normalize(candidate.content());
        if (parentText.isBlank() || childText.contains(parentText)) {
            return withParentMetadata(candidate, parent.get(), false);
        }
        String merged = parentText + "\n\n" + childText;
        Map<String, Object> metadata = new HashMap<>(candidate.metadata() == null ? Map.of() : candidate.metadata());
        metadata.put("parentChunkId", parent.get().chunkId());
        metadata.put("parentBoundaryType", parent.get().chunkBoundaryType());
        metadata.put("parentExpanded", true);
        metadata.put("expansionMode", "enrich");
        return new RetrievalCandidate(
                candidate.libraryId(),
                candidate.documentId(),
                candidate.chunkId(),
                candidate.indexVersionId(),
                merged,
                candidate.score(),
                Map.copyOf(metadata)
        );
    }

    private Optional<RetrievalCandidate> injectParentCandidate(
            RetrievalCandidate candidate,
            Map<UUID, RetrievalCandidate> existing
    ) {
        UUID parentChunkId = readUuid(candidate.metadata(), "parentChunkId");
        if (parentChunkId == null) {
            return Optional.empty();
        }
        if (existing.containsKey(parentChunkId)) {
            return Optional.empty();
        }
        Optional<DocumentChunk> parent = findChunk(parentChunkId);
        if (parent.isEmpty()) {
            return Optional.empty();
        }
        if (!Boolean.TRUE.equals(readObject(candidate.metadata(), "parentExpanded"))) {
            return Optional.empty();
        }
        Map<String, Object> metadata = new HashMap<>(parent.get().metadata() == null ? Map.of() : parent.get().metadata());
        metadata.put("injectedFromChildChunkId", candidate.chunkId());
        metadata.put("parentInjected", true);
        metadata.put("expansionMode", "inject-parent");
        double inheritedScore = candidate.score() * 0.98d;
        return Optional.of(new RetrievalCandidate(
                parent.get().libraryId(),
                parent.get().documentId(),
                parent.get().chunkId(),
                parent.get().indexVersionId(),
                parent.get().content(),
                inheritedScore,
                Map.copyOf(metadata)
        ));
    }

    private static RetrievalCandidate withParentMetadata(
            RetrievalCandidate candidate,
            DocumentChunk parent,
            boolean expanded
    ) {
        Map<String, Object> metadata = new HashMap<>(candidate.metadata() == null ? Map.of() : candidate.metadata());
        metadata.put("parentChunkId", parent.chunkId());
        metadata.put("parentBoundaryType", parent.chunkBoundaryType());
        metadata.put("parentExpanded", expanded);
        return new RetrievalCandidate(
                candidate.libraryId(),
                candidate.documentId(),
                candidate.chunkId(),
                candidate.indexVersionId(),
                candidate.content(),
                candidate.score(),
                Map.copyOf(metadata)
        );
    }

    private static UUID readUuid(Map<String, Object> metadata, String key) {
        Object value = readObject(metadata, key);
        if (value == null) {
            return null;
        }
        if (value instanceof UUID uuid) {
            return uuid;
        }
        try {
            return UUID.fromString(String.valueOf(value).trim());
        } catch (IllegalArgumentException ignored) {
            return null;
        }
    }

    private static Object readObject(Map<String, Object> metadata, String key) {
        if (metadata == null) {
            return null;
        }
        return metadata.get(key);
    }

    private static String normalize(String text) {
        return text == null ? "" : text.trim();
    }

    private static boolean readBoolean(Map<String, Object> policy, String key, boolean defaultValue) {
        if (policy == null || policy.get(key) == null) {
            return defaultValue;
        }
        Object value = policy.get(key);
        if (value instanceof Boolean booleanValue) {
            return booleanValue;
        }
        return Boolean.parseBoolean(String.valueOf(value).trim());
    }
}
