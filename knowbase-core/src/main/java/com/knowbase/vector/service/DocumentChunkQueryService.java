package com.knowbase.vector.service;

import com.knowbase.ingest.dto.DocumentChunkListResponse;
import com.knowbase.ingest.service.DocumentQueryService;
import com.knowbase.vector.dto.DocumentChunkRow;
import com.knowbase.vector.mapper.DocumentChunkMapper;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

@Service
public class DocumentChunkQueryService {

    private final DocumentQueryService documentQueryService;
    private final DocumentChunkMapper chunkMapper;

    public DocumentChunkQueryService(DocumentQueryService documentQueryService, DocumentChunkMapper chunkMapper) {
        this.documentQueryService = documentQueryService;
        this.chunkMapper = chunkMapper;
    }

    public DocumentChunkListResponse listByDocId(UUID docId, int page, int size) {
        var doc = documentQueryService.get(docId);
        int safePage = Math.max(1, page);
        int safeSize = Math.min(100, Math.max(1, size));
        long total = chunkMapper.countByDocIdAndVersion(docId, doc.version());
        int offset = (safePage - 1) * safeSize;
        List<DocumentChunkRow> rows = chunkMapper.listByDocIdAndVersionPaged(
                docId, doc.version(), offset, safeSize);
        List<DocumentChunkListResponse.ChunkItem> items = rows.stream()
                .map(row -> new DocumentChunkListResponse.ChunkItem(
                        row.chunkIndex(),
                        row.content() != null ? row.content().length() : 0,
                        row.content()))
                .toList();
        return new DocumentChunkListResponse(
                doc.docId(),
                doc.fileName(),
                doc.version(),
                items,
                total,
                safePage,
                safeSize);
    }
}
