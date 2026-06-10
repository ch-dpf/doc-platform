package com.knowbase.ingest.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.knowbase.ingest.domain.DocMetadata;
import com.knowbase.ingest.dto.DocumentListQuery;
import com.knowbase.ingest.dto.DocumentResponse;
import com.knowbase.ingest.dto.PageResponse;
import com.knowbase.ingest.support.DocMetadataStore;
import com.knowbase.vector.dto.DocChunkCountRow;
import com.knowbase.vector.dto.DocVersionPair;
import com.knowbase.vector.mapper.DocumentChunkMapper;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class DocumentQueryService {

    private final DocMetadataStore repository;
    private final DocumentChunkMapper chunkMapper;

    public DocumentQueryService(DocMetadataStore repository, DocumentChunkMapper chunkMapper) {
        this.repository = repository;
        this.chunkMapper = chunkMapper;
    }

    public PageResponse<DocumentResponse> list(DocumentListQuery query) {
        IPage<DocMetadata> page = repository.list(query);
        Map<String, Integer> chunkCounts = resolveChunkCounts(page.getRecords());
        return new PageResponse<>(
                page.getRecords().stream()
                        .map(doc -> DocumentResponse.from(doc, chunkCounts.get(chunkKey(doc))))
                        .toList(),
                page.getTotal(),
                query.page(),
                query.size());
    }

    public DocumentResponse get(UUID docId) {
        DocMetadata doc = repository.findByDocIdAndDeletedFalse(docId)
                .orElseThrow(() -> new DocumentNotFoundException(docId));
        Integer chunkCount = resolveChunkCounts(List.of(doc)).get(chunkKey(doc));
        return DocumentResponse.from(doc, chunkCount);
    }

    private Map<String, Integer> resolveChunkCounts(List<DocMetadata> docs) {
        if (docs == null || docs.isEmpty()) {
            return Map.of();
        }
        List<DocVersionPair> pairs = docs.stream()
                .map(doc -> new DocVersionPair(doc.getDocId(), doc.getVersion()))
                .toList();
        return chunkMapper.countByDocVersions(pairs).stream()
                .collect(Collectors.toMap(
                        row -> chunkKey(row.docId(), row.version()),
                        DocChunkCountRow::chunkCount,
                        (a, b) -> a));
    }

    private static String chunkKey(DocMetadata doc) {
        return chunkKey(doc.getDocId(), doc.getVersion());
    }

    private static String chunkKey(UUID docId, int version) {
        return docId + ":" + version;
    }
}
