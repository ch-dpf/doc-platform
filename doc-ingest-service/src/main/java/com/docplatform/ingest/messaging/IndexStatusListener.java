package com.docplatform.ingest.messaging;

import com.docplatform.contract.ContractJson;
import com.docplatform.contract.DocumentEventType;
import com.docplatform.contract.DocumentIndexedEvent;
import com.docplatform.contract.DocumentLifecycleEvent;
import com.docplatform.contract.KafkaTopics;
import com.docplatform.ingest.domain.DocMetadata;
import com.docplatform.ingest.domain.IndexStatus;
import com.docplatform.ingest.support.DocMetadataStore;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
public class IndexStatusListener {

    private static final Logger log = LoggerFactory.getLogger(IndexStatusListener.class);

    private final DocMetadataStore repository;

    public IndexStatusListener(DocMetadataStore repository) {
        this.repository = repository;
    }

    @KafkaListener(topics = KafkaTopics.DOC_LIFECYCLE, groupId = "doc-ingest-index-status")
    @Transactional
    public void onLifecycleEvent(String payload) {
        DocumentLifecycleEvent event = ContractJson.read(payload);
        if (event.eventType() != DocumentEventType.DOCUMENT_INDEXED) {
            return;
        }
        DocumentIndexedEvent indexed = (DocumentIndexedEvent) event;
        repository.findById(indexed.docId()).ifPresent(doc -> {
            if (doc.getVersion() == indexed.version()) {
                doc.setIndexStatus(IndexStatus.INDEXED);
                repository.save(doc);
                log.info("Updated index status for doc {} v{}", indexed.docId(), indexed.version());
            } else {
                log.warn(
                        "Ignored DOCUMENT_INDEXED for doc {} v{} (current metadata version is v{})",
                        indexed.docId(),
                        indexed.version(),
                        doc.getVersion());
            }
        });
    }
}
