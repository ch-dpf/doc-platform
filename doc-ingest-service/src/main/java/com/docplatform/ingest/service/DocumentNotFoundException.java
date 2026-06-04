package com.docplatform.ingest.service;

import java.util.UUID;

public class DocumentNotFoundException extends RuntimeException {

    public DocumentNotFoundException(UUID docId) {
        super("Document not found: " + docId);
    }
}
