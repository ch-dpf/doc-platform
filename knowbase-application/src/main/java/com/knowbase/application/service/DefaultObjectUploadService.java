package com.knowbase.application.service;

import com.knowbase.api.result.ObjectUploadResult;
import com.knowbase.storage.ObjectStorage;
import com.knowbase.storage.StoredObject;

import java.io.InputStream;
import java.util.UUID;

public final class DefaultObjectUploadService {

    private final ObjectStorage objectStorage;
    private final String defaultBucket;

    public DefaultObjectUploadService(ObjectStorage objectStorage, String defaultBucket) {
        this.objectStorage = objectStorage;
        this.defaultBucket = defaultBucket;
    }

    public ObjectUploadResult upload(String bucket, String filename, InputStream inputStream, String contentType) {
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

    private static String sanitize(String filename) {
        if (filename == null || filename.isBlank()) {
            return "upload.bin";
        }
        return filename.replace("\\", "/").replace("..", "_");
    }
}
