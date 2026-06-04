package com.docplatform.vector.config;

import com.pgvector.PGvector;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.reactive.function.client.WebClient;

import javax.sql.DataSource;
import java.sql.Connection;

@Configuration
@EnableConfigurationProperties({
    OllamaProperties.class,
    EmbeddingProperties.class,
    ChunkingProperties.class,
    MinioProperties.class,
    RagProperties.class
})
public class AppConfig {

    @Bean
    WebClient ollamaWebClient(OllamaProperties props) {
        return WebClient.builder()
                .baseUrl(props.getBaseUrl())
                .build();
    }

    @Bean
    ApplicationRunner pgvectorTypeRegistration(DataSource dataSource) {
        return args -> {
            try (Connection connection = dataSource.getConnection()) {
                PGvector.addVectorType(connection);
            }
        };
    }
}
