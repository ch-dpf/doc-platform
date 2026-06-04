package com.docplatform.ingest.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI ingestOpenApi() {
        return new OpenAPI()
                .info(new Info()
                        .title("doc-ingest-service API")
                        .description("文档采集与上传服务")
                        .version("1.0.0"));
    }
}
