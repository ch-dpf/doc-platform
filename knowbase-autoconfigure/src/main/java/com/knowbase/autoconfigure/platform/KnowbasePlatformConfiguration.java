package com.knowbase.autoconfigure.platform;

import com.knowbase.chat.config.ChatProperties;
import com.knowbase.ingest.config.IngestProperties;
import com.knowbase.ingest.config.MinioProperties;
import com.knowbase.ingest.config.OcrProperties;
import com.knowbase.ingest.config.StorageProperties;
import com.knowbase.ingest.config.TabularPipelineProperties;
import com.knowbase.ingest.config.TextNormalizationProperties;
import com.knowbase.vector.config.ChunkingProperties;
import com.knowbase.vector.config.EmbeddingProperties;
import com.knowbase.vector.config.OllamaProperties;
import com.knowbase.vector.config.RagProperties;
import com.knowbase.vector.config.RetrievalProperties;
import com.knowbase.vector.mybatis.PostgresUuidTypeHandler;
import com.pgvector.PGvector;
import org.apache.ibatis.session.SqlSessionFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.reactive.function.client.WebClient;

import com.knowbase.autoconfigure.datasource.KnowbaseDataSourceConfiguration;
import com.knowbase.autoconfigure.mybatis.KnowbaseMybatisConfiguration;

import javax.sql.DataSource;
import java.sql.Connection;
import java.util.UUID;

@Configuration
@EnableConfigurationProperties({
    StorageProperties.class,
    MinioProperties.class,
    IngestProperties.class,
    TabularPipelineProperties.class,
    OcrProperties.class,
    TextNormalizationProperties.class,
    OllamaProperties.class,
    EmbeddingProperties.class,
    ChunkingProperties.class,
    RagProperties.class,
    RetrievalProperties.class,
    ChatProperties.class
})
public class KnowbasePlatformConfiguration {

    @Bean
    WebClient ollamaWebClient(OllamaProperties props) {
        return WebClient.builder()
                .baseUrl(props.getBaseUrl())
                .build();
    }

    @Bean
    ApplicationRunner knowbasePgvectorTypeRegistration(
            @Qualifier(KnowbaseDataSourceConfiguration.BEAN_NAME) DataSource dataSource) {
        return args -> {
            try (Connection connection = dataSource.getConnection()) {
                PGvector.addVectorType(connection);
            }
        };
    }

    @Bean
    ApplicationRunner knowbasePostgresUuidTypeHandlerRegistration(
            @Qualifier(KnowbaseMybatisConfiguration.SESSION_FACTORY_BEAN) SqlSessionFactory sqlSessionFactory) {
        return args -> sqlSessionFactory
                .getConfiguration()
                .getTypeHandlerRegistry()
                .register(UUID.class, PostgresUuidTypeHandler.class);
    }
}
