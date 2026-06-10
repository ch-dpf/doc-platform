package com.knowbase.ingest.service;

import com.knowbase.event.DocumentReadyForIndexEvent;
import com.knowbase.ingest.domain.DocMetadata;
import com.knowbase.ingest.domain.IndexStatus;
import com.knowbase.ingest.domain.ParseStatus;
import com.knowbase.ingest.dto.DocumentResponse;
import com.knowbase.ingest.storage.ObjectStorageService;
import com.knowbase.ingest.support.DocMetadataStore;
import com.knowbase.library.service.LibraryConfigResolver;
import com.knowbase.platform.DocumentIndexCoordinator;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

/**
 * 人工审核通过后触发向量索引。
 */
@Service
public class DocumentIndexApprovalService {

    private final DocMetadataStore repository;
    private final ObjectStorageService storageService;
    private final DocumentIndexCoordinator indexCoordinator;
    private final LibraryConfigResolver libraryConfigResolver;

    public DocumentIndexApprovalService(
            DocMetadataStore repository,
            ObjectStorageService storageService,
            DocumentIndexCoordinator indexCoordinator,
            LibraryConfigResolver libraryConfigResolver) {
        this.repository = repository;
        this.storageService = storageService;
        this.indexCoordinator = indexCoordinator;
        this.libraryConfigResolver = libraryConfigResolver;
    }

    @Transactional
    public DocumentResponse approveForIndexing(UUID docId) {
        DocMetadata doc = repository.findByDocIdAndDeletedFalse(docId)
                .orElseThrow(() -> new DocumentNotFoundException(docId));
        if (doc.getParseStatus() != ParseStatus.PARSED) {
            throw new IllegalStateException("文档尚未解析完成，无法批准索引");
        }
        if (doc.getIndexStatus() == IndexStatus.INDEXED) {
            return DocumentResponse.from(doc);
        }
        if (doc.getParsedTextKey() == null || doc.getParsedTextKey().isBlank()) {
            throw new IllegalStateException("文档缺少解析结果，无法批准索引");
        }

        doc.setIndexRequested(true);
        doc.setIndexStatus(IndexStatus.PENDING);
        repository.save(doc);

        String url = storageService.presignedGetUrl(doc.getParsedTextKey());
        DocumentReadyForIndexEvent readyEvent = DocumentReadyForIndexEvent.create(
                doc.getLibraryId(),
                doc.getDocId(),
                doc.getTenantId(),
                doc.getVersion(),
                doc.getChecksumSha256(),
                doc.getMimeType(),
                url,
                doc.getParsedTextKey());
        indexCoordinator.processReadyForIndex(readyEvent);
        return DocumentResponse.from(doc);
    }
}
