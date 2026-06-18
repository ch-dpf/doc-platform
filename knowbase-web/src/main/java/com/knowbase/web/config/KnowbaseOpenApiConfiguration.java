package com.knowbase.web.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import org.springdoc.core.configuration.SpringDocConfiguration;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;

@AutoConfiguration(before = SpringDocConfiguration.class)
@ConditionalOnProperty(prefix = "knowbase.web", name = "exposed", havingValue = "true")
public class KnowbaseOpenApiConfiguration {

    @Bean
    OpenAPI knowbaseOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("KnowBase API")
                        .description("KnowBase 知识库 RAG 平台 REST 接口文档")
                        .version("v1")
                        .contact(new Contact().name("KnowBase")));
    }
}
