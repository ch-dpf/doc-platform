package com.knowbase.application.service;

import com.knowbase.api.result.BatchObjectUploadResult;
import com.knowbase.api.result.ObjectUploadResult;
import com.knowbase.api.result.UploadFailureResult;
import com.knowbase.storage.ObjectStorage;
import com.knowbase.storage.StoredObject;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public final class DefaultObjectUploadService {

    public static final int DEFAULT_MAX_FILES_PER_BATCH = 50;
    public static final long DEFAULT_MAX_FILE_SIZE_BYTES = 100L * 1024 * 1024;

    private final ObjectStorage objectStorage;
    private final String defaultBucket;
    private final int maxFilesPerBatch;
    private final long maxFileSizeBytes;

    public DefaultObjectUploadService(ObjectStorage objectStorage, String defaultBucket) {
        this(objectStorage, defaultBucket, DEFAULT_MAX_FILES_PER_BATCH, DEFAULT_MAX_FILE_SIZE_BYTES);
    }

    public DefaultObjectUploadService(
            ObjectStorage objectStorage,
            String defaultBucket,
            int maxFilesPerBatch,
            long maxFileSizeBytes
    ) {
        this.objectStorage = objectStorage;
        this.defaultBucket = defaultBucket;
        this.maxFilesPerBatch = Math.max(1, maxFilesPerBatch);
        this.maxFileSizeBytes = Math.max(1L, maxFileSizeBytes);
    }

    public ObjectUploadResult upload(String bucket, String filename, InputStream inputStream, String contentType) {
        return upload(bucket, filename, inputStream, contentType, -1L);
    }

    public ObjectUploadResult upload(
            String bucket,
            String filename,
            InputStream inputStream,
            String contentType,
            long sizeBytes
    ) {
        validateUpload(filename, sizeBytes);
        String targetBucket = bucket == null || bucket.isBlank() ? defaultBucket : bucket;
        String objectKey = UUID.randomUUID() + "/" + sanitize(filename);
        StoredObject stored = objectStorage.put(targetBucket, objectKey, inputStream, contentType);
        return new ObjectUploadResult(
                stored.bucket(),
                stored.objectKey(),
                stored.uri(),
                stored.contentType(),
                stored.size()
        );
    }

    public BatchObjectUploadResult uploadBatch(String bucket, List<UploadCandidate> files) {
        if (files == null || files.isEmpty()) {
            throw new IllegalArgumentException("请至少选择一个文件");
        }
        if (files.size() > maxFilesPerBatch) {
            throw new IllegalArgumentException("单次最多上传 " + maxFilesPerBatch + " 个文件");
        }
        List<ObjectUploadResult> uploaded = new ArrayList<>();
        List<UploadFailureResult> failures = new ArrayList<>();
        for (UploadCandidate file : files) {
            String filename = file.filename() == null || file.filename().isBlank() ? "upload.bin" : file.filename();
            try {
                uploaded.add(upload(bucket, filename, file.inputStream(), file.contentType(), file.sizeBytes()));
            } catch (Exception exception) {
                failures.add(new UploadFailureResult(filename, failureMessage(exception)));
            }
        }
        return new BatchObjectUploadResult(List.copyOf(uploaded), List.copyOf(failures));
    }

    public void validateUpload(String filename, long sizeBytes) {
        if (sizeBytes >= 0 && sizeBytes > maxFileSizeBytes) {
            throw new IllegalArgumentException("单个文件不能超过 " + (maxFileSizeBytes / 1024 / 1024) + " MB");
        }
    }

    public int maxFilesPerBatch() {
        return maxFilesPerBatch;
    }

    public long maxFileSizeBytes() {
        return maxFileSizeBytes;
    }

    public record UploadCandidate(String filename, InputStream inputStream, String contentType, long sizeBytes) {
    }

    private static String failureMessage(Exception exception) {
        return exception.getMessage() == null ? exception.getClass().getSimpleName() : exception.getMessage();
    }

    private static String sanitize(String filename) {
        if (filename == null || filename.isBlank()) {
            return "upload.bin";
        }
        return filename.replace("\\", "/").replace("..", "_");
    }
}
