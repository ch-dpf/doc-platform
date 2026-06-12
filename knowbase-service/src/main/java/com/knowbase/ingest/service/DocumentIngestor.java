package com.knowbase.ingest.service;

import com.knowbase.ingest.config.IngestProperties;
import com.knowbase.ingest.domain.DocMetadata;
import com.knowbase.ingest.domain.IndexStatus;
import com.knowbase.ingest.domain.ParseStatus;
import com.knowbase.ingest.domain.SourceType;
import com.knowbase.ingest.dto.DocumentResponse;
import com.knowbase.ingest.storage.ObjectStorageService;
import com.knowbase.ingest.support.DocMetadataStore;
import com.knowbase.ingest.support.DocumentCustomMetadataSupport;
import com.knowbase.pipeline.config.ChunkProfileService;
import com.knowbase.pipeline.config.IngestProfileSupport;
import com.knowbase.ingest.support.MimeTypeAllowlist;
import com.knowbase.library.config.VersionPolicySettings;
import com.knowbase.library.service.LibraryCapacityValidator;
import com.knowbase.library.service.LibraryConfigResolver;
import com.knowbase.library.service.VectorLibraryService;
import com.knowbase.library.service.VersionUpdateStrategy;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.io.ByteArrayInputStream;
import java.security.MessageDigest;
import java.util.HexFormat;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

@Service
public class DocumentIngestor {

    private final DocMetadataStore repository;
    private final ObjectStorageService storageService;
    private final DocumentParseService parseService;
    private final DocumentPipelineService pipelineService;
    private final LibraryConfigResolver libraryConfigResolver;
    private final LibraryCapacityValidator capacityValidator;
    private final VectorLibraryService vectorLibraryService;
    private final IngestProperties ingestProperties;
    private final ChunkProfileService chunkProfileService;

