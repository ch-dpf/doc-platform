package com.docplatform.ingest.storage;

import com.docplatform.ingest.config.MinioProperties;
import com.docplatform.ingest.config.StorageProperties;
import com.docplatform.ingest.domain.DocMetadata;
import io.minio.GetObjectArgs;
import io.minio.GetPresignedObjectUrlArgs;
import io.minio.ListObjectsArgs;
import io.minio.MinioClient;
import io.minio.PutObjectArgs;
import io.minio.RemoveObjectArgs;
import io.minio.RemoveObjectsArgs;
import io.minio.Result;
import io.minio.http.Method;
import io.minio.messages.DeleteError;
import io.minio.messages.DeleteObject;
import io.minio.messages.Item;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

public class MinioDocumentObjectStorage implements DocumentObjectStorage {

    private static final Logger log = LoggerFactory.getLogger(MinioDocumentObjectStorage.class);

    private final MinioClient minioClient;
    private final MinioProperties minioProperties;
    private final StorageProperties storageProperties;

    public MinioDocumentObjectStorage(
            MinioClient minioClient,
            MinioProperties minioProperties,
            StorageProperties storageProperties) {
        this.minioClient = minioClient;
        this.minioProperties = minioProperties;
        this.storageProperties = storageProperties;
    }

    @Override
    public String type() {
        return "minio";
    }

    @Override
    public void putObject(String objectKey, InputStream stream, long size, String contentType) {
        String key = storageProperties.normalizeObjectKey(objectKey);
        try {
            minioClient.putObject(PutObjectArgs.builder()
                    .bucket(minioProperties.getBucket())
                    .object(key)
                    .stream(stream, size, -1)
                    .contentType(contentType)
                    .build());
        } catch (Exception e) {
            throw new StorageException("Failed to store object: " + key, e);
        }
    }

    @Override
    public String readAsString(String objectKey) {
        String key = storageProperties.normalizeObjectKey(objectKey);
        try (InputStream stream = minioClient.getObject(GetObjectArgs.builder()
                .bucket(minioProperties.getBucket())
                .object(key)
                .build())) {
            return new String(stream.readAllBytes(), StandardCharsets.UTF_8);
        } catch (Exception e) {
            throw new StorageException("Failed to read object: " + key, e);
        }
    }

    @Override
    public void removeDocumentArtifacts(DocMetadata doc) {
        removeObjectIfPresent(doc.getStorageKey());
        removeObjectIfPresent(doc.getParsedTextKey());
        String prefix = doc.getTenantId() + "/" + doc.getDocId() + "/";
        int removed = removeByPrefix(prefix);
        log.info("Removed MinIO artifacts for doc {}: prefix={}, batchDeleted={}", doc.getDocId(), prefix, removed);
    }

    @Override
    public void removeObjectIfPresent(String objectKey) {
        if (objectKey == null || objectKey.isBlank()) {
            return;
        }
        String key = storageProperties.normalizeObjectKey(objectKey);
        try {
            minioClient.removeObject(RemoveObjectArgs.builder()
                    .bucket(minioProperties.getBucket())
                    .object(key)
                    .build());
            log.debug("Removed object {}", key);
        } catch (Exception e) {
            throw new StorageException("Failed to remove object: " + key, e);
        }
    }

    @Override
    public int removeByPrefix(String prefix) {
        String normalizedPrefix = storageProperties.normalizeObjectKey(prefix);
        try {
            List<DeleteObject> toRemove = new ArrayList<>();
            for (Result<Item> result : minioClient.listObjects(ListObjectsArgs.builder()
                    .bucket(minioProperties.getBucket())
                    .prefix(normalizedPrefix)
                    .recursive(true)
                    .build())) {
                toRemove.add(new DeleteObject(result.get().objectName()));
            }
            if (toRemove.isEmpty()) {
                return 0;
            }
            Iterable<Result<DeleteError>> errors = minioClient.removeObjects(RemoveObjectsArgs.builder()
                    .bucket(minioProperties.getBucket())
                    .objects(toRemove)
                    .build());
            int removed = 0;
            for (Result<DeleteError> errorResult : errors) {
                DeleteError error = errorResult.get();
                if (error != null) {
                    throw new StorageException(
                            "Failed to remove " + error.objectName() + ": " + error.message());
                }
                removed++;
            }
            return removed;
        } catch (StorageException e) {
            throw e;
        } catch (Exception e) {
            throw new StorageException("Failed to remove objects with prefix: " + normalizedPrefix, e);
        }
    }

    @Override
    public String resolveAccessUrl(String objectKey) {
        String key = storageProperties.normalizeObjectKey(objectKey);
        try {
            return minioClient.getPresignedObjectUrl(GetPresignedObjectUrlArgs.builder()
                    .method(Method.GET)
                    .bucket(minioProperties.getBucket())
                    .object(key)
                    .expiry(minioProperties.getPresignExpiryMinutes(), TimeUnit.MINUTES)
                    .build());
        } catch (Exception e) {
            throw new StorageException("Failed to presign URL for: " + key, e);
        }
    }
}
