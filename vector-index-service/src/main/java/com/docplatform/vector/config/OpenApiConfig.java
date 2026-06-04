package com.docplatform.vector.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI vectorOpenApi() {
        return new OpenAPI()
                .info(new Info()
                        .title("vector-index-service API")
                        .description("向量索引与语义检索服务")
                        .version("1.0.0"));
    }
}
