package com.docplatform.ingest.service;

import com.docplatform.contract.DocumentDeletedEvent;
import com.docplatform.contract.DocumentReadyForIndexEvent;
import com.docplatform.ingest.domain.DocMetadata;
import com.docplatform.ingest.domain.IndexStatus;
import com.docplatform.ingest.domain.ParseStatus;
import com.docplatform.ingest.event.DocumentEventPublisher;
import com.docplatform.ingest.support.DocMetadataStore;
import com.docplatform.ingest.support.ParsedTextNormalizer;
import com.docplatform.ingest.storage.ObjectStorageService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.util.UUID;

@Service
public class DocumentPipelineService {

    private static final Logger log = LoggerFactory.getLogger(DocumentPipelineService.class);

    private final DocMetadataStore repository;
    private final ObjectStorageService storageService;
    private final DocumentParseService parseService;
    private final ParsedTextNormalizer textNormalizer;
    private final DocumentEventPublisher eventPublisher;

    public DocumentPipelineService(
            DocMetadataStore repository,
            ObjectStorageService storageService,
            DocumentParseService parseService,
            ParsedTextNormalizer textNormalizer,
            DocumentEventPublisher eventPublisher) {
        this.repository = repository;
        this.storageService = storageService;
        this.parseService = parseService;
        this.textNormalizer = textNormalizer;
        this.eventPublisher = eventPublisher;
    }

    /**
     * 在事务提交后再触发异步解析，避免重复上传升版本后异步任务仍读到旧 version，
     * 导致 Kafka 事件版本与库中不一致、indexStatus 一直停在 PENDING。
     */
    public void scheduleProcessAfterCommit(UUID docId, int version, byte[] fileBytes, String fileName) {
        Runnable task = () -> processAsync(docId, version, fileBytes, fileName);
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    task.run();
                }
            });
        } else {
            task.run();
        }
    }

    @Async
    @Transactional
    public void processAsync(UUID docId, int expectedVersion, byte[] fileBytes, String fileName) {
        DocMetadata doc = repository.findById(docId).orElse(null);
        if (doc == null || doc.isDeleted()) {
            return;
        }
        if (doc.getVersion() != expectedVersion) {
            log.warn(
                    "Skip stale parse job for doc {}: expected v{} but db is v{}",
                    docId,
                    expectedVersion,
                    doc.getVersion());
            return;
        }
        try {
            doc.setParseStatus(ParseStatus.PARSING);
            repository.save(doc);

            String plainText;
            try (ByteArrayInputStream in = new ByteArrayInputStream(fileBytes)) {
                plainText = textNormalizer.normalize(parseService.extractText(in, fileName));
            }

            String parsedKey =
                    doc.getTenantId() + "/" + doc.getDocId() + "/v" + expectedVersion + "/parsed.txt";
            byte[] textBytes = plainText.getBytes(StandardCharsets.UTF_8);
            storageService.putObject(parsedKey, new ByteArrayInputStream(textBytes), textBytes.length, "text/plain");

            doc.setParsedTextKey(parsedKey);
            doc.setParseStatus(ParseStatus.PARSED);
            if (doc.isIndexRequested()) {
                doc.setIndexStatus(IndexStatus.PENDING);
            }
            repository.save(doc);

            if (doc.isIndexRequested()) {
                String url = storageService.presignedGetUrl(parsedKey);
                eventPublisher.publish(DocumentReadyForIndexEvent.create(
                        doc.getDocId(),
                        doc.getTenantId(),
                        expectedVersion,
                        doc.getChecksumSha256(),
                        doc.getMimeType(),
                        url,
                        parsedKey));
                log.info("Published DOCUMENT_READY_FOR_INDEX for {} v{}", doc.getDocId(), expectedVersion);
            }
        } catch (Exception e) {
            log.error("Parse pipeline failed for {}", docId, e);
            doc.setParseStatus(ParseStatus.FAILED);
            doc.setIndexStatus(IndexStatus.FAILED);
            repository.save(doc);
        }
    }

    /** 软删除：保留元数据与 MinIO 对象，发布事件由向量服务清理索引。 */
    @Transactional
    public void deleteDocument(UUID docId) {
        DocMetadata doc = repository.findByDocIdAndDeletedFalse(docId)
                .orElseThrow(() -> new DocumentNotFoundException(docId));
        doc.setDeleted(true);
        repository.save(doc);
        eventPublisher.publish(DocumentDeletedEvent.create(doc.getDocId(), doc.getTenantId(), doc.getVersion()));
    }

    /**
     * 物理删除：移除 MinIO 下该文档全部版本对象，删除 ingest 元数据行，并通知向量服务清理。
     */
    @Transactional
    public void purgeDocument(UUID docId) {
        DocMetadata doc = repository.findById(docId)
                .orElseThrow(() -> new DocumentNotFoundException(docId));
        storageService.removeDocumentArtifacts(doc);
        if (!doc.isDeleted()) {
            eventPublisher.publish(
                    DocumentDeletedEvent.create(doc.getDocId(), doc.getTenantId(), doc.getVersion()));
        }
        repository.deleteByDocId(docId);
        log.info("Purged document {} metadata and storage artifacts", docId);
    }
}
