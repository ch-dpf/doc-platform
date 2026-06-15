package com.knowbase.vector.retrieval;

import com.knowbase.vector.mapper.DocumentChunkMapper;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/** 库内 submitter 白名单（从 chunk.metadata 聚合），用于问句人员最长匹配。 */
@Component
public class LibrarySubmitterIndex {

    private final DocumentChunkMapper chunkMapper;
    private final ConcurrentHashMap<UUID, CachedSubmitters> cache = new ConcurrentHashMap<>();

    public LibrarySubmitterIndex(DocumentChunkMapper chunkMapper) {
        this.chunkMapper = chunkMapper;
    }

    public List<String> listSubmitters(UUID libraryId) {
        if (libraryId == null) {
            return List.of();
        }
        CachedSubmitters cached = cache.computeIfAbsent(libraryId, id -> load(id));
        if (cached.expiresAtMs < System.currentTimeMillis()) {
            cached = load(libraryId);
            cache.put(libraryId, cached);
        }
        return cached.submitters;
    }

    public Optional<String> longestMatch(UUID libraryId, String text) {
        if (text == null || text.isBlank()) {
            return Optional.empty();
        }
        String best = null;
        for (String name : listSubmitters(libraryId)) {
            if (text.contains(name) && (best == null || name.length() > best.length())) {
                best = name;
            }
        }
        return Optional.ofNullable(best);
    }

    public List<String> matchAll(UUID libraryId, String text) {
        if (text == null || text.isBlank()) {
            return List.of();
        }
        List<String> matched = new ArrayList<>();
        for (String name : listSubmitters(libraryId)) {
            if (text.contains(name)) {
                matched.add(name);
            }
        }
        matched.sort(Comparator.comparingInt(String::length).reversed());
        return List.copyOf(matched);
    }

    public void invalidate(UUID libraryId) {
        if (libraryId != null) {
            cache.remove(libraryId);
        }
    }

    private CachedSubmitters load(UUID libraryId) {
        List<String> names = chunkMapper.findDistinctSubmitters(libraryId).stream()
                .sorted((a, b) -> Integer.compare(b.length(), a.length()))
                .toList();
        long expires = System.currentTimeMillis() + 300_000L;
        return new CachedSubmitters(names, expires);
    }

    private record CachedSubmitters(List<String> submitters, long expiresAtMs) {}
}