    public DocumentIngestor(
            DocMetadataStore repository,
            ObjectStorageService storageService,
            DocumentParseService parseService,
            DocumentPipelineService pipelineService,
            LibraryConfigResolver libraryConfigResolver,
            LibraryCapacityValidator capacityValidator,
            VectorLibraryService vectorLibraryService,
            IngestProperties ingestProperties,
            ChunkProfileService chunkProfileService) {
        this.repository = repository;
        this.storageService = storageService;
        this.parseService = parseService;
        this.pipelineService = pipelineService;
        this.libraryConfigResolver = libraryConfigResolver;
        this.capacityValidator = capacityValidator;
        this.vectorLibraryService = vectorLibraryService;
        this.ingestProperties = ingestProperties;
        this.chunkProfileService = chunkProfileService;
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public DocumentResponse ingestOne(UUID libraryId, String tenantId, byte[] bytes, String fileName, boolean autoIndex) {
        return ingestOne(libraryId, tenantId, bytes, fileName, autoIndex, null, null);
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public DocumentResponse ingestOne(
            UUID libraryId,
            String tenantId,
            byte[] bytes,
            String fileName,
            boolean autoIndex,
            String customMetadataJson) {
        return ingestOne(libraryId, tenantId, bytes, fileName, autoIndex, customMetadataJson, null);
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public DocumentResponse ingestOne(
            UUID libraryId,
            String tenantId,
            byte[] bytes,
            String fileName,
            boolean autoIndex,
            String customMetadataJson,
            String ingestProfileJson) {
        libraryConfigResolver.requireLibrary(libraryId);
        validateFileSize(fileName, bytes.length);
        String checksum = sha256(bytes);
        String mimeType = parseService.detectMimeType(bytes, fileName);
        validateMimeType(mimeType, fileName, libraryId);
        String normalizedProfile = IngestProfileSupport.prepareForUpload(ingestProfileJson);
        chunkProfileService.validateNewProfileAllowed(libraryId, mimeType, normalizedProfile);

        Optional<DocMetadata> existing =
                repository.findByLibraryTenantChecksum(libraryId, tenantId, checksum);
        if (existing.isPresent()) {
            return handleDuplicateUpload(
                    existing.get(), libraryId, bytes, fileName, mimeType, autoIndex, customMetadataJson, ingestProfileJson);
        }

        capacityValidator.requireNewDocument(libraryId, bytes.length, fileName);

        UUID docId = UUID.randomUUID();
        DocMetadata doc = new DocMetadata();
        doc.setDocId(docId);
        doc.setLibraryId(libraryId);
        doc.setTenantId(tenantId);
        doc.setSourceType(SourceType.UPLOAD);
        doc.setFileName(fileName);
        doc.setMimeType(mimeType);
        doc.setSizeBytes(bytes.length);
        doc.setChecksumSha256(checksum);
        doc.setParseStatus(ParseStatus.PENDING);
        doc.setVersion(1);
        boolean indexRequested = resolveIndexRequested(libraryId, autoIndex);
        doc.setIndexRequested(indexRequested);
        doc.setIndexStatus(indexRequested ? IndexStatus.PENDING : null);
        doc.setDeleted(false);
        doc.setCustomMetadataJson(DocumentCustomMetadataSupport.normalizeJson(customMetadataJson));
        doc.setIngestProfileJson(normalizedProfile);
        doc.setStorageKey(buildStorageKey(doc, fileName));

        repository.save(doc);
        vectorLibraryService.incrementDocumentCount(libraryId, 1);
        storeAndProcess(doc, bytes, fileName, mimeType);
        return DocumentResponse.from(doc);
    }

    private DocumentResponse handleDuplicateUpload(
            DocMetadata doc,
            UUID libraryId,
            byte[] bytes,
            String fileName,
            String mimeType,
            boolean autoIndex,
            String customMetadataJson,
            String ingestProfileJson) {
        String normalizedNew = IngestProfileSupport.prepareForUpload(ingestProfileJson);
        String normalizedExisting = IngestProfileSupport.prepareForUpload(doc.getIngestProfileJson());
        if (normalizedNew != null && !Objects.equals(normalizedNew, normalizedExisting)) {
            throw InvalidDocumentException.duplicateDifferentChunkProfile(
                    fileName, doc.getChunkProfileId());
        }

        VersionPolicySettings policy = libraryConfigResolver.versionPolicyFor(libraryId);
        VersionUpdateStrategy strategy = VersionUpdateStrategy.from(policy);

        if (strategy == VersionUpdateStrategy.INCREMENTAL) {
            return DocumentResponse.from(doc);
        }

        if (strategy == VersionUpdateStrategy.OVERWRITE) {
            capacityValidator.requireReplaceDocument(libraryId, doc.getSizeBytes(), bytes.length, fileName);
            prepareForReprocess(doc, fileName, mimeType, bytes.length, autoIndex, false, customMetadataJson, ingestProfileJson);
            repository.save(doc);
            storeAndProcess(doc, bytes, fileName, mimeType);
            return DocumentResponse.from(doc);
        }

        capacityValidator.requireAdditionalVersionStorage(libraryId, bytes.length, fileName);
        prepareForReprocess(doc, fileName, mimeType, bytes.length, autoIndex, true, customMetadataJson, ingestProfileJson);
        repository.save(doc);
        storeAndProcess(doc, bytes, fileName, mimeType);
        return DocumentResponse.from(doc);
    }

    private void prepareForReprocess(
            DocMetadata doc,
            String fileName,
            String mimeType,
            long sizeBytes,
            boolean autoIndex,
            boolean incrementVersion,
            String customMetadataJson,
            String ingestProfileJson) {
        if (incrementVersion) {
            doc.setVersion(doc.getVersion() + 1);
        }
        doc.setFileName(fileName);
        doc.setMimeType(mimeType);
        doc.setSizeBytes(sizeBytes);
        if (customMetadataJson != null) {
            doc.setCustomMetadataJson(DocumentCustomMetadataSupport.normalizeJson(customMetadataJson));
        }
        doc.setIngestProfileJson(IngestProfileSupport.prepareForUpload(ingestProfileJson));
        doc.setParseStatus(ParseStatus.PENDING);
        boolean indexRequested = resolveIndexRequested(doc.getLibraryId(), autoIndex);
        doc.setIndexRequested(indexRequested);
        doc.setIndexStatus(indexRequested ? IndexStatus.PENDING : null);
        doc.setStorageKey(buildStorageKey(doc, fileName));
    }

    private boolean resolveIndexRequested(UUID libraryId, boolean autoIndex) {
        if (libraryConfigResolver.requiresManualReview(libraryId)) {
            return false;
        }
        return autoIndex;
    }

    private void storeAndProcess(DocMetadata doc, byte[] bytes, String fileName, String mimeType) {
        String storageKey = buildStorageKey(doc, fileName);
        doc.setStorageKey(storageKey);
        storageService.putObject(storageKey, new ByteArrayInputStream(bytes), bytes.length, mimeType);
        repository.save(doc);
        pipelineService.scheduleProcessAfterCommit(doc.getDocId(), doc.getVersion(), bytes, fileName);
    }

    private static String buildStorageKey(DocMetadata doc, String fileName) {
        return doc.getTenantId()
                + "/"
                + doc.getLibraryId()
                + "/"
                + doc.getDocId()
                + "/v"
                + doc.getVersion()
                + "/raw/"
                + fileName;
    }

    private void validateFileSize(String fileName, long sizeBytes) {
        long max = ingestProperties.getMaxFileSizeBytes();
        if (sizeBytes > max) {
            throw InvalidDocumentException.fileTooLarge(fileName, sizeBytes, max);
        }
    }

    private void validateMimeType(String mimeType, String fileName, UUID libraryId) {
        List<String> allowed = libraryConfigResolver.allowedMimeTypes(libraryId);
        if (!MimeTypeAllowlist.isAllowed(mimeType, fileName, allowed)) {
            throw InvalidDocumentException.mimeNotAllowed(fileName, mimeType, allowed);
        }
    }

    private static String sha256(byte[] data) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            return HexFormat.of().formatHex(digest.digest(data));
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }
}
