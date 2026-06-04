package com.docplatform.config;

import com.docplatform.ingest.config.IngestProperties;
import com.docplatform.ingest.config.MinioProperties;
import com.docplatform.ingest.config.StorageProperties;
import com.docplatform.ingest.config.TextNormalizationProperties;
import com.docplatform.vector.config.ChunkingProperties;
import com.docplatform.vector.config.EmbeddingProperties;
import com.docplatform.vector.config.OllamaProperties;
import com.docplatform.vector.config.RagProperties;
import com.docplatform.vector.mybatis.PostgresUuidTypeHandler;
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
    TextNormalizationProperties.class,
    OllamaProperties.class,
    EmbeddingProperties.class,
    ChunkingProperties.class,
    RagProperties.class
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
