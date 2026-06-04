package com.docplatform.ingest.service;

import com.docplatform.ingest.config.IngestProperties;
import com.docplatform.ingest.domain.DocMetadata;
import com.docplatform.ingest.domain.IndexStatus;
import com.docplatform.ingest.domain.ParseStatus;
import com.docplatform.ingest.domain.SourceType;
import com.docplatform.ingest.dto.DocumentResponse;
import com.docplatform.ingest.storage.ObjectStorageService;
import com.docplatform.ingest.support.DocMetadataStore;
import com.docplatform.ingest.support.MimeTypeAllowlist;
import com.docplatform.library.service.LibraryConfigResolver;
import com.docplatform.library.service.VectorLibraryService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.io.ByteArrayInputStream;
import java.security.MessageDigest;
import java.util.HexFormat;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
public class DocumentIngestor {

    private final DocMetadataStore repository;
    private final ObjectStorageService storageService;
    private final DocumentParseService parseService;
    private final DocumentPipelineService pipelineService;
    private final LibraryConfigResolver libraryConfigResolver;
    private final VectorLibraryService vectorLibraryService;
    private final IngestProperties ingestProperties;

    public DocumentIngestor(
            DocMetadataStore repository,
            ObjectStorageService storageService,
            DocumentParseService parseService,
            DocumentPipelineService pipelineService,
            LibraryConfigResolver libraryConfigResolver,
            VectorLibraryService vectorLibraryService,
            IngestProperties ingestProperties) {
        this.repository = repository;
        this.storageService = storageService;
        this.parseService = parseService;
        this.pipelineService = pipelineService;
        this.libraryConfigResolver = libraryConfigResolver;
        this.vectorLibraryService = vectorLibraryService;
        this.ingestProperties = ingestProperties;
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public DocumentResponse ingestOne(UUID libraryId, String tenantId, byte[] bytes, String fileName, boolean autoIndex) {
        libraryConfigResolver.requireLibrary(libraryId);
        validateFileSize(fileName, bytes.length);
        String checksum = sha256(bytes);
        String mimeType = parseService.detectMimeType(bytes, fileName);
        validateMimeType(mimeType, fileName, libraryId);

        Optional<DocMetadata> existing =
                repository.findByLibraryTenantChecksum(libraryId, tenantId, checksum);
        if (existing.isPresent()) {
            DocMetadata doc = existing.get();
            doc.setVersion(doc.getVersion() + 1);
            doc.setParseStatus(ParseStatus.PENDING);
            doc.setIndexRequested(autoIndex);
            doc.setIndexStatus(autoIndex ? IndexStatus.PENDING : null);
            doc.setStorageKey(buildStorageKey(doc, fileName));
            repository.save(doc);
            storeAndProcess(doc, bytes, fileName, mimeType);
            return DocumentResponse.from(doc);
        }

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
        doc.setIndexRequested(autoIndex);
        doc.setIndexStatus(autoIndex ? IndexStatus.PENDING : null);
        doc.setDeleted(false);
        doc.setStorageKey(buildStorageKey(doc, fileName));

        repository.save(doc);
        vectorLibraryService.incrementDocumentCount(libraryId, 1);
        storeAndProcess(doc, bytes, fileName, mimeType);
        return DocumentResponse.from(doc);
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
