package com.knowbase.autoconfigure.web;

import com.knowbase.autoconfigure.KnowbaseProperties;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.tags.Tag;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.util.List;

@Configuration
@ConditionalOnProperty(prefix = "knowbase.web", name = "expose-controllers", havingValue = "true")
@ComponentScan(
        basePackages = {
            "com.knowbase.ingest.controller",
            "com.knowbase.vector.controller",
            "com.knowbase.library.controller",
            "com.knowbase.chat.controller"
        })
public class KnowbaseWebAutoConfiguration {

    @Bean
    public WebMvcConfigurer knowbaseCorsConfigurer() {
        return new WebMvcConfigurer() {
            @Override
            public void addCorsMappings(CorsRegistry registry) {
                registry.addMapping("/api/**")
                        .allowedOriginPatterns("*")
                        .allowedMethods("GET", "POST", "PUT", "DELETE", "OPTIONS")
                        .allowedHeaders("*");
            }
        };
    }

    @Bean
    @ConditionalOnProperty(prefix = "knowbase.web", name = "expose-openapi", havingValue = "true", matchIfMissing = true)
    public OpenAPI knowbaseOpenApi(KnowbaseProperties properties) {
        return new OpenAPI()
                .info(new Info()
                        .title("知库 API")
                        .description("企业知识库：文档采集入库、向量索引与 RAG 智能问答")
                        .version("1.0.0"))
                .tags(List.of(new Tag().name("知识库管理").description("知识库 CRUD 与分节配置")));
    }
}
