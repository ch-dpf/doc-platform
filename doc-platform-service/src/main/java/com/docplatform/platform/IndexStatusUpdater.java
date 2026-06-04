package com.docplatform.platform;

import com.docplatform.ingest.domain.DocMetadata;
import com.docplatform.ingest.domain.IndexStatus;
import com.docplatform.ingest.support.DocMetadataStore;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
public class IndexStatusUpdater {

    private static final Logger log = LoggerFactory.getLogger(IndexStatusUpdater.class);

    private final DocMetadataStore repository;

    public IndexStatusUpdater(DocMetadataStore repository) {
        this.repository = repository;
    }

    @Transactional
    public void markIndexed(UUID docId, int version) {
        repository.findById(docId).ifPresent(doc -> {
            if (doc.getVersion() == version) {
                doc.setIndexStatus(IndexStatus.INDEXED);
                repository.save(doc);
                log.info("Updated index status for doc {} v{}", docId, version);
            } else {
                log.warn(
                        "Ignored index complete for doc {} v{} (current metadata version is v{})",
                        docId,
                        version,
                        doc.getVersion());
            }
        });
    }

    @Transactional
    public void markFailed(UUID docId, int version) {
        repository.findById(docId).ifPresent(doc -> {
            if (doc.getVersion() == version) {
                doc.setIndexStatus(IndexStatus.FAILED);
                repository.save(doc);
                log.warn("Marked index status FAILED for doc {} v{}", docId, version);
            }
        });
    }
}
