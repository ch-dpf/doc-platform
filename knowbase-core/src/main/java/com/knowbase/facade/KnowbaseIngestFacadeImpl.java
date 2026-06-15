package com.knowbase.facade;

import com.knowbase.api.facade.KnowbaseIngestFacade;
import com.knowbase.api.result.DocumentIngestResult;
import com.knowbase.ingest.dto.DocumentResponse;
import com.knowbase.ingest.service.DocumentIngestor;
import com.knowbase.ingest.service.DocumentQueryService;
import com.knowbase.library.service.LibraryConfigResolver;

import java.io.IOException;
import java.io.InputStream;
import java.util.UUID;

public class KnowbaseIngestFacadeImpl implements KnowbaseIngestFacade {

    private final DocumentIngestor documentIngestor;
    private final DocumentQueryService documentQueryService;
    private final LibraryConfigResolver libraryConfigResolver;
    private final KnowbaseTenantSupport tenantSupport;

    public KnowbaseIngestFacadeImpl(
            DocumentIngestor documentIngestor,
            DocumentQueryService documentQueryService,
            LibraryConfigResolver libraryConfigResolver,
            KnowbaseTenantSupport tenantSupport) {
        this.documentIngestor = documentIngestor;
        this.documentQueryService = documentQueryService;
        this.libraryConfigResolver = libraryConfigResolver;
        this.tenantSupport = tenantSupport;
    }

    @Override
    public DocumentIngestResult upload(
            UUID libraryId,
            String tenantId,
            String fileName,
            String contentType,
            long sizeBytes,
            InputStream content,
            boolean autoIndex) {
        libraryConfigResolver.requireLibrary(libraryId);
        libraryConfigResolver.requireUploadAllowed(libraryId);
        String effectiveTenant = tenantSupport.resolve(tenantId);
        try {
            byte[] bytes = content.readAllBytes();
            DocumentResponse doc = documentIngestor.ingestOne(
                    libraryId, effectiveTenant, bytes, fileName, autoIndex, null, null);
            return toResult(doc);
        } catch (IOException e) {
            throw new IllegalStateException("Failed to read upload content", e);
        }
    }

    @Override
    public DocumentIngestResult getDocument(UUID docId) {
        return toResult(documentQueryService.get(docId));
    }

    private static DocumentIngestResult toResult(DocumentResponse doc) {
        return new DocumentIngestResult(
                doc.docId(),
                doc.libraryId(),
                doc.tenantId(),
                doc.fileName(),
                doc.parseStatus(),
                doc.indexStatus(),
                doc.version());
    }
}
