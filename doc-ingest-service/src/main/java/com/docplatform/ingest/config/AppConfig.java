package com.docplatform.ingest.config;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;

@Configuration
@EnableConfigurationProperties({
    MinioProperties.class,
    IngestProperties.class,
    TextNormalizationProperties.class
})
@EnableAsync
public class AppConfig {
}
