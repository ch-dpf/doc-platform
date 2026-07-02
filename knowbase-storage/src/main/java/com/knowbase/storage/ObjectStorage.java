package com.knowbase.storage;

import java.io.InputStream;

public interface ObjectStorage {

    default String storageType() {
        return getClass().getSimpleName();
    }

    StoredObject put(String bucket, String objectKey, InputStream inputStream, String contentType);

    InputStream get(String bucket, String objectKey);

    void delete(String bucket, String objectKey);
}
