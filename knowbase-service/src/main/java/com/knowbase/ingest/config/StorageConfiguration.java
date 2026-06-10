package com.knowbase.ingest.config;

import com.knowbase.ingest.storage.DocumentObjectStorage;
import com.knowbase.ingest.storage.LocalFsDocumentObjectStorage;
import com.knowbase.ingest.storage.MinioDocumentObjectStorage;
import io.minio.MinioClient;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class StorageConfiguration {

    @Bean
    @ConditionalOnProperty(name = "storage.type", havingValue = "minio", matchIfMissing = true)
    DocumentObjectStorage minioDocumentObjectStorage(
            MinioClient minioClient,
            MinioProperties minioProperties,
            StorageProperties storageProperties) {
        return new MinioDocumentObjectStorage(minioClient, minioProperties, storageProperties);
    }

    @Bean
    @ConditionalOnProperty(name = "storage.type", havingValue = "local-fs")
    DocumentObjectStorage localFsDocumentObjectStorage(StorageProperties storageProperties) {
        return new LocalFsDocumentObjectStorage(storageProperties);
    }
}
