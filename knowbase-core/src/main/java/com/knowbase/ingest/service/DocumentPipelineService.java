package com.knowbase.ingest.service;

import com.knowbase.event.DocumentDeletedEvent;
import com.knowbase.event.DocumentReadyForIndexEvent;
import com.knowbase.ingest.domain.DocMetadata;
import com.knowbase.ingest.domain.IndexStatus;
import com.knowbase.ingest.domain.ParseStatus;
import com.knowbase.ingest.support.DocMetadataStore;
import com.knowbase.library.service.LibraryConfigResolver;
import com.knowbase.pipeline.config.ChunkProfileFingerprint;
import com.knowbase.pipeline.config.EffectiveConfigResolver;
import com.knowbase.pipeline.config.EffectivePipelineConfig;
import com.knowbase.pipeline.config.IngestProfileSupport;
import com.knowbase.pipeline.content.ContentSignalsSupport;
import com.knowbase.platform.DocumentIndexCoordinator;
import com.knowbase.ingest.support.DocumentCleaningService;
import com.knowbase.ingest.support.ParsedTextNormalizer;
import com.knowbase.vector.chunk.ChunkTextPreprocessor;
import com.knowbase.ingest.storage.ObjectStorageService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import com.knowbase.tx.KnowbaseTransactional;
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
    private final DocumentIndexCoordinator indexCoordinator;
    private final LibraryConfigResolver libraryConfigResolver;
    private final EffectiveConfigResolver effectiveConfigResolver;
    private final DocumentCleaningService documentCleaningService;

    public DocumentPipelineService(
            DocMetadataStore repository,
            ObjectStorageService storageService,
            DocumentParseService parseService,
            ParsedTextNormalizer textNormalizer,
            DocumentIndexCoordinator indexCoordinator,
            LibraryConfigResolver libraryConfigResolver,
            EffectiveConfigResolver effectiveConfigResolver,
            DocumentCleaningService documentCleaningService) {
        this.repository = repository;
        this.storageService = storageService;
        this.parseService = parseService;
        this.textNormalizer = textNormalizer;
        this.indexCoordinator = indexCoordinator;
        this.libraryConfigResolver = libraryConfigResolver;
        this.effectiveConfigResolver = effectiveConfigResolver;
        this.documentCleaningService = documentCleaningService;
    }

    /**
     * 在事务提交后再触发异步解析，避免重复上传升版本后异步任务仍读到旧 version。
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

    @Async("knowbaseTaskExecutor")
    @KnowbaseTransactional
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

            EffectivePipelineConfig effective =
                    effectiveConfigResolver.forDocument(doc.getLibraryId(), doc.getDocId());
            String plainText = parseService.extractText(
                    fileBytes,
                    fileName,
                    doc.getMimeType(),
                    effectiveConfigResolver.parseOptions(effective));
            if (effective.isTextNormalizationEnabled()) {
                plainText = textNormalizer.normalize(plainText, effective.normalization());
            }
            plainText = documentCleaningService.apply(plainText, effective.cleaning());
            plainText = ChunkTextPreprocessor.prepare(plainText);

            EffectivePipelineConfig withContent = effectiveConfigResolver.forIngestWithContent(
                    doc.getLibraryId(),
                    doc.getMimeType(),
                    IngestProfileSupport.parse(doc.getIngestProfileJson()),
                    plainText);
            doc.setChunkProfileId(ChunkProfileFingerprint.compute(doc.getLibraryId(), withContent));
            String signalsJson = ContentSignalsSupport.toJson(withContent.contentSignals());
            if (signalsJson != null) {
                doc.setContentSignalsJson(signalsJson);
            }

            String parsedKey = doc.getTenantId()
                    + "/"
                    + doc.getLibraryId()
                    + "/"
                    + doc.getDocId()
                    + "/v"
                    + expectedVersion
                    + "/parsed.txt";
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
                DocumentReadyForIndexEvent readyEvent = DocumentReadyForIndexEvent.create(
                        doc.getLibraryId(),
                        doc.getDocId(),
                        doc.getTenantId(),
                        expectedVersion,
                        doc.getChecksumSha256(),
                        doc.getMimeType(),
                        url,
                        parsedKey);
                indexCoordinator.processReadyForIndex(readyEvent);
                log.info("Triggered indexing for {} v{}", doc.getDocId(), expectedVersion);
            }
        } catch (Exception e) {
            log.error("Parse pipeline failed for {}", docId, e);
            doc.setParseStatus(ParseStatus.FAILED);
            doc.setIndexStatus(IndexStatus.FAILED);
            repository.save(doc);
        }
    }

    /** 软删除：保留元数据与 MinIO 对象，同步清理向量索引。 */
    @KnowbaseTransactional
    public void deleteDocument(UUID docId) {
        DocMetadata doc = repository.findByDocIdAndDeletedFalse(docId)
                .orElseThrow(() -> new DocumentNotFoundException(docId));
        doc.setDeleted(true);
        repository.save(doc);
        indexCoordinator.processDeleted(
                DocumentDeletedEvent.create(doc.getDocId(), doc.getTenantId(), doc.getVersion()));
    }

    /**
     * 物理删除：移除 MinIO 下该文档全部版本对象，删除 ingest 元数据行，并通知向量服务清理。
     */
    @KnowbaseTransactional
    public void purgeDocument(UUID docId) {
        DocMetadata doc = repository.findById(docId)
                .orElseThrow(() -> new DocumentNotFoundException(docId));
        storageService.removeDocumentArtifacts(doc);
        if (!doc.isDeleted()) {
            indexCoordinator.processDeleted(
                    DocumentDeletedEvent.create(doc.getDocId(), doc.getTenantId(), doc.getVersion()));
        }
        repository.deleteByDocId(docId);
        log.info("Purged document {} metadata and storage artifacts", docId);
    }
}
