package com.knowbase.api.result;

public record ObjectUploadResult(
        String bucket,
        String objectKey,
        String uri,
        String contentType,
        long size,
        String storageType
) {
}
