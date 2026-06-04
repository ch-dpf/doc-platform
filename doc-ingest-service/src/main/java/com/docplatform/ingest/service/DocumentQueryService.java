package com.docplatform.ingest.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.docplatform.ingest.domain.DocMetadata;
import com.docplatform.ingest.dto.DocumentListQuery;
import com.docplatform.ingest.dto.DocumentResponse;
import com.docplatform.ingest.dto.PageResponse;
import com.docplatform.ingest.support.DocMetadataStore;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
public class DocumentQueryService {

    private final DocMetadataStore repository;

    public DocumentQueryService(DocMetadataStore repository) {
        this.repository = repository;
    }

    public PageResponse<DocumentResponse> list(DocumentListQuery query) {
        IPage<DocMetadata> page = repository.list(query);
        return new PageResponse<>(
                page.getRecords().stream().map(DocumentResponse::from).toList(),
                page.getTotal(),
                query.page(),
                query.size());
    }

    public DocumentResponse get(UUID docId) {
        return repository.findByDocIdAndDeletedFalse(docId)
                .map(DocumentResponse::from)
                .orElseThrow(() -> new DocumentNotFoundException(docId));
    }
}
