package com.docplatform.ingest.storage;

import com.docplatform.ingest.config.MinioProperties;
import com.docplatform.ingest.domain.DocMetadata;
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
import org.springframework.stereotype.Service;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

@Service
public class ObjectStorageService {

    private static final Logger log = LoggerFactory.getLogger(ObjectStorageService.class);

    private final MinioClient minioClient;
    private final MinioProperties properties;

    public ObjectStorageService(MinioClient minioClient, MinioProperties properties) {
        this.minioClient = minioClient;
        this.properties = properties;
    }

    public void putObject(String objectKey, InputStream stream, long size, String contentType) {
        try {
            minioClient.putObject(PutObjectArgs.builder()
                    .bucket(properties.getBucket())
                    .object(objectKey)
                    .stream(stream, size, -1)
                    .contentType(contentType)
                    .build());
        } catch (Exception e) {
            throw new StorageException("Failed to store object: " + objectKey, e);
        }
    }

    /**
     * 删除文档在 MinIO 中的全部对象：已知 key + 按 doc 前缀批量清理各版本 raw/parsed。
     */
    public void removeDocumentArtifacts(DocMetadata doc) {
        removeObjectIfPresent(doc.getStorageKey());
        removeObjectIfPresent(doc.getParsedTextKey());
        String prefix = doc.getTenantId() + "/" + doc.getDocId() + "/";
        int removed = removeByPrefix(prefix);
        log.info(
                "Removed MinIO artifacts for doc {}: prefix={}, batchDeleted={}",
                doc.getDocId(),
                prefix,
                removed);
    }

    public void removeObjectIfPresent(String objectKey) {
        if (objectKey == null || objectKey.isBlank()) {
            return;
        }
        try {
            minioClient.removeObject(RemoveObjectArgs.builder()
                    .bucket(properties.getBucket())
                    .object(objectKey)
                    .build());
            log.debug("Removed object {}", objectKey);
        } catch (Exception e) {
            throw new StorageException("Failed to remove object: " + objectKey, e);
        }
    }

    /**
     * 删除指定前缀下的所有对象。MinIO 的 removeObjects 必须消费返回的 Iterable，否则不会真正删除。
     *
     * @return 批量删除的对象数量
     */
    public int removeByPrefix(String prefix) {
        try {
            List<DeleteObject> toRemove = new ArrayList<>();
            for (Result<Item> result : minioClient.listObjects(ListObjectsArgs.builder()
                    .bucket(properties.getBucket())
                    .prefix(prefix)
                    .recursive(true)
                    .build())) {
                toRemove.add(new DeleteObject(result.get().objectName()));
            }
            if (toRemove.isEmpty()) {
                log.debug("No objects found under prefix {}", prefix);
                return 0;
            }
            Iterable<Result<DeleteError>> errors = minioClient.removeObjects(RemoveObjectsArgs.builder()
                    .bucket(properties.getBucket())
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
            throw new StorageException("Failed to remove objects with prefix: " + prefix, e);
        }
    }

    public String presignedGetUrl(String objectKey) {
        try {
            return minioClient.getPresignedObjectUrl(GetPresignedObjectUrlArgs.builder()
                    .method(Method.GET)
                    .bucket(properties.getBucket())
                    .object(objectKey)
                    .expiry(properties.getPresignExpiryMinutes(), TimeUnit.MINUTES)
                    .build());
        } catch (Exception e) {
            throw new StorageException("Failed to presign URL for: " + objectKey, e);
        }
    }
}
