package com.docplatform.library.service;

import com.docplatform.ingest.dto.DocumentResponse;
import com.docplatform.ingest.service.DocumentIngestor;
import com.docplatform.library.domain.UploadTask;
import com.docplatform.library.domain.UploadTaskStatus;
import com.docplatform.library.mapper.UploadTaskMapper;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.UUID;

@Component
public class UploadTaskWorker {

    private final UploadTaskMapper taskMapper;
    private final DocumentIngestor documentIngestor;

    public UploadTaskWorker(UploadTaskMapper taskMapper, DocumentIngestor documentIngestor) {
        this.taskMapper = taskMapper;
        this.documentIngestor = documentIngestor;
    }

    @Async
    public void process(
            UUID taskId, UUID libraryId, String tenantId, byte[] bytes, String fileName, boolean autoIndex) {
        UploadTask task = taskMapper.selectById(taskId);
        if (task == null) {
            return;
        }
        try {
            task.setProgress(60);
            task.setUpdatedAt(Instant.now());
            taskMapper.updateById(task);

            DocumentResponse doc = documentIngestor.ingestOne(libraryId, tenantId, bytes, fileName, autoIndex);
            task.setDocId(doc.docId());
            task.setStatus(UploadTaskStatus.COMPLETED);
            task.setProgress(100);
            task.setUpdatedAt(Instant.now());
            taskMapper.updateById(task);
        } catch (Exception e) {
            task.setStatus(UploadTaskStatus.FAILED);
            task.setErrorMessage(e.getMessage());
            task.setProgress(100);
            task.setUpdatedAt(Instant.now());
            taskMapper.updateById(task);
        }
    }
}
