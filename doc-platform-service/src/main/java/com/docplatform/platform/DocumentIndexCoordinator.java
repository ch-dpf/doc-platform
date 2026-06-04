package com.docplatform.platform;

import com.docplatform.event.DocumentDeletedEvent;
import com.docplatform.event.DocumentEventType;
import com.docplatform.event.DocumentReadyForIndexEvent;
import com.docplatform.vector.service.IdempotencyService;
import com.docplatform.vector.service.IndexingService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

/**
 * 单体应用内衔接采集与向量索引，替代原 Kafka 事件链路。
 */
@Service
public class DocumentIndexCoordinator {

    private static final Logger log = LoggerFactory.getLogger(DocumentIndexCoordinator.class);

    private final IndexingService indexingService;
    private final IdempotencyService idempotencyService;
    private final IndexStatusUpdater indexStatusUpdater;

    public DocumentIndexCoordinator(
            IndexingService indexingService,
            IdempotencyService idempotencyService,
            IndexStatusUpdater indexStatusUpdater) {
        this.indexingService = indexingService;
        this.idempotencyService = idempotencyService;
        this.indexStatusUpdater = indexStatusUpdater;
    }

    public void scheduleIndexAfterCommit(DocumentReadyForIndexEvent event) {
        Runnable task = () -> processReadyForIndex(event);
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

    @Async
    public void processReadyForIndex(DocumentReadyForIndexEvent event) {
        if (idempotencyService.isProcessed(
                event.docId(), event.version(), DocumentEventType.DOCUMENT_READY_FOR_INDEX)) {
            log.debug("Skipping duplicate index for doc {} v{}", event.docId(), event.version());
            return;
        }
        try {
            indexingService.index(event);
            idempotencyService.markProcessed(
                    event.docId(), event.version(), DocumentEventType.DOCUMENT_READY_FOR_INDEX);
        } catch (Exception e) {
            log.error(
                    "Index failed for doc {} v{}: {}",
                    event.docId(),
                    event.version(),
                    e.getMessage(),
                    e);
            indexStatusUpdater.markFailed(event.docId(), event.version());
        }
    }

    public void processDeleted(DocumentDeletedEvent event) {
        if (idempotencyService.isProcessed(
                event.docId(), event.version(), DocumentEventType.DOCUMENT_DELETED)) {
            log.debug("Skipping duplicate delete for doc {} v{}", event.docId(), event.version());
            return;
        }
        indexingService.delete(event);
        idempotencyService.markProcessed(
                event.docId(), event.version(), DocumentEventType.DOCUMENT_DELETED);
    }
}
