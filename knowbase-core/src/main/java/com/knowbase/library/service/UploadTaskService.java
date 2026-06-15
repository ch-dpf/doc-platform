package com.knowbase.library.service;

import com.knowbase.ingest.config.IngestProperties;
import com.knowbase.ingest.storage.ObjectStorageService;
import com.knowbase.library.domain.UploadTask;
import com.knowbase.library.domain.UploadTaskStatus;
import com.knowbase.library.dto.UploadTaskResponse;
import com.knowbase.library.mapper.UploadTaskMapper;
import org.springframework.stereotype.Service;
import com.knowbase.tx.KnowbaseTransactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.ByteArrayInputStream;
import java.time.Instant;
import java.util.UUID;

@Service
public class UploadTaskService {

    private final UploadTaskMapper taskMapper;
    private final ObjectStorageService storageService;
    private final UploadTaskWorker uploadTaskWorker;
    private final LibraryConfigResolver libraryConfigResolver;
    private final IngestProperties ingestProperties;

    public UploadTaskService(
            UploadTaskMapper taskMapper,
            ObjectStorageService storageService,
            UploadTaskWorker uploadTaskWorker,
            LibraryConfigResolver libraryConfigResolver,
            IngestProperties ingestProperties) {
        this.taskMapper = taskMapper;
        this.storageService = storageService;
        this.uploadTaskWorker = uploadTaskWorker;
        this.libraryConfigResolver = libraryConfigResolver;
        this.ingestProperties = ingestProperties;
    }

    public boolean shouldUploadAsync(long sizeBytes) {
        return sizeBytes > ingestProperties.getAsyncUploadThresholdBytes();
    }

    @KnowbaseTransactional
    public UploadTaskResponse submitAsync(
            UUID libraryId,
            String tenantId,
            MultipartFile file,
            boolean autoIndex,
            String documentMetadata,
            String ingestProfile)
            throws Exception {
        libraryConfigResolver.requireLibrary(libraryId);
        libraryConfigResolver.requireUploadAllowed(libraryId);
        byte[] bytes = file.getBytes();
        String fileName = file.getOriginalFilename() != null ? file.getOriginalFilename() : "unknown";

        UUID taskId = UUID.randomUUID();
        UploadTask task = new UploadTask();
        task.setTaskId(taskId);
        task.setLibraryId(libraryId);
        task.setTenantId(tenantId);
        task.setFileName(fileName);
        task.setStatus(UploadTaskStatus.UPLOADING);
        task.setProgress(10);
        Instant now = Instant.now();
        task.setCreatedAt(now);
        task.setUpdatedAt(now);
        taskMapper.insert(task);

        String stagingKey = tenantId + "/" + libraryId + "/uploads/" + taskId + "/raw/" + fileName;
        storageService.putObject(stagingKey, new ByteArrayInputStream(bytes), bytes.length, file.getContentType());
        task.setProgress(40);
        task.setStatus(UploadTaskStatus.PROCESSING);
        task.setUpdatedAt(Instant.now());
        taskMapper.updateById(task);

        uploadTaskWorker.process(
                taskId, libraryId, tenantId, bytes, fileName, autoIndex, documentMetadata, ingestProfile);
        return UploadTaskResponse.from(taskMapper.selectById(taskId));
    }

    public UploadTaskResponse submitAsync(
            UUID libraryId, String tenantId, MultipartFile file, boolean autoIndex, String documentMetadata)
            throws Exception {
        return submitAsync(libraryId, tenantId, file, autoIndex, documentMetadata, null);
    }

    public UploadTaskResponse submitAsync(
            UUID libraryId, String tenantId, MultipartFile file, boolean autoIndex) throws Exception {
        return submitAsync(libraryId, tenantId, file, autoIndex, null, null);
    }

    public UploadTaskResponse get(UUID taskId) {
        UploadTask task = taskMapper.selectById(taskId);
        if (task == null) {
            throw new IllegalArgumentException("上传任务不存在: " + taskId);
        }
        return UploadTaskResponse.from(task);
    }

}
