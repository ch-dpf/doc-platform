package com.knowbase.storage;

import io.minio.BucketExistsArgs;
import io.minio.GetObjectArgs;
import io.minio.MakeBucketArgs;
import io.minio.MinioClient;
import io.minio.PutObjectArgs;
import io.minio.RemoveObjectArgs;

import java.io.ByteArrayInputStream;
import java.io.InputStream;

public final class MinioObjectStorage implements ObjectStorage {

    private final MinioClient client;
    private final boolean autoCreateBucket;

    public MinioObjectStorage(MinioClient client, boolean autoCreateBucket) {
        this.client = client;
        this.autoCreateBucket = autoCreateBucket;
    }

    @Override
    public StoredObject put(String bucket, String objectKey, InputStream inputStream, String contentType) {
        try {
            ensureBucket(bucket);
            byte[] bytes = inputStream.readAllBytes();
            client.putObject(PutObjectArgs.builder()
                    .bucket(bucket)
                    .object(objectKey)
                    .stream(new ByteArrayInputStream(bytes), bytes.length, -1)
                    .contentType(contentType == null ? "application/octet-stream" : contentType)
                    .build());
            return new StoredObject(bucket, objectKey, "minio://" + bucket + "/" + objectKey, contentType, bytes.length);
        } catch (Exception exception) {
            throw new IllegalStateException("MinIO 上传失败: " + bucket + "/" + objectKey, exception);
        }
    }

    @Override
    public InputStream get(String bucket, String objectKey) {
        try {
            return client.getObject(GetObjectArgs.builder()
                    .bucket(bucket)
                    .object(objectKey)
                    .build());
        } catch (Exception exception) {
            throw new IllegalStateException("MinIO 读取失败: " + bucket + "/" + objectKey, exception);
        }
    }

    @Override
    public void delete(String bucket, String objectKey) {
        try {
            client.removeObject(RemoveObjectArgs.builder()
                    .bucket(bucket)
                    .object(objectKey)
                    .build());
        } catch (Exception exception) {
            throw new IllegalStateException("MinIO 删除失败: " + bucket + "/" + objectKey, exception);
        }
    }

    private void ensureBucket(String bucket) throws Exception {
        boolean exists = client.bucketExists(BucketExistsArgs.builder().bucket(bucket).build());
        if (!exists && autoCreateBucket) {
            client.makeBucket(MakeBucketArgs.builder().bucket(bucket).build());
        }
    }
}
