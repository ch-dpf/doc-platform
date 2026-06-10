package com.knowbase.ingest.service;

import com.knowbase.ingest.config.IngestProperties;
import com.knowbase.ingest.dto.BatchUploadItemResult;
import com.knowbase.ingest.dto.BatchUploadResponse;
import com.knowbase.ingest.dto.DocumentResponse;
import com.knowbase.ingest.dto.UploadConstraintsResponse;
import com.knowbase.ingest.storage.ObjectStorageService;
import com.knowbase.library.dto.UploadTaskResponse;
import com.knowbase.library.service.LibraryConfigResolver;
import com.knowbase.library.service.UploadTaskService;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Service
public class UploadService {

    private final DocumentIngestor documentIngestor;
    private final UploadTaskService uploadTaskService;
    private final ObjectStorageService storageService;
    private final IngestProperties ingestProperties;
    private final LibraryConfigResolver libraryConfigResolver;
    private final DocumentOcrService ocrService;

    public UploadService(
            DocumentIngestor documentIngestor,
            UploadTaskService uploadTaskService,
            ObjectStorageService storageService,
            IngestProperties ingestProperties,
            LibraryConfigResolver libraryConfigResolver,
            DocumentOcrService ocrService) {
        this.documentIngestor = documentIngestor;
        this.uploadTaskService = uploadTaskService;
        this.storageService = storageService;
        this.ingestProperties = ingestProperties;
        this.libraryConfigResolver = libraryConfigResolver;
        this.ocrService = ocrService;
    }

    public UploadConstraintsResponse uploadConstraints(UUID libraryId) {
        libraryConfigResolver.requireLibrary(libraryId);
        var versionPolicy = libraryConfigResolver.versionPolicyFor(libraryId);
        String updateStrategy = versionPolicy.isEnabled()
                ? versionPolicy.getUpdateStrategy()
                : "overwrite";
        return new UploadConstraintsResponse(
                libraryConfigResolver.allowedMimeTypes(libraryId),
                ingestProperties.getMaxFileSizeBytes(),
                ingestProperties.getMaxFileSize().toString(),
                ingestProperties.getMaxBatchFiles(),
                storageService.storageType(),
                libraryConfigResolver.ingestSourceMode(libraryId),
                libraryConfigResolver.isUploadAllowed(libraryId),
                libraryConfigResolver.isCollectAllowed(libraryId),
                libraryConfigResolver.parsingFor(libraryId).isOcrEnabled(),
                ocrService.isAvailable(),
                libraryConfigResolver.requiresManualReview(libraryId),
                updateStrategy);
    }

    public DocumentResponse upload(
            UUID libraryId,
            String tenantId,
            MultipartFile file,
            boolean autoIndex,
            String documentMetadata)
            throws IOException {
        libraryConfigResolver.requireLibrary(libraryId);
        libraryConfigResolver.requireUploadAllowed(libraryId);
        if (uploadTaskService.shouldUploadAsync(file.getSize())) {
            throw new ResponseStatusException(
                    HttpStatus.ACCEPTED,
                    "文件较大，请使用异步上传接口 /api/v1/libraries/uploads");
        }
        return documentIngestor.ingestOne(
                libraryId,
                tenantId,
                file.getBytes(),
                resolveFileName(file),
                autoIndex,
                documentMetadata);
    }

    public DocumentResponse upload(UUID libraryId, String tenantId, MultipartFile file, boolean autoIndex)
            throws IOException {
        return upload(libraryId, tenantId, file, autoIndex, null);
    }

    public UploadTaskResponse uploadAsync(
            UUID libraryId, String tenantId, MultipartFile file, boolean autoIndex, String documentMetadata)
            throws Exception {
        libraryConfigResolver.requireUploadAllowed(libraryId);
        return uploadTaskService.submitAsync(libraryId, tenantId, file, autoIndex, documentMetadata);
    }

    public UploadTaskResponse uploadAsync(UUID libraryId, String tenantId, MultipartFile file, boolean autoIndex)
            throws Exception {
        return uploadAsync(libraryId, tenantId, file, autoIndex, null);
    }

    public BatchUploadResponse uploadBatch(
            UUID libraryId, String tenantId, MultipartFile[] files, boolean autoIndex, String documentMetadata) {
        libraryConfigResolver.requireUploadAllowed(libraryId);
        if (files == null || files.length == 0) {
            throw new InvalidDocumentException(
                    InvalidDocumentException.CODE_BATCH_LIMIT,
                    "请至少选择一个文件",
                    null,
                    null,
                    libraryConfigResolver.allowedMimeTypes(libraryId));
        }
        if (files.length > ingestProperties.getMaxBatchFiles()) {
            throw new InvalidDocumentException(
                    InvalidDocumentException.CODE_BATCH_LIMIT,
                    "单次最多上传 " + ingestProperties.getMaxBatchFiles() + " 个文件",
                    null,
                    null,
                    libraryConfigResolver.allowedMimeTypes(libraryId));
        }

        List<BatchUploadItemResult> items = new ArrayList<>();
        int succeeded = 0;
        for (MultipartFile file : files) {
            String fileName = resolveFileName(file);
            try {
                DocumentResponse doc;
                if (uploadTaskService.shouldUploadAsync(file.getSize())) {
                    UploadTaskResponse task =
                            uploadTaskService.submitAsync(libraryId, tenantId, file, autoIndex, documentMetadata);
                    items.add(new BatchUploadItemResult(
                            fileName,
                            true,
                            null,
                            "ASYNC_TASK",
                            "已提交异步任务: " + task.taskId(),
                            task.taskId()));
                } else {
                    doc = documentIngestor.ingestOne(
                            libraryId, tenantId, file.getBytes(), fileName, autoIndex, documentMetadata);
                    items.add(new BatchUploadItemResult(fileName, true, doc, null, null));
                }
                succeeded++;
            } catch (InvalidDocumentException e) {
                items.add(new BatchUploadItemResult(
                        fileName, false, null, e.getErrorCode(), e.getMessage()));
            } catch (Exception e) {
                String message = e.getMessage() != null ? e.getMessage() : e.getClass().getSimpleName();
                items.add(new BatchUploadItemResult(fileName, false, null, "UPLOAD_FAILED", message));
            }
        }
        return new BatchUploadResponse(files.length, succeeded, files.length - succeeded, items);
    }

    public BatchUploadResponse uploadBatch(
            UUID libraryId, String tenantId, MultipartFile[] files, boolean autoIndex) {
        return uploadBatch(libraryId, tenantId, files, autoIndex, null);
    }

    private static String resolveFileName(MultipartFile file) {
        return file.getOriginalFilename() != null ? file.getOriginalFilename() : "unknown";
    }
}
