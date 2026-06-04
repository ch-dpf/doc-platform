package com.docplatform.vector.messaging;

import com.docplatform.contract.ContractJson;
import com.docplatform.contract.DocumentDeletedEvent;
import com.docplatform.contract.DocumentEventType;
import com.docplatform.contract.DocumentLifecycleEvent;
import com.docplatform.contract.DocumentReadyForIndexEvent;
import com.docplatform.contract.KafkaTopics;
import com.docplatform.vector.service.IdempotencyService;
import com.docplatform.vector.service.IndexingService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.stereotype.Component;

@Component
public class DocumentLifecycleListener {

    private static final Logger log = LoggerFactory.getLogger(DocumentLifecycleListener.class);

    private final IdempotencyService idempotencyService;
    private final IndexingService indexingService;

    public DocumentLifecycleListener(IdempotencyService idempotencyService, IndexingService indexingService) {
        this.idempotencyService = idempotencyService;
        this.indexingService = indexingService;
    }

    @KafkaListener(topics = KafkaTopics.DOC_LIFECYCLE, groupId = "vector-index-service")
    public void onEvent(String payload, Acknowledgment ack) {
        DocumentLifecycleEvent event = ContractJson.read(payload);
        if (event.eventType() == DocumentEventType.DOCUMENT_INDEXED) {
            ack.acknowledge();
            return;
        }
        if (idempotencyService.isProcessed(event.docId(), event.version(), event.eventType())) {
            log.debug("Skipping duplicate event {}", event.eventType());
            ack.acknowledge();
            return;
        }
        try {
            switch (event.eventType()) {
                case DOCUMENT_READY_FOR_INDEX -> indexingService.index((DocumentReadyForIndexEvent) event);
                case DOCUMENT_DELETED -> indexingService.delete((DocumentDeletedEvent) event);
                default -> log.warn("Unhandled event type: {}", event.eventType());
            }
            idempotencyService.markProcessed(event.docId(), event.version(), event.eventType());
            ack.acknowledge();
        } catch (Exception e) {
            log.error(
                    "Failed to process {} for doc {} v{}: {}",
                    event.eventType(),
                    event.docId(),
                    event.version(),
                    e.getMessage(),
                    e);
            throw e;
        }
    }
}
