package com.docplatform.ingest.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI docPlatformOpenApi() {
        return new OpenAPI()
                .info(new Info()
                        .title("doc-platform API")
                        .description("文档采集入库、向量索引与 RAG 问答")
                        .version("1.0.0"));
    }
}
