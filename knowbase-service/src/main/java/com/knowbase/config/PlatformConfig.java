package com.knowbase.config;

import com.knowbase.chat.config.ChatProperties;
import com.knowbase.ingest.config.IngestProperties;
import com.knowbase.ingest.config.MinioProperties;
import com.knowbase.ingest.config.OcrProperties;
import com.knowbase.ingest.config.StorageProperties;
import com.knowbase.ingest.config.TextNormalizationProperties;
import com.knowbase.vector.config.ChunkingProperties;
import com.knowbase.vector.config.EmbeddingProperties;
import com.knowbase.vector.config.OllamaProperties;
import com.knowbase.vector.config.RagProperties;
import com.knowbase.vector.config.RetrievalProperties;
import com.knowbase.vector.mybatis.PostgresUuidTypeHandler;
import com.pgvector.PGvector;
import org.apache.ibatis.session.SqlSessionFactory;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.reactive.function.client.WebClient;

import javax.sql.DataSource;
import java.sql.Connection;
import java.util.UUID;

@Configuration
@EnableConfigurationProperties({
    StorageProperties.class,
    MinioProperties.class,
    IngestProperties.class,
    OcrProperties.class,
    TextNormalizationProperties.class,
    OllamaProperties.class,
    EmbeddingProperties.class,
    ChunkingProperties.class,
    RagProperties.class,
    RetrievalProperties.class,
    ChatProperties.class
})
public class PlatformConfig {

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

    @Bean
    ApplicationRunner postgresUuidTypeHandlerRegistration(SqlSessionFactory sqlSessionFactory) {
        return args -> sqlSessionFactory
                .getConfiguration()
                .getTypeHandlerRegistry()
                .register(UUID.class, PostgresUuidTypeHandler.class);
    }
}
