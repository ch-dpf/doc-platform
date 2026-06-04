package com.docplatform.vector.storage;

import com.docplatform.vector.config.MinioProperties;
import io.minio.GetObjectArgs;
import io.minio.MinioClient;
import org.springframework.stereotype.Component;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;

@Component
public class ParsedTextObjectStore {

    private final MinioClient minioClient;
    private final MinioProperties properties;

    public ParsedTextObjectStore(MinioClient minioClient, MinioProperties properties) {
        this.minioClient = minioClient;
        this.properties = properties;
    }

    public String readAsString(String objectKey) {
        try (InputStream stream = minioClient.getObject(GetObjectArgs.builder()
                .bucket(properties.getBucket())
                .object(objectKey)
                .build())) {
            return new String(stream.readAllBytes(), StandardCharsets.UTF_8);
        } catch (Exception e) {
            throw new StorageException("Failed to read object: " + objectKey, e);
        }
    }
}
