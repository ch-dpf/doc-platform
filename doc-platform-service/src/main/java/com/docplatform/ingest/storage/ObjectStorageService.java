package com.docplatform.ingest.storage;

import com.docplatform.ingest.domain.DocMetadata;
import org.springframework.stereotype.Service;

import java.io.InputStream;

/** 面向业务层的存储门面，委托给可配置的 {@link DocumentObjectStorage} 实现。 */
@Service
public class ObjectStorageService {

    private final DocumentObjectStorage delegate;

    public ObjectStorageService(DocumentObjectStorage delegate) {
        this.delegate = delegate;
    }

    public String storageType() {
        return delegate.type();
    }

    public void putObject(String objectKey, InputStream stream, long size, String contentType) {
        delegate.putObject(objectKey, stream, size, contentType);
    }

    public String readAsString(String objectKey) {
        return delegate.readAsString(objectKey);
    }

    public void removeDocumentArtifacts(DocMetadata doc) {
        delegate.removeDocumentArtifacts(doc);
    }

    public void removeObjectIfPresent(String objectKey) {
        delegate.removeObjectIfPresent(objectKey);
    }

    public int removeByPrefix(String prefix) {
        return delegate.removeByPrefix(prefix);
    }

    public String presignedGetUrl(String objectKey) {
        return delegate.resolveAccessUrl(objectKey);
    }
}
