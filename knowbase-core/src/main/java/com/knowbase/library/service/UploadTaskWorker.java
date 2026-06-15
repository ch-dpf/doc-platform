package com.knowbase.library.service;

import com.knowbase.ingest.dto.DocumentResponse;
import com.knowbase.ingest.service.DocumentIngestor;
import com.knowbase.library.domain.UploadTask;
import com.knowbase.library.domain.UploadTaskStatus;
import com.knowbase.library.mapper.UploadTaskMapper;
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

    @Async("knowbaseTaskExecutor")
    public void process(
            UUID taskId,
            UUID libraryId,
            String tenantId,
            byte[] bytes,
            String fileName,
            boolean autoIndex,
            String documentMetadata,
            String ingestProfile) {
        UploadTask task = taskMapper.selectById(taskId);
        if (task == null) {
            return;
        }
        try {
            task.setProgress(60);
            task.setUpdatedAt(Instant.now());
            taskMapper.updateById(task);

            DocumentResponse doc = documentIngestor.ingestOne(
                    libraryId, tenantId, bytes, fileName, autoIndex, documentMetadata, ingestProfile);
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
