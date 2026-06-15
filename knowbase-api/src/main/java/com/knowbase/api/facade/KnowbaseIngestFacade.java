package com.knowbase.api.facade;

import com.knowbase.api.result.DocumentIngestResult;

import java.io.InputStream;
import java.util.UUID;

public interface KnowbaseIngestFacade {

    DocumentIngestResult upload(
            UUID libraryId,
            String tenantId,
            String fileName,
            String contentType,
            long sizeBytes,
            InputStream content,
            boolean autoIndex);

    DocumentIngestResult getDocument(UUID docId);
}
