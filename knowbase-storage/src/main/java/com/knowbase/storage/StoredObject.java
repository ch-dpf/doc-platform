package com.knowbase.storage;

public record StoredObject(
        String bucket,
        String objectKey,
        String uri,
        String contentType,
        long size
) {
}
