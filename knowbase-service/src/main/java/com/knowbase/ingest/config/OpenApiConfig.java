package com.knowbase.ingest.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI knowbaseOpenApi() {
        return new OpenAPI()
                .info(new Info()
                        .title("知库 API")
                        .description("企业知识库：文档采集入库、向量索引与 RAG 智能问答")
                        .version("1.0.0"));
    }
}
